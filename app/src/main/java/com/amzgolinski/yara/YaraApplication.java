package com.amzgolinski.yara;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.http.LoggingMode;
import net.dean.jraw.http.oauth.Credentials;


public class YaraApplication extends Application {

  public static final Credentials CREDENTIALS =
      Credentials.installedApp(
          "zmhW2FxYKlE5cQ",
          "https://github.com/amzgolinski/udacity-capstone"
      );

  @Override
  public void onCreate() {
    super.onCreate();
    RedditClient reddit = new AndroidRedditClient(this);
    reddit.setLoggingMode(LoggingMode.NEVER);
    AuthenticationManager.get().init(
        reddit,
        new RefreshTokenHandler(new AndroidTokenStore(this), reddit));
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    MultiDex.install(this);
  }
}
