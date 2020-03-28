package com.newmoon.dark.view.timepickerview;

final class OnItemSelectedRunnable implements Runnable {
    final WheelView loopView;

    OnItemSelectedRunnable(WheelView loopView) {
        this.loopView = loopView;
    }

    @Override
    public final void run() {
        loopView.onItemSelectedListener.onItemSelected(loopView.getCurrentItem());
    }
}
