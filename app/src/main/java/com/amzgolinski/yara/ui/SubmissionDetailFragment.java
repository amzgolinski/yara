package com.amzgolinski.yara.ui;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.amzgolinski.yara.R;
import com.amzgolinski.yara.adapter.CommentsAdapter;
import com.amzgolinski.yara.adapter.SubmissionDetailAdapter;
import com.amzgolinski.yara.callbacks.RedditDownloadCallback;
import com.amzgolinski.yara.data.RedditContract;
import com.amzgolinski.yara.model.CommentItem;
import com.amzgolinski.yara.service.YaraUtilityService;
import com.amzgolinski.yara.tasks.FetchCommentsTask;
import com.amzgolinski.yara.util.Utils;
import com.commonsware.cwac.merge.MergeAdapter;

import java.util.ArrayList;

import okhttp3.internal.Util;


public class SubmissionDetailFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor>, RedditDownloadCallback {

  private static final String LOG_TAG = SubmissionDetailFragment.class.getName();
  private static final String URI = "uri";
  private static final String COMMENTS = "comments";

  // loader
  private static final int SUBMISSION_LOADER_ID = 1;

  private static final String[] SUBMISSION_COLUMNS = {
      RedditContract.SubmissionsEntry.COLUMN_ID,
      RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID,
      RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_ID,
      RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_NAME,
      RedditContract.SubmissionsEntry.COLUMN_TITLE,
      RedditContract.SubmissionsEntry.COLUMN_TEXT,
      RedditContract.SubmissionsEntry.COLUMN_URL,
      RedditContract.SubmissionsEntry.COLUMN_THUMBNAIL,
      RedditContract.SubmissionsEntry.COLUMN_COMMENT_COUNT,
      RedditContract.SubmissionsEntry.COLUMN_SCORE,
      RedditContract.SubmissionsEntry.COLUMN_VOTE,
      RedditContract.SubmissionsEntry.COLUMN_AUTHOR,
      RedditContract.SubmissionsEntry.COLUMN_HINT,
  };

  public static final int COL_ID = 0;
  public static final int COL_SUBMISSION_ID = 1;
  public static final int COL_SUBREDDIT_ID = 2;
  public static final int COL_SUBREDDIT_NAME = 3;
  public static final int COL_TITLE = 4;
  public static final int COL_TEXT = 5;
  public static final int COL_URL = 6;
  public static final int COL_THUMBNAIL = 7;
  public static final int COL_COMMENT_COUNT = 8;
  public static final int COL_SCORE = 9;
  public static final int COL_VOTE = 10;
  public static final int COL_AUTHOR = 11;
  public static final int COL_HINT = 11;

  private Uri mSubmissionUri;
  private MergeAdapter mMergeAdapter;
  private SubmissionDetailAdapter mSubmissionAdapter;
  private CommentsAdapter mCommentsAdapter;
  private ArrayList<CommentItem> mComments;
  private BroadcastReceiver mReciever;
  private String mUrl;

