package com.newmoon.dark.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class HomeKeyWatcher {

    private Context mContext;
    private IntentFilter mFilter;
    private OnHomePressedListener mListener;
    private HomeKeyReceiver mReceiver;
    private boolean isRegistered = false;

    public interface OnHomePressedListener {
        void onHomePressed();
        void onRecentsPressed();
    }

    public HomeKeyWatcher(Context context) {
        mContext = context;
        mFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
    }

    public void setOnHomePressedListener(OnHomePressedListener listener) {
        mListener = listener;
        mReceiver = new HomeKeyReceiver();
    }

    public void startWatch() {
        if (mReceiver != null && !isRegistered) {
            isRegistered = true;
            BroadcastCenter.register(mContext, mReceiver, mFilter);
        }
    }

    public void stopWatch() {
        if (mReceiver != null && isRegistered) {
            isRegistered = false;
            BroadcastCenter.unregister(mContext, mReceiver);
        }
        mListener = null;
        mReceiver = null;
    }

    class HomeKeyReceiver implements BroadcastListener {
        final String SYSTEM_DIALOG_REASON_KEY = "reason";
        final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (reason != null) {
                    if (mListener != null) {
                        if (reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
                            mListener.onHomePressed();
                        } else if (reason.equals(SYSTEM_DIALOG_REASON_RECENT_APPS)) {
                            mListener.onRecentsPressed();
                        }
                    }
                }
            }
        }
    }
}
