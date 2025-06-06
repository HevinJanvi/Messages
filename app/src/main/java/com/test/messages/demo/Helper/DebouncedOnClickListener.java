package com.test.messages.demo.Helper;

import android.os.SystemClock;
import android.view.View;

public abstract class DebouncedOnClickListener implements View.OnClickListener {

    private final long minimumIntervalMillis;
    private long lastCLick;
    public abstract void onDebouncedClick(View v);
    public DebouncedOnClickListener(long minimumIntervalMillis) {
        this.minimumIntervalMillis = minimumIntervalMillis;
        this.lastCLick = 0L;
    }

    @Override
    public void onClick(View clickedView) {
        long currentTimestamp = SystemClock.uptimeMillis();

        if(Math.abs(currentTimestamp - lastCLick) > minimumIntervalMillis) {
            lastCLick = currentTimestamp;
            onDebouncedClick(clickedView);
        }
    }
}