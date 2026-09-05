import os
from PIL import Image, ImageEnhance
import numpy as np
import cv2

# ========== helpers ==========
SUPPORTED_EXT = (".jpg", ".jpeg", ".png", ".webp")
BG_RATIO = 0.1
BG_THRES = 30

# Return sorted list of image from folder
def getImages(folder):
    files = []
    for e in os.scandir(folder):
        if e.is_file() and os.path.splitext(e.name)[1].lower() in SUPPORTED_EXT:
            files.append(e.path)
    return sorted(files)

# Create result/ subdir and return its path
def mkOutDir(path):
    base = path if os.path.isdir(path) else os.path.dirname(path)
    out = os.path.join(base, "result")
    os.makedirs(out, exist_ok=True)
    return out

# Sort 4 points [TL, TR, BR, BL]
def sortCorners(pts):
    out = np.zeros((4, 2), dtype="float32")
    s = pts.sum(axis=1)
    out[0] = pts[np.argmin(s)]  # TL: smallest x+y
    out[2] = pts[np.argmax(s)]  # BR: largest x+y
    d = np.diff(pts, axis=1)
    out[1] = pts[np.argmin(d)]  # TR: smallest x-y
    out[3] = pts[np.argmax(d)]  # BL: largest x-y
    return out

# Warp image to rectangle using 4 corner
def warpQuad(img, pts):
    corners = sortCorners(pts)
    tl, tr, br, bl = corners
    w = max(int(np.linalg.norm(br - bl)), int(np.linalg.norm(tr - tl)))
    h = max(int(np.linalg.norm(tr - br)), int(np.linalg.norm(tl - bl)))
    if w < 2 or h < 2:
        return None # degenerate quad
    dst = np.array([[0, 0], [w-1, 0], [w-1, h-1], [0, h-1]], dtype="float32")
    try:
        M = cv2.getPerspectiveTransform(corners, dst)
        return cv2.warpPerspective(img, M, (w, h))
    except cv2.error:
        return None # collinear or singular points

# Find largest quad contour in binary mask
def findQuad(mask, w, h):
    cnts, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not cnts:
        return None
    # check top 3 contours by area
    for cnt in sorted(cnts, key=cv2.contourArea, reverse=True)[:3]:
        area = cv2.contourArea(cnt)
        if area < w * h * 0.05 or area > w * h * 0.98: # reject too small or full-image
            continue
        peri = cv2.arcLength(cnt, True)
        # try increasing epsilon until 4-sided polygon found
        for eps in [0.02, 0.03, 0.04, 0.05, 0.06]:
            approx = cv2.approxPolyDP(cnt, eps * peri, True)
            if len(approx) == 4:
                return approx.reshape(4, 2).astype("float32")
        # fallback: use minimum area bounding rectangle
        return cv2.boxPoints(cv2.minAreaRect(cnt)).astype("float32")
    return None

# Detect document quad from pre-built mask and scale
def detectQuadFromMask(mask, scale, imgW, imgH):
    sh, sw = mask.shape[:2]
    q = findQuad(mask, sw, sh)
    if q is not None:
        return q / scale
    return None

# Build document mask via border color sampling in Lab space (equalized luminance)
def buildMaskColor(img):
    h, w = img.shape[:2]
    scale = min(1024 / max(h, w), 1.0)
    sm = cv2.resize(img, (int(w * scale), int(h * scale)))
    sh, sw = sm.shape[:2]

    # equalize luminance channel before color comparison
    smLab = cv2.cvtColor(sm, cv2.COLOR_BGR2Lab)
    l, a, b = cv2.split(smLab)
    l = cv2.equalizeHist(l)
    smLabEq = cv2.merge([l, a, b]).astype(np.float32)

    # sample border strip and compute median color in equalized Lab
    bw = max(1, int(min(sh, sw) * BG_RATIO))
    border = np.concatenate([
        smLabEq[:bw,  :].reshape(-1, 3),
        smLabEq[-bw:, :].reshape(-1, 3),
        smLabEq[:,  :bw].reshape(-1, 3),
        smLabEq[:, -bw:].reshape(-1, 3),
    ])
    bgLab = np.median(border, axis=0)

    # pixels within threshold distance are background
    dist = np.linalg.norm(smLabEq - bgLab, axis=2)
    bgCand = (dist <= BG_THRES).astype(np.uint8)

    # keep only border-connected background regions
    _, labeled = cv2.connectedComponents(bgCand, connectivity=8)
    touching = (
        set(labeled[0, :]) | set(labeled[-1, :]) |
        set(labeled[:, 0]) | set(labeled[:, -1])
    ) - {0}
    if not touching:
        return None, None
    connBg = np.isin(labeled, list(touching)).astype(np.uint8) * 255
    mask = (255 - connBg).astype(np.uint8) # document = white, border-bg = black
    k = cv2.getStructuringElement(cv2.MORPH_RECT, (7, 7))
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, k, iterations=3)
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN,  k, iterations=2)
    return mask, scale

