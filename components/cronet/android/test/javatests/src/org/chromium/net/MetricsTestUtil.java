// Copyright 2016 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net;

import static java.time.temporal.ChronoUnit.MILLIS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.net.http.RequestFinishedInfo;

import java.time.Instant;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;

/**
 * Classes which are useful for testing Cronet's metrics implementation and are needed in more than
 * one test file.
 */
public class MetricsTestUtil {
    /**
     * Executor which runs tasks only when told to with runAllTasks().
     */
    public static class TestExecutor implements Executor {
        private final LinkedList<Runnable> mTaskQueue = new LinkedList<Runnable>();

        @Override
        public void execute(Runnable task) {
            mTaskQueue.add(task);
        }

        public void runAllTasks() {
            try {
                while (mTaskQueue.size() > 0) {
                    mTaskQueue.remove().run();
                }
            } catch (NoSuchElementException e) {
                throw new RuntimeException("Task was removed during iteration", e);
            }
        }
    }

    // Helper method to assert date1 is equals to or after date2.
    // Truncate to MILLIS because CronetMetrics are in MILLIS.
    public static void assertAfter(Instant date1, Instant date2) {
        Instant date1Ms = date1.truncatedTo(MILLIS);
        Instant date2Ms = date2.truncatedTo(MILLIS);
        assertTrue("date1: " + date1 + ", date2: " + date2Ms,
                date1Ms.isAfter(date2Ms) || date1Ms.equals(date2Ms));
    }

    /**
     * Check existence of all the timing metrics that apply to most test requests,
     * except those that come from net::LoadTimingInfo::ConnectTiming.
     * Also check some timing differences, focusing on things we can't check with asserts in the
     * CronetMetrics constructor.
     * Don't check push times here.
     */
    public static void checkTimingMetrics(
            RequestFinishedInfo.Metrics metrics, Instant startTime, Instant endTime) {
        assertNotNull(metrics.getRequestStart());
        assertAfter(metrics.getRequestStart(), startTime);
        assertNotNull(metrics.getSendingStart());
        assertAfter(metrics.getSendingStart(), startTime);
        assertNotNull(metrics.getSendingEnd());
        assertAfter(endTime, metrics.getSendingEnd());
        assertNotNull(metrics.getResponseStart());
        assertAfter(metrics.getResponseStart(), startTime);
        assertNotNull(metrics.getRequestEnd());
        assertAfter(endTime, metrics.getRequestEnd());
        assertAfter(metrics.getRequestEnd(), metrics.getRequestStart());
    }

    /**
     * Check that the timing metrics which come from net::LoadTimingInfo::ConnectTiming exist,
     * except SSL times in the case of non-https requests.
     */
    public static void checkHasConnectTiming(
            RequestFinishedInfo.Metrics metrics, Instant startTime, Instant endTime, boolean isSsl) {
        assertNotNull(metrics.getDnsStart());
        assertAfter(metrics.getDnsStart(), startTime);
        assertNotNull(metrics.getDnsEnd());
        assertAfter(endTime, metrics.getDnsEnd());
        assertNotNull(metrics.getConnectStart());
        assertAfter(metrics.getConnectStart(), startTime);
        assertNotNull(metrics.getConnectEnd());
        assertAfter(endTime, metrics.getConnectEnd());
        if (isSsl) {
            assertNotNull(metrics.getSslStart());
            assertAfter(metrics.getSslStart(), startTime);
            assertNotNull(metrics.getSslEnd());
            assertAfter(endTime, metrics.getSslEnd());
        } else {
            assertNull(metrics.getSslStart());
            assertNull(metrics.getSslEnd());
        }
    }

    /**
     * Check that the timing metrics from net::LoadTimingInfo::ConnectTiming don't exist.
     */
    public static void checkNoConnectTiming(RequestFinishedInfo.Metrics metrics) {
        assertNull(metrics.getDnsStart());
        assertNull(metrics.getDnsEnd());
        assertNull(metrics.getSslStart());
        assertNull(metrics.getSslEnd());
        assertNull(metrics.getConnectStart());
        assertNull(metrics.getConnectEnd());
    }

    /**
     * Check that RequestFinishedInfo looks the way it should look for a normal successful request.
     */
    public static void checkRequestFinishedInfo(
            RequestFinishedInfo info, String url, Instant startTime, Instant endTime) {
        assertNotNull("RequestFinishedInfo.Listener must be called", info);
        assertEquals(url, info.getUrl());
        assertNotNull(info.getResponseInfo());
        assertNull(info.getException());
        RequestFinishedInfo.Metrics metrics = info.getMetrics();
        assertNotNull("RequestFinishedInfo.getMetrics() must not be null", metrics);
        // Check new timing metrics
        checkTimingMetrics(metrics, startTime, endTime);
        assertNull(metrics.getPushStart());
        assertNull(metrics.getPushEnd());
        // Check data use metrics
        assertTrue(metrics.getSentByteCount() > 0);
        assertTrue(metrics.getReceivedByteCount() > 0);
    }
}
