package com.newmoon.dark.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A helper class that registers broadcasts to system AMS on behalf of application logic and
 * calls listeners with in-process direct method calls. This class is to reduce number of
 * receivers directly registered on AMS that introduce a lot of inter-process traffic.
 */
@SuppressWarnings("unused")
public class BroadcastCenter {

    public static final String ACTION_UNORDERED_SCREEN_ON = "unordered_screen_on";
    public static final String ACTION_UNORDERED_SCREEN_OFF = "unordered_screen_off";

    private static final String TAG = BroadcastCenter.class.getSimpleName();

    private static final HandlerThread sHandlerThread = new HandlerThread("BroadcastCenter");

    private static final Object sLock = new Object();
    private static final MultiHashMap<String, BroadcastListener> sListeners = new MultiHashMap<>();
    private static final HashMap<String, BroadcastReceiver> sSystemReceivers = new HashMap<>();

    static {
        sHandlerThread.start();
    }

    public static void register(Context context, BroadcastListener listener, IntentFilter filter) {
        int actionCount = filter.countActions();
        int dataSchemesCount = filter.countDataSchemes();
        String[] dataSchemes = new String[dataSchemesCount];
        for (int i = 0; i < dataSchemesCount; i++) {
            dataSchemes[i] = filter.getDataScheme(i);
        }
        synchronized (sLock) {
            for (int i = 0; i < actionCount; i++) {
                String action = filter.getAction(i);
                int numListenersForThisAction = sListeners.putToList(action, listener);
                Log.d(TAG, "Register, listener count for action " + action
                        + ": " + numListenersForThisAction);
                if (numListenersForThisAction == 1) {
                    registerReceiverLocked(context, action, dataSchemes);
                }
            }
        }
    }

    public static void unregister(Context context, BroadcastListener listenerToRemove) {
        synchronized (sLock) {
            Set<Map.Entry<String, List<BroadcastListener>>> listenerEntries = sListeners.entrySet();
            for (Map.Entry<String, List<BroadcastListener>> entryForAnAction : listenerEntries) {
                String action = entryForAnAction.getKey();
                List<BroadcastListener> listeners = entryForAnAction.getValue();
                for (Iterator<BroadcastListener> iterator = listeners.iterator();
                     iterator.hasNext(); ) {
                    BroadcastListener listener = iterator.next();
                    if (listener == listenerToRemove) {
                        iterator.remove();
                    }
                }
                int numListeners = listeners.size();
                Log.d(TAG, "Unregister, listener count for action " + action
                        + ": " + numListeners);
                if (numListeners == 0) {
                    unregisterReceiverLocked(context, action);
                }
            }
        }
    }

    private static void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!TextUtils.isEmpty(action)) {
            List<BroadcastListener> listenersCopy = null;
            synchronized (sLock) {
                List<BroadcastListener> listeners = sListeners.get(action);
                if (listeners != null && !listeners.isEmpty()) {
                    Log.i(TAG, "Broadcasting " + action + " to " + listeners.size() + " listener(s)");

                    // Make a copy in case the listener unregisters itself in onReceive
                    listenersCopy = new ArrayList<>(listeners);
                }
            }
            if (listenersCopy != null) {
                for (BroadcastListener listener : listenersCopy) {
                    if (listener != null) {
                        listener.onReceive(context, intent);
                    }
                }
            }
        } else {
            Log.w(TAG, "Broadcast with no action");
        }
    }

    private static void registerReceiverLocked(Context context, String action, String[] dataSchemes) {
        BroadcastReceiver systemReceiver = new PostOnNextFrameReceiver() {
            @Override
            protected void onPostReceive(Context context, Intent intent) {
                if (intent == null) {
                    Log.i(TAG, "onPostReceive: intent is null.");
                    return;
                }
                BroadcastCenter.onReceive(context, intent);
            }
        };
        sSystemReceivers.put(action, systemReceiver);
        Log.i(TAG, "Register system receiver for action " + action
                + ", system receiver count: " + sSystemReceivers.size());
        IntentFilter intentFilter = new IntentFilter(action);
        for (int i = 0; i < dataSchemes.length; i++) {
            intentFilter.addDataScheme(dataSchemes[i]);
        }

        registerReceiverSafely(context, systemReceiver, intentFilter);
    }

    private static void unregisterReceiverLocked(Context context, String action) {
        BroadcastReceiver systemReceiver = sSystemReceivers.remove(action);
        Log.i(TAG, "Unregister system receiver for action " + action
                + ", system receiver count: " + sSystemReceivers.size());
        unregisterReceiverSafely(context, systemReceiver);
    }

    private static void registerReceiverSafely(Context context,
                                               BroadcastReceiver receiver, IntentFilter filter) {
        try {
            String action = filter.getAction(0);
            Log.d(TAG, "Registering action: " + action);
            if (TextUtils.equals(action, Intent.ACTION_SCREEN_ON) || TextUtils.equals(action, Intent.ACTION_SCREEN_OFF)) {
                context.registerReceiver(receiver, filter, null, new Handler(sHandlerThread.getLooper()));
            } else {
                context.registerReceiver(receiver, filter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering broadcast receiver: " + receiver);
            e.printStackTrace();
        }
    }

    private static void unregisterReceiverSafely(Context context, BroadcastReceiver receiver) {
        if (receiver == null) {
            return;
        }
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering broadcast receiver: " + receiver);
            e.printStackTrace();
        }
    }
}
