package com.example.k7ebookificate;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// CameraX continuous capture for book scanning.
public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "Camera";

    private int slot;
    private int snapCount = 0;
    private Camera camera;
    private ImageCapture capture;
    private ExecutorService camWork;
    private ScaleGestureDetector scaleDetector;
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initCamera();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // Bind CameraX preview and capture use cases.
    private void initCamera() {
        ListenableFuture<ProcessCameraProvider> fut = ProcessCameraProvider.getInstance(this);

        fut.addListener(() -> {
            try {
                ProcessCameraProvider prov = fut.get();
                PreviewView pv = findViewById(R.id.previewView);
                ResolutionSelector resSel = new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .build();

                Preview preview = new Preview.Builder()
                        .setResolutionSelector(resSel)
                        .build();
                preview.setSurfaceProvider(pv.getSurfaceProvider());

                capture = new ImageCapture.Builder()
                        .setResolutionSelector(resSel)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setJpegQuality(95)
                        .build();

                prov.unbindAll();
                camera = prov.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture);

                setupTouchControls(pv);
            } catch (Exception e) {
                Toast.makeText(this, "ERR camera init: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Setup touch-to-focus and pinch-to-zoom on the preview.
    private void setupTouchControls(PreviewView pv) {
        // Pinch-to-zoom
        scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        if (camera == null) return true;
                        ZoomState zoomState = camera.getCameraInfo().getZoomState().getValue();
                        float currentZoom = zoomState != null ? zoomState.getZoomRatio() : 1f;
                        float newZoom = currentZoom * detector.getScaleFactor();
                        camera.getCameraControl().setZoomRatio(newZoom);
                        return true;
                    }
                });

        // Touch-to-focus + pinch-to-zoom combined listener
        pv.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP && !scaleDetector.isInProgress()) {
                MeteringPointFactory factory = pv.getMeteringPointFactory();
                MeteringPoint point = factory.createPoint(event.getX(), event.getY());
                FocusMeteringAction action = new FocusMeteringAction.Builder(point).build();
                camera.getCameraControl().startFocusAndMetering(action);
                v.performClick();
            }
            return true;
        });
    }

    // Capture a single photo and save to storage.
    private void takePhoto() {
        if (capture == null || taking) return;
        taking = true;

        capture.takePicture(camWork, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy img) {
                try (img) {
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
                        Toast.makeText(CameraActivity.this, "ERR save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        taking = false;
                    });
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException ex) {
                Log.e(TAG, "capture: " + ex.getMessage(), ex);
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this, "ERR capture: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    taking = false;
                });
            }
        });
    }

    // Build minimal EXIF APP1 with only Orientation tag.
    private byte[] makeMinimalExif(int orientation) {
        byte[] app1 = new byte[36];
        ByteBuffer bb = ByteBuffer.wrap(app1).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 0xFF); bb.put((byte) 0xE1); // APP1 marker
        bb.putShort((short) 34); // segment length
        bb.put((byte)'E'); bb.put((byte)'x'); bb.put((byte)'i'); bb.put((byte)'f');
        bb.put((byte) 0); bb.put((byte) 0);
        bb.put((byte)'M'); bb.put((byte)'M'); // TIFF BE
        bb.putShort((short) 0x002A); bb.putInt(8); // magic + IFD offset
        bb.putShort((short) 1); // 1 entry
        bb.putShort((short) 0x0112); // Orientation tag
        bb.putShort((short) 3); bb.putInt(1); // type SHORT, count 1
        bb.putShort((short) orientation); // value
        bb.putShort((short) 0); bb.putInt(0); // padding + next IFD
        return app1;
    }

    // Save camera JPEG with minimal EXIF.
    private void saveImage(ImageProxy img) throws Exception {
        ByteBuffer buf = img.getPlanes()[0].getBuffer();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);

        int deg = img.getImageInfo().getRotationDegrees();
        int orient = deg == 90 ? 6 : deg == 180 ? 3 : deg == 270 ? 8 : 1;

        IO1.VFile dir = Core.getStoreDir(this, slot);
        String name = Core.fmtFileName(Core.getSlotTag(slot), Core.nextFileNum(this, dir), "jpg");
        IO1.VFile out = dir.CreateFile(this, "image/jpeg", name);
        if (out == null) throw new Exception("createFile failed: " + name);

        try (OutputStream os = out.OpenWriter(this, false)) {
            if (data.length < 4 || (data[0] & 0xFF) != 0xFF || (data[1] & 0xFF) != 0xD8) {
                os.write(data); return;
            }
            os.write(0xFF); os.write(0xD8); // SOI
            os.write(makeMinimalExif(orient)); // minimal EXIF

            // skip existing metadata (APP0-APP15, COM)
            int pos = 2;
            while (pos + 3 < data.length) {
                if ((data[pos] & 0xFF) != 0xFF) break;
                int m = data[pos + 1] & 0xFF;
                if (m == 0xD9 || m == 0xDA) break;
                int len = ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
                if ((m >= 0xE0 && m <= 0xEF) || m == 0xFE) pos += 2 + len;
                else break;
            }
            if (pos < data.length) os.write(data, pos, data.length - pos);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camWork != null) camWork.shutdown();
    }
}
