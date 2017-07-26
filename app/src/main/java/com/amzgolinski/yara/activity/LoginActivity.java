package com.amzgolinski.yara.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import com.amzgolinski.yara.R;
import com.amzgolinski.yara.YaraApplication;
import com.amzgolinski.yara.service.YaraUtilityService;
import com.amzgolinski.yara.util.Utils;

import java.net.URL;

public class LoginActivity extends AppCompatActivity {

  private static final String LOG_TAG = LoginActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    // Create our RedditClient
    final OAuthHelper helper = AuthenticationManager.get().getRedditClient().getOAuthHelper();

    // OAuth2 scopes to request. See https://www.reddit.com/dev/api/oauth for a full list
    String[] scopes = getResources().getStringArray(R.array.scopes);

    final URL authorizationUrl =
        helper.getAuthorizationUrl(YaraApplication.CREDENTIALS, true, true, scopes);

    final WebView webView = ((WebView) findViewById(R.id.webview));
    webView.clearHistory();
    webView.clearFormData();
    // Load the authorization URL into the browser
    webView.loadUrl(authorizationUrl.toExternalForm());
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (url.contains("code=")) {
          // We've detected the redirect URL
          onUserChallenge(url, YaraApplication.CREDENTIALS);
        } else if (url.contains("error=")) {
          Toast.makeText(
              LoginActivity.this,
              getResources().getString(R.string.press_allow),
              Toast.LENGTH_SHORT).show();
          webView.loadUrl(authorizationUrl.toExternalForm());
        }
      }
    });
  }

  private void onUserChallenge(final String url, final Credentials creds) {
    new AsyncTask<String, Void, String>() {
      @Override
      protected String doInBackground(String... params) {
        try {
          OAuthData data = AuthenticationManager
              .get()
              .getRedditClient()
              .getOAuthHelper()
              .onUserChallenge(params[0], creds);
          AuthenticationManager.get().getRedditClient().authenticate(data);

          String user = AuthenticationManager.get().getRedditClient().getAuthenticatedUser();
          String token =
              AuthenticationManager.get().getRedditClient().getOAuthHelper().getRefreshToken();

          Utils.setCurrentUser(LoginActivity.this.getApplicationContext(), user);

          return AuthenticationManager.get().getRedditClient().getAuthenticatedUser();
        } catch (NetworkException networkException) {
          Log.e(LOG_TAG, "Error logging into account.", networkException);
          Utils.handleError(LoginActivity.this, YaraUtilityService.STATUS_NETWORK_EXCEPTION);
          return null;
        } catch (OAuthException oauthException) {
          Log.e(LOG_TAG, "Could not log in", oauthException);
          Utils.handleError(LoginActivity.this, YaraUtilityService.STATUS_AUTH_EXCEPTION);
          return null;
        }
      }

      @Override
      protected void onPostExecute(String s) {
        Log.i(LOG_TAG, s);
        LoginActivity.this.finish();
        // start the SubmissionListActivity
        Intent intent =
            new Intent(LoginActivity.this.getApplicationContext(), SubmissionListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
      }
    }.execute(url);
  }
}