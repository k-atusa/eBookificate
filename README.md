# eBookificate v1.0.0

> Framework for easily creating eBooks/PDFs from compact, high-compression image scans.

## Android Usage

- Shoot document pages in order with the camera and store them into one of 5 named storage slots.
- Place the document on a plain, monochromatic background and ensure uniform, shadow-free lighting to improve image quality.
- Convert captured images on-device to JPEG / PNG / WebP (lossy, lossless, or half-resolution), all conversions strip EXIF metadata automatically.
- Export a storage slot as a ZIP archive directly to Downloads for transfer to PC.
- Use the external file converter to batch-convert arbitrary images from device storage with the same format options.

## PostProcessing Usage

- `crop()`: Auto-detects document edges and applies perspective warp to produce a flat, rectangular crop.
- `resize()`, `rotate()`: Percentage-based downscale and rotation with re-encoding to high-compression WebP, JPEG, or PNG.
- `mkpdf()`: Merges all processed images into a single optimized PDF file.

## Build Executable

```bash
gradlew.bat clean
gradlew.bat [assembleRelease|assembleDebug]
cd android/app/build/outputs/apk/debug
```