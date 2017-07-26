package com.amzgolinski.yara.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class RedditProvider extends ContentProvider {

  private static final String LOG_TAG = RedditProvider.class.getName();

  private static final UriMatcher sUriMatcher = buildUriMatcher();
  private RedditDbHelper mRedditDbHelper;

  private static final int SUBREDDIT = 100;
  private static final int SUBREDDIT_WITH_ID = 101;
  private static final int SUBMISSION = 200;
  private static final int SUBMISSION_WITH_ID = 201;
  private static final int SUBMISSION_WITH_SUBREDDIT_ID = 202;
  private static final int COMMENT = 300;
  private static final int COMMENT_WITH_SUBMISSION_ID = 301;

  private static UriMatcher buildUriMatcher() {
    final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
    final String authority = RedditContract.CONTENT_AUTHORITY;

    // Subreddits
    matcher.addURI(authority, RedditContract.SubredditsEntry.TABLE_NAME, SUBREDDIT);

    matcher.addURI(authority, RedditContract.SubredditsEntry.TABLE_NAME + "/#", SUBREDDIT_WITH_ID);

    // Submissions
    matcher.addURI(authority, RedditContract.SubmissionsEntry.TABLE_NAME, SUBMISSION);

    matcher.addURI(
        authority,
        RedditContract.SubmissionsEntry.TABLE_NAME + "/#",
        SUBMISSION_WITH_ID);

    matcher.addURI(
        authority,
        RedditContract.SubmissionsEntry.TABLE_NAME + "/submission/#",
        SUBMISSION_WITH_SUBREDDIT_ID);

    // Comments
    matcher.addURI(authority, RedditContract.CommentsEntry.TABLE_NAME, COMMENT);

    matcher.addURI(
        authority,
        RedditContract.CommentsEntry.TABLE_NAME + "/#",
        COMMENT_WITH_SUBMISSION_ID);

    return matcher;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    final SQLiteDatabase db = mRedditDbHelper.getWritableDatabase();
    final int match = sUriMatcher.match(uri);
    int rowsDeleted = 0;

    switch (match) {

      case SUBREDDIT: {
        rowsDeleted = db.delete(
            RedditContract.SubredditsEntry.TABLE_NAME,
            selection,
            selectionArgs
        );
        break;
      }

      case SUBREDDIT_WITH_ID: {

        rowsDeleted = db.delete(
            RedditContract.SubredditsEntry.TABLE_NAME,
            RedditContract.SubredditsEntry.COLUMN_SUBREDDIT_ID+ " = ?",
            new String[]{String.valueOf(ContentUris.parseId(uri))}
        );

        break;
      }

      case SUBMISSION: {

        rowsDeleted = db.delete(
            RedditContract.SubmissionsEntry.TABLE_NAME,
            selection,
            selectionArgs
        );

        break;
      }

      case SUBMISSION_WITH_ID: {

        rowsDeleted = db.delete(
            RedditContract.SubmissionsEntry.TABLE_NAME,
            RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID + " = ?",
            new String[]{String.valueOf(ContentUris.parseId(uri))}
        );

        break;
      }

      case SUBMISSION_WITH_SUBREDDIT_ID: {

        rowsDeleted = db.delete(
            RedditContract.SubmissionsEntry.TABLE_NAME,
            RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_ID + " = ?",
            new String[]{String.valueOf(ContentUris.parseId(uri))}
        );

        break;
      }

      case COMMENT_WITH_SUBMISSION_ID: {

        rowsDeleted = db.delete(
            RedditContract.CommentsEntry.TABLE_NAME,
            RedditContract.CommentsEntry.COLUMN_SUBMISSION_ID + " = ?",
            new String[]{String.valueOf(ContentUris.parseId(uri))}
        );

        break;
      }

      default:
        throw new UnsupportedOperationException("Unknown uri: " + uri);

    }

    if (rowsDeleted > 0) {
      getContext().getContentResolver().notifyChange(uri, null);
    }

    return rowsDeleted;
  }

  @Override
  public String getType(Uri uri) {
    final int match = sUriMatcher.match(uri);

    switch (match) {
      case SUBREDDIT:
        Log.d(LOG_TAG, "type is sSUBREDDIT");
        return RedditContract.SubredditsEntry.CONTENT_TYPE;

      case SUBREDDIT_WITH_ID:
        return RedditContract.SubredditsEntry.CONTENT_ITEM_TYPE;

      case SUBMISSION:
        Log.d(LOG_TAG, "type is sSUBREDDIT");
        return RedditContract.SubmissionsEntry.CONTENT_TYPE;

      case SUBMISSION_WITH_ID:
        Log.d(LOG_TAG, "type is SUBMISSION_WITH_ID");
        return RedditContract.SubmissionsEntry.CONTENT_ITEM_TYPE;

      case SUBMISSION_WITH_SUBREDDIT_ID:
        Log.d(LOG_TAG, "type is SUBMISSION_WITH_ID");
        return RedditContract.SubmissionsEntry.CONTENT_ITEM_TYPE;

      case COMMENT:
        return RedditContract.CommentsEntry.CONTENT_TYPE;

      case COMMENT_WITH_SUBMISSION_ID:
        return RedditContract.CommentsEntry.CONTENT_ITEM_TYPE;

      default:
        throw new UnsupportedOperationException("Unknown uri: " + uri);
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    final SQLiteDatabase db = mRedditDbHelper.getWritableDatabase();
    final int match = sUriMatcher.match(uri);
    Uri uriToReturn;

    switch (match) {
      case SUBREDDIT: {
        long _id = db.insert(RedditContract.SubredditsEntry.TABLE_NAME, null, values);
        // insert unless it is already contained in the database
        if (_id > 0) {
          uriToReturn = RedditContract.SubredditsEntry.buildSubredditUri(_id);
        } else {
          throw new android.database.SQLException("Failed to insert row into: " + uri);
        }
        break;
      }

      case SUBMISSION: {
        Log.d(LOG_TAG, "SUBMISSION");
        long _id = db.insert(RedditContract.SubmissionsEntry.TABLE_NAME, null, values);
        Log.d(LOG_TAG, "SUBMISSION: " + _id);
        // insert unless it is already contained in the database
        if (_id > 0) {
          uriToReturn = RedditContract.SubmissionsEntry.buildSubmissionUri(_id);
        } else {
          throw new android.database.SQLException("Failed to insert row into: " + uri);
        }
        break;
      }

      case COMMENT: {
        long _id = db.insert(RedditContract.CommentsEntry.TABLE_NAME, null, values);
        // insert unless it is already contained in the database
        if (_id > 0) {
          uriToReturn = RedditContract.CommentsEntry.buildSubmissionUri(_id);
        } else {
          throw new android.database.SQLException("Failed to insert row into: " + uri);
        }
        break;
      }

      default: {
        throw new UnsupportedOperationException("Unknown uri: " + uri);
      }
    }

    getContext().getContentResolver().notifyChange(uri, null);
    return uriToReturn;
  }

  @Override
  public boolean onCreate() {
    mRedditDbHelper = new RedditDbHelper(getContext());
    return true;
  }

  private Cursor getSubreddit (String[] projection, String selection, String[] selectionArgs,
                               String sortOrder) {

    return mRedditDbHelper.getReadableDatabase().query(
        RedditContract.SubredditsEntry.TABLE_NAME,
        projection,
        selection,
        selectionArgs,
        null, // GROUP BY
        null, // HAVING
        sortOrder
    );
  }

  private Cursor getSubredditById (Uri uri, String[] projection, String sortOrder) {

    String selection = RedditContract.SubredditsEntry.TABLE_NAME + "."
        + RedditContract.SubredditsEntry.COLUMN_SUBREDDIT_ID + " = ?";
    String[] args = new String[] {String.valueOf(ContentUris.parseId(uri))};
    return getSubreddit(projection, selection, args, sortOrder);
  }

  private Cursor getSubmissions(String[] projection, String selection, String[] selectionArgs,
                                String sortOrder) {

    return mRedditDbHelper.getReadableDatabase().query(
        RedditContract.SubmissionsEntry.TABLE_NAME,
        projection,
        selection,
        selectionArgs,
        null, // GROUP BY
        null, // HAVING
        sortOrder
    );
  }

  private Cursor getSubmissionsById (Uri uri, String[] projection,
                                              String sortOrder) {

    String selection = RedditContract.SubmissionsEntry.TABLE_NAME + "."
        + RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID + " = ?";
    String[] args = new String[] {String.valueOf(ContentUris.parseId(uri))};
    return getSubmissions(projection, selection, args, sortOrder);
  }

  private Cursor getSubmissionsBySubredditId (Uri uri, String[] projection,
                                       String sortOrder) {

    String selection = RedditContract.SubmissionsEntry.TABLE_NAME + "."
        + RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_ID + " = ?";
    String[] args = new String[] {String.valueOf(ContentUris.parseId(uri))};
    return getSubmissions(projection, selection, args, sortOrder);
  }

  private Cursor getComments(String[] projection, String selection, String[] selectionArgs,
                            String sortOrder) {

    return mRedditDbHelper.getReadableDatabase().query(
        RedditContract.CommentsEntry.TABLE_NAME,
        projection,
        selection,
        selectionArgs,
        null, // GROUP BY
        null, // HAVING
        sortOrder
    );
  }

  private Cursor getCommentsBySubmissionId (Uri uri, String[] projection, String sortOrder) {

    String selection = RedditContract.CommentsEntry.TABLE_NAME +
        "." + RedditContract.CommentsEntry.COLUMN_SUBMISSION_ID +
        " = ?";
    String[] args = new String[] {String.valueOf(ContentUris.parseId(uri))};
    return getSubmissions(projection, selection, args, sortOrder);
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                      String sortOrder) {
    Cursor results = null;
    switch(sUriMatcher.match(uri)) {

      case SUBREDDIT: {
        results = getSubreddit(projection, selection, selectionArgs, sortOrder);
        break;
      }

      case SUBREDDIT_WITH_ID: {
        results = getSubredditById(uri, projection, sortOrder);
        break;
      }

      case SUBMISSION: {
        results = getSubmissions(projection, selection, selectionArgs, sortOrder);
        break;
      }

      case SUBMISSION_WITH_ID: {
        results = getSubmissionsById(uri, projection, sortOrder);
        break;
      }

      case SUBMISSION_WITH_SUBREDDIT_ID: {
        results = getSubmissionsBySubredditId(uri, projection, sortOrder);
        break;
      }

      case COMMENT: {
        results = getComments(projection, selection, selectionArgs, sortOrder);
        break;
      }

      case COMMENT_WITH_SUBMISSION_ID: {
        results = getCommentsBySubmissionId(uri, projection, sortOrder);
        break;
      }

      default:{
        throw new UnsupportedOperationException("Unknown uri: " + uri);
      }

    }
    return results;
  }

  @Override
  public int bulkInsert(Uri uri, ContentValues[] values) {

    final int match = sUriMatcher.match(uri);
    switch (match) {
      case SUBREDDIT: {
        return doBulkInsert(uri, RedditContract.SubredditsEntry.TABLE_NAME, values);
      }

      case SUBMISSION: {
        return doBulkInsert(uri, RedditContract.SubmissionsEntry.TABLE_NAME, values);
      }

      case COMMENT: {
        return doBulkInsert(uri, RedditContract.CommentsEntry.TABLE_NAME, values);
      }

      default:
        return super.bulkInsert(uri, values);
    }

  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    Log.d(LOG_TAG, "update");
    final SQLiteDatabase db = mRedditDbHelper.getWritableDatabase();
    int rowsUpdated = 0;

    if (values == null){
      throw new IllegalArgumentException("Null content values not allowed");
    }

    int match = sUriMatcher.match(uri);
    switch(match){

      case SUBREDDIT: {
        rowsUpdated = db.update(
            RedditContract.SubredditsEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs);
        break;
      }

      case SUBREDDIT_WITH_ID: {
        rowsUpdated = db.update(
            RedditContract.SubredditsEntry.TABLE_NAME,
            values,
            RedditContract.SubredditsEntry.COLUMN_SUBREDDIT_ID + " = ?",
            new String[] {String.valueOf(ContentUris.parseId(uri))});
        break;
      }

      case SUBMISSION_WITH_ID: {
        rowsUpdated = db.update(
            RedditContract.SubmissionsEntry.TABLE_NAME,
            values,
            RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID + " = ?",
            new String[] {String.valueOf(ContentUris.parseId(uri))});
        break;
      }

      case SUBMISSION_WITH_SUBREDDIT_ID: {
        rowsUpdated = db.update(
            RedditContract.SubmissionsEntry.TABLE_NAME,
            values,
            RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_ID + " = ?",
            new String[] {String.valueOf(ContentUris.parseId(uri))});
        break;
      }

      case COMMENT_WITH_SUBMISSION_ID: {
        rowsUpdated = db.update(
            RedditContract.CommentsEntry.TABLE_NAME,
            values,
            RedditContract.CommentsEntry.COLUMN_SUBMISSION_ID + " = ?",
            new String[] {String.valueOf(ContentUris.parseId(uri))});
        break;
      }

      default:{
        throw new UnsupportedOperationException("Unknown uri: " + uri);
      }
    }

    if (rowsUpdated > 0){
      Log.d(LOG_TAG, uri.toString());
      getContext().getContentResolver().notifyChange(uri, null);
    }

    return rowsUpdated;
  }

  private int doBulkInsert(Uri uri, String tableName, ContentValues[] values) {
    int numInserted = 0;
    final SQLiteDatabase db = mRedditDbHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      for (ContentValues value : values) {

        //Log.d(LOG_TAG, value.toString());
        if (value == null) {
          throw new IllegalArgumentException("Null content values not allowed");
        }

        long _id = db.insert(tableName, null, value);
        if (_id != -1) {
          numInserted++;
        }
      }
      if (numInserted > 0) {
        db.setTransactionSuccessful();
      }
    } finally {
      db.endTransaction();
    }
    getContext().getContentResolver().notifyChange(uri, null);
    return numInserted;
  }
}
