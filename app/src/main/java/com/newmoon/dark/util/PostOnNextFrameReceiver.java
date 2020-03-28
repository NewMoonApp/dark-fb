package com.newmoon.dark.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.newmoon.common.util.Threads;

public abstract class PostOnNextFrameReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Threads.postOnMainThread(new Runnable() {
            @Override
            public void run() {
                PostOnNextFrameReceiver.this.onPostReceive(context, intent);
            }
        });
    }

    protected abstract void onPostReceive(Context context, Intent intent);
}
