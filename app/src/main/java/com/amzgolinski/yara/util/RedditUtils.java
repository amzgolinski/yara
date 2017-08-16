package com.amzgolinski.yara.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.amzgolinski.yara.R;
import com.amzgolinski.yara.callbacks.AccountRetrievedCallback;
import com.amzgolinski.yara.sync.SubredditSyncAdapter;
import com.amzgolinski.yara.tasks.FetchLoggedInAccountTask;
import com.amzgolinski.yara.tasks.RefreshAccessTokenTask;

import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

public class RedditUtils {

  private static final String LOG_TAG = AndroidUtils.class.getName();

  public static final int UPVOTE = 1;
  public static final int NOVOTE = 0;
  public static final int DOWNVOTE = -1;

  private static final String NSFW = "nsfw";

  public static void clearAuthRefreshStatus(Context context) {
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.remove(context.getString(R.string.refresh_status_key));
    editor.apply();
  }

  public static String getCurrentUser(Context context) {
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(context);
    String currentUser
        = prefs.getString(context.getString(R.string.current_user_key), StringUtils.EMPTY_STRING);
    return currentUser;
  }

  @SuppressWarnings("ResourceType")
  public static
  int
  getSyncStatus(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    return prefs.getInt(
        context.getString(R.string.sync_status_key), SubredditSyncAdapter.LOCATION_STATUS_UNKNOWN);
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

  public static boolean isAuthRefreshing(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean refreshing = prefs.getBoolean(context.getString(R.string.refresh_status_key), false);
    return refreshing;
  }

  public static boolean isLoggedIn(Context context) {
    return !RedditUtils.getCurrentUser(context).equals(StringUtils.EMPTY_STRING);
  }

  static boolean isMarkedNsfw(String toCheck) {
    return toCheck.toLowerCase().contains(NSFW);
  }

  public static boolean isSubmissionReadOnly(Submission submission) {
    return (submission.isArchived() || submission.isLocked());
  }

  public static boolean isValidSubmission(Submission submission) {
    return (
        (!submission.isNsfw()) ||
            (RedditUtils.isMarkedNsfw(submission.getTitle())) ||
            (!submission.isHidden()) ||
            (!submission.isStickied())
    );
  }

  public static void logOutCurrentUser(Context context) {
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.remove(context.getString(R.string.current_user_key));
    editor.apply();
  }

  public static String longToRedditId(long id) {
    return Long.toString(id, 36);
  }

  public static long redditIdToLong(String redditId) {
    return Long.parseLong(redditId, 36);
  }

  public static long redditParentIdToLong(String parentId) {
    return Long.parseLong(parentId.substring(3), 36);
  }

  public static void setAuthRefreshStatus(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(context.getString(R.string.refresh_status_key), true);
    editor.commit();
  }

  public static void setCurrentUser(Context context, String username) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(context.getString(R.string.current_user_key), username);
    editor.apply();
  }

  public static void updateAuth(Context context, AuthenticationState state,
                                AccountRetrievedCallback callback) {
    switch (state) {
      case READY:
        new FetchLoggedInAccountTask(context, callback).execute();
        break;
      case NEED_REFRESH:
        if (!RedditUtils.isAuthRefreshing(context)) {
          new RefreshAccessTokenTask(context, callback).execute();
          setAuthRefreshStatus(context);
        }
        break;
      case NONE:
        Log.d(LOG_TAG, state.toString());
        break;
    }
  }

}
