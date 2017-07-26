package com.amzgolinski.yara.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.amzgolinski.yara.YaraApplication;
import com.amzgolinski.yara.callbacks.AccountRetrievedCallback;
import com.amzgolinski.yara.service.YaraUtilityService;
import com.amzgolinski.yara.util.Utils;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.models.LoggedInAccount;


public class RefreshAccessTokenTask extends AsyncTask<Void, Void, LoggedInAccount> {

  private static final String LOG_TAG = RefreshAccessTokenTask.class.getName();

  private Context mContext;
  private AccountRetrievedCallback mCallback;
  private String mMessage;

  public RefreshAccessTokenTask(Context context, AccountRetrievedCallback callback) {
    mContext = context;
    mCallback = callback;
    mMessage = YaraUtilityService.STATUS_OK;
  }

  @Override
  protected LoggedInAccount doInBackground(Void... params) {
    LoggedInAccount account = null;

    if (!Utils.isNetworkAvailable(mContext)) {
      mMessage = YaraUtilityService.STATUS_NO_INTERNET;
      return null;
    }

    try {
      AuthenticationManager.get().refreshAccessToken(YaraApplication.CREDENTIALS);
      AuthenticationManager.get().getRedditClient().getAuthenticatedUser();
      AuthenticationManager.get().getRedditClient().getOAuthHelper().getRefreshToken();
      account = AuthenticationManager.get().getRedditClient().me();
      Log.d(LOG_TAG, "reauthenticated");

    } catch (NoSuchTokenException | OAuthException e) {
      Log.e(LOG_TAG, "Could not refresh access token", e);
      mMessage = YaraUtilityService.STATUS_AUTH_EXCEPTION;
    }
    return account;
  }

  @Override
  protected void onPostExecute(LoggedInAccount account) {
    Log.d(LOG_TAG, "onPostExecute");
    Utils.clearAuthRefreshStatus(mContext);
    mCallback.onAccountRetrieved(account, mMessage);
  }
}
