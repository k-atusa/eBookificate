import os
from PIL import Image
import numpy as np
import cv2

# ========== helpers ==========

SUPPORTED_EXT = (".jpg", ".jpeg", ".png", ".webp")
BG_RATIO = 0.05
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

# Detect document via HSV saturation, GrabCut, or Canny fallback
def detectQuad(img, colorbg=True):
    h, w = img.shape[:2]
    scale = min(1024 / max(h, w), 1.0)
    sm = cv2.resize(img, (int(w * scale), int(h * scale)))
    sh, sw = sm.shape[:2]

    # method 1: border color sampling + border-connected region filtering
    if colorbg:
        b = max(1, int(min(sh, sw) * BG_RATIO)) # border strip width
        border = np.concatenate( [ sm[:b, :].reshape(-1, 3), sm[-b:, :].reshape(-1, 3), sm[:, :b].reshape(-1, 3), sm[:, -b:].reshape(-1, 3) ] )
        bgColor = np.median(border, axis=0).astype(np.uint8)
        bgLab = cv2.cvtColor(bgColor.reshape(1, 1, 3), cv2.COLOR_BGR2Lab).reshape(3).astype(np.float32)
        smLab = cv2.cvtColor(sm, cv2.COLOR_BGR2Lab).astype(np.float32)
        dist = np.linalg.norm(smLab - bgLab, axis=2)
        bgCand = (dist <= BG_THRES).astype(np.uint8) # bg candidate

        # keep only bg components that touch the image border
        _, labeled = cv2.connectedComponents(bgCand, connectivity=8)
        touching = ( set(labeled[0, :]) | set(labeled[-1, :]) | set(labeled[:, 0]) | set(labeled[:, -1]) ) - {0}
        if touching: # skip if no border-touching background found
            connBg = np.isin(labeled, list(touching)).astype(np.uint8) * 255
            mask = (255 - connBg).astype(np.uint8) # document = white, border-bg = black
            k = cv2.getStructuringElement(cv2.MORPH_RECT, (7, 7))
            mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, k, iterations=3)
            mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN,  k, iterations=2)
            q = findQuad(mask, sw, sh)
            if q is not None:
                return q / scale

    # method 2: GrabCut treats image border as background
    roi = (sw // 20, sh // 20, sw - sw // 10, sh - sh // 10)
    gc = np.zeros((sh, sw), np.uint8)
    try:
        cv2.grabCut(sm, gc, roi, np.zeros((1, 65), np.float64), np.zeros((1, 65), np.float64), 5, cv2.GC_INIT_WITH_RECT)
        mask2 = np.where((gc == 2) | (gc == 0), 0, 255).astype("uint8")
        k = cv2.getStructuringElement(cv2.MORPH_RECT, (9, 9))
        mask2 = cv2.morphologyEx(mask2, cv2.MORPH_CLOSE, k, iterations=3)
        q = findQuad(mask2, sw, sh)
        if q is not None:
            return q / scale
    except Exception:
        pass

    # method 3: Canny edge detection as last resort
    gray = cv2.GaussianBlur(cv2.cvtColor(sm, cv2.COLOR_BGR2GRAY), (5, 5), 0)
    edges = cv2.Canny(gray, 30, 100)
    edges = cv2.dilate(edges, cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5)), iterations=2)
    q = findQuad(edges, sw, sh)
    if q is not None:
        return q / scale

    return None

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
def resize(path, size=100, quality=80, format="webp"):
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

# Merge images into PDF with JPEG
def mkpdf(path, quality=90, outName="output.pdf"):
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

# Detect and extract document
def crop(path, colorbg=True, rect=True, lossless=False):
    files = getImages(path) if os.path.isdir(path) else [path]
    outDir = mkOutDir(path)

    for f in files:
        img = cv2.imread(f)
        if img is None:
            print(f"[crop] load failed: {f}")
            continue
        h, w = img.shape[:2]
        quad = detectQuad(img, colorbg=colorbg)

        if quad is None:
            print(f"[crop] detection failed: {f} (saving original)")
            out = img
        elif rect: # perspective warp to a flat rectangle
            out = warpQuad(img, quad) or img
        else: # simple bounding box crop
            x, y, bw, bh = cv2.boundingRect(quad.astype(int))
            cropped = img[max(0, y):min(h, y+bh), max(0, x):min(w, x+bw)]
            out = cropped if cropped.size > 0 else img  # guard empty crop

        if lossless: # cv2 webp is lossy by default
            outPath = os.path.join(outDir, os.path.splitext(os.path.basename(f))[0] + ".webp")
            Image.fromarray(cv2.cvtColor(out, cv2.COLOR_BGR2RGB)).save(outPath, format="WEBP", lossless=True)
        else:
            outPath = os.path.join(outDir, os.path.basename(f))
            cv2.imwrite(outPath, out)
        print(f"[crop] {f}")

    print(f"[crop] done ({len(files)} files)")