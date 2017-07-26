package com.amzgolinski.yara.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class RedditDbHelper extends SQLiteOpenHelper {

  public static final String LOG_TAG = RedditDbHelper.class.getSimpleName();

  private static final String DATABASE_NAME = "reddit.db";
  private static final int DATABASE_VERSION = 3;

  public RedditDbHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }


  @Override
  public void onCreate(SQLiteDatabase sqLiteDatabase) {

    // Subreddit
    final String SQL_CREATE_SUBREDDIT_TABLE = "CREATE TABLE " +
        RedditContract.SubredditsEntry.TABLE_NAME + " (" +
        RedditContract.SubredditsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        RedditContract.SubredditsEntry.COLUMN_SUBREDDIT_ID + " INTEGER DEFAULT 0, " +
        RedditContract.SubredditsEntry.COLUMN_TITLE + " TEXT, " +
        RedditContract.SubredditsEntry.COLUMN_NAME + " TEXT, " +
        RedditContract.SubredditsEntry.COLUMN_SELECTED + " INTEGER NOT NULL DEFAULT 0," +
        RedditContract.SubredditsEntry.COLUMN_RELATIVE_LOCATION + " TEXT, " +

        " UNIQUE (" + RedditContract.SubredditsEntry.COLUMN_SUBREDDIT_ID +
        ") ON CONFLICT IGNORE);";

    Log.d(LOG_TAG, SQL_CREATE_SUBREDDIT_TABLE);

    sqLiteDatabase.execSQL(SQL_CREATE_SUBREDDIT_TABLE);

    // Submission
    final String SQL_CREATE_SUBMISSIONS_TABLE = "CREATE TABLE " +
        RedditContract.SubmissionsEntry.TABLE_NAME + " ( " +
        RedditContract.SubmissionsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID + " TEXT NOT NULL, " +
        RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_ID + " TEXT NOT NULL, " +
        RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_NAME + " TEXT NOT NULL, " +
        RedditContract.SubmissionsEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
        RedditContract.SubmissionsEntry.COLUMN_AUTHOR + " TEXT NOT NULL, " +
        RedditContract.SubmissionsEntry.COLUMN_COMMENT_COUNT + " TEXT NOT NULL, " +
        RedditContract.SubmissionsEntry.COLUMN_TYPE + " TEXT, " +
        RedditContract.SubmissionsEntry.COLUMN_TEXT + " TEXT, " +
        RedditContract.SubmissionsEntry.COLUMN_URL + " TEXT NOT NULL, " +
        RedditContract.SubmissionsEntry.COLUMN_SCORE + " INTEGER, " +
        RedditContract.SubmissionsEntry.COLUMN_VOTE + " INTEGER, " +
        RedditContract.SubmissionsEntry.COLUMN_IS_READ_ONLY + " INTEGER, " +
        RedditContract.SubmissionsEntry.COLUMN_THUMBNAIL + " TEXT, " +
        RedditContract.SubmissionsEntry.COLUMN_HINT + " TEXT, " +

        " FOREIGN KEY (" + RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_ID +
        ") REFERENCES " + RedditContract.SubredditsEntry.TABLE_NAME + " (" +
        RedditContract.SubredditsEntry.COLUMN_SUBREDDIT_ID + ") ON DELETE CASCADE," +

        "UNIQUE (" + RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID+
        ") ON CONFLICT REPLACE);";

    Log.d(LOG_TAG, SQL_CREATE_SUBMISSIONS_TABLE);

    sqLiteDatabase.execSQL(SQL_CREATE_SUBMISSIONS_TABLE);

    // Comments
    final String SQL_CREATE_COMMENTS_TABLE = "CREATE TABLE " +
        RedditContract.CommentsEntry.TABLE_NAME + " ( " +
        RedditContract.CommentsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        RedditContract.CommentsEntry.COLUMN_COMMENT_ID + " INTEGER NOT NULL, " +
        RedditContract.CommentsEntry.COLUMN_SUBMISSION_ID + " INTEGER NOT NULL, " +
        RedditContract.CommentsEntry.COLUMN_PARENT_ID + " INTEGER NOT NULL, " +
        RedditContract.CommentsEntry.COLUMN_AUTHOR + " TEXT NOT NULL, " +
        RedditContract.CommentsEntry.COLUMN_DEPTH + " INTEGER NOT NULL, " +
        RedditContract.CommentsEntry.COLUMN_BODY + " TEXT NOT NULL, " +
        RedditContract.CommentsEntry.COLUMN_TYPE + " INTEGER, " +
        RedditContract.CommentsEntry.COLUMN_NUM_CHILDREN + " INTEGER, " +
        RedditContract.CommentsEntry.COLUMN_SCORE + " INTEGER, " +
        RedditContract.CommentsEntry.COLUMN_VOTE + " INTEGER, " +
        RedditContract.CommentsEntry.COLUMN_IS_ARCHIVED + " INTEGER, " +
        RedditContract.CommentsEntry.COLUMN_TEXT + " TEXT, " +
        RedditContract.CommentsEntry.COLUMN_IS_VISIBLE + " INTEGER NOT NULL, " +

        " FOREIGN KEY (" + RedditContract.CommentsEntry.COLUMN_SUBMISSION_ID +
        ") REFERENCES " + RedditContract.SubmissionsEntry.TABLE_NAME + " (" +
        RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID + "));";

    Log.d(LOG_TAG, SQL_CREATE_COMMENTS_TABLE);

    sqLiteDatabase.execSQL(SQL_CREATE_COMMENTS_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion,
                        int newVersion) {

    Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to " +
        newVersion + ". OLD DATA WILL BE DESTROYED");

    // Drop the Subreddits table
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + RedditContract.SubredditsEntry.TABLE_NAME);

    // Drop the Subreddits ID Sequence
    sqLiteDatabase.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '"
        + RedditContract.SubredditsEntry.TABLE_NAME + "'");

    // Drop the Submissions table
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + RedditContract.SubmissionsEntry.TABLE_NAME);

    // Drop the Submissions ID Sequence
    sqLiteDatabase.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '"
        + RedditContract.SubmissionsEntry.TABLE_NAME + "'");

    // Drop the Comments table
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + RedditContract.CommentsEntry.TABLE_NAME);

    // Drop the Comments ID Sequence
    sqLiteDatabase.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '"
        + RedditContract.CommentsEntry.TABLE_NAME + "'");

    onCreate(sqLiteDatabase);

  }

}
