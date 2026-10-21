package dev.zaidan.igglass;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import de.robv.android.xposed.XposedBridge;

/**
 * Privacy-minimal, asynchronous diagnostics for the experimental LSPosed module.
 * API 29+ MediaStore Downloads supports creating app-owned items with no
 * READ/WRITE_EXTERNAL_STORAGE and no MANAGE_EXTERNAL_STORAGE permission.
 *
 * Since this hook runs in Instagram's process, MediaStore owns the item under
 * Instagram's UID. User can open it with Downloads / a file manager.
 * This logger never reads or logs credentials, account labels or feed content.
 */
final class DevFileLog {
    private static final Object LOCK = new Object();
    private static final int MAX_BYTES = 512 * 1024;
    private static Handler worker;
    private static Context appContext;
    private static Uri outputUri;
    private static String displayName;
    private static boolean creationFailed;
    private static int bytesWritten;
    private static final StringBuilder pending = new StringBuilder();

    private DevFileLog() {}

    static void start(Context context) {
        if (context == null) return;
        synchronized (LOCK) {
            if (worker != null) return;
            appContext = context.getApplicationContext();
            HandlerThread thread = new HandlerThread("IGGlass-dev-log");
            thread.start();
            worker = new Handler(thread.getLooper());
            displayName = "IGGlass-0.1-dev-"
                    + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                            .format(new Date()) + ".txt";
        }
        note("SESSION START; Android=" + Build.VERSION.SDK_INT
                + "; module=0.1-dev; Insta452; diagnostics do not include personal data");
    }

    static void note(String message) {
        String clean = message == null ? "(null)" : message.replace('\n', ' ')
                .replace('\r', ' ');
        if (clean.length() > 350) clean = clean.substring(0, 350);
        XposedBridge.log("IGGlass-DEV: " + clean);
        Handler handler;
        synchronized (LOCK) {
            if (bytesWritten >= MAX_BYTES) return;
            String t = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
                    .format(new Date());
            pending.append(t).append(' ').append(clean).append('\n');
            handler = worker;
        }
        if (handler != null) handler.post(DevFileLog::flush);
    }

    private static void flush() {
        final String lines;
        synchronized (LOCK) {
            if (pending.length() == 0 || creationFailed
                    || bytesWritten >= MAX_BYTES) return;
            lines = pending.toString();
            pending.setLength(0);
        }
        try {
            if (outputUri == null) createFile();
            if (outputUri == null) return;
            byte[] data = lines.getBytes(StandardCharsets.UTF_8);
            if (bytesWritten + data.length > MAX_BYTES) {
                data = ("[IGGlass: 512 KiB log limit reached]\n")
                        .getBytes(StandardCharsets.UTF_8);
            }
            ContentResolver resolver = appContext.getContentResolver();
            try (OutputStream out = resolver.openOutputStream(outputUri, "wa")) {
                if (out == null) throw new IllegalStateException("null output stream");
                out.write(data);
                out.flush();
            }
            bytesWritten += data.length;
        } catch (Throwable e) {
            synchronized (LOCK) { creationFailed = true; }
            XposedBridge.log("IGGlass-DEV: DOWNLOAD LOG ERROR "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void createFile() {
        if (appContext == null || creationFailed) return;
        if (Build.VERSION.SDK_INT < 29) {
            creationFailed = true;
            XposedBridge.log("IGGlass-DEV: Downloads logger needs Android 10+");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/");
        outputUri = appContext.getContentResolver().insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (outputUri == null) {
            creationFailed = true;
            XposedBridge.log("IGGlass-DEV: Downloads insert returned null");
        } else {
            XposedBridge.log("IGGlass-DEV: log file created in Download/" + displayName);
        }
    }
}
