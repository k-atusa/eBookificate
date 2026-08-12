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

    private final List<IO1.VFile> selectedFiles = new ArrayList<>();
    private TextView txtSelected;
    private TextView txtProgress;
    private RadioGroup radioMode;
    private View btnConvert;
    private boolean isWorking = false;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    List<IO1.VFile> files = IO1.HandleSelectedFile(result.getData());
                    selectedFiles.clear();
                    selectedFiles.addAll(files);
                    txtSelected.setText(files.size() + "개 선택됨");
                }
            });

    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // Proceed regardless; notification permission is nice-to-have
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_convert);

        txtSelected = findViewById(R.id.txtSelected);
        txtProgress = findViewById(R.id.txtProgress);
        radioMode = findViewById(R.id.radioMode);
        btnConvert = findViewById(R.id.btnConvert);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // File selection
        findViewById(R.id.btnSelect).setOnClickListener(v -> {
            IO1.SelectFile(filePickerLauncher, true);
        });

        // Convert button
        btnConvert.setOnClickListener(v -> startConvert());

        // Observe service progress
        SVCC1 chan = SVCC1.getChan();
        chan.IntSlots[0].observe(this, current -> updateProgress());
        chan.IntSlots[1].observe(this, total -> updateProgress());
        chan.StringSlots[0].observe(this, status -> {
            if (status != null && !status.isEmpty()) {
                txtProgress.setText(status);
            }
        });
        chan.ToMainBus.observe(this, event -> {
            if (event != null && "DONE".equals(event.action)) {
                isWorking = false;
                btnConvert.setEnabled(true);
                txtProgress.setText("완료");
                Toast.makeText(this, "변환 완료", Toast.LENGTH_SHORT).show();
            }
        });

        // Request notification permission (for foreground service)
        requestNotifPermission();
    }

    private void startConvert() {
        if (isWorking) return;
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "파일을 먼저 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String mode = getSelectedMode();
        isWorking = true;
        btnConvert.setEnabled(false);
        txtProgress.setVisibility(View.VISIBLE);
        txtProgress.setText("변환 준비 중...");

        // Start foreground service
        Intent intent = new Intent(this, ConvertService.class);
        intent.setAction("CONVERT");
        intent.putParcelableArrayListExtra("files", new ArrayList<>(selectedFiles));
        intent.putExtra("mode", mode);
        startForegroundService(intent);
    }

    private String getSelectedMode() {
        int id = radioMode.getCheckedRadioButtonId();
        if (id == R.id.modeJpg) return "jpg";
        if (id == R.id.modePng) return "png";
        if (id == R.id.modeWebp) return "webp";
        if (id == R.id.modeWebpLossless) return "webp_lossless";
        if (id == R.id.modeWebpHalf) return "webp_half";
        return "none";
    }

    private void updateProgress() {
        SVCC1 chan = SVCC1.getChan();
        Integer current = chan.IntSlots[0].getValue();
        Integer total = chan.IntSlots[1].getValue();
        if (current != null && total != null && total > 0) {
            txtProgress.setVisibility(View.VISIBLE);
            txtProgress.setText(current + "/" + total + " 변환 중...");
        }
    }

    private void requestNotifPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
