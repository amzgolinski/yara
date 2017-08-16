package com.amzgolinski.yara.util;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.amzgolinski.yara.R;
import com.amzgolinski.yara.service.YaraUtilityService;

public class AndroidUtils {

  private static final String LOG_TAG = AndroidUtils.class.getName();

  public static int convertDpToPixels(Context context, int dp) {
      return (int) (context.getResources().getDisplayMetrics().density * dp + 0.5f);
  }

  public static boolean isCursorEmpty(Cursor data) {
    return (data == null || !data.moveToNext());
  }

  public static boolean isNetworkAvailable(Context ctx) {
    ConnectivityManager cm =
        (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  public static void handleError(Context context, String message) {

    if (message.equals(YaraUtilityService.STATUS_NETWORK_EXCEPTION)) {
      AndroidUtils.showToast(context, context.getString(R.string.error_network_exception));
    } else if (message.equals(YaraUtilityService.STATUS_NO_INTERNET)) {
      AndroidUtils.showToast(context, context.getString(R.string.error_no_internet));
    } else if (message.equals(YaraUtilityService.STATUS_API_EXCEPTION)) {
      AndroidUtils.showToast(context, context.getString(R.string.error_api_exception));
    } else if (message.equals(YaraUtilityService.STATUS_AUTH_EXCEPTION)) {
      AndroidUtils.showToast(context, context.getString(R.string.error_auth_exception));
    }
  }

  public static void showToast(Context context, String message) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
  }

}

