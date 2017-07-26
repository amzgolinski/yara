package com.amzgolinski.yara.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.amzgolinski.yara.R;
import com.amzgolinski.yara.data.RedditContract;
import com.amzgolinski.yara.util.Utils;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.UserSubredditsPaginator;

import org.apache.commons.lang3.StringEscapeUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class SubredditSyncAdapter extends AbstractThreadedSyncAdapter {

  // log tag
  public static final String LOG_TAG = SubredditSyncAdapter.class.getName();

  // widget action
  public static final String ACTION_DATA_UPDATED = "com.amzgolinski.yara.ACTION_DATA_UPDATED";

  // Sync internal in seconds.
  // 60 seconds (1 minute) * 60 = 1 hour
  public static final int SYNC_INTERVAL = 60 * 60;
  public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
      LOCATION_STATUS_OK,
      LOCATION_STATUS_SERVER_DOWN,
      LOCATION_STATUS_SERVER_INVALID,
      LOCATION_STATUS_UNKNOWN,
      LOCATION_STATUS_INVALID})
  public @interface LocationStatus {
  }

  public static final int LOCATION_STATUS_OK = 0;
  public static final int LOCATION_STATUS_SERVER_DOWN = 1;
  public static final int LOCATION_STATUS_SERVER_INVALID = 2;
  public static final int LOCATION_STATUS_UNKNOWN = 3;
  public static final int LOCATION_STATUS_INVALID = 4;

  public SubredditSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
  }

  /**
   * Helper method to schedule the sync adapter periodic execution
   */
  public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
    Log.d(LOG_TAG, "configurePeriodicSync");
    Account account = getSyncAccount(context);
    String authority = context.getString(R.string.content_authority);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // we can enable inexact timers in our periodic sync
      SyncRequest request = new SyncRequest.Builder().
          syncPeriodic(syncInterval, flexTime).
          setSyncAdapter(account, authority).
          setExtras(new Bundle()).build();
      ContentResolver.requestSync(request);
    } else {
      ContentResolver.addPeriodicSync(account, authority, new Bundle(), syncInterval);
    }
  }

  /**
   * Helper method to get the fake account to be used with SyncAdapter, or make
   * a new one if the fake account doesn't exist yet.  If we make a new account,
   * we call the onAccountCreated method so we can initialize things.
   *
   * @param context The context used to access the account service
   * @return a fake account.
   */
  public static Account getSyncAccount(Context context) {
    Log.d(LOG_TAG, "getSyncAccount");
    // Get an instance of the Android account manager
    AccountManager accountManager =
        (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

    // Create the account type and default account
    Account newAccount = new Account(
        context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

    Log.d(LOG_TAG, newAccount.toString());
    // If the password doesn't exist, the account doesn't exist
    if (null == accountManager.getPassword(newAccount)) {
      if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
        return null;
      }
      onAccountCreated(newAccount, context);
    }
    return newAccount;
  }

  public static void initializeSyncAdapter(Context context) {
    Log.d(LOG_TAG, "initializeSyncAdapter");
    getSyncAccount(context);
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
                            ContentProviderClient provider,
                            SyncResult syncResult) {

    Log.d(LOG_TAG, "Starting sync");
    RedditClient redditClient = AuthenticationManager.get().getRedditClient();
    UserSubredditsPaginator paginator = new UserSubredditsPaginator(redditClient, "subscriber");

    HashMap<String, Subreddit> latestSubreddits = new HashMap<>();

    while (paginator.hasNext()) {
      Listing<Subreddit> subreddits = paginator.next();
      for (Subreddit subreddit : subreddits) {
        //Log.d(LOG_TAG, "Subreddit " + subreddit.toString());
        if (!subreddit.isNsfw() && subreddit.isUserSubscriber()) {
          latestSubreddits.put(subreddit.getId(), subreddit);
        }
      }
    }
    ArrayList<Subreddit> subreddits = new ArrayList<>(latestSubreddits.values());
    int numInserted = addSubreddits(subreddits);
    Log.i(LOG_TAG, "Inserted " + numInserted +  " subreddits");
    int numSubmissions = processSubreddits(subreddits);
    Log.i(LOG_TAG, "Inserted " + numSubmissions +  " submissions");
    updateWidgets();
    setSyncStatus(getContext(), LOCATION_STATUS_OK);
    return;
  }

  public static void syncImmediately(Context context) {
    Log.d(LOG_TAG, "syncImmediately");
    Bundle bundle = new Bundle();
    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
    ContentResolver.requestSync(getSyncAccount(context),
        context.getString(R.string.content_authority), bundle);
  }

  private int processSubreddits(ArrayList<Subreddit> subreddits) {

    int processed = 0;
    Log.d(LOG_TAG, "processSubreddits");
    RedditClient redditClient = AuthenticationManager.get().getRedditClient();

    for (Subreddit subreddit : subreddits) {
      SubredditPaginator paginator
          = new SubredditPaginator(redditClient, subreddit.getDisplayName());

      List<Submission> submissions = null;
      submissions = paginator.next();

      if (submissions == null) {
        Log.d(LOG_TAG, "Submissions was null");
        return processed;
      }

      ArrayList<Submission> toAdd = new ArrayList<>();
      for (Submission submission : submissions) {
        Log.d(LOG_TAG, submission.toString());
        if (Utils.isValidSubmission(submission)) {
          toAdd.add(submission);
        }
      }
      processed = addSubmissions(toAdd);
    }
    return processed;
  }

  private static void onAccountCreated(Account newAccount, Context context) {

    Log.d(LOG_TAG, "onAccountCreated");

    // Since we've created an account
    SubredditSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

    // Without calling setSyncAutomatically, our periodic sync will not be
    // enabled.
    ContentResolver.setSyncAutomatically(
        newAccount,
        context.getString(R.string.content_authority),
        true
    );

    //  Finally, let's do a sync to get things started
    syncImmediately(context);
  }

  private static void setSyncStatus(Context c, @LocationStatus int locationStatus) {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
    SharedPreferences.Editor spe = sp.edit();
    spe.putInt(c.getString(R.string.sync_status_key), locationStatus);
    spe.commit();
  }

  /**
   *
   */
  private void updateWidgets() {
    Context context = getContext();
    // Setting the package ensures that only components in our app will receive
    // the broadcast
    Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
        .setPackage(context.getPackageName());
    context.sendBroadcast(dataUpdatedIntent);


    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(dataUpdatedIntent);

  }

  private int addSubmissions(ArrayList<Submission> submissions) {
    Log.d(LOG_TAG, "addSubmissions");

    int numInserted = 0;

    Vector<ContentValues> contentValuesVector = new Vector<>(submissions.size());
    for (Submission submission : submissions) {
      ContentValues submissionValues = submissionToValue(submission);
      contentValuesVector.add(submissionValues);
      //mContext.getContentResolver().insert(RedditContract.SubmissionsEntry.CONTENT_URI, submissionValues);
    }

    ContentValues[] contentValuesArray = new ContentValues[contentValuesVector.size()];
    contentValuesVector.toArray(contentValuesArray);

    numInserted = getContext().getContentResolver()
        .bulkInsert(RedditContract.SubmissionsEntry.CONTENT_URI, contentValuesArray);

    return numInserted;
  }

  private int addSubreddits(ArrayList<Subreddit> subreddits) {

    int numInserted = 0;

    if (subreddits.size() > 0) {
      Vector<ContentValues> contentValuesVector = new Vector<ContentValues>(subreddits.size());

      for (Subreddit subreddit : subreddits) {
        ContentValues subredditValues = subredditToValue(subreddit);
        //Log.d(LOG_TAG, "Subreddit Values: " + subredditValues.toString());
        contentValuesVector.add(subredditValues);
      }

      ContentValues[] contentValuesArray = new ContentValues[contentValuesVector.size()];
      contentValuesVector.toArray(contentValuesArray);

      numInserted = getContext().getContentResolver()
          .bulkInsert(RedditContract.SubredditsEntry.CONTENT_URI, contentValuesArray);
    }
    return numInserted;
  }

  private ContentValues subredditToValue(Subreddit subreddit) {

    ContentValues toReturn = new ContentValues();
    toReturn.put(RedditContract.SubredditsEntry.COLUMN_SUBREDDIT_ID, Utils.redditIdToLong(subreddit.getId()));
    toReturn.put(RedditContract.SubredditsEntry.COLUMN_NAME, subreddit.getDisplayName());
    toReturn.put(RedditContract.SubredditsEntry.COLUMN_RELATIVE_LOCATION, subreddit.getRelativeLocation());
    toReturn.put(RedditContract.SubredditsEntry.COLUMN_TITLE, subreddit.getTitle());
    toReturn.put(RedditContract.SubredditsEntry.COLUMN_SELECTED, "1");
    return toReturn;
  }

  private ContentValues submissionToValue(Submission submission) {
    ContentValues toReturn = new ContentValues();
    toReturn.put(RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID, Utils.redditIdToLong(submission.getId()));
    toReturn.put(
        RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_ID,
        Utils.redditParentIdToLong(submission.getSubredditId())
    );
    toReturn.put(RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_NAME, submission.getSubredditName());
    toReturn.put(RedditContract.SubmissionsEntry.COLUMN_AUTHOR, submission.getAuthor());
    toReturn.put(
        RedditContract.SubmissionsEntry.COLUMN_TITLE,
        StringEscapeUtils.unescapeHtml4(submission.getTitle())
    );
    toReturn.put(RedditContract.SubmissionsEntry.COLUMN_URL, submission.getUrl());
    toReturn.put(RedditContract.SubmissionsEntry.COLUMN_COMMENT_COUNT, submission.getCommentCount());
    toReturn.put(RedditContract.SubmissionsEntry.COLUMN_SCORE, submission.getScore());
    int readOnly = (Utils.isSubmissionReadOnly(submission) ? 1 : 0);
    toReturn.put(RedditContract.SubmissionsEntry.COLUMN_IS_READ_ONLY, readOnly);
    toReturn.put(RedditContract.SubmissionsEntry.COLUMN_THUMBNAIL, submission.getThumbnail());

    String selfText = submission.data("selftext_html");
    if (!Utils.isStringEmpty(selfText)) {
      selfText = StringEscapeUtils.unescapeHtml4(selfText);
      selfText = Utils.removeHtmlSpacing(selfText);
    }
    toReturn.put(RedditContract.SubmissionsEntry.COLUMN_TEXT, selfText);
    toReturn.put(RedditContract.SubmissionsEntry.COLUMN_VOTE, submission.getVote().getValue());

    return toReturn;
  }
}
