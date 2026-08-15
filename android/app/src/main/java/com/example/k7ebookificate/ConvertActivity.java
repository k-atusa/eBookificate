package com.example.k7ebookificate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ConvertActivity extends AppCompatActivity {

    private final List<IO1.VFile> selFiles = new ArrayList<>();
    private TextView txtSelect;
    private TextView txtStatus;
    private RadioGroup radioMode;
    private View btnConvert;
    private boolean busy = false;

    private final ActivityResultLauncher<Intent> pickLaunch = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                    List<IO1.VFile> files = IO1.HandleSelectedFile(r.getData());
                    selFiles.clear();
                    selFiles.addAll(files);
                    txtSelect.setText(files.size() + " selected");
                }
            });

    private final ActivityResultLauncher<String> notifPerm = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), ok -> {
            });

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.view_convert);

        txtSelect = findViewById(R.id.txtSelected);
        txtStatus = findViewById(R.id.txtProgress);
        radioMode = findViewById(R.id.radioMode);
        btnConvert = findViewById(R.id.btnConvert);

        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // File picker
        findViewById(R.id.btnSelect).setOnClickListener(v -> IO1.SelectFile(pickLaunch, true));

        // Convert
        btnConvert.setOnClickListener(v -> startConv());

        // Observe service progress
        SVCC1 ch = SVCC1.getChan();
        ch.IntSlots[0].observe(this, c -> showProgress());
        ch.IntSlots[1].observe(this, t -> showProgress());
        ch.StringSlots[0].observe(this, s -> {
            if (s != null && !s.isEmpty())
                txtStatus.setText(s);
        });
        ch.ToMainBus.observe(this, ev -> {
            if (ev != null && "DONE".equals(ev.action)) {
                busy = false;
                btnConvert.setEnabled(true);
                txtStatus.setText("Done");
                Toast.makeText(this, "Convert done", Toast.LENGTH_SHORT).show();
            }
        });

        // Request notification permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    // Launch conversion via foreground service.
    private void startConv() {
        if (busy)
            return;
        if (selFiles.isEmpty()) {
            Toast.makeText(this, "Select files first", Toast.LENGTH_SHORT).show();
            return;
        }

        String mode = getMode();
        busy = true;
        btnConvert.setEnabled(false);
        txtStatus.setVisibility(View.VISIBLE);
        txtStatus.setText("Preparing...");

        Intent it = new Intent(this, ConvertService.class);
        it.setAction("CONVERT");
        it.putParcelableArrayListExtra("files", new ArrayList<>(selFiles));
        it.putExtra("mode", mode);
        startForegroundService(it);
    }

    // Resolve selected radio button to mode string.
    private String getMode() {
        int id = radioMode.getCheckedRadioButtonId();
        if (id == R.id.modeJpg)
            return "jpg";
        if (id == R.id.modePng)
            return "png";
        if (id == R.id.modeWebp)
            return "webp";
        if (id == R.id.modeWebpLossless)
            return "webp_lossless";
        if (id == R.id.modeWebpHalf)
            return "webp_half";
        return "none";
    }

    // Update progress text from SVCC1 slots.
    private void showProgress() {
        SVCC1 ch = SVCC1.getChan();
        Integer cur = ch.IntSlots[0].getValue();
        Integer total = ch.IntSlots[1].getValue();
        if (cur != null && total != null && total > 0) {
            txtStatus.setVisibility(View.VISIBLE);
            txtStatus.setText(cur + "/" + total + " converting...");
        }
    }
}
