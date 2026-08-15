package com.example.k7ebookificate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Foreground service for batch convert, storage convert, ZIP export.
public class ConvertService extends Service {

    private static final String CH_ID = "conv_ch";
    private static final int NOTIF_ID = 1;
    private ExecutorService worker;

    @Override
    public void onCreate() {
        super.onCreate();
        worker = Executors.newSingleThreadExecutor();
        initChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start foreground immediately
        startForeground(NOTIF_ID, buildNotif("Preparing..."),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);

        if (intent == null || intent.getAction() == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        worker.submit(() -> {
            try {
                switch (action) {
                    case "CONVERT":      runConvert(extras); break;
                    case "CONV_STORE":   runConvStore(extras); break;
                    case "EXPORT_ZIP":   runExportZip(extras); break;
                }
            } finally {
                SVCC1.getChan().SendToMain("DONE", null);
                stopSelf();
            }
        });

        return START_NOT_STICKY;
    }

    private void runConvert(Bundle ext) {
        if (ext == null) return;
        ArrayList<IO1.VFile> files = ext.getParcelableArrayList("files", IO1.VFile.class);
        String mode = ext.getString("mode", "none");
        if (files == null || files.isEmpty()) return;

        setStatus("Converting...");
        SVCC1.getChan().SetInt(1, files.size());

        Core.batchConvert(this, files, mode, (cur, total) -> {
            SVCC1.getChan().SetInt(0, cur);
            pushNotif(cur + "/" + total + " converting...");
            setStatus(cur + "/" + total + " converting...");
        });
        setStatus("Done");
    }

    private void runConvStore(Bundle ext) {
        if (ext == null) return;
        int slot = ext.getInt("slot", -1);
        String mode = ext.getString("mode", "none");
        if (slot < 0 || slot > 4) return;

        setStatus("Converting storage...");

        Core.convStorage(this, slot, mode, (cur, total) -> {
            SVCC1.getChan().SetInt(0, cur);
            SVCC1.getChan().SetInt(1, total);
            pushNotif(cur + "/" + total + " converting...");
            setStatus(cur + "/" + total + " converting...");
        });
        setStatus("Done");
    }

    private void runExportZip(Bundle ext) {
        if (ext == null) return;
        int slot = ext.getInt("slot", -1);
        if (slot < 0 || slot > 4) return;

        setStatus("Exporting ZIP...");

        Core.exportZip(this, slot, (cur, total) -> {
            SVCC1.getChan().SetInt(0, cur);
            SVCC1.getChan().SetInt(1, total);
            pushNotif(cur + "/" + total + " zipping...");
            setStatus(cur + "/" + total + " zipping...");
        });
        setStatus("Done");
    }

    // Create notification channel.
    private void initChannel() {
        NotificationChannel ch = new NotificationChannel(
                CH_ID, "Convert Task", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Image conversion progress");
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    private Notification buildNotif(String text) {
        return new Notification.Builder(this, CH_ID)
                .setContentTitle("eBookificate")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setOngoing(true)
                .build();
    }

    private void pushNotif(String text) {
        getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotif(text));
    }

    private void setStatus(String msg) {
        SVCC1.getChan().SetString(0, msg);
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (worker != null) worker.shutdown();
    }
}
