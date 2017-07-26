package com.amzgolinski.yara.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.amzgolinski.yara.callbacks.AccountRetrievedCallback;
import com.amzgolinski.yara.service.YaraUtilityService;
import com.amzgolinski.yara.util.Utils;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.models.LoggedInAccount;

public class FetchLoggedInAccountTask extends AsyncTask<Void, Void, LoggedInAccount> {

  private static final String LOG_TAG = FetchLoggedInAccountTask.class.getName();

  private Context mContext;
  private AccountRetrievedCallback mCallback;
  private String mMessage;

  public FetchLoggedInAccountTask(Context context, AccountRetrievedCallback callback) {
    mContext = context;
    mCallback = callback;

  }

  @Override
  protected LoggedInAccount doInBackground(Void... params) {
    mMessage = YaraUtilityService.STATUS_OK;
    LoggedInAccount account = null;
    if (!Utils.isNetworkAvailable(mContext)) {
      mMessage = YaraUtilityService.STATUS_NO_INTERNET;
      return null;
    }
    try {
      account = AuthenticationManager.get().getRedditClient().me();
      Utils.setCurrentUser(mContext, account.getFullName());
    } catch (NetworkException networkException) {
      Log.e(LOG_TAG, "NetworkException", networkException);
      mMessage = YaraUtilityService.STATUS_AUTH_EXCEPTION;
    }
    return account;
  }

  @Override
  protected void onPostExecute(LoggedInAccount account) {
    Log.d(LOG_TAG, mMessage);
    mCallback.onAccountRetrieved(account, mMessage);
  }

}
