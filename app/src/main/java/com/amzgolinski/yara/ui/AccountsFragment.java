package com.amzgolinski.yara.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.amzgolinski.yara.R;
import com.amzgolinski.yara.activity.LoginActivity;
import com.amzgolinski.yara.service.YaraUtilityService;
import com.amzgolinski.yara.util.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AccountsFragment extends Fragment {

  private static final String LOG_TAG = AccountsFragment.class.getName();

  @BindView(R.id.account_name) TextView mAccountName;
  @BindView(R.id.delete_account_button) ImageButton mDeleteButton;
  @BindView(R.id.divider) View mDivider;
  @BindView(R.id.account_image) ImageView mImageView;
  @BindView(R.id.add_account_button) ImageButton mAddAccountButton;
  @BindView(R.id.add_account_title) TextView mAddAccountTitle;

  private String mUsername;
  private BroadcastReceiver mReceiver;

  public AccountsFragment() {
    // empty
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mReceiver = new BroadcastReceiver() {

      @Override
      public void onReceive(Context context, Intent intent) {
        getActivity().onBackPressed();
      }
    };
  }

  @Override
  public void onPause() {
    super.onPause();
    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver((mReceiver));
  }

  @Override
  public void onResume() {
    super.onResume();
    LocalBroadcastManager.getInstance(getContext()).registerReceiver(
        mReceiver, new IntentFilter(YaraUtilityService.ACTION_DELETE_ACCOUNT));
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {

    View root = inflater.inflate(R.layout.fragment_accounts, container, false);
    ButterKnife.bind(this, root);
    return root;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    final Context context = this.getContext();
    if (Utils.isLoggedIn(context)) {

      mUsername = Utils.getCurrentUser(context);
      mAccountName.setText(mUsername);

      mDeleteButton.setOnClickListener(new View.OnClickListener() {

        public void onClick(View view) {
          YaraUtilityService.deleteAccount(context);

        }
      });
      mAddAccountTitle.setTextColor(getContext().getResources().getColor(R.color.gray_700, null));
      Drawable addButton = getResources().getDrawable(R.drawable.ic_add_circle_outline_black_24dp);
      addButton.setTint(getResources().getColor(R.color.gray_700, null));
      mAddAccountButton.setImageDrawable(addButton);
      mAddAccountButton.setClickable(false);

    } else {

      mAccountName.setVisibility(View.GONE);
      mDivider.setVisibility(View.GONE);
      mImageView.setVisibility(View.GONE);
      mDeleteButton.setVisibility(View.GONE);
      mAddAccountButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (Utils.isNetworkAvailable(getContext())) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            startActivity(new Intent(context, LoginActivity.class));
          } else {
            Utils.showToast(
                getContext(),
                getContext().getResources().getString(R.string.error_no_internet)
            );
          }
        }
      });
    }
  }
}
