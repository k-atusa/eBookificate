package com.example.k7ebookificate;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StorageActivity extends AppCompatActivity {
    private int slot;
    private ImageAdapter adapter;
    private TextView txtTitle, txtPage, txtStatus;
    private View btnConv, btnAdd, btnExport, btnReset, btnPrev, btnNext;

    private List<IO1.VFile> allFiles = new ArrayList<>();
    private int curPage = 0;
    private int maxPage = 1;
    private boolean busy = false;

    // Camera result refreshes file list.
    private final ActivityResultLauncher<Intent> camLaunch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> reloadFiles());

    private final ActivityResultLauncher<String> camPerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), ok -> {
                if (ok) openCamera();
                else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String> notifPerm = registerForActivityResult(new ActivityResultContracts.RequestPermission(), ok -> {});

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.view_storage);
        slot = getIntent().getIntExtra("slot", 0);

        // Bind views
        txtTitle  = findViewById(R.id.txtTitle);
        txtPage   = findViewById(R.id.txtPage);
        txtStatus = findViewById(R.id.txtProgress);
        btnConv   = findViewById(R.id.btnConvert);
        btnAdd    = findViewById(R.id.btnAdd);
        btnExport = findViewById(R.id.btnExport);
        btnReset  = findViewById(R.id.btnReset);
        btnPrev   = findViewById(R.id.btnPrev);
        btnNext   = findViewById(R.id.btnNext);

        // Set title
        txtTitle.setText(Core.loadNames(this)[slot]);

        // Grid setup
        RecyclerView grid = findViewById(R.id.recyclerImages);
        grid.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new ImageAdapter(this);
        grid.setAdapter(adapter);

        // Long press to delete
        adapter.setOnItemLongClickListener((pos, file) -> {
            String name = file.GetName(this);
            new AlertDialog.Builder(this)
                    .setTitle("Delete")
                    .setMessage("Delete " + name + "?")
                    .setPositiveButton("Delete", (d, w) -> {
                        file.Delete(this);
                        reloadFiles();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Tap to view fullscreen
        adapter.setOnItemClickListener((pos, file) -> showImage(file));

        // Action buttons
        View btnBack = findViewById(R.id.btnBack);
        btnBack.setClickable(true);
        btnBack.setOnClickListener(v -> finish());

        btnConv.setClickable(true);
        btnConv.setOnClickListener(v -> pickConvMode());
        btnAdd.setClickable(true);
        btnAdd.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                camPerm.launch(Manifest.permission.CAMERA);
            }
        });
        btnExport.setClickable(true);
        btnExport.setOnClickListener(v -> askExport());
        btnReset.setClickable(true);
        btnReset.setOnClickListener(v -> askReset());

        // Pagination
        btnPrev.setOnClickListener(v -> { if (curPage > 0) { curPage--; showPage(); } });
        btnNext.setOnClickListener(v -> { if (curPage < maxPage - 1) { curPage++; showPage(); } });

        // Observe service progress
        SVCC1 ch = SVCC1.getChan();
        ch.StringSlots[0].observe(this, s -> {
            if (s != null && !s.isEmpty()) {
                txtStatus.setVisibility(View.VISIBLE);
                txtStatus.setText(s);
            }
        });
        ch.ToMainBus.observe(this, ev -> {
            if (ev != null && "DONE".equals(ev.action)) {
                busy = false;
                setMenuOn(true);
                txtStatus.setVisibility(View.GONE);
                reloadFiles();
                Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();
            }
        });

        // Request notification permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override protected void onResume() { super.onResume(); reloadFiles(); }
    @Override protected void onDestroy() { super.onDestroy(); adapter.shutdown(); }

    // Reload file list and refresh page.
    private void reloadFiles() {
        IO1.VFile dir = Core.getStoreDir(this, slot);
        allFiles = dir.ListDir(this);
        allFiles.sort((a, b) -> a.GetName(this).compareTo(b.GetName(this)));

        maxPage = Math.max(1, (int) Math.ceil((double) allFiles.size() / Core.PAGE_SIZE));
        if (curPage >= maxPage) curPage = maxPage - 1;
        showPage();
    }

    // Display current page of thumbnails.
    private void showPage() {
        int from = curPage * Core.PAGE_SIZE;
        int to = Math.min(from + Core.PAGE_SIZE, allFiles.size());
        List<IO1.VFile> page = (from < allFiles.size()) ? allFiles.subList(from, to) : new ArrayList<>();
        adapter.setItems(page);
        txtPage.setText((curPage + 1) + " / " + maxPage);
        btnPrev.setEnabled(curPage > 0);
        btnNext.setEnabled(curPage < maxPage - 1);
    }

    // Launch camera activity for this slot.
    private void openCamera() {
        Intent it = new Intent(this, CameraActivity.class);
        it.putExtra("slot", slot);
        camLaunch.launch(it);
    }

    // Show conversion mode picker dialog.
    private void pickConvMode() {
        if (busy) return;
        String[] labels = {"JPG", "PNG", "WebP", "WebP (Half)"};
        String[] modes  = {"jpg", "png", "webp", "webp_half"};

        new AlertDialog.Builder(this)
                .setTitle("Convert Mode")
                .setItems(labels, (d, i) -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Convert")
                            .setMessage("Convert all images to " + labels[i] + "?")
                            .setPositiveButton("Convert", (d2, w) -> runConvStore(modes[i]))
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Start storage conversion via foreground service.
    private void runConvStore(String mode) {
        busy = true;
        setMenuOn(false);
        txtStatus.setVisibility(View.VISIBLE);
        txtStatus.setText("Preparing...");

        Intent it = new Intent(this, ConvertService.class);
        it.setAction("CONV_STORE");
        it.putExtra("slot", slot);
        it.putExtra("mode", mode);
        try {
            startForegroundService(it);
        } catch (Exception e) {
            busy = false;
            setMenuOn(true);
            txtStatus.setVisibility(View.GONE);
            Toast.makeText(this, "ERR start service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Confirm and start ZIP export.
    private void askExport() {
        if (busy) return;
        if (allFiles.isEmpty()) {
            Toast.makeText(this, "No files to export", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = Core.SLOT_TAG[slot] + ".zip";
        new AlertDialog.Builder(this)
                .setTitle("Export")
                .setMessage("Export " + allFiles.size() + " files as " + name + "?")
                .setPositiveButton("Export", (d, w) -> {
                    busy = true;
                    setMenuOn(false);
                    txtStatus.setVisibility(View.VISIBLE);
                    txtStatus.setText("Preparing...");
                    Intent it = new Intent(this, ConvertService.class);
                    it.setAction("EXPORT_ZIP");
                    it.putExtra("slot", slot);
                    try {
                        startForegroundService(it);
                    } catch (Exception e) {
                        busy = false;
                        setMenuOn(true);
                        txtStatus.setVisibility(View.GONE);
                        Toast.makeText(StorageActivity.this, "ERR start service: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Confirm and reset storage.
    private void askReset() {
        if (busy) return;
        new AlertDialog.Builder(this)
                .setTitle("Reset")
                .setMessage("Delete all files?\nThis cannot be undone.")
                .setPositiveButton("Reset", (d, w) -> {
                    Core.resetStore(this, slot);
                    reloadFiles();
                    Toast.makeText(this, "Reset done", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Toggle action button states.
    private void setMenuOn(boolean on) {
        btnConv.setEnabled(on);
        btnAdd.setEnabled(on);
        btnExport.setEnabled(on);
        btnReset.setEnabled(on);
    }

    // Show fullscreen image in a dialog.
    private void showImage(IO1.VFile file) {
        Dialog dlg = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setBackgroundColor(0xFF000000);

        try {
            int rot = Core.exifRotation(this, file);
            int maxDim = Math.max(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream is = file.OpenReader(this)) { BitmapFactory.decodeStream(is, null, opts); }

            int sample = 1;
            while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) sample *= 2;

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            Bitmap bmp;
            try (InputStream is = file.OpenReader(this)) { bmp = BitmapFactory.decodeStream(is, null, opts); }

            if (bmp != null) {
                bmp = Core.applyRotation(bmp, rot);
                final Bitmap finalBmp = bmp;
                iv.setImageBitmap(finalBmp);
                dlg.setOnDismissListener(d -> finalBmp.recycle());
            }
        } catch (IOException e) {
            Toast.makeText(this, "ERR open image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        iv.setOnClickListener(v -> dlg.dismiss());
        dlg.setContentView(iv);
        dlg.show();
    }
}
