package com.example.k7ebookificate;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// Core utility: image conversion, storage, settings, ZIP export.
public class Core {
    private static final String TAG = "Core";
    private static final String CONF_FILE = "settings.txt";
    public static final String[] SLOT_TAG = { "A", "B", "C", "D", "E" };
    private static final String[] INIT_NAME = { "Scan 1", "Scan 2", "Scan 3", "Scan 4", "Scan 5" };
    public static final String OUT_DIR = "eBookificate";
    public static final int PAGE_SIZE = 30;

    // Show error via Toast.
    private static void showErr(Context ctx, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
    }

    // Get or create storage directory for slot 0~4.
    public static IO1.VFile getStoreDir(Context ctx, int slot) {
        File dir = new File(ctx.getFilesDir(), SLOT_TAG[slot]);
        if (!dir.exists())
            dir.mkdir();
        return IO1.GetLocal(ctx, SLOT_TAG[slot]);
    }

    public static String getSlotTag(int slot) {
        return SLOT_TAG[slot];
    }

    // Load 5 storage names from settings.txt.
    public static String[] loadNames(Context ctx) {
        String[] names = new String[5];
        System.arraycopy(INIT_NAME, 0, names, 0, 5);
        File conf = new File(ctx.getFilesDir(), CONF_FILE);
        if (!conf.exists())
            return names;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(conf)))) {
            for (int i = 0; i < 5; i++) {
                String line = br.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    names[i] = line.trim();
                }
            }
        } catch (IOException e) {
            showErr(ctx, "ERR load: " + e.getMessage());
        }
        return names;
    }

    // Save a single storage name by index.
    public static void saveName(Context ctx, int idx, String name) {
        String[] names = loadNames(ctx);
        names[idx] = name;

        File conf = new File(ctx.getFilesDir(), CONF_FILE);
        try (BufferedWriter bw = new BufferedWriter( new OutputStreamWriter( new java.io.FileOutputStream(conf) ) )) {
            for (int i = 0; i < 5; i++) {
                bw.write(names[i]);
                bw.newLine();
            }
        } catch (IOException e) {
            showErr(ctx, "ERR save: " + e.getMessage());
        }
    }

    // Find next available number by scanning existing files.
    public static int nextFileNum(Context ctx, IO1.VFile dir) {
        List<IO1.VFile> files = dir.ListDir(ctx);
        int maxNum = -1;
        for (IO1.VFile f : files) {
            String name = f.GetName(ctx);
            if (name == null || name.length() < 2)
                continue;
            int dot = name.lastIndexOf('.');
            if (dot <= 1)
                continue;
            try {
                int num = Integer.parseInt(name.substring(1, dot));
                if (num > maxNum)
                    maxNum = num;
            } catch (NumberFormatException ignored) { }
        }
        return maxNum + 1;
    }

    // Format filename: prefix + 5-digit number + extension.
    public static String fmtFileName(String tag, int num, String ext) {
        return String.format("%s%05d.%s", tag, num, ext);
    }

    // Read EXIF orientation as rotation degrees (0, 90, 180, 270).
    public static int exifRotation(Context ctx, IO1.VFile file) {
        try (InputStream is = file.OpenReader(ctx)) {
            int o = new ExifInterface(is).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (o == ExifInterface.ORIENTATION_ROTATE_90) return 90;
            if (o == ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (o == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        } catch (Exception ignored) { }
        return 0;
    }

    // Rotate bitmap by degrees; returns original if degrees == 0.
    public static Bitmap applyRotation(Bitmap bmp, int degrees) {
        if (degrees == 0 || bmp == null) return bmp;
        Matrix m = new Matrix();
        m.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        bmp.recycle();
        return rotated;
    }

    // Decode, apply EXIF rotation, re-encode (strips all metadata).
    public static String convImage(Context ctx, IO1.VFile src, String mode, OutputStream dest) {
        try {
            Bitmap bmp;
            try (InputStream is = src.OpenReader(ctx)) { bmp = BitmapFactory.decodeStream(is); }
            if (bmp == null) return null;
            bmp = applyRotation(bmp, exifRotation(ctx, src));

            // Resolve output format
            Bitmap.CompressFormat fmt;
            String ext;
            int qual;

            switch (mode) {
                case "jpg":
                    fmt = Bitmap.CompressFormat.JPEG;
                    ext = "jpg";
                    qual = 95;
                    break;
                case "png":
                    fmt = Bitmap.CompressFormat.PNG;
                    ext = "png";
                    qual = 100;
                    break;
                case "webp":
                    fmt = Bitmap.CompressFormat.WEBP_LOSSY;
                    ext = "webp";
                    qual = 90;
                    break;
                case "webp_lossless":
                    fmt = Bitmap.CompressFormat.WEBP_LOSSLESS;
                    ext = "webp";
                    qual = 100;
                    break;
                case "webp_half":
                    fmt = Bitmap.CompressFormat.WEBP_LOSSY;
                    ext = "webp";
                    qual = 90;
                    // Scale to half resolution
                    Bitmap half = Bitmap.createScaledBitmap(bmp, Math.max(1, bmp.getWidth() / 2), Math.max(1, bmp.getHeight() / 2), true);
                    bmp.recycle();
                    bmp = half;
                    break;
                default:
                    // Re-encode in original format
                    String srcLow = src.GetName(ctx).toLowerCase();
                    if (srcLow.endsWith(".png")) {
                        fmt = Bitmap.CompressFormat.PNG;
                        ext = "png";
                        qual = 100;
                    } else if (srcLow.endsWith(".webp")) {
                        fmt = Bitmap.CompressFormat.WEBP_LOSSY;
                        ext = "webp";
                        qual = 90;
                    } else {
                        fmt = Bitmap.CompressFormat.JPEG;
                        ext = "jpg";
                        qual = 95;
                    }
                    break;
            }

            // Compress and write
            bmp.compress(fmt, qual, dest);
            bmp.recycle();

            // Build output filename
            String srcName = src.GetName(ctx);
            int dot = srcName.lastIndexOf('.');
            String base = (dot > 0) ? srcName.substring(0, dot) : srcName;
            return base + "." + ext;

        } catch (IOException e) {
            return null;
        }
    }

    // Resolve MIME type from extension.
    public static String mimeType(String ext) {
        switch (ext.toLowerCase()) {
            case "jpg": case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }

    // Resolve file extension from conversion mode.
    public static String modeToExt(String mode) {
        switch (mode) {
            case "jpg":
                return "jpg";
            case "png":
                return "png";
            case "webp": case "webp_lossless": case "webp_half":
                return "webp";
            default:
                return null;
        }
    }

    // Create output file in Downloads/eBookificate via MediaStore.
    public static IO1.VFile makeOutFile(Context ctx, String name) {
        ContentValues cv = new ContentValues();
        String mime = "application/octet-stream";
        int dot = name.lastIndexOf('.');
        if (dot > 0)
            mime = mimeType(name.substring(dot + 1).toLowerCase());

        cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
        cv.put(MediaStore.Downloads.MIME_TYPE, mime);
        cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + OUT_DIR);

        Uri col = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri uri = ctx.getContentResolver().insert(col, cv);
        return uri != null ? new IO1.VFile(uri, false) : null;
    }

    // Export storage slot as ZIP to Downloads.
    public static boolean exportZip(Context ctx, int slot, ProgressCB cb) {
        IO1.VFile dir = getStoreDir(ctx, slot);
        List<IO1.VFile> files = dir.ListDir(ctx);
        if (files.isEmpty())
            return false;

        String zipName = SLOT_TAG[slot] + ".zip";
        IO1.VFile zipFile = makeOutFile(ctx, zipName);
        if (zipFile == null)
            return false;

        try (OutputStream os = zipFile.OpenWriter(ctx, false); ZipOutputStream zos = new ZipOutputStream(os)) {
            byte[] buf = new byte[8192];
            int cur = 0;

            for (IO1.VFile f : files) {
                String name = f.GetName(ctx);
                zos.putNextEntry(new ZipEntry(name));
                try (InputStream is = f.OpenReader(ctx)) {
                    int len;
                    while ((len = is.read(buf)) > 0)
                        zos.write(buf, 0, len);
                }
                zos.closeEntry();
                cur++;
                if (cb != null)
                    cb.onProgress(cur, files.size());
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Batch convert files to Downloads/eBookificate.
    public static int batchConvert(Context ctx, List<IO1.VFile> srcs, String mode, ProgressCB cb) {
        int ok = 0;
        for (int i = 0; i < srcs.size(); i++) {
            IO1.VFile src = srcs.get(i);
            String outName = convFileName(ctx, src, mode);
            IO1.VFile out = makeOutFile(ctx, outName);
            if (out != null) {
                try (OutputStream os = out.OpenWriter(ctx, false)) {
                    if (convImage(ctx, src, mode, os) != null)
                        ok++;
                    else
                        out.Delete(ctx);
                } catch (IOException e) {
                    out.Delete(ctx);
                }
            }
            if (cb != null)
                cb.onProgress(i + 1, srcs.size());
        }
        return ok;
    }

    // Convert all images in a storage slot in-place.
    public static int convStorage(Context ctx, int slot, String mode, ProgressCB cb) {
        IO1.VFile dir = getStoreDir(ctx, slot);
        List<IO1.VFile> files = dir.ListDir(ctx);
        if (files.isEmpty())
            return 0;
        String newExt = modeToExt(mode);
        int ok = 0;

        for (int i = 0; i < files.size(); i++) {
            IO1.VFile src = files.get(i);
            String srcName = src.GetName(ctx);

            // Resolve output extension
            String ext = newExt;
            if (ext == null) {
                int dot = srcName.lastIndexOf('.');
                ext = (dot > 0) ? srcName.substring(dot + 1) : "jpg";
            }

            // Write to temp file, then swap
            String tmpName = "._tmp_" + i + "." + ext;
            IO1.VFile tmp = null;
            try {
                tmp = dir.CreateFile(ctx, mimeType(ext), tmpName);
                if (tmp == null)
                    continue;

                try (OutputStream os = tmp.OpenWriter(ctx, false)) {
                    if (convImage(ctx, src, mode, os) != null) {
                        String outName = srcName;
                        int dot = srcName.lastIndexOf('.');
                        if (dot > 0 && newExt != null)
                            outName = srcName.substring(0, dot) + "." + newExt;
                        src.Delete(ctx);
                        tmp.Rename(ctx, outName);
                        tmp = null;
                        ok++;
                    }
                }
            } catch (IOException e) {
                showErr(ctx, "ERR convert: " + e.getMessage());
            } finally {
                // Cleanup leftover temp file
                if (tmp != null)
                    tmp.Delete(ctx);
            }

            if (cb != null)
                cb.onProgress(i + 1, files.size());
        }
        return ok;
    }

    // Build converted output filename.
    private static String convFileName(Context ctx, IO1.VFile src, String mode) {
        String name = src.GetName(ctx);
        String ext = modeToExt(mode);
        if (ext == null)
            return name;
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        return base + "." + ext;
    }

    // Delete all files in a storage slot.
    public static void resetStore(Context ctx, int slot) {
        IO1.VFile dir = getStoreDir(ctx, slot);
        for (IO1.VFile f : dir.ListDir(ctx))
            f.Delete(ctx);
    }

    public interface ProgressCB {
        void onProgress(int current, int total);
    }
}
