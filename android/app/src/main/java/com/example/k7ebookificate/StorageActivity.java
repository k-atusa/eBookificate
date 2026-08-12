package com.example.k7ebookificate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StorageActivity extends AppCompatActivity {

    private int slot;
    private ImageAdapter adapter;
    private TextView txtTitle, txtPage, txtProgress;
    private View btnConvert, btnAdd, btnExport, btnReset, btnPrev, btnNext;

    private List<IO1.VFile> allFiles = new ArrayList<>();
    private int currentPage = 0;
    private int totalPages = 1;
    private boolean isWorking = false;

    // Camera launcher: returns to this activity and refreshes
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                refreshFiles();
            });

    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_storage);

        slot = getIntent().getIntExtra("slot", 0);

        // Views
        txtTitle = findViewById(R.id.txtTitle);
        txtPage = findViewById(R.id.txtPage);
        txtProgress = findViewById(R.id.txtProgress);
        btnConvert = findViewById(R.id.btnConvert);
        btnAdd = findViewById(R.id.btnAdd);
        btnExport = findViewById(R.id.btnExport);
        btnReset = findViewById(R.id.btnReset);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        // Title
        String[] names = Core.loadStorageNames(this);
        txtTitle.setText(names[slot]);

        // RecyclerView setup
        RecyclerView recycler = findViewById(R.id.recyclerImages);
        recycler.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new ImageAdapter(this);
        recycler.setAdapter(adapter);

        // Long press → delete
        adapter.setOnItemLongClickListener((position, file) -> {
            String name = file.GetName(this);
            new AlertDialog.Builder(this)
                    .setTitle("삭제")
                    .setMessage(name + " 을(를) 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (d, w) -> {
                        file.Delete(this);
                        refreshFiles();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        // Action buttons — ensure clickable
        View backBtn = findViewById(R.id.btnBack);
        backBtn.setClickable(true);
        backBtn.setOnClickListener(v -> finish());

        btnConvert.setClickable(true);
        btnConvert.setOnClickListener(v -> showConvertDialog());
        btnAdd.setClickable(true);
        btnAdd.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                cameraPermLauncher.launch(Manifest.permission.CAMERA);
            }
        });
        btnExport.setClickable(true);
        btnExport.setOnClickListener(v -> confirmExport());
        btnReset.setClickable(true);
        btnReset.setOnClickListener(v -> confirmReset());

        // Pagination
        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                updatePage();
            }
        });
        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                updatePage();
            }
        });

        // Observe service progress
        SVCC1 chan = SVCC1.getChan();
        chan.StringSlots[0].observe(this, status -> {
            if (status != null && !status.isEmpty()) {
                txtProgress.setVisibility(View.VISIBLE);
                txtProgress.setText(status);
            }
        });
        chan.ToMainBus.observe(this, event -> {
            if (event != null && "DONE".equals(event.action)) {
                isWorking = false;
                setMenuEnabled(true);
                txtProgress.setVisibility(View.GONE);
                refreshFiles();
                Toast.makeText(this, "작업 완료", Toast.LENGTH_SHORT).show();
            }
        });

        // Request notification permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFiles();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.shutdown();
    }

    // ========== File Loading & Pagination ==========

    private void refreshFiles() {
        IO1.VFile storageDir = Core.getStorageDir(this, slot);
        allFiles = storageDir.ListDir(this);

        // Sort by name
        allFiles.sort((a, b) -> {
            String na = a.GetName(this);
            String nb = b.GetName(this);
            return na.compareTo(nb);
        });

        totalPages = Math.max(1, (int) Math.ceil((double) allFiles.size() / Core.PAGE_SIZE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        updatePage();
    }

    private void updatePage() {
        int start = currentPage * Core.PAGE_SIZE;
        int end = Math.min(start + Core.PAGE_SIZE, allFiles.size());

        List<IO1.VFile> pageItems = (start < allFiles.size())
                ? allFiles.subList(start, end)
                : new ArrayList<>();
        adapter.setItems(pageItems);

        txtPage.setText((currentPage + 1) + " / " + totalPages);
        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled(currentPage < totalPages - 1);
    }

    // ========== Actions ==========

    private void launchCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("slot", slot);
        cameraLauncher.launch(intent);
    }

    private void showConvertDialog() {
        if (isWorking) return;
        String[] modes = {"JPG", "PNG", "WebP", "WebP (Half)"};
        String[] modeValues = {"jpg", "png", "webp", "webp_half"};

        new AlertDialog.Builder(this)
                .setTitle("변환 모드 선택")
                .setItems(modes, (d, which) -> {
                    new AlertDialog.Builder(this)
                            .setTitle("변환")
                            .setMessage("저장소의 모든 이미지를 " + modes[which] + " 로 변환하시겠습니까?")
                            .setPositiveButton("변환", (d2, w2) -> {
                                startStorageConvert(modeValues[which]);
                            })
                            .setNegativeButton("취소", null)
                            .show();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void startStorageConvert(String mode) {
        isWorking = true;
        setMenuEnabled(false);
        txtProgress.setVisibility(View.VISIBLE);
        txtProgress.setText("변환 준비 중...");

        Intent intent = new Intent(this, ConvertService.class);
        intent.setAction("CONVERT_STORAGE");
        intent.putExtra("slot", slot);
        intent.putExtra("mode", mode);
        startForegroundService(intent);
    }

    private void confirmExport() {
        if (isWorking) return;
        if (allFiles.isEmpty()) {
            Toast.makeText(this, "내보낼 파일이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String zipName = Core.SLOT_PREFIXES[slot] + ".zip";
        new AlertDialog.Builder(this)
                .setTitle("내보내기")
                .setMessage(allFiles.size() + "개 파일을 " + zipName + " 으로 내보내시겠습니까?")
                .setPositiveButton("내보내기", (d, w) -> {
                    isWorking = true;
                    setMenuEnabled(false);
                    txtProgress.setVisibility(View.VISIBLE);
                    txtProgress.setText("내보내기 준비 중...");

                    Intent intent = new Intent(this, ConvertService.class);
                    intent.setAction("EXPORT_ZIP");
                    intent.putExtra("slot", slot);
                    startForegroundService(intent);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void confirmReset() {
        if (isWorking) return;
        new AlertDialog.Builder(this)
                .setTitle("초기화")
                .setMessage("저장소의 모든 파일을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
                .setPositiveButton("초기화", (d, w) -> {
                    Core.resetStorage(this, slot);
                    refreshFiles();
                    Toast.makeText(this, "초기화 완료", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void setMenuEnabled(boolean enabled) {
        btnConvert.setEnabled(enabled);
        btnAdd.setEnabled(enabled);
        btnExport.setEnabled(enabled);
        btnReset.setEnabled(enabled);
    }
}
