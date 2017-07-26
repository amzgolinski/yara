package com.amzgolinski.yara.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.amzgolinski.yara.R;
import com.amzgolinski.yara.adapter.SubmissionListAdapter;
import com.amzgolinski.yara.callbacks.AccountRetrievedCallback;
import com.amzgolinski.yara.callbacks.RedditDownloadCallback;
import com.amzgolinski.yara.data.RedditContract;
import com.amzgolinski.yara.service.YaraUtilityService;
import com.amzgolinski.yara.sync.SubredditSyncAdapter;
import com.amzgolinski.yara.tasks.FetchSubredditsTask;
import com.amzgolinski.yara.util.Utils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.models.LoggedInAccount;
import net.dean.jraw.models.Subreddit;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SubmissionListFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor>, AccountRetrievedCallback {

  private final String LOG_TAG = SubmissionListFragment.class.getName();

  private static final int SUBMISSIONS_LOADER = 0;

  private static final String[] SUBMISSION_COLUMNS = {
      RedditContract.SubmissionsEntry.COLUMN_ID,
      RedditContract.SubmissionsEntry.COLUMN_SUBMISSION_ID,
      RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_ID,
      RedditContract.SubmissionsEntry.COLUMN_SUBREDDIT_NAME,
      RedditContract.SubmissionsEntry.COLUMN_TITLE,
      RedditContract.SubmissionsEntry.COLUMN_TEXT,
      RedditContract.SubmissionsEntry.COLUMN_AUTHOR,
      RedditContract.SubmissionsEntry.COLUMN_THUMBNAIL,
      RedditContract.SubmissionsEntry.COLUMN_COMMENT_COUNT,
      RedditContract.SubmissionsEntry.COLUMN_SCORE,
      RedditContract.SubmissionsEntry.COLUMN_VOTE,
  };

  public static final int COL_ID = 0;
  public static final int COL_SUBMISSION_ID = 1;
  public static final int COL_SUBREDDIT_ID = 2;
  public static final int COL_SUBREDDIT_NAME = 3;
  public static final int COL_TITLE = 4;
  public static final int COL_TEXT = 5;
  public static final int COL_AUTHOR = 6;
  public static final int COL_THUMBNAIL = 7;
  public static final int COL_COMMENT_COUNT = 8;
  public static final int COL_SCORE = 9;
  public static final int COL_VOTE = 10;

  // Views
  @BindView(R.id.submission_list_progress_bar_layout)
  ViewGroup mProgress;
  @BindView(R.id.submission_list_swipe_refresh)
  SwipeRefreshLayout mSwipeRefreshLayout;
  @BindView(R.id.empty)
  TextView mEmpty;

  private SubmissionListAdapter mAdapter;
  private BroadcastReceiver mReceiver;


  public SubmissionListFragment() {
    // empty
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    Log.d(LOG_TAG, "onActivityCreated");
    super.onActivityCreated(savedInstanceState);
    getLoaderManager().initLoader(SUBMISSIONS_LOADER, null, this);
    mReceiver = new BroadcastReceiver() {

      @Override
      public void onReceive(Context context, Intent intent) {
        boolean status = intent.getBooleanExtra(YaraUtilityService.PARAM_STATUS, true);
        if (!status) {
          Utils.handleError(getContext(), intent.getStringExtra(YaraUtilityService.PARAM_MESSAGE));
        } else {
          restartLoader();
          mProgress.setVisibility(View.GONE);
        }
      }
    };
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    Log.d(LOG_TAG, "onCreateLoader");
    return new CursorLoader(
        getActivity(),
        RedditContract.SubmissionsEntry.CONTENT_URI,
        SUBMISSION_COLUMNS,
        null,
        null,
        RedditContract.SubmissionsEntry.COLUMN_SCORE + " DESC"
    );
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {

    Log.d(LOG_TAG, "onCreateView");
    View root = inflater.inflate(R.layout.fragment_submission_list, container, false);
    ButterKnife.bind(this, root);
    RecyclerView submissionsList = (RecyclerView) root.findViewById(R.id.submission_list);
    mSwipeRefreshLayout.setOnRefreshListener(
        new SwipeRefreshLayout.OnRefreshListener() {
          @Override
          public void onRefresh() {
            Log.d(LOG_TAG, "onRefresh");
            fetchSubreddits();
          }
        });
    RecyclerView.ItemDecoration itemDecoration = new
        DividerItemDecoration(this.getContext(), DividerItemDecoration.VERTICAL_LIST);
    submissionsList.addItemDecoration(itemDecoration);
    submissionsList.setLayoutManager(new LinearLayoutManager(getContext()));
    mAdapter = new SubmissionListAdapter(getActivity(), null, new RedditDownloadCallback() {
      @Override
      public void onDownloadComplete(Object result, String message) {
        Log.d(LOG_TAG, "QWERTY");
        if (message.equals(YaraUtilityService.STATUS_OK)) {
          restartLoader();
        } else {
          Utils.handleError(getContext(), message);
        }
      }
    });
    submissionsList.setAdapter(mAdapter);
    mEmpty.setVisibility(View.GONE);

    // ad unit
    MobileAds.initialize(getContext(), getContext().getResources().getString(R.string.pub_id));
    AdView mAdView = (AdView) root.findViewById(R.id.ad_view);
    AdRequest adRequest = new AdRequest.Builder().build();
    mAdView.loadAd(adRequest);

    return root;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    Log.d(LOG_TAG, "onLoadFinished");
    //Log.d(LOG_TAG, DatabaseUtils.dumpCursorToString(data));
    Log.d(LOG_TAG, "Logged in: " + Utils.isLoggedIn(getContext()));
    if (mProgress.getVisibility() == View.VISIBLE) {
      mProgress.setVisibility(View.GONE);
    }
    mAdapter.swapCursor(data);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    Log.d(LOG_TAG, "onLoaderReset");
    mAdapter.swapCursor(null);
  }

  @Override
  public void onPause() {
    Log.d(LOG_TAG, "onPause");
    super.onPause();
    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver((mReceiver));
  }

  @Override
  public void onResume() {
    Log.d(LOG_TAG, "onResume");
    getLoaderManager().restartLoader(SUBMISSIONS_LOADER, null, this);
    super.onResume();

    LocalBroadcastManager.getInstance(getContext()).registerReceiver(
        mReceiver, new IntentFilter(YaraUtilityService.ACTION_SUBMIT_VOTE));

    LocalBroadcastManager.getInstance(getContext()).registerReceiver(
        mReceiver, new IntentFilter(YaraUtilityService.ACTION_SUBREDDIT_UNSUBSCRIBE));

    LocalBroadcastManager.getInstance(getContext()).registerReceiver(
        mReceiver, new IntentFilter(SubredditSyncAdapter.ACTION_DATA_UPDATED));

    if (Utils.isLoggedIn(getContext()) && !Utils.isRefreshing(getContext())) {
      AuthenticationState state = AuthenticationManager.get().checkAuthState();
      Utils.updateAuth(getContext(), state, this);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    Log.d(LOG_TAG, "onSaveInstanceState");
    super.onSaveInstanceState(outState);
  }

  private void fetchSubreddits() {
    new FetchSubredditsTask(this.getContext(), new RedditDownloadCallback() {
      @Override
      public void onDownloadComplete(Object result, String message) {
        Log.d(LOG_TAG, "onDownloadComplete");

        HashMap<String, Subreddit> subreddits = (HashMap) result;
        if (mSwipeRefreshLayout.isRefreshing()) {
          mSwipeRefreshLayout.setRefreshing(false);
        }
        Log.d(LOG_TAG, "onDownloadComplete: " + subreddits.size());
        if (mProgress.getVisibility() == View.VISIBLE) {
          mProgress.setVisibility(View.GONE);
        }

        if (message.equals(YaraUtilityService.STATUS_OK)) {

          if (subreddits.size() > 0) {
            restartLoader();
          } else {
            mEmpty.setText(getContext().getString(R.string.error_no_subreddits));
            mEmpty.setVisibility(View.VISIBLE);
          }
        } else {
          Utils.handleError(getContext(), message);
        }
      }
    }).execute();
  }

  private void restartLoader() {
    if (isAdded()) {
      getLoaderManager().restartLoader(SUBMISSIONS_LOADER, null, this);
    }
  }

  public void onAccountRetrieved(LoggedInAccount account, String message) {
    Log.d(LOG_TAG, "onAccountRetrieved");
    if (!message.equals(YaraUtilityService.STATUS_OK)) {
      Utils.handleError(getContext(), message);
    } else {
      fetchSubreddits();
    }
  }

  private Cursor advertisementCursor(Cursor data) {
    MatrixCursor cursor = new MatrixCursor(data.getColumnNames());
    int count = 0;
    while (data.moveToNext()) {
      count++;
      if (count % 4 == 0) {
        cursor.addRow(new Object[]{"AD"});
      } else {
        Object[] row = new Object[]{
            cursor.getInt(COL_ID),
            cursor.getInt(COL_SUBMISSION_ID),
            cursor.getInt(COL_SUBREDDIT_ID),
            cursor.getString(COL_SUBREDDIT_NAME),
            cursor.getString(COL_TITLE),
            cursor.getString(COL_TEXT),
            cursor.getString(COL_AUTHOR),
            cursor.getString(COL_THUMBNAIL),
            cursor.getInt(COL_COMMENT_COUNT),
            cursor.getInt(COL_SCORE),
            cursor.getInt(COL_VOTE),
        };
        cursor.addRow(row);
      }
    }
    return cursor;
  }

}