# Build document mask via GrabCut or Canny fallback (border-agnostic)
def buildMaskBorder(img):
    h, w = img.shape[:2]
    scale = min(1024 / max(h, w), 1.0)
    sm = cv2.resize(img, (int(w * scale), int(h * scale)))
    sh, sw = sm.shape[:2]

    # method 1: GrabCut treats image border as background
    roi = (sw // 20, sh // 20, sw - sw // 10, sh - sh // 10)
    gc = np.zeros((sh, sw), np.uint8)
    try:
        cv2.grabCut(sm, gc, roi, np.zeros((1, 65), np.float64), np.zeros((1, 65), np.float64), 5, cv2.GC_INIT_WITH_RECT)
        mask = np.where((gc == 2) | (gc == 0), 0, 255).astype("uint8")
        k = cv2.getStructuringElement(cv2.MORPH_RECT, (9, 9))
        mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, k, iterations=3)
        q = findQuad(mask, sw, sh)
        if q is not None:
            return mask, scale
    except Exception:
        pass

    # method 2: Canny edge detection as last resort
    gray = cv2.GaussianBlur(cv2.cvtColor(sm, cv2.COLOR_BGR2GRAY), (5, 5), 0)
    edges = cv2.Canny(gray, 30, 100)
    edges = cv2.dilate(edges, cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5)), iterations=2)
    return edges, scale

# Save mask image alongside result
def saveMask(mask, outDir, basename):
    maskPath = os.path.join(outDir, "mask_" + os.path.splitext(basename)[0] + ".png")
    cv2.imwrite(maskPath, mask)

# Save cv2 image to outDir with quality-aware encoding
def saveImg(img, outDir, basename, lossless=False):
    if lossless: # cv2 webp is lossy by default
        outPath = os.path.join(outDir, os.path.splitext(basename)[0] + ".webp")
        Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB)).save(outPath, format="WEBP", lossless=True)
    else:
        outPath = os.path.join(outDir, basename)
        ext = os.path.splitext(basename)[1].lower()
        if ext in (".jpg", ".jpeg"):
            cv2.imwrite(outPath, img, [cv2.IMWRITE_JPEG_QUALITY, 95])
        elif ext == ".webp":
            cv2.imwrite(outPath, img, [cv2.IMWRITE_WEBP_QUALITY, 90])
        else:
            cv2.imwrite(outPath, img)

# ========== functions ==========

# Rotate image by angle counter-clockwise
def rotate(path, angle=90, lossless=False):
    files = getImages(path) if os.path.isdir(path) else [path]
    outDir = mkOutDir(path)

    for f in files:
        img = Image.open(f)
        # expand canvas to prevent clipping
        rotated = img.rotate(angle, expand=True)
        if lossless:
            outPath = os.path.join(outDir, os.path.splitext(os.path.basename(f))[0] + ".webp")
            rotated.save(outPath, format="WEBP", lossless=True)
        else:
            outPath = os.path.join(outDir, os.path.basename(f))
            rotated.save(outPath)
        print(f"[rotate] {f}")

    print(f"[rotate] done ({len(files)} files)")

# Resize image by percentage and save in specified format (webp, jpg, png)
def resize(path, size=100, quality=90, format="webp"):
    fmt = format.lower().strip(".")
    if fmt in ("jpg", "jpeg"):
        fmt = "jpeg"
    elif fmt not in ("webp", "png"):
        fmt = "webp"
    ext = ".jpg" if fmt == "jpeg" else f".{fmt}"
    files = getImages(path) if os.path.isdir(path) else [path]
    outDir = mkOutDir(path)

    for f in files:
        img = Image.open(f)
        if size != 100:
            w = max(1, int(img.width  * size / 100))
            h = max(1, int(img.height * size / 100))
            img = img.resize((w, h), Image.LANCZOS)
        outPath = os.path.join(outDir, os.path.splitext(os.path.basename(f))[0] + ext)
        if fmt == "webp":
            img.save(outPath, format="WEBP", quality=quality, method=6)
        elif fmt == "jpeg":
            img.convert("RGB").save(outPath, format="JPEG", quality=quality, optimize=True)
        else:
            img.save(outPath, format="PNG")
        print(f"[resize] {f}")

    print(f"[resize] done ({len(files)} files)")

