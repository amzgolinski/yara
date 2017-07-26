package com.amzgolinski.yara.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.amzgolinski.yara.R;
import com.amzgolinski.yara.ui.SubredditFragment;

import java.util.ArrayList;

public class SubredditAdapter extends RecyclerView.Adapter<SubredditAdapter.ViewHolder> {

  private static final String LOG_TAG = SubredditAdapter.class.getName();

  // Store the context for easy access
  private Context mContext;
  private CursorAdapter mCursorAdapter;
  private ArrayList<String> mToRemove;

  public SubredditAdapter(Context context, Cursor cursor) {

    mToRemove = new ArrayList<>();

    mContext = context;
    mCursorAdapter = new CursorAdapter(mContext, cursor, 0) {

      @Override
      public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View subredditView = inflater.inflate(R.layout.subreddit_item, parent, false);
        return subredditView;
      }

      @Override
      public void bindView(View view, Context context, Cursor cursor) {
        //Log.d(LOG_TAG, DatabaseUtils.dumpCursorToString(cursor));
        TextView subredditNameTextView = (TextView) view.findViewById(R.id.subreddit_name);

        String subredditName = cursor.getString(SubredditFragment.COL_NAME);
        String formattedName = String.format(
            context.getResources().getString(R.string.subreddit_name),
            subredditName
        );
        subredditNameTextView.setText(formattedName);

        CheckBox checkBox = (CheckBox) view.findViewById(R.id.subreddit_checkBox);
        checkBox.setTag(subredditName);
        checkBox.setChecked(true);
        checkBox.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            mToRemove.add((String) v.getTag());
          }
        });
      }
    };
  }

  public void swapCursor(Cursor cursor) {
    //Log.d(LOG_TAG, "swapCursor");
    mCursorAdapter.swapCursor(cursor);
    notifyDataSetChanged();
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    // Passing the binding operation to cursor loader
    mCursorAdapter.getCursor().moveToPosition(position);
    mCursorAdapter.bindView(holder.itemView, mContext, mCursorAdapter.getCursor());

  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    // Passing the inflater job to the cursor-adapter
    View v = mCursorAdapter.newView(mContext, mCursorAdapter.getCursor(), parent);
    return new ViewHolder(v);
  }

  @Override
  public int getItemCount() {
    //Log.d(LOG_TAG, "swapCursor: " + mCursorAdapter.getCount());
    return mCursorAdapter.getCount();
  }

  // Provide a direct reference to each of the views within a data item
  // Used to cache the views within the item layout for fast access
  static class ViewHolder extends RecyclerView.ViewHolder {

    // Your holder should contain a member variable
    // for any view that will be set as you render a row
    TextView mSubredditName;

    // We also create a constructor that accepts the entire item row
    // and does the view lookups to find each subview
    public ViewHolder(View itemView) {

      // Stores the itemView in a public final member variable that can be used
      // to access the context from any ViewHolder instance.
      super(itemView);
      mSubredditName = (TextView) itemView.findViewById(R.id.subreddit_name);

    }
  }

  public ArrayList<String> getToRemove() {
    return mToRemove;
  }
}
