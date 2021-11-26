package com.example.cast.client.broad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.example.cast.client.IConnectListener;

public class UIBroadReceiver extends BroadcastReceiver {

    public final static String UI_UPDATA = "android.ui.updata.NOTIFACATION";
    private IConnectListener mListener;

    public UIBroadReceiver(IConnectListener listener) {
        this.mListener = listener;
    }

    public IntentFilter getFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(UI_UPDATA);
        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isUpdate = intent.getBooleanExtra("isUpdate",false);
        mListener.onStatus(isUpdate);
    }

}