  public SubmissionDetailFragment() {
    // empty
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    getLoaderManager().initLoader(SUBMISSION_LOADER_ID, null, this);
    if (savedInstanceState != null) {
      mSubmissionUri = savedInstanceState.getParcelable(URI);
      mComments = savedInstanceState.getParcelableArrayList(COMMENTS);
    } else {
      mSubmissionUri = getActivity().getIntent().getData();
    }

    String id = Utils.longToRedditId(ContentUris.parseId(mSubmissionUri));
    new FetchCommentsTask(this.getContext(), this).execute(id);
    mReciever =  new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        boolean status = intent.getBooleanExtra(YaraUtilityService.PARAM_STATUS, true);
        if (!status) {
          handleError(intent);
        } else {
          handleSuccess(intent);
        }
      }
    };
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d(LOG_TAG, "onCreate");
    setHasOptionsMenu(true);
    super.onCreate(savedInstanceState);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    if (mSubmissionUri != null) {
      Log.v(LOG_TAG, mSubmissionUri.toString());
      return getCursorLoader(mSubmissionUri, SUBMISSION_COLUMNS);
    }
    return null;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    Log.d(LOG_TAG, "onCreateView");
    View root = inflater.inflate(R.layout.fragment_submission_detail, container, false);
    mSubmissionAdapter = new SubmissionDetailAdapter(getContext(), null, 0);
    mCommentsAdapter = new CommentsAdapter(getContext(), new ArrayList<CommentItem>());
    mMergeAdapter = new MergeAdapter();
    mMergeAdapter.addAdapter(mSubmissionAdapter);
    mMergeAdapter.addAdapter(mCommentsAdapter);

    ListView commentsView = (ListView) root.findViewById(R.id.submission_detail_listview);
    commentsView.setAdapter(mMergeAdapter);
    commentsView.setEmptyView(root.findViewById(R.id.empty));

    return root;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu_submission_detail_fragment, menu);
  }

  @Override
  public void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(LOG_TAG, "onSaveInstanceState");
    outState.putParcelable(URI, mSubmissionUri);
    outState.putParcelableArrayList(COMMENTS, mComments);
  }

  @Override
  public void onDownloadComplete(Object result, String message) {
    if (message.equals(YaraUtilityService.STATUS_OK)) {
      mComments = (ArrayList<CommentItem>) result;
      mCommentsAdapter.setComments(mComments);
      mCommentsAdapter.addAll(mComments);
    } else {
      Utils.handleError(getContext(), message);
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    Log.d(LOG_TAG, "onLoaderReset");
    mSubmissionAdapter.swapCursor(null);

  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    Log.d(LOG_TAG, "onLoadFinished");
    //Log.d(LOG_TAG, DatabaseUtils.dumpCursorToString(data));

    if (data.moveToNext()) {
      mUrl = data.getString(COL_URL);
    }
    mSubmissionAdapter.swapCursor(data);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Log.d(LOG_TAG, "onOptionsItemSelected");
    // Handle item selection
    boolean isNetworkAvailable = Utils.isNetworkAvailable(getContext());
    String msg = getContext().getResources().getString(R.string.error_no_internet);

    switch (item.getItemId()) {
      case R.id.action_share:
        if (isNetworkAvailable) {
          startActivity(Intent.createChooser(
              createShareSubmissionIntent(),
              getContext().getResources().getString(R.string.share_text))
          );
        } else {
          Utils.showToast(getContext(), msg);
        }
        return true;

      case R.id.action_refresh:
        if (isNetworkAvailable) {
          msg = getContext().getResources().getString(R.string.refreshing);
          long submissionId = ContentUris.parseId(mSubmissionUri);
          YaraUtilityService.refreshSubmission(getContext(), submissionId);
          mCommentsAdapter.setComments(new ArrayList<CommentItem>());
        }
        Utils.showToast(getContext(), msg);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReciever);
  }

  @Override
  public void onResume() {
    super.onResume();

    LocalBroadcastManager.getInstance(getContext()).registerReceiver(
        mReciever, new IntentFilter(YaraUtilityService.ACTION_LOAD_MORE_COMMENTS));

    LocalBroadcastManager.getInstance(getContext()).registerReceiver(
        mReciever, new IntentFilter(YaraUtilityService.ACTION_SUBMIT_VOTE));

    LocalBroadcastManager.getInstance(getContext()).registerReceiver(
        mReciever, new IntentFilter(YaraUtilityService.ACTION_REFRESH_SUBMISSION));
  }

  @Override
  public void onStart() {
    super.onStart();
  }

  private void handleError(Intent intent) {
    Utils.handleError(getContext(), intent.getStringExtra(YaraUtilityService.PARAM_MESSAGE));
  }

  private void handleSuccess(Intent intent) {
    if (intent.getAction().equals(YaraUtilityService.ACTION_SUBMIT_VOTE)) {
      restartLoader();
    } else if (intent.getAction().equals(YaraUtilityService.ACTION_REFRESH_SUBMISSION)) {
      restartLoader();
      String id = Utils.longToRedditId(ContentUris.parseId(mSubmissionUri));
      new FetchCommentsTask(
          SubmissionDetailFragment.this.getContext(),
          SubmissionDetailFragment.this).execute(id);
    }
    else if (intent.getAction().equals(YaraUtilityService.ACTION_LOAD_MORE_COMMENTS)) {
      final ArrayList comments =
          intent.getParcelableArrayListExtra(YaraUtilityService.PARAM_COMMENTS);
      mCommentsAdapter.reloadComments(comments);
    }
  }

  private Intent createShareSubmissionIntent() {
    Intent sendIntent = new Intent();
    sendIntent.setAction(Intent.ACTION_SEND);
    sendIntent.putExtra(Intent.EXTRA_TEXT, mUrl);
    sendIntent.setType("text/plain");
    return sendIntent;
  }

  private CursorLoader getCursorLoader(Uri uri, String[] columns) {
    return new CursorLoader(
        getActivity(),
        uri,
        columns,
        null,
        null,
        null
    );
  }

  private void restartLoader() {
    if (isAdded()) {
      getLoaderManager().restartLoader(SUBMISSION_LOADER_ID, null, this);
    }
  }
}
