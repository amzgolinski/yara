package com.amzgolinski.yara.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.amzgolinski.yara.callbacks.RedditDownloadCallback;
import com.amzgolinski.yara.model.CommentItem;
import com.amzgolinski.yara.service.YaraUtilityService;
import com.amzgolinski.yara.util.Utils;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;


public class FetchCommentsTask extends AsyncTask<String, Void, ArrayList<CommentItem>> {

  private static final String LOG_TAG = FetchCommentsTask.class.getName();

  private Context mContext;
  private RedditDownloadCallback mCallback;
  private String mMessage;

  public FetchCommentsTask(Context context, RedditDownloadCallback callback) {
    mContext = context;
    mCallback = callback;
  }

  public ArrayList<CommentItem> doInBackground(String... params) {
    //Log.d(LOG_TAG, "doInBackground");
    ArrayList<CommentItem> toReturn = new ArrayList<>();
    mMessage = YaraUtilityService.STATUS_OK;
    if (!Utils.isNetworkAvailable(mContext)) {
      mMessage = YaraUtilityService.STATUS_NO_INTERNET;
      return toReturn;
    }

    try {
      RedditClient redditClient = AuthenticationManager.get().getRedditClient();
      //Log.d(LOG_TAG, "Submission: " + params[0]);
      Submission fullSubmissionData = redditClient.getSubmission(params[0]);
      CommentNode rootNode = fullSubmissionData.getComments();
      toReturn = CommentItem.walkTree(rootNode.walkTree().toList());
    } catch (NetworkException networkException) {
      Log.d(LOG_TAG, networkException.toString());
      mMessage = YaraUtilityService.STATUS_NETWORK_EXCEPTION;
    }
    //Log.d(LOG_TAG, Integer.toString(rootNode.getTotalSize()));
    return toReturn;
  }

  public void onPostExecute(ArrayList<CommentItem> comments) {
    mCallback.onDownloadComplete(comments, mMessage);
  }

}
