package com.amzgolinski.yara.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SubredditSyncService extends Service {
  private static final String LOG_TAG = SubredditSyncService.class.getName();

  private static final Object sSyncAdapterLock = new Object();
  private static SubredditSyncAdapter sSunshineSyncAdapter = null;

  public SubredditSyncService() {

  }

  @Override
  public void onCreate(){
    Log.d(LOG_TAG, "onCreate");
    synchronized (sSyncAdapterLock) {
      if (sSunshineSyncAdapter == null) {
        sSunshineSyncAdapter =
            new SubredditSyncAdapter(getApplicationContext(), true);
      }
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return sSunshineSyncAdapter.getSyncAdapterBinder();
  }
}
