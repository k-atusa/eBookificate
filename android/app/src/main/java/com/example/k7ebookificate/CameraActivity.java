package com.example.k7ebookificate;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
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

// CameraX continuous capture for book scanning.
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "Camera";

    private int slot;
    private int snapCount = 0;
    private ImageCapture capture;
    private ExecutorService camWork;
    private TextView txtCount;
    private boolean taking = false;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.view_camera);

        slot = getIntent().getIntExtra("slot", 0);
        txtCount = findViewById(R.id.txtCount);
        camWork = Executors.newSingleThreadExecutor();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCapture).setOnClickListener(v -> takePhoto());

        // Start camera if permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            initCamera();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // Bind CameraX preview and capture use cases.
    private void initCamera() {
        ListenableFuture<ProcessCameraProvider> fut =
                ProcessCameraProvider.getInstance(this);

        fut.addListener(() -> {
            try {
                ProcessCameraProvider prov = fut.get();
                PreviewView pv = findViewById(R.id.previewView);
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(pv.getSurfaceProvider());

                capture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                prov.unbindAll();
                prov.bindToLifecycle(this,
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, capture);
            } catch (Exception e) {
                Log.e(TAG, "initCamera: " + e.getMessage(), e);
                Toast.makeText(this,
                        "ERR: camera init: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Capture a single photo and save to storage.
    private void takePhoto() {
        if (capture == null || taking) return;
        taking = true;

        capture.takePicture(camWork, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy img) {
                try {
                    saveImage(img);
                    snapCount++;
                    runOnUiThread(() -> {
                        txtCount.setText(snapCount + " captured");
                        // Blink animation for visibility
                        Animation blink = new AlphaAnimation(1f, 0f);
                        blink.setDuration(125);
                        blink.setRepeatMode(Animation.REVERSE);
                        blink.setRepeatCount(3);
                        txtCount.startAnimation(blink);
                        taking = false;
                    });
                } catch (Exception e) {
                    Log.e(TAG, "saveImage: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(CameraActivity.this,
                                "ERR: save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        taking = false;
                    });
                } finally {
                    img.close();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException ex) {
                Log.e(TAG, "capture: " + ex.getMessage(), ex);
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this,
                            "ERR: capture: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    taking = false;
                });
            }
        });
    }

    // Decode ImageProxy and write JPEG to storage slot.
    private void saveImage(ImageProxy img) throws Exception {
        ByteBuffer buf = img.getPlanes()[0].getBuffer();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bmp == null) throw new Exception("decode failed");

        IO1.VFile dir = Core.getStoreDir(this, slot);
        int num = Core.nextFileNum(this, dir);
        String tag = Core.getSlotTag(slot);
        String name = Core.fmtFileName(tag, num, "jpg");

        IO1.VFile out = dir.CreateFile(this, "image/jpeg", name);
        if (out == null) throw new Exception("createFile failed: " + name);

        try (OutputStream os = out.OpenWriter(this, false)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, os);
        }
        bmp.recycle();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camWork != null) camWork.shutdown();
    }
}
