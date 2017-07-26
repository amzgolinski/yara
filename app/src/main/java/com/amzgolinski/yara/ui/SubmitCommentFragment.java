package com.amzgolinski.yara.ui;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amzgolinski.yara.R;
import com.amzgolinski.yara.data.RedditContract;
import com.amzgolinski.yara.service.YaraUtilityService;
import com.amzgolinski.yara.util.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SubmitCommentFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final String LOG_TAG = SubmitCommentFragment.class.getName();

  // loader
  private static final int SUBMISSION_LOADER_ID = 1;

  private static final String[] SUBMISSION_COLUMNS = {
      RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID,
      RedditContract.SubmissionsEntry.COLUMN_TITLE,
  };

  public static final int COL_SUBMISSION_ID = 0;
  public static final int COL_TITLE = 1;

  @BindView(R.id.submit_comment_progress) ProgressBar mSubmitComment;
  @BindView(R.id.submit_comment_text) EditText mComment;
  @BindView(R.id.submit_comment_submission_title) TextView mSubmissionTitle;

  private BroadcastReceiver mCommentReceiver;
  private Uri mSubmissionUri;
  private long mSubmissionId;

  public SubmitCommentFragment() {
    // empty
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    Log.d(LOG_TAG, "onActivityCreated");
    super.onActivityCreated(savedInstanceState);
    getLoaderManager().initLoader(SUBMISSION_LOADER_ID, null, this);
    mSubmissionUri = getActivity().getIntent().getData();
    mSubmissionId = ContentUris.parseId(mSubmissionUri);
    mCommentReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        mSubmitComment.setVisibility(View.GONE);
        boolean status = intent.getBooleanExtra(YaraUtilityService.PARAM_STATUS, true);
        if (!status) {
          Utils.handleError(context, intent.getStringExtra(YaraUtilityService.PARAM_MESSAGE));
        } else {
          getActivity().onBackPressed();
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
    Log.d(LOG_TAG, "onCreateLoader");
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
    View view = inflater.inflate(R.layout.fragment_submit_comment, container, false);
    ButterKnife.bind(this, view);
    return view;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    Log.d(LOG_TAG, "onCreateOptionsMenu");
    inflater.inflate(R.menu.menu_submit_comment, menu);
  }

  @Override
  public void onPause() {
    Log.d(LOG_TAG, "onPause");
    super.onPause();
    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver((mCommentReceiver));
  }

  @Override
  public void onResume() {
    Log.d(LOG_TAG, "onResume");
    super.onResume();
    LocalBroadcastManager.getInstance(getContext()).registerReceiver(
        mCommentReceiver, new IntentFilter(YaraUtilityService.ACTION_SUBMIT_COMMENT));
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    Log.d(LOG_TAG, "onLoaderReset");

  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    Log.d(LOG_TAG, "onLoadFinished");
    if (data != null && data.moveToFirst()) {
      mSubmissionTitle.setText(data.getString(SubmitCommentFragment.COL_TITLE));
    }

  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Log.d(LOG_TAG, "onOptionsItemSelected");
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.post_comment:
        String comment = mComment.getText().toString();
        mSubmitComment.setVisibility(View.VISIBLE);
        YaraUtilityService.submitComment(getContext(), mSubmissionId, comment);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
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
}
