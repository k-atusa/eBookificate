package com.example.k7ebookificate;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CameraX-based continuous capture activity for book scanning.
 * Stays open until user presses back. Each tap of the shutter button
 * saves a photo to the storage slot with auto-incremented numbering.
 */
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    private int slot;
    private int captureCount = 0;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private TextView txtCount;
    private boolean isCapturing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_camera);

        slot = getIntent().getIntExtra("slot", 0);
        txtCount = findViewById(R.id.txtCount);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Capture button
        findViewById(R.id.btnCapture).setOnClickListener(v -> capturePhoto());

        // Start camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview
                PreviewView previewView = findViewById(R.id.previewView);
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // ImageCapture
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Use back camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Bind use cases
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Log.e(TAG, "Camera start failed", e);
                Toast.makeText(this, "카메라 시작 실패", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        if (imageCapture == null || isCapturing) return;
        isCapturing = true;

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                try {
                    saveImageToStorage(image);
                    captureCount++;
                    runOnUiThread(() -> {
                        txtCount.setText(captureCount + "장 촬영");
                        isCapturing = false;
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Save failed", e);
                    runOnUiThread(() -> {
                        Toast.makeText(CameraActivity.this, "저장 실패", Toast.LENGTH_SHORT).show();
                        isCapturing = false;
                    });
                } finally {
                    image.close();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Capture failed", exception);
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this, "촬영 실패", Toast.LENGTH_SHORT).show();
                    isCapturing = false;
                });
            }
        });
    }

    private void saveImageToStorage(ImageProxy image) throws Exception {
        // Convert ImageProxy to Bitmap
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        if (bitmap == null) throw new Exception("Failed to decode captured image");

        // Get next number and save
        IO1.VFile storageDir = Core.getStorageDir(this, slot);
        int nextNum = Core.getNextNumber(this, storageDir);
        String prefix = Core.getSlotPrefix(slot);
        String fileName = Core.formatFileName(prefix, nextNum, "jpg");

        IO1.VFile outFile = storageDir.CreateFile(this, "image/jpeg", fileName);
        if (outFile == null) throw new Exception("Failed to create output file");

        try (OutputStream os = outFile.OpenWriter(this, false)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
        }
        bitmap.recycle();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}
