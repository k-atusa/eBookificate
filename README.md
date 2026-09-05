# eBookificate v1.0.1

> Framework for easily creating eBooks/PDFs from compact, high-compression image scans.

## Android Usage

- Shoot document pages in order with the camera and store them into one of 5 named storage slots.
- Place the document on a plain, monochromatic background and ensure uniform, shadow-free lighting to improve image quality.
- Convert captured images on-device to JPEG / PNG / WebP (lossy, lossless, or half-resolution), all conversions strip EXIF metadata automatically.
- Export a storage slot as a ZIP archive directly to Downloads for transfer to PC.
- Use the external file converter to batch-convert arbitrary images from device storage with the same format options.

## PostProcessing Usage

- `cropColor()`: Detects document edges via luminance-equalized Lab color difference; saves intermediate detection mask as `mask_*`.
- `cropBorder()`: Detects document edges using GrabCut or Canny fallback (background-color-agnostic); saves intermediate detection mask as `mask_*`.
- `cropSize()`: Crops a fixed pixel region of given width-height centered on the image, with no edge detection.
- `rotate()`: Rotates all images by a given angle (counter-clockwise) and saves them.
- `resize()`: Scales images by a percentage and re-encodes them at a specified quality.
- `sharpen()`: Adjusts contrast and sharpness of images (positive values strengthen, negative values soften).
- `mkpdf()`: Merges all processed images into a single optimized PDF file.

## Build Executable

```bash
gradlew.bat clean
gradlew.bat [assembleRelease|assembleDebug]
cd android/app/build/outputs/apk/debug
```
