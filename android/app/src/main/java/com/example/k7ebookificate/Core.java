package com.example.k7ebookificate;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Core utility library for eBookificate.
 * Provides image conversion, storage management, settings, ZIP export.
 */
public class Core {
    private static final String SETTINGS_FILE = "settings.txt";
    public static final String[] SLOT_PREFIXES = {"A", "B", "C", "D", "E"};
    private static final String[] DEFAULT_NAMES = {"Scan 1", "Scan 2", "Scan 3", "Scan 4", "Scan 5"};
    public static final String OUTPUT_FOLDER = "eBookificate";
    public static final int PAGE_SIZE = 30;

    // ========== Storage Folder ==========

    /** Get or create the storage directory for a slot (0~4) → A~E folder in filesDir */
    public static IO1.VFile getStorageDir(Context context, int slot) {
        File dir = new File(context.getFilesDir(), SLOT_PREFIXES[slot]);
        if (!dir.exists()) dir.mkdir();
        return IO1.GetLocal(context, SLOT_PREFIXES[slot]);
    }

    public static String getSlotPrefix(int slot) {
        return SLOT_PREFIXES[slot];
    }

    // ========== Settings (settings.txt) ==========

    /** Load all 5 storage names from settings.txt */
    public static String[] loadStorageNames(Context context) {
        String[] names = new String[5];
        System.arraycopy(DEFAULT_NAMES, 0, names, 0, 5);

        File settingsFile = new File(context.getFilesDir(), SETTINGS_FILE);
        if (!settingsFile.exists()) return names;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new java.io.FileInputStream(settingsFile)))) {
            for (int i = 0; i < 5; i++) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    names[i] = line.trim();
                }
            }
        } catch (IOException e) { /* return defaults */ }
        return names;
    }

    /** Save a single storage name by index */
    public static void saveStorageName(Context context, int index, String name) {
        String[] names = loadStorageNames(context);
        names[index] = name;

        File settingsFile = new File(context.getFilesDir(), SETTINGS_FILE);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new java.io.FileOutputStream(settingsFile)))) {
            for (int i = 0; i < 5; i++) {
                writer.write(names[i]);
                writer.newLine();
            }
        } catch (IOException e) { /* ignore */ }
    }

    // ========== Next Number Calculation ==========

    /**
     * Find the next available number for a storage slot.
     * Scans files like A00003.jpg, returns max+1 (e.g., 4).
     * If empty, returns 0.
     */
    public static int getNextNumber(Context context, IO1.VFile storageDir) {
        List<IO1.VFile> files = storageDir.ListDir(context);
        int maxNum = -1;
        for (IO1.VFile f : files) {
            String name = f.GetName(context);
            if (name == null || name.length() < 2) continue;
            int dotPos = name.lastIndexOf('.');
            if (dotPos <= 1) continue;
            try {
                int num = Integer.parseInt(name.substring(1, dotPos));
                if (num > maxNum) maxNum = num;
            } catch (NumberFormatException e) { /* skip */ }
        }
        return maxNum + 1;
    }

    /** Format a storage filename: prefix + 5-digit number + extension */
    public static String formatFileName(String prefix, int number, String extension) {
        return String.format("%s%05d.%s", prefix, number, extension);
    }

    // ========== Image Conversion ==========

    /**
     * Convert an image, writing result to the given OutputStream.
     * Strips all metadata by decoding and re-encoding.
     * @return the output filename (with correct extension), or null on error
     */
    public static String convertImage(Context context, IO1.VFile source, String mode, OutputStream dest) {
        try {
            // Decode bitmap from source
            Bitmap bitmap;
            try (InputStream is = source.OpenReader(context)) {
                bitmap = BitmapFactory.decodeStream(is);
            }
            if (bitmap == null) return null;

            // Determine output format
            Bitmap.CompressFormat format;
            String extension;
            int quality;

            switch (mode) {
                case "jpg":
                    format = Bitmap.CompressFormat.JPEG;
                    extension = "jpg";
                    quality = 90;
                    break;
                case "png":
                    format = Bitmap.CompressFormat.PNG;
                    extension = "png";
                    quality = 100;
                    break;
                case "webp":
                    format = Bitmap.CompressFormat.WEBP_LOSSY;
                    extension = "webp";
                    quality = 80;
                    break;
                case "webp_lossless":
                    format = Bitmap.CompressFormat.WEBP_LOSSLESS;
                    extension = "webp";
                    quality = 100;
                    break;
                case "webp_half":
                    format = Bitmap.CompressFormat.WEBP_LOSSY;
                    extension = "webp";
                    quality = 80;
                    // Scale to half resolution
                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                            Math.max(1, bitmap.getWidth() / 2),
                            Math.max(1, bitmap.getHeight() / 2), true);
                    bitmap.recycle();
                    bitmap = scaled;
                    break;
                case "none":
                default:
                    // Re-encode in original format (strips metadata)
                    String srcName = source.GetName(context).toLowerCase();
                    if (srcName.endsWith(".png")) {
                        format = Bitmap.CompressFormat.PNG;
                        extension = "png";
                        quality = 100;
                    } else if (srcName.endsWith(".webp")) {
                        format = Bitmap.CompressFormat.WEBP_LOSSY;
                        extension = "webp";
                        quality = 80;
                    } else {
                        format = Bitmap.CompressFormat.JPEG;
                        extension = "jpg";
                        quality = 90;
                    }
                    break;
            }

            // Compress to output
            bitmap.compress(format, quality, dest);
            bitmap.recycle();

            // Build output filename
            String srcName = source.GetName(context);
            int dotPos = srcName.lastIndexOf('.');
            String baseName = (dotPos > 0) ? srcName.substring(0, dotPos) : srcName;
            return baseName + "." + extension;

        } catch (IOException e) {
            return null;
        }
    }

    /** Get MIME type from extension */
    public static String getMimeType(String extension) {
        switch (extension.toLowerCase()) {
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "webp": return "image/webp";
            default: return "application/octet-stream";
        }
    }

    /** Get extension from mode string */
    public static String getExtensionForMode(String mode) {
        switch (mode) {
            case "jpg": return "jpg";
            case "png": return "png";
            case "webp": case "webp_lossless": case "webp_half": return "webp";
            default: return null; // none: keep original
        }
    }

    // ========== Downloads/eBookificate output ==========

    /** Create a file in Downloads/eBookificate/ via MediaStore */
    public static IO1.VFile createOutputFile(Context context, String fileName) {
        ContentValues values = new ContentValues();

        // Determine MIME type
        String mimeType = "application/octet-stream";
        int dotPos = fileName.lastIndexOf('.');
        if (dotPos > 0) {
            String ext = fileName.substring(dotPos + 1).toLowerCase();
            mimeType = getMimeType(ext);
        }

        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        values.put(MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/" + OUTPUT_FOLDER);

        Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri newUri = context.getContentResolver().insert(collection, values);
        return newUri != null ? new IO1.VFile(newUri, false) : null;
    }

    // ========== ZIP Export ==========

    /**
     * Export all files in a storage directory to a ZIP file in Downloads/eBookificate/.
     * @param progressCallback called with (current, total) for each file processed
     * @return true on success
     */
    public static boolean exportAsZip(Context context, int slot,
                                       ProgressCallback progressCallback) {
        IO1.VFile storageDir = getStorageDir(context, slot);
        List<IO1.VFile> files = storageDir.ListDir(context);
        if (files.isEmpty()) return false;

        // Sort by name
        List<String> names = new ArrayList<>();
        for (IO1.VFile f : files) names.add(f.GetName(context));
        Collections.sort(names);

        String zipName = SLOT_PREFIXES[slot] + ".zip";
        IO1.VFile zipFile = createOutputFile(context, zipName);
        if (zipFile == null) return false;

        try (OutputStream os = zipFile.OpenWriter(context, false);
             ZipOutputStream zos = new ZipOutputStream(os)) {

            byte[] buffer = new byte[8192];
            int current = 0;

            for (IO1.VFile f : files) {
                String name = f.GetName(context);
                zos.putNextEntry(new ZipEntry(name));

                try (InputStream is = f.OpenReader(context)) {
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();

                current++;
                if (progressCallback != null) {
                    progressCallback.onProgress(current, files.size());
                }
            }
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    // ========== Batch Convert (for Service) ==========

    /**
     * Batch convert images to Downloads/eBookificate/.
     * Used by ConvertActivity → ConvertService.
     */
    public static int batchConvertToDownloads(Context context, List<IO1.VFile> sources, String mode,
                                               ProgressCallback progressCallback) {
        int success = 0;
        for (int i = 0; i < sources.size(); i++) {
            IO1.VFile src = sources.get(i);
            String outName = getConvertedFileName(context, src, mode);
            IO1.VFile outFile = createOutputFile(context, outName);
            if (outFile != null) {
                try (OutputStream os = outFile.OpenWriter(context, false)) {
                    String result = convertImage(context, src, mode, os);
                    if (result != null) success++;
                } catch (IOException e) { /* skip */ }
            }
            if (progressCallback != null) {
                progressCallback.onProgress(i + 1, sources.size());
            }
        }
        return success;
    }

    /**
     * Convert all images in a storage slot, replacing files in-place.
     * Used by StorageActivity → ConvertService.
     */
    public static int convertStorage(Context context, int slot, String mode,
                                      ProgressCallback progressCallback) {
        IO1.VFile storageDir = getStorageDir(context, slot);
        List<IO1.VFile> files = storageDir.ListDir(context);
        if (files.isEmpty()) return 0;

        String prefix = SLOT_PREFIXES[slot];
        String newExt = getExtensionForMode(mode);
        int success = 0;

        for (int i = 0; i < files.size(); i++) {
            IO1.VFile src = files.get(i);
            String srcName = src.GetName(context);

            // Determine output extension
            String ext = newExt;
            if (ext == null) {
                // none mode: keep original extension
                int dotPos = srcName.lastIndexOf('.');
                ext = (dotPos > 0) ? srcName.substring(dotPos + 1) : "jpg";
            }

            // Create temp output file
            String tempName = "_tmp_" + i + "." + ext;
            try {
                IO1.VFile tmpFile = storageDir.CreateFile(context, getMimeType(ext), tempName);
                if (tmpFile == null) continue;

                try (OutputStream os = tmpFile.OpenWriter(context, false)) {
                    String result = convertImage(context, src, mode, os);
                    if (result != null) {
                        // Delete original, rename temp to proper name
                        String outName = srcName;
                        int dotPos = srcName.lastIndexOf('.');
                        if (dotPos > 0 && newExt != null) {
                            outName = srcName.substring(0, dotPos) + "." + newExt;
                        }
                        src.Delete(context);
                        tmpFile.Rename(context, outName);
                        success++;
                    } else {
                        tmpFile.Delete(context);
                    }
                }
            } catch (IOException e) { /* skip */ }

            if (progressCallback != null) {
                progressCallback.onProgress(i + 1, files.size());
            }
        }
        return success;
    }

    /** Get the output filename for a converted file */
    private static String getConvertedFileName(Context context, IO1.VFile source, String mode) {
        String srcName = source.GetName(context);
        String newExt = getExtensionForMode(mode);
        if (newExt == null) return srcName; // none mode
        int dotPos = srcName.lastIndexOf('.');
        String baseName = (dotPos > 0) ? srcName.substring(0, dotPos) : srcName;
        return baseName + "." + newExt;
    }

    /** Reset a storage slot: delete all files */
    public static void resetStorage(Context context, int slot) {
        IO1.VFile storageDir = getStorageDir(context, slot);
        List<IO1.VFile> files = storageDir.ListDir(context);
        for (IO1.VFile f : files) {
            f.Delete(context);
        }
    }

    // ========== Callback Interface ==========

    public interface ProgressCallback {
        void onProgress(int current, int total);
    }
}
