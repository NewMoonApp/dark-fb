package com.newmoon.dark.util;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public interface BroadcastListener {

    void onReceive(Context context, Intent intent);
}
