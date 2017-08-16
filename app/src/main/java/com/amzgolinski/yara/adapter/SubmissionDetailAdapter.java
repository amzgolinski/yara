package com.amzgolinski.yara.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.TextView;

import com.amzgolinski.yara.R;
import com.amzgolinski.yara.service.YaraUtilityService;
import com.amzgolinski.yara.ui.SubmissionDetailFragment;
import com.amzgolinski.yara.ui.SubmissionListFragment;
import com.amzgolinski.yara.util.RedditUtils;
import com.amzgolinski.yara.util.StringUtils;
import com.squareup.picasso.Picasso;

import net.dean.jraw.models.VoteDirection;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SubmissionDetailAdapter extends CursorAdapter {

  private static final String LOG_TAG = SubmissionDetailAdapter.class.getName();

  @BindView(R.id.submission_detail_title) TextView mSubmissionTitle;
  @BindView(R.id.submission_detail_text) TextView mSubmissionText;
  @BindView(R.id.submission_detail_subreddit_name) TextView mSubredditName;
  @BindView(R.id.submission_detail_author) TextView mAuthor;
  @BindView(R.id.submission_detail_image) ImageView mThumbnail;


  public SubmissionDetailAdapter(Context context, Cursor c, int flags) {
    super(context, c, flags);
  }

  @Override
  public View newView(Context context, Cursor data, ViewGroup parent) {
    LayoutInflater inflater = LayoutInflater.from(context);
    View detailView = inflater.inflate(R.layout.submission_detail, parent, false);
    ButterKnife.bind(this, detailView);
    return detailView;
  }

  @Override
  public void bindView(View view, final Context context, final Cursor cursor) {

    String submissionId
        = Integer.toString(cursor.getInt(SubmissionListFragment.COL_SUBMISSION_ID));

    // submission title
    mSubmissionTitle.setText(cursor.getString(SubmissionDetailFragment.COL_TITLE));

    // submission subreddit name
    String subredditText = String.format(
        mContext.getResources().getString(R.string.subreddit_name),
        cursor.getString(SubmissionDetailFragment.COL_SUBREDDIT_NAME)
    );
    mSubredditName.setText(subredditText);
    String selfText = cursor.getString(SubmissionDetailFragment.COL_TEXT);
    if (StringUtils.isStringEmpty(selfText)) {
      mSubmissionText.setVisibility(View.GONE);
    } else {
      mSubmissionText.setText(Html.fromHtml(selfText));
      mSubmissionText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    // author
    mAuthor.setText(cursor.getString(SubmissionDetailFragment.COL_AUTHOR));

    // submission comment count
    TextView commentCount = (TextView) view.findViewById(R.id.submission_detail_comments);
    commentCount.setText(
        String.format(
            context.getResources().getString(R.string.submission_comment_count),
            cursor.getInt(SubmissionDetailFragment.COL_COMMENT_COUNT))
    );

    // submission score
    String score = cursor.getString(SubmissionDetailFragment.COL_SCORE);
    TextView scoreView = (TextView) view.findViewById(R.id.submission_detail_score);
    scoreView.setText(score);

    // submission thumbnail
    String thumbnail = cursor.getString(SubmissionDetailFragment.COL_THUMBNAIL);
    Log.d(LOG_TAG, "URL: " + thumbnail);
    final String url = cursor.getString(SubmissionDetailFragment.COL_URL);
    if (!StringUtils.isStringEmpty(thumbnail) && URLUtil.isValidUrl(thumbnail)) {
      Picasso.with(context)
          .load(thumbnail)
          .error(R.drawable.ic_do_not_distrub_black_24dp)
          .into(mThumbnail);

      if (!StringUtils.isStringEmpty(url)) {
        mThumbnail.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            if (intent.resolveActivity(context.getPackageManager()) != null) {
              context.startActivity(intent);
            }

          }
        });
      }
    } else {
      mThumbnail.setVisibility(View.GONE);
    }

    int vote  = cursor.getInt(SubmissionDetailFragment.COL_VOTE);
    ImageView upArrowView = (ImageView) view.findViewById(R.id.up_arrow);
    Drawable upArrow = context.getDrawable(R.drawable.ic_arrow_up_bold_grey600_24dp);
    if (vote == VoteDirection.UPVOTE.getValue()) {
      upArrow.setTint(ContextCompat.getColor(context, R.color.accent));
    } else {
      upArrow.setTint(ContextCompat.getColor(context, R.color.gray_600));
    }
    upArrowView.setImageDrawable(upArrow);
    upArrowView.setTag(R.string.submission_id, submissionId);
    upArrowView.setTag(R.string.vote, vote);
    upArrowView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        long submissionId = Long.parseLong((String)v.getTag(R.string.submission_id));
        int vote = (Integer) v.getTag(R.string.vote);
        YaraUtilityService.submitVote(mContext, submissionId, vote, RedditUtils.UPVOTE);
      }
    });

    ImageView downArrowView = (ImageView) view.findViewById(R.id.down_arrow);
    Drawable downArrow = context.getDrawable(R.drawable.ic_arrow_down_bold_grey600_24dp);
    if (vote == VoteDirection.DOWNVOTE.getValue()) {
      downArrow.setTint(ContextCompat.getColor(context, R.color.accent));
    } else {
      downArrow.setTint(ContextCompat.getColor(context, R.color.gray_600));
    }
    downArrowView.setImageDrawable(downArrow);
    downArrowView.setTag(R.string.submission_id, submissionId);
    downArrowView.setTag(R.string.vote, vote);
    downArrowView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        long submissionId = Long.parseLong((String)v.getTag(R.string.submission_id));
        int vote = (Integer) v.getTag(R.string.vote);
        YaraUtilityService.submitVote(mContext, submissionId, vote, RedditUtils.DOWNVOTE);
      }
    });
  }

}
