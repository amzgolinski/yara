package com.amzgolinski.yara.tasks;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.UserSubredditsPaginator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.amzgolinski.yara.callbacks.RedditDownloadCallback;
import com.amzgolinski.yara.data.RedditContract;
import com.amzgolinski.yara.data.RedditContract.SubredditsEntry;
import com.amzgolinski.yara.data.RedditContract.SubmissionsEntry;
import com.amzgolinski.yara.service.YaraUtilityService;
import com.amzgolinski.yara.sync.SubredditSyncAdapter;
import com.amzgolinski.yara.util.Utils;


public class FetchSubredditsTask extends AsyncTask<Void, Void, HashMap<String, Subreddit>> {

  private Context mContext;
  private RedditDownloadCallback mCallback;
  private String mMessage;

  public FetchSubredditsTask(Context context, RedditDownloadCallback callback) {
    mContext = context;
    mCallback = callback;
  }

  private static final String LOG_TAG = FetchSubredditsTask.class.getName();

  public HashMap<String, Subreddit> doInBackground(Void... params) {
    HashMap<String, Subreddit> latestSubreddits = new HashMap<>();

    if (!Utils.isNetworkAvailable(mContext)) {
      mMessage = YaraUtilityService.STATUS_NO_INTERNET;
      return latestSubreddits;
    }

    try {

      RedditClient redditClient = AuthenticationManager.get().getRedditClient();
      UserSubredditsPaginator paginator = new UserSubredditsPaginator(redditClient, "subscriber");

      while (paginator.hasNext()) {
        Listing<Subreddit> subreddits = paginator.next();
        for (Subreddit subreddit : subreddits) {
          if (!subreddit.isNsfw() && subreddit.isUserSubscriber()) {
            latestSubreddits.put(subreddit.getId(), subreddit);
          }
        }
      }
      ArrayList<Subreddit> subreddits = new ArrayList<>(latestSubreddits.values());
      addSubreddits(subreddits);
      processSubreddits(subreddits);
      mMessage = YaraUtilityService.STATUS_OK;
    } catch (NetworkException networkException) {
      Log.e(LOG_TAG, networkException.toString());
      mMessage = YaraUtilityService.STATUS_NETWORK_EXCEPTION;
    }
    return latestSubreddits;
  }

  private int processSubreddits(ArrayList<Subreddit> subreddits) {

    int processed = 0;
    RedditClient redditClient = AuthenticationManager.get().getRedditClient();

    for (Subreddit subreddit : subreddits) {
      SubredditPaginator paginator
          = new SubredditPaginator(redditClient, subreddit.getDisplayName());

      List<Submission> submissions = null;
      submissions = paginator.next();

      if (submissions == null) {
        return processed;
      }

      ArrayList<Submission> toAdd = new ArrayList<>();
      for (Submission submission : submissions) {
        //Log.d(LOG_TAG, submission.toString());
        if (Utils.isValidSubmission(submission)) {
          toAdd.add(submission);
        }
      }
      processed = addSubmissions(toAdd);
    }
    return processed;
  }

  @Override
  public void onPostExecute(HashMap<String, Subreddit> result) {
    Log.d(LOG_TAG, "onPostExecute");
    Intent dataUpdated = new Intent();
    dataUpdated.setAction(SubredditSyncAdapter.ACTION_DATA_UPDATED);
    mContext.sendBroadcast(dataUpdated);
    mCallback.onDownloadComplete(result, mMessage);
  }

  private int addSubmissions(ArrayList<Submission> submissions) {
    int numInserted;

    Vector<ContentValues> contentValuesVector = new Vector<>(submissions.size());
    for (Submission submission : submissions) {
      ContentValues submissionValues
          = RedditContract.SubmissionsEntry.submissionToContentValue(submission);
      contentValuesVector.add(submissionValues);
    }

    ContentValues[] contentValuesArray = new ContentValues[contentValuesVector.size()];
    contentValuesVector.toArray(contentValuesArray);

    numInserted = mContext.getContentResolver()
        .bulkInsert(SubmissionsEntry.CONTENT_URI, contentValuesArray);

    return numInserted;
  }

  private int addSubreddits(ArrayList<Subreddit> subreddits) {

    int numInserted = 0;

    if (subreddits.size() > 0) {
      Vector<ContentValues> contentValuesVector = new Vector<ContentValues>(subreddits.size());

      for (Subreddit subreddit : subreddits) {
        ContentValues subredditValues = subredditToValue(subreddit);
        contentValuesVector.add(subredditValues);
      }

      ContentValues[] contentValuesArray = new ContentValues[contentValuesVector.size()];
      contentValuesVector.toArray(contentValuesArray);

      numInserted = mContext.getContentResolver()
          .bulkInsert(SubredditsEntry.CONTENT_URI, contentValuesArray);
    }
    return numInserted;
  }

  private ContentValues subredditToValue(Subreddit subreddit) {

    ContentValues toReturn = new ContentValues();
    toReturn.put(SubredditsEntry.COLUMN_SUBREDDIT_ID, Utils.redditIdToLong(subreddit.getId()));
    toReturn.put(SubredditsEntry.COLUMN_NAME, subreddit.getDisplayName());
    toReturn.put(SubredditsEntry.COLUMN_RELATIVE_LOCATION, subreddit.getRelativeLocation());
    toReturn.put(SubredditsEntry.COLUMN_TITLE, subreddit.getTitle());
    toReturn.put(SubredditsEntry.COLUMN_SELECTED, "1");
    return toReturn;
  }

}
