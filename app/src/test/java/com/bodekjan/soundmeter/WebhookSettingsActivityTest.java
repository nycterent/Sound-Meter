package com.bodekjan.soundmeter;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for WebhookSettingsActivity helper methods.
 */
public class WebhookSettingsActivityTest {

    @Test
    public void intervalIndex_exactMatch5() {
        assertEquals(0, WebhookSettingsActivity.intervalIndex(5));
    }

    @Test
    public void intervalIndex_exactMatch10() {
        assertEquals(1, WebhookSettingsActivity.intervalIndex(10));
    }

    @Test
    public void intervalIndex_exactMatch30() {
        assertEquals(2, WebhookSettingsActivity.intervalIndex(30));
    }

    @Test
    public void intervalIndex_exactMatch60() {
        assertEquals(3, WebhookSettingsActivity.intervalIndex(60));
    }

    @Test
    public void intervalIndex_unknownFallsBackToDefault() {
        // Unknown value should fall back to DEFAULT_INTERVAL (10) index
        assertEquals(1, WebhookSettingsActivity.intervalIndex(99));
    }

    @Test
    public void intervalIndex_zeroFallsBackToDefault() {
        assertEquals(1, WebhookSettingsActivity.intervalIndex(0));
    }

    @Test
    public void intervalIndex_negativeFallsBackToDefault() {
        assertEquals(1, WebhookSettingsActivity.intervalIndex(-1));
    }
}
