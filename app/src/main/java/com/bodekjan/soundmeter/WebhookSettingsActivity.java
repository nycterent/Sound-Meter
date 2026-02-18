package com.bodekjan.soundmeter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class WebhookSettingsActivity extends Activity {

    static final String PREFS_NAME = "webhook_prefs";
    static final String PREF_URL = "webhook_url";
    static final String PREF_INTERVAL = "webhook_interval";
    static final int DEFAULT_INTERVAL = 10;

    private EditText editUrl;
    private Spinner spinnerInterval;

    private static final int[] INTERVALS = {5, 10, 30, 60};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webhook_settings);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle(getString(R.string.webhook_title));
        }

        editUrl = findViewById(R.id.editWebhookUrl);
        spinnerInterval = findViewById(R.id.spinnerInterval);
        Button btnSave = findViewById(R.id.btnSaveWebhook);
        Button btnClear = findViewById(R.id.btnClearWebhook);

        // Populate interval spinner
        String[] labels = new String[INTERVALS.length];
        for (int i = 0; i < INTERVALS.length; i++) {
            labels[i] = INTERVALS[i] + " " + getString(R.string.webhook_seconds);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerInterval.setAdapter(adapter);

        // Load saved values
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUrl = prefs.getString(PREF_URL, "");
        int savedInterval = prefs.getInt(PREF_INTERVAL, DEFAULT_INTERVAL);
        editUrl.setText(savedUrl);
        spinnerInterval.setSelection(intervalIndex(savedInterval));

        btnSave.setOnClickListener(v -> {
            String url = editUrl.getText().toString().trim();
            if (!url.isEmpty() && !url.startsWith("https://")) {
                Toast.makeText(this, getString(R.string.webhook_invalid_url), Toast.LENGTH_SHORT).show();
                return;
            }
            int interval = INTERVALS[spinnerInterval.getSelectedItemPosition()];
            prefs.edit()
                    .putString(PREF_URL, url)
                    .putInt(PREF_INTERVAL, interval)
                    .apply();
            Toast.makeText(this, getString(R.string.webhook_saved), Toast.LENGTH_SHORT).show();
            finish();
        });

        btnClear.setOnClickListener(v -> {
            prefs.edit().remove(PREF_URL).remove(PREF_INTERVAL).apply();
            editUrl.setText("");
            spinnerInterval.setSelection(intervalIndex(DEFAULT_INTERVAL));
            Toast.makeText(this, getString(R.string.webhook_cleared), Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static int intervalIndex(int seconds) {
        // Try to find an exact match for the requested interval.
        for (int i = 0; i < INTERVALS.length; i++) {
            if (INTERVALS[i] == seconds) {
                return i;
            }
        }

        // If no exact match, fall back to the index of DEFAULT_INTERVAL if present.
        for (int i = 0; i < INTERVALS.length; i++) {
            if (INTERVALS[i] == DEFAULT_INTERVAL) {
                return i;
            }
        }

        // As a last resort, fall back to the first interval.
        return 0;
    }
}
