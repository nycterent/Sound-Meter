package com.bodekjan.soundmeter;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages periodic HTTP webhook POSTs with exponential-backoff retry.
 * A single-threaded ExecutorService runs network calls off the main thread.
 * All scheduling and state updates happen on the main thread via Handler.
 * The next tick is only scheduled AFTER the current POST returns, so
 * there can never be overlapping requests.
 */
public class WebhookManager {

    private static final String TAG = "WebhookManager";

    public interface StatusListener {
        /** Called on the main thread after every attempt. */
        void onStatusChanged(boolean success);
    }

    private static final int MAX_RETRY_DELAY_MS = 60_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private StatusListener statusListener;

    private volatile String webhookUrl;
    private volatile int intervalSeconds;
    private volatile boolean running = false;
    private int retryDelayMs = 0;

    /** Kicks off a single POST, then schedules the next one when done. */
    private final Runnable postRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            final String url = webhookUrl;
            final String body = buildPayload();
            executor.execute(() -> {
                boolean ok = sendPost(url, body);
                handler.post(() -> {
                    if (!running) return;
                    if (ok) {
                        retryDelayMs = 0;
                    } else {
                        // exponential backoff: 2s, 4s, 8s … capped at MAX_RETRY_DELAY_MS
                        retryDelayMs = retryDelayMs == 0
                                ? 2_000
                                : Math.min(retryDelayMs * 2, MAX_RETRY_DELAY_MS);
                    }
                    if (statusListener != null) {
                        statusListener.onStatusChanged(ok);
                    }
                    // Schedule next tick only after this one finished
                    int nextDelay = retryDelayMs > 0 ? retryDelayMs : intervalSeconds * 1000;
                    handler.postDelayed(postRunnable, nextDelay);
                });
            });
        }
    };

    public void setStatusListener(StatusListener listener) {
        this.statusListener = listener;
    }

    public void start(String url, int intervalSecs) {
        if (url == null || url.isEmpty()) return;
        stop(); // clear any previous run
        this.webhookUrl = url;
        this.intervalSeconds = intervalSecs;
        this.retryDelayMs = 0;
        this.running = true;
        handler.post(postRunnable);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(postRunnable);
    }

    /**
     * Fully shuts down this manager and its background executor.
     * Call this when the owning component (e.g., Activity/Service) is destroyed.
     */
    public void shutdown() {
        stop();
        executor.shutdownNow();
    }

    public boolean isRunning() {
        return running;
    }

    // -----------------------------------------------------------------------

    String buildPayload() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date());
        String rawModel = (Build.MODEL != null) ? Build.MODEL.toLowerCase(Locale.US) : "unknown";
        String deviceId = rawModel.replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "");

        try {
            JSONObject json = new JSONObject();
            json.put("db_level", Math.round(World.dbCount * 10.0) / 10.0);
            json.put("db_max", Math.round(World.maxDB * 10.0) / 10.0);
            json.put("db_min", Math.round(World.minDB * 10.0) / 10.0);
            json.put("timestamp", timestamp);
            json.put("device_id", deviceId);
            return json.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to build JSON payload", e);
            return "{}";
        }
    }

    boolean sendPost(String urlString, String jsonBody) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setDoOutput(true);

            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            int code = conn.getResponseCode();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            Log.w(TAG, "Webhook POST failed: " + e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
