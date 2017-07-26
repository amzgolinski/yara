package com.amzgolinski.yara.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.amzgolinski.yara.R;

public class SubmissionDetailActivity extends AppCompatActivity {

  private static final String LOG_TAG = SubmissionDetailActivity.class.getName();
  private Uri mSubmissionUri;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_submission_detail);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

    setSupportActionBar(toolbar);

    mSubmissionUri = getIntent().getData();
    Log.d(LOG_TAG, mSubmissionUri.toString());
    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(SubmissionDetailActivity.this, SubmitCommentActivity.class);
        intent.setData(mSubmissionUri);
        startActivity(intent);
      }
    });

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

}