# Adjust contrast and sharpness of images (positive values strengthen, negative soften)
def sharpen(path, contrast=0.05, sharp=0.05, lossless=False):
    files = getImages(path) if os.path.isdir(path) else [path]
    outDir = mkOutDir(path)

    for f in files:
        img = Image.open(f).convert("RGB")
        img = ImageEnhance.Contrast(img).enhance(1.0 + contrast)
        img = ImageEnhance.Sharpness(img).enhance(1.0 + sharp)
        basename = os.path.basename(f)
        if lossless:
            outPath = os.path.join(outDir, os.path.splitext(basename)[0] + ".webp")
            img.save(outPath, format="WEBP", lossless=True)
        else:
            ext = os.path.splitext(basename)[1].lower()
            outPath = os.path.join(outDir, basename)
            if ext in (".jpg", ".jpeg"):
                img.save(outPath, format="JPEG", quality=95, optimize=True)
            elif ext == ".webp":
                img.save(outPath, format="WEBP", quality=90, method=6)
            else:
                img.save(outPath, format="PNG")
        print(f"[sharpen] {f}")

    print(f"[sharpen] done ({len(files)} files)")

# Merge images into PDF with JPEG
def mkpdf(path, quality=95, outName="output.pdf"):
    files = getImages(path) if os.path.isdir(path) else [path]
    if not files:
        print("[mkpdf] no images found")
        return

    outDir = mkOutDir(path)
    outPath = os.path.join(outDir, outName)
    imgs = [ ]
    for f in files:
        imgs.append(Image.open(f).convert("RGB"))
        print(f"[mkpdf] load: {f}")

    # save all pages
    imgs[0].save(outPath, format="PDF", save_all=True, append_images=imgs[1:], quality=quality, optimize=True)
    print(f"[mkpdf] done ({len(files)} pages)")

# Detect and extract document using luminance-equalized color difference
def cropColor(path, rect=True, lossless=False):
    files = getImages(path) if os.path.isdir(path) else [path]
    outDir = mkOutDir(path)

    for f in files:
        img = cv2.imread(f)
        if img is None:
            print(f"[cropColor] load failed: {f}")
            continue
        h, w = img.shape[:2]
        basename = os.path.basename(f)

        mask, scale = buildMaskColor(img)
        if mask is None:
            print(f"[cropColor] detection failed: {f} (saving original)")
            out = img
        else:
            saveMask(mask, outDir, basename)
            quad = detectQuadFromMask(mask, scale, w, h)
            if quad is None:
                print(f"[cropColor] quad not found: {f} (saving original)")
                out = img
            elif rect: # perspective warp to a flat rectangle
                out = warpQuad(img, quad)
                if out is None:
                    out = img
            else: # simple bounding box crop
                x, y, bw, bh = cv2.boundingRect(quad.astype(int))
                cropped = img[max(0, y):min(h, y+bh), max(0, x):min(w, x+bw)]
                out = cropped if cropped.size > 0 else img # guard empty crop

        saveImg(out, outDir, basename, lossless=lossless)
        print(f"[cropColor] {f}")

    print(f"[cropColor] done ({len(files)} files)")

# Detect and extract document using GrabCut / Canny edge (border-agnostic)
def cropBorder(path, rect=True, lossless=False):
    files = getImages(path) if os.path.isdir(path) else [path]
    outDir = mkOutDir(path)

    for f in files:
        img = cv2.imread(f)
        if img is None:
            print(f"[cropBorder] load failed: {f}")
            continue
        h, w = img.shape[:2]
        basename = os.path.basename(f)

        mask, scale = buildMaskBorder(img)
        saveMask(mask, outDir, basename)
        quad = detectQuadFromMask(mask, scale, w, h)

        if quad is None:
            print(f"[cropBorder] detection failed: {f} (saving original)")
            out = img
        elif rect: # perspective warp to a flat rectangle
            out = warpQuad(img, quad)
            if out is None:
                out = img
        else: # simple bounding box crop
            x, y, bw, bh = cv2.boundingRect(quad.astype(int))
            cropped = img[max(0, y):min(h, y+bh), max(0, x):min(w, x+bw)]
            out = cropped if cropped.size > 0 else img # guard empty crop

        saveImg(out, outDir, basename, lossless=lossless)
        print(f"[cropBorder] {f}")

    print(f"[cropBorder] done ({len(files)} files)")

# Crop a fixed pixel region centered on the image
def cropSize(path, cropW=1600, cropH=1200, lossless=False):
    files = getImages(path) if os.path.isdir(path) else [path]
    outDir = mkOutDir(path)

    for f in files:
        img = cv2.imread(f)
        if img is None:
            print(f"[cropSize] load failed: {f}")
            continue
        h, w = img.shape[:2]
        basename = os.path.basename(f)

        # clamp crop dimensions to actual image size
        cw = min(cropW, w)
        ch = min(cropH, h)

        # compute centered crop coordinates
        x = (w - cw) // 2
        y = (h - ch) // 2
        out = img[y:y+ch, x:x+cw]

        saveImg(out, outDir, basename, lossless=lossless)
        print(f"[cropSize] {f}")

    print(f"[cropSize] done ({len(files)} files)")
