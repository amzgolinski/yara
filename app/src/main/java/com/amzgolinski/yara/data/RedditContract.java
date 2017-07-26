package com.amzgolinski.yara.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.BaseColumns;

import com.amzgolinski.yara.util.Utils;

import net.dean.jraw.models.Submission;

import org.apache.commons.lang3.StringEscapeUtils;


public class RedditContract {

  static final String CONTENT_AUTHORITY = "com.amzgolinski.yara.provider";

  private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

  private static final String PATH_SUBREDDITS   = "subreddits";
  private static final String PATH_SUBMISSIONS  = "submissions";
  private static final String PATH_COMMENTS     = "comments";

  public static final class SubredditsEntry implements BaseColumns {

    public static final Uri CONTENT_URI =
        BASE_CONTENT_URI.buildUpon().appendPath(PATH_SUBREDDITS).build();

    static final String CONTENT_TYPE =
        ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SUBREDDITS;

    static final String CONTENT_ITEM_TYPE =
        ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SUBREDDITS;

    // table name
    static final String TABLE_NAME = "subreddits";

    // columns
    public static final String COLUMN_ID                = "_id";
    public static final String COLUMN_SUBREDDIT_ID      = "subreddit_id";
    public static final String COLUMN_NAME              = "name";
    public static final String COLUMN_TITLE             = "title";
    public static final String COLUMN_RELATIVE_LOCATION = "relative_location";
    public static final String COLUMN_SELECTED          = "selected";

    public static Uri buildSubredditUri(long id) {
      return ContentUris.withAppendedId(CONTENT_URI, id);
    }

  }

  public static final class SubmissionsEntry implements BaseColumns {

    public static final Uri CONTENT_URI =
        BASE_CONTENT_URI.buildUpon().appendPath(PATH_SUBMISSIONS).build();

    static final String CONTENT_TYPE =
        ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SUBMISSIONS;

    static final String CONTENT_ITEM_TYPE =
        ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SUBMISSIONS;

    // table name
    static final String TABLE_NAME = "submissions";

    // columns
    public static final String COLUMN_ID             = "_id";
    public static final String COLUMN_SUBMISSION_ID  = "submission_id";
    public static final String COLUMN_SUBREDDIT_ID   = "subreddit_id";
    public static final String COLUMN_SUBREDDIT_NAME = "subreddit_name";
    public static final String COLUMN_TITLE          = "title";
    public static final String COLUMN_AUTHOR         = "author";
    public static final String COLUMN_URL            = "url";
    public static final String COLUMN_TEXT           = "text";
    public static final String COLUMN_COMMENT_COUNT  = "comment_count";
    public static final String COLUMN_THUMBNAIL      = "thumbnail";
    public static final String COLUMN_SCORE          = "score";
    public static final String COLUMN_VOTE           = "vote";
    public static final String COLUMN_IS_READ_ONLY   = "read_only";
    public static final String COLUMN_TYPE           = "type";
    public static final String COLUMN_HINT           = "hint";

    public static ContentValues submissionToContentValue(Submission submission) {
      ContentValues toReturn = new ContentValues();
      toReturn.put(SubmissionsEntry.COLUMN_SUBMISSION_ID, Utils.redditIdToLong(submission.getId()));
      toReturn.put(
          SubmissionsEntry.COLUMN_SUBREDDIT_ID,
          Utils.redditParentIdToLong(submission.getSubredditId())
      );
      toReturn.put(SubmissionsEntry.COLUMN_SUBREDDIT_NAME, submission.getSubredditName());
      toReturn.put(SubmissionsEntry.COLUMN_AUTHOR, submission.getAuthor());
      toReturn.put(
          SubmissionsEntry.COLUMN_TITLE,
          StringEscapeUtils.unescapeHtml4(submission.getTitle())
      );
      toReturn.put(SubmissionsEntry.COLUMN_URL, submission.getUrl());
      toReturn.put(SubmissionsEntry.COLUMN_COMMENT_COUNT, submission.getCommentCount());
      toReturn.put(SubmissionsEntry.COLUMN_SCORE, submission.getScore());
      int readOnly = (Utils.isSubmissionReadOnly(submission) ? 1 : 0);
      toReturn.put(SubmissionsEntry.COLUMN_IS_READ_ONLY, readOnly);
      toReturn.put(SubmissionsEntry.COLUMN_THUMBNAIL, submission.getThumbnail());

      String selfText = submission.data("selftext_html");
      if (!Utils.isStringEmpty(selfText)) {
        selfText = StringEscapeUtils.unescapeHtml4(selfText);
        selfText = Utils.removeHtmlSpacing(selfText);
      }
      toReturn.put(SubmissionsEntry.COLUMN_TEXT, selfText);
      toReturn.put(SubmissionsEntry.COLUMN_VOTE, submission.getVote().getValue());
      toReturn.put(SubmissionsEntry.COLUMN_HINT, submission.getPostHint().toString());

      return toReturn;
    }

    public static Uri buildSubmissionUri(long id) {
      return ContentUris.withAppendedId(CONTENT_URI, id);
    }

    public static Uri buildSubmissionUriWithSubredditId(long id) {
      Uri submissionWithSubreddit = CONTENT_URI.buildUpon().appendPath("submission").build();
      return ContentUris.withAppendedId(submissionWithSubreddit, id);
    }

  }

  public static final class CommentsEntry implements BaseColumns {

    public static final Uri CONTENT_URI =
        BASE_CONTENT_URI.buildUpon().appendPath(PATH_COMMENTS).build();

    public static final String CONTENT_TYPE =
        ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_COMMENTS;

    public static final String CONTENT_ITEM_TYPE =
        ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_COMMENTS;

    // table name
    public static final String TABLE_NAME = "comments";

    // columns
    public static final String COLUMN_ID            = "_id";
    public static final String COLUMN_COMMENT_ID    = "comment_id";
    public static final String COLUMN_SUBMISSION_ID = "submission_id";
    public static final String COLUMN_PARENT_ID     = "parent_id";
    public static final String COLUMN_AUTHOR        = "relative_location";
    public static final String COLUMN_BODY          = "body";
    public static final String COLUMN_SCORE         = "score";
    public static final String COLUMN_VOTE          = "vote";
    public static final String COLUMN_DEPTH         = "depth";
    public static final String COLUMN_TEXT          = "text";
    public static final String COLUMN_IS_VISIBLE    = "visible";
    public static final String COLUMN_IS_ARCHIVED   = "archived";
    public static final String COLUMN_TYPE          = "type";
    public static final String COLUMN_NUM_CHILDREN  = "child_count";

    public static Uri buildSubmissionUri(long id) {
      return ContentUris.withAppendedId(CONTENT_URI, id);
    }

  }

}
