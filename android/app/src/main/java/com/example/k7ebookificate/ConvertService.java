package com.example.k7ebookificate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground Service for heavy tasks: batch convert, storage convert, ZIP export.
 * Communicates progress via SVCC1 shared slots.
 *
 * Actions (via Intent):
 *   "CONVERT"         — batch convert files to Downloads/eBookificate/
 *                       extras: ArrayList<VFile> "files", String "mode"
 *   "CONVERT_STORAGE" — convert all files in a storage slot in-place
 *                       extras: int "slot", String "mode"
 *   "EXPORT_ZIP"      — export a storage slot as ZIP
 *                       extras: int "slot"
 *
 * Progress reported via SVCC1:
 *   IntSlots[0] = current, IntSlots[1] = total
 *   StringSlots[0] = status message
 *   ToMainBus → "DONE" when complete
 */
public class ConvertService extends Service {

    private static final String CHANNEL_ID = "convert_channel";
    private static final int NOTIFICATION_ID = 1;

    private ExecutorService executor;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start foreground immediately
        Notification notification = buildNotification("작업 준비 중...");
        startForeground(NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);

        if (intent == null || intent.getAction() == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        executor.submit(() -> {
            try {
                switch (action) {
                    case "CONVERT":
                        handleConvert(extras);
                        break;
                    case "CONVERT_STORAGE":
                        handleConvertStorage(extras);
                        break;
                    case "EXPORT_ZIP":
                        handleExportZip(extras);
                        break;
                }
            } finally {
                SVCC1.getChan().SendToMain("DONE", null);
                stopSelf();
            }
        });

        return START_NOT_STICKY;
    }

    private void handleConvert(Bundle extras) {
        if (extras == null) return;
        ArrayList<IO1.VFile> files = extras.getParcelableArrayList("files", IO1.VFile.class);
        String mode = extras.getString("mode", "none");
        if (files == null || files.isEmpty()) return;

        updateStatus("변환 시작...");
        SVCC1.getChan().SetInt(1, files.size());

        Core.batchConvertToDownloads(this, files, mode, (current, total) -> {
            SVCC1.getChan().SetInt(0, current);
            updateNotification(current + "/" + total + " 변환 중...");
            updateStatus(current + "/" + total + " 변환 중...");
        });

        updateStatus("변환 완료");
    }

    private void handleConvertStorage(Bundle extras) {
        if (extras == null) return;
        int slot = extras.getInt("slot", -1);
        String mode = extras.getString("mode", "none");
        if (slot < 0 || slot > 4) return;

        updateStatus("저장소 변환 시작...");

        Core.convertStorage(this, slot, mode, (current, total) -> {
            SVCC1.getChan().SetInt(0, current);
            SVCC1.getChan().SetInt(1, total);
            updateNotification(current + "/" + total + " 변환 중...");
            updateStatus(current + "/" + total + " 변환 중...");
        });

        updateStatus("변환 완료");
    }

    private void handleExportZip(Bundle extras) {
        if (extras == null) return;
        int slot = extras.getInt("slot", -1);
        if (slot < 0 || slot > 4) return;

        updateStatus("ZIP 내보내기 중...");

        Core.exportAsZip(this, slot, (current, total) -> {
            SVCC1.getChan().SetInt(0, current);
            SVCC1.getChan().SetInt(1, total);
            updateNotification(current + "/" + total + " 묶는 중...");
            updateStatus(current + "/" + total + " 묶는 중...");
        });

        updateStatus("내보내기 완료");
    }

    // ========== Notification ==========

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "변환 작업", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("이미지 변환/내보내기 진행 알림");
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel);
    }

    private Notification buildNotification(String text) {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("eBookificate")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIFICATION_ID, buildNotification(text));
    }

    private void updateStatus(String status) {
        SVCC1.getChan().SetString(0, status);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }
}
