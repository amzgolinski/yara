package com.amzgolinski.yara.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.amzgolinski.yara.R;
import com.amzgolinski.yara.callbacks.AccountRetrievedCallback;
import com.amzgolinski.yara.service.YaraUtilityService;
import com.amzgolinski.yara.sync.SubredditSyncAdapter;
import com.amzgolinski.yara.tasks.FetchLoggedInAccountTask;
import com.amzgolinski.yara.tasks.RefreshAccessTokenTask;

import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

public class Utils {

  public static final String EMPTY_STRING = "";
  public static final int UPVOTE = 1;
  public static final int NOVOTE = 0;
  public static final int DOWNVOTE = -1;

  private static final String LOG_TAG = Utils.class.getName();
  private static final String SYNC_IN_PROGRESS = "sync_in_progress";

  public static int convertDpToPixels(Context context, int dp) {
      return (int) (context.getResources().getDisplayMetrics().density * dp + 0.5f);
  }

  public static void logOutCurrentUser(Context context) {
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(context);

    SharedPreferences.Editor editor = prefs.edit();
    editor.remove(context.getString(R.string.current_user_key));
    editor.apply();
  }

  public static VoteDirection getVote(int currentVote, int newVote) {
    VoteDirection toReturn = VoteDirection.NO_VOTE;

    if (newVote > currentVote ) {
      toReturn = VoteDirection.UPVOTE;
    } else if (newVote < currentVote) {
      toReturn = VoteDirection.DOWNVOTE;
    }
    Log.d(LOG_TAG, toReturn.toString());
    return toReturn;
  }

  public static boolean isCursorEmpty(Cursor data) {
    return (data == null || !data.moveToNext());
  }

  public static String getCurrentUser(Context context) {
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(context);
    String currentUser
        = prefs.getString(context.getString(R.string.current_user_key), EMPTY_STRING);
    return currentUser;
  }

  public static boolean isLoggedIn(Context context) {
    return !Utils.getCurrentUser(context).equals(EMPTY_STRING);
  }

  public static boolean isMarkedNsfw(String toCheck) {
    return toCheck.toLowerCase().contains("nsfw");
  }

  public static boolean isNetworkAvailable(Context ctx) {
    ConnectivityManager cm =
        (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  public static boolean isStringEmpty(String toTest) {
    return (toTest == null || toTest.equals(EMPTY_STRING));
  }

  public static boolean isSubmissionReadOnly(Submission submission) {
    return (submission.isArchived() || submission.isLocked());
  }

  public static boolean isValidSubmission(Submission submission) {
    return (
        (!submission.isNsfw()) ||
            (Utils.isMarkedNsfw(submission.getTitle())) ||
            (!submission.isHidden()) ||
            (!submission.isStickied())
    );
  }

  @SuppressWarnings("ResourceType")
  public static
  int
  getSyncStatus(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    return prefs.getInt(
        context.getString(R.string.sync_status_key), SubredditSyncAdapter.LOCATION_STATUS_UNKNOWN);
  }

  public static String longToRedditId(long id) {
    return Long.toString(id, 36);
  }

  public static void setCurrentUser(Context context, String username) {
    Log.d(LOG_TAG, "Current user: " + username);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(context.getString(R.string.current_user_key), username);
    editor.apply();
  }

  public static long redditIdToLong(String redditId) {
    return Long.parseLong(redditId, 36);
  }

  public static long redditParentIdToLong(String parentId) {
    return Long.parseLong(parentId.substring(3), 36);
  }

  public static String removeHtmlSpacing(String html) {
    html = html.replace("<div class=\"md\">", "");
    html = html.replace("</div>", "");
    html = html.replace("<p>", "");
    html = html.replace("</p>", "");
    return html;
  }

  public static void handleError(Context context, String message) {

    if (message.equals(YaraUtilityService.STATUS_NETWORK_EXCEPTION)) {
      Utils.showToast(context, context.getString(R.string.error_network_exception));
    } else if (message.equals(YaraUtilityService.STATUS_NO_INTERNET)) {
      Utils.showToast(context, context.getString(R.string.error_no_internet));
    } else if (message.equals(YaraUtilityService.STATUS_API_EXCEPTION)) {
      Utils.showToast(context, context.getString(R.string.error_api_exception));
    } else if (message.equals(YaraUtilityService.STATUS_AUTH_EXCEPTION)) {
      Utils.showToast(context, context.getString(R.string.error_auth_exception));
    }
  }

  public static void showToast(Context context, String message) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
  }

  public static void updateAuth(Context context, AuthenticationState state,
                                AccountRetrievedCallback callback) {
    switch (state) {
      case READY:
        new FetchLoggedInAccountTask(context, callback).execute();
        break;
      case NEED_REFRESH:
        if (!Utils.isRefreshing(context)) {
          new RefreshAccessTokenTask(context, callback).execute();
          setAuthRefreshStatus(context);
        }
        break;
      case NONE:
        Log.d(LOG_TAG, state.toString());
        break;
    }
  }

  public static void setAuthRefreshStatus(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(context.getString(R.string.refresh_status_key), true);
    editor.commit();
  }

  public static boolean isRefreshing(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean refreshing = prefs.getBoolean(context.getString(R.string.refresh_status_key), false);
    return refreshing;
  }

  public static void clearAuthRefreshStatus(Context context) {
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(context);

    SharedPreferences.Editor editor = prefs.edit();
    editor.remove(context.getString(R.string.refresh_status_key));
    editor.apply();
  }

}

