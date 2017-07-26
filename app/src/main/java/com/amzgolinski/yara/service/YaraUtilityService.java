package com.amzgolinski.yara.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.NetworkOnMainThreadException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.amzgolinski.yara.YaraApplication;
import com.amzgolinski.yara.data.RedditContract;
import com.amzgolinski.yara.model.CommentItem;
import com.amzgolinski.yara.model.YaraContribution;
import com.amzgolinski.yara.model.YaraThing;
import com.amzgolinski.yara.util.Utils;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.models.VoteDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class YaraUtilityService extends IntentService {

  private static final String LOG_TAG = YaraUtilityService.class.getName();

  // Actions
  public static final String ACTION_SUBMIT_VOTE = "com.amzgolinski.yara.service.action.VOTE";
  public static final String ACTION_SUBMIT_COMMENT = "com.amzgolinski.yara.service.action.COMMENT";
  public static final String ACTION_LOAD_MORE_COMMENTS
      = "com.amzgolinski.yara.service.action.LOAD_COMMENTS";
  public static final String ACTION_DELETE_ACCOUNT
      = "com.amzgolinski.yara.service.action.DELETE_ACCOUNT";
  public static final String ACTION_SUBMISSIONS_UPDATED
      = "com.amzgolinski.yara.service.action.SUBMISSIONS_UPDATED";
  public static final String ACTION_SUBREDDIT_UNSUBSCRIBE
      = "com.amzgolinski.yara.service.action.REDDIT_UNSUBSCRIBE";

  public static final String ACTION_REFRESH_SUBMISSION
      = "com.amzgolinski.yara.service.action.ACTION_REFRESH_SUBMISSION";

  // parameters
  public static final String PARAM_COMMENTS = "com.amzgolinski.yara.service.extra.COMMENTS";
  public static final String PARAM_STATUS = "com.amzgolinski.yara.service.extra.STATUS";
  public static final String PARAM_MESSAGE = "com.amzgolinski.yara.service.extra.MESSAGE";

  private static final String COMMENT = "com.amzgolinski.yara.service.extra.COMMENT";
  private static final String CURRENT_VOTE = "com.amzgolinski.yara.service.extra.CURRENT_VOTE";
  private static final String NEW_VOTE = "com.amzgolinski.yara.service.extra.NEW_VOTE";
  private static final String POSITION = "com.amzgolinski.yara.service.extra.POSITION";
  private static final String SUBMISSION_ID = "com.amzgolinski.yara.service.extra.SUBMISSION_ID";
  private static final String SUBREDDITS = "com.amzgolinski.yara.service.extra.SUBREDDIT_ID";

  public static final String STATUS_OK = "com.amzgolinski.yara.service.action.OK";
  public static final String STATUS_NO_INTERNET = "com.amzgolinski.yara.service.action.NO_INTERNET";
  public static final String STATUS_NETWORK_EXCEPTION
      = "com.amzgolinski.yara.service.action.NETWORK_EXCEPTION";
  public static final String STATUS_API_EXCEPTION
      = "com.amzgolinski.yara.service.action.API_EXCEPTION";
  public static final String STATUS_AUTH_EXCEPTION =
      "com.amzgolinski.yara.service.action.AUTH_EXCEPTION";

  public YaraUtilityService() {
    super("YaraUtilityService");
  }


  public static void fetchMoreComments(Context context, ArrayList<CommentItem> comments,
                                       int position) {

    Log.d(LOG_TAG, "fetchMoreComments");
    Log.d(LOG_TAG, "comments: " + comments.size() + " position " + position);
    Intent intent = new Intent(context, YaraUtilityService.class);
    intent.setAction(ACTION_LOAD_MORE_COMMENTS);
    intent.putExtra(PARAM_COMMENTS, comments);
    intent.putExtra(POSITION, position);
    context.startService(intent);
  }

  public static void deleteAccount(Context context) {

    Log.d(LOG_TAG, "removeUser");
    Intent intent = new Intent(context, YaraUtilityService.class);
    intent.setAction(ACTION_DELETE_ACCOUNT);
    context.startService(intent);
  }

  public static void refreshSubmission(Context context, long submissionId) {
    Intent intent = new Intent(context, YaraUtilityService.class);
    intent.setAction(ACTION_REFRESH_SUBMISSION);
    intent.putExtra(SUBMISSION_ID, submissionId);
    context.startService(intent);
  }

  public static void submitVote(Context context, long submissionId, int currentVote, int newVote) {
    Intent intent = new Intent(context, YaraUtilityService.class);
    intent.setAction(ACTION_SUBMIT_VOTE);
    intent.putExtra(SUBMISSION_ID, submissionId);
    intent.putExtra(CURRENT_VOTE, currentVote);
    intent.putExtra(NEW_VOTE, newVote);
    context.startService(intent);
  }

  public static void submitComment(Context context, long submissionId, String comment) {
    Log.d(LOG_TAG, "submitComment");
    Log.d(LOG_TAG, "submission: " + submissionId + " comment " + comment);
    Intent intent = new Intent(context, YaraUtilityService.class);
    intent.setAction(ACTION_SUBMIT_COMMENT);
    intent.putExtra(SUBMISSION_ID, submissionId);
    intent.putExtra(COMMENT, comment);
    context.startService(intent);
  }

  public static void subredditUnsubscribe(Context context, ArrayList<String> subreddits) {
    Log.d(LOG_TAG, "subredditUnsubscribe");
    Log.d(LOG_TAG, "subreddit: " + subreddits.size());
    Intent intent = new Intent(context, YaraUtilityService.class);
    intent.setAction(ACTION_SUBREDDIT_UNSUBSCRIBE);
    intent.putExtra(SUBREDDITS, subreddits);
    context.startService(intent);
  }


  @Override
  protected void onHandleIntent(Intent intent) {
    Log.d(LOG_TAG, "onHandleIntent");
    if (intent != null) {

      final String action = intent.getAction();
      if (!Utils.isNetworkAvailable(getApplicationContext())) {
        broadcastResult(new Intent(intent.getAction()), false, STATUS_NO_INTERNET);
        return;
      }
      // vote for submission
      if (ACTION_SUBMIT_VOTE.equals(action)) {
        final long submissionId = intent.getLongExtra(SUBMISSION_ID, 1L);
        final int currentVote = intent.getIntExtra(CURRENT_VOTE, 0);
        final int newVote = intent.getIntExtra(NEW_VOTE, 0);
        handleSubmitVote(submissionId, currentVote, newVote);
      } else if (ACTION_SUBMIT_COMMENT.equals(action)) {
        final long submissionId = intent.getLongExtra(SUBMISSION_ID, Long.MIN_VALUE);
        final String comment = intent.getStringExtra(COMMENT);
        handleSubmitComment(submissionId, comment);
      } else if (ACTION_LOAD_MORE_COMMENTS.equals(action)) {
        final ArrayList comments = intent.getParcelableArrayListExtra(PARAM_COMMENTS);
        final int position = intent.getIntExtra(POSITION, Integer.MIN_VALUE);
        handleLoadMoreComments(comments, position);
      } else if (ACTION_SUBREDDIT_UNSUBSCRIBE.equals(action)) {
        final ArrayList subreddits = intent.getParcelableArrayListExtra(SUBREDDITS);
        handleUnsubscribeSubreddit(subreddits);
      } else if (ACTION_REFRESH_SUBMISSION.equals(action)) {
        final long submissionId = intent.getLongExtra(SUBMISSION_ID, Long.MIN_VALUE);
        handleRefreshSubmission(submissionId);
      } else if (ACTION_DELETE_ACCOUNT.equals(action)) {
        handleDeleteAccount();
      }
    }
  }

  private void broadcastResult(Intent result, boolean status, String message) {
    Log.d(LOG_TAG, "broadcastResult");
    result.putExtra(PARAM_STATUS, status);
    result.putExtra(PARAM_MESSAGE, message);
    LocalBroadcastManager.getInstance(this).sendBroadcast(result);
  }

  private void handleDeleteAccount() {
    Log.d(LOG_TAG, "handleDeleteAccount");
    Intent result = new Intent(ACTION_DELETE_ACCOUNT);
    boolean status = true;
    String message = STATUS_OK;

    try {
      AuthenticationManager.get()
          .getRedditClient()
          .getOAuthHelper()
          .revokeAccessToken(YaraApplication.CREDENTIALS);
      AuthenticationManager.get().getRedditClient().deauthenticate();

      int numDeleted = this.getContentResolver()
          .delete(RedditContract.SubmissionsEntry.CONTENT_URI, null, null);
      Log.d(LOG_TAG, "Deleted " + numDeleted);

      numDeleted = this.getContentResolver()
          .delete(RedditContract.SubredditsEntry.CONTENT_URI, null, null);

      Log.d(LOG_TAG, "Deleted " + numDeleted);
      Utils.logOutCurrentUser(getApplicationContext());
      result.putExtra(PARAM_STATUS, STATUS_OK);
    } catch (NetworkException networkException) {
      Log.e(LOG_TAG, networkException.toString());
      message = STATUS_NETWORK_EXCEPTION;
      status = false;
    }

    broadcastResult(result, status, message);
  }

  private void handleLoadMoreComments(ArrayList<CommentItem> comments, int position) {
    Log.d(LOG_TAG, "handleLoadMoreComments");
    Intent result = new Intent(ACTION_LOAD_MORE_COMMENTS);
    boolean status = true;
    String message = STATUS_OK;
    ArrayList<CommentItem> toReturn;
    CommentItem item = comments.get(position);

    try {
      RedditClient redditClient = AuthenticationManager.get().getRedditClient();

      //Log.d(LOG_TAG, wrapper.toString());
      Submission fullSubmissionData = redditClient.getSubmission(item.getSubmissionId());
      CommentNode rootNode = fullSubmissionData.getComments();
      toReturn = loadMoreComments(rootNode, item, comments);
      comments.remove(position);
      comments.addAll(position, toReturn);
      result.putExtra(PARAM_COMMENTS, comments);
    } catch (NetworkException networkException) {
      Log.e(LOG_TAG, networkException.toString());
      status = false;
      message = STATUS_NETWORK_EXCEPTION;
    }
    broadcastResult(result, status, message);
  }

  private void handleRefreshSubmission(long submissionId) {
    Log.d(LOG_TAG, "handleRefreshSubmission");
    Intent result = new Intent(ACTION_REFRESH_SUBMISSION);
    boolean status = true;
    String message = STATUS_OK;
    String id = Utils.longToRedditId(submissionId); // converting long to Reddit ID
    RedditClient reddit = AuthenticationManager.get().getRedditClient();
    // submit reply to the server
    // TODO: handle error
    try {
      Submission submission = reddit.getSubmission(id);
      updateSubmission(submission);
    } catch (NetworkException networkException) {
      Log.d(LOG_TAG, networkException.toString());
      status = false;
    }

    broadcastResult(result, status, message);
  }

  private void handleSubmitComment(long submissionId, String commentText) {
    Log.d(LOG_TAG, "handleSubmitComment");
    Intent result = new Intent(ACTION_SUBMIT_COMMENT);
    boolean status = true;
    String message = STATUS_OK;

    String id = Utils.longToRedditId(submissionId); // converting long to Reddit ID
    RedditClient reddit = AuthenticationManager.get().getRedditClient();
    // submit reply to the server
    // TODO: handle error
    try {
      YaraContribution contrib = new YaraContribution(id);
      new AccountManager(reddit).reply(contrib, commentText);
      Submission updated = reddit.getSubmission(id);
      updateCommentCount(updated);
    } catch (NetworkException | ApiException exception) {
      Log.d(LOG_TAG, exception.toString());
      status = false;
      message = STATUS_NETWORK_EXCEPTION;
    }

    broadcastResult(result, status, message);
  }

  private void handleSubmitVote(long submissionId, int currentVote, int newVote) {

    Intent result = new Intent(ACTION_SUBMIT_VOTE);
    boolean status = true;
    String message = STATUS_OK;

    VoteDirection direction = Utils.getVote(currentVote, newVote);

    RedditClient reddit = AuthenticationManager.get().getRedditClient();
    String redditId = Utils.longToRedditId(submissionId);
    YaraThing submission = new YaraThing(redditId, YaraThing.Type.SUBMISSION);

    // submit vote to the server
    try {
      new AccountManager(reddit).vote(submission, direction);
      Submission updated = reddit.getSubmission(submission.getId());
      Log.d(LOG_TAG, updated.toString());
      updateScore(updated);
    } catch (NetworkException networkException) {
      Log.e(LOG_TAG, networkException.getMessage());
      status = false;
      message = STATUS_NETWORK_EXCEPTION;
    } catch (ApiException apiException) {
      Log.e(LOG_TAG, apiException.getMessage());
      status = false;
      message = STATUS_API_EXCEPTION;

    }
    broadcastResult(result, status, message);
  }

  private void handleUnsubscribeSubreddit(ArrayList<String> subreddits) {

    Intent result = new Intent(ACTION_SUBREDDIT_UNSUBSCRIBE);
    boolean status = true;
    String message = STATUS_OK;
    RedditClient reddit = AuthenticationManager.get().getRedditClient();

    for (String subredditName : subreddits) {
      try {
        RedditClient client = AuthenticationManager.get().getRedditClient();
        Subreddit subreddit = client.getSubreddit(subredditName);
        new AccountManager(reddit).unsubscribe(subreddit);
        this.deleteSubreddit(subreddit.getId());
      } catch (NetworkException exception) {
        Log.d(LOG_TAG, exception.getMessage());
        status = false;
        message = STATUS_NETWORK_EXCEPTION;
      }
    }
    this.broadcastResult(result, status, message);
  }

  private void updateSubmission(Submission submission) {
    long id = Utils.redditIdToLong(submission.getId());
    Uri submissionUri = RedditContract.SubmissionsEntry.buildSubmissionUri(id);
    ContentValues toInsert = RedditContract.SubmissionsEntry.submissionToContentValue(submission);
    this.getContentResolver().update(submissionUri, toInsert, null, null);
  }

  private void deleteSubreddit(String subredditId) {

    Log.d(LOG_TAG, "Deleting " + subredditId);
    long id = Utils.redditIdToLong(subredditId);
    Log.d(LOG_TAG, "Deleting " + id);
    String selector = RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_ID + " = ?";
    String[] args = new String[] {Long.toString(id)};
    int numDeleted = this.getContentResolver()
        .delete(RedditContract.SubmissionsEntry.CONTENT_URI, selector, args);
    Log.d(LOG_TAG, "Deleted " + numDeleted);

    Uri subredditUri = RedditContract.SubredditsEntry.buildSubredditUri(id);
    this.getContentResolver().delete(subredditUri, null, null);
  }

  private ArrayList<CommentItem> loadMoreComments(CommentNode rootNode, CommentItem wrapper,
                                                  ArrayList<CommentItem> current) {
    Log.d(LOG_TAG, "loadMoreComments");

    ArrayList<CommentItem> toReturn;
    Stack<CommentItem> moreComments = new Stack<>();

    RedditClient redditClient = AuthenticationManager.get().getRedditClient();
    CommentNode parent = rootNode.findChild(wrapper.getId()).orNull();
    parent.loadMoreComments(redditClient);
    Iterable<CommentNode> iterable = parent.walkTree();
    List<CommentNode> filteredList = new ArrayList<>();
    boolean found = false;
    for (CommentNode node : iterable) {
      if (found) {
        if (!isCommentLoaded(node.getComment().getId(), current)) {
          filteredList.add(node);
        }
      } else if (node.getComment().getId().equals(wrapper.getId().substring(3))) {
        found = true;
      }
    }
    toReturn = CommentItem.walkTree(filteredList);
    return toReturn;
  }

  private boolean isCommentLoaded(String id, ArrayList<CommentItem> comments) {

    boolean inList = false;
    int index = 0;
    while (!inList && index < comments.size() ) {
      inList = id.equals((comments.get(index).getId()));
      index++;
    }
    return inList;
  }

  private int updateScore(Submission submission) {
    ContentValues values = new ContentValues();
    values.put(RedditContract.SubmissionsEntry.COLUMN_VOTE, submission.getVote().getValue());
    values.put(RedditContract.SubmissionsEntry.COLUMN_SCORE, submission.getScore());

    long id = Utils.redditIdToLong(submission.getId());
    Uri submissionUri = RedditContract.SubmissionsEntry.buildSubmissionUri(id);
    int numUpdated = this.getContentResolver().update(
        submissionUri,
        values,
        RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID + " =  ? ",
        new String[]{submission.getId()});
    return numUpdated;
  }

  private int updateCommentCount(Submission submission) {
    ContentValues values = new ContentValues();
    values.put(RedditContract.SubmissionsEntry.COLUMN_COMMENT_COUNT, submission.getCommentCount());

    long id = Utils.redditIdToLong(submission.getId());
    Uri submissionUri = RedditContract.SubmissionsEntry.buildSubmissionUri(id);
    int numUpdated = this.getContentResolver().update(
        submissionUri,
        values,
        RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID + " =  ? ",
        new String[]{submission.getId()});
    return numUpdated;
  }


}
