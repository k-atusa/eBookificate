# eBookificate v1.0.0

> Framework for easily creating eBooks/PDFs from compact, high-compression image scans.

## Android Usage

- Shoot document pages in order and save them all to one folder.
- Place the document on a plain, solid-color background (monochromatic desk or wall) to improve auto-detection accuracy.
- Ensure uniform, shadow-free lighting to maximize compression quality.
- The app strips all image metadata (EXIF) on save for privacy and reduced file size.
- Use simple conversion mode to batch-export shots as clean JPEG/WebP without any post-processing.

## PostProcessing Usage

- `crop()`: Auto-detects document edges and applies perspective warp to produce a flat, rectangular crop.
- `resize()`: Percentage-based downscale with re-encoding to high-compression WebP, JPEG, or PNG.
- `mkpdf()`: Merges all processed images into a single optimized PDF file.

## Build Executable

```bash
gradlew.bat clean
gradlew.bat [assembleRelease|assembleDebug]
cd android/app/build/outputs/apk/debug
```