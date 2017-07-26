package com.amzgolinski.yara.service;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.amzgolinski.yara.R;
import com.amzgolinski.yara.data.RedditContract;

public class YaraWidgetService extends RemoteViewsService {

  public static final String LOG_TAG = YaraWidgetService.class.getName();

  public YaraWidgetService() {

  }

  private static final String[] SUBMISSION_COLUMNS = {
      RedditContract.SubmissionsEntry.COLUMN_ID,
      RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID,
      RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_NAME,
      RedditContract.SubmissionsEntry.COLUMN_TITLE,
      RedditContract.SubmissionsEntry.COLUMN_SCORE,
  };

  public static final int COL_ID = 0;
  public static final int COL_SUBMISSION_ID = 1;
  public static final int INDEX_COLUMN_SUBREDDIT_NAME = 2;
  public static final int INDEX_COLUMN_TITLE = 3;
  public static final int INDEX_COLUMN_SCORE = 4;

  @Override
  public RemoteViewsFactory onGetViewFactory(Intent intent) {
    Log.d(LOG_TAG, "onGetViewFactory");
    return new SubmissionRemoteViewsFactory(getApplicationContext(), intent);
  }

  class SubmissionRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Cursor mCursor;
    private Context mContext;

    public SubmissionRemoteViewsFactory(Context context, Intent intent) {
      mContext = context;
    }

    public void onCreate() {
      Log.d(LOG_TAG, "onCreate");
      Uri submissionsUri = RedditContract.SubmissionsEntry.CONTENT_URI;
      mCursor = getContentResolver().query(
          submissionsUri,
          SUBMISSION_COLUMNS,
          null,
          null,
          RedditContract.SubmissionsEntry.COLUMN_SCORE + " ASC");
    }

    public void onDestroy() {
      Log.d(LOG_TAG, "onDestroy");
      if (mCursor != null) {
        mCursor.close();
        mCursor = null;
      }
    }

    public int getCount() {
      //Log.d(LOG_TAG, "getCount");
      return (mCursor == null ? 0 : mCursor.getCount());
    }

    public RemoteViews getViewAt(int position) {
      //Log.d(LOG_TAG, "getViewAt");

      if (position == AdapterView.INVALID_POSITION ||
          mCursor == null ||
          !mCursor.moveToPosition(position)) {
        return null;
      }

      RemoteViews views =
          new RemoteViews(getPackageName(), R.layout.widget_item);

      // title
      String title = mCursor.getString(INDEX_COLUMN_TITLE);
      views.setTextViewText(R.id.widget_item_title, title);

      // subreddit
      String subredditText = String.format(
          mContext.getResources().getString(R.string.subreddit_name),
          mCursor.getString(INDEX_COLUMN_SUBREDDIT_NAME)
      );
      int score = mCursor.getInt(INDEX_COLUMN_SCORE);
      String detail = String.format(
          mContext.getResources().getString(R.string.appwidget_text_detail),
          subredditText,
          score
      );
      views.setTextViewText(R.id.widget_item_detail, detail);

      final Intent fillInIntent = new Intent();
      String submissionId = Integer.toString(mCursor.getInt(COL_SUBMISSION_ID));
      Uri submissionUri =
          RedditContract.SubmissionsEntry.buildSubmissionUri(Long.parseLong(submissionId));
      fillInIntent.setData(submissionUri);
      views.setOnClickFillInIntent(R.id.widget_submission_item, fillInIntent);

      return views;
    }

    public RemoteViews getLoadingView() {
      return new RemoteViews(getPackageName(), R.layout.widget_item);
    }

    public int getViewTypeCount() {
      return 1;
    }

    public long getItemId(int position) {
      /*
      if (mCursor.moveToPosition(position))
        return mCursor.getLong(COL_SUBMISSION_ID);
        */
      return position;
    }

    public boolean hasStableIds() {
      return true;
    }

    public void onDataSetChanged() {
      Log.d(LOG_TAG, "onDataSetChanged");

      if (mCursor != null) {
        mCursor.close();
      }
      final long identityToken = Binder.clearCallingIdentity();

      Uri submissionsUri = RedditContract.SubmissionsEntry.CONTENT_URI;
      mCursor = getContentResolver().query(
          submissionsUri,
          SUBMISSION_COLUMNS,
          null,
          null,
          RedditContract.SubmissionsEntry.COLUMN_SCORE + " DESC");

      Binder.restoreCallingIdentity(identityToken);

    }
  }
}
