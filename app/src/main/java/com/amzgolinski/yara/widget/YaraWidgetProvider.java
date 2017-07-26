package com.amzgolinski.yara.widget;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.amzgolinski.yara.R;
import com.amzgolinski.yara.activity.SubmissionDetailActivity;
import com.amzgolinski.yara.activity.SubmissionListActivity;
import com.amzgolinski.yara.service.YaraUtilityService;
import com.amzgolinski.yara.service.YaraWidgetService;


public class YaraWidgetProvider extends AppWidgetProvider {

  public static final String LOG_TAG = YaraWidgetProvider.class.getName();
  public static final String TOAST_ACTION = "com.example.android.stackwidget.TOAST_ACTION";
  public static final String EXTRA_ITEM = "com.example.android.stackwidget.EXTRA_ITEM";

  static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                              int appWidgetId) {

    CharSequence widgetText = context.getString(R.string.appwidget_text);
    // Construct the RemoteViews object
    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_submissions);

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views);
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

    Log.d(LOG_TAG, "onUpdate");

    for (int appWidgetId : appWidgetIds) {
      RemoteViews views =
          new RemoteViews(context.getPackageName(), R.layout.widget_submissions);

      // Create an Intent to launch MainActivity

      Intent mainActivity = new Intent(context, SubmissionDetailActivity.class);

      PendingIntent pendingIntent =
          PendingIntent.getActivity(context, 0, mainActivity, 0);
      views.setPendingIntentTemplate(R.id.widget_submission_list, pendingIntent);

      views.setRemoteAdapter(
          R.id.widget_submission_list,
          new Intent(context, YaraWidgetService.class)
      );

      Intent test = new Intent(context, SubmissionDetailActivity.class);
      PendingIntent clickPendingIntentTemplate =
          TaskStackBuilder.create(context)
              .addNextIntentWithParentStack(test)
              .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

      views.setPendingIntentTemplate(
          R.id.widget_submission_list,
          clickPendingIntentTemplate);

      views.setEmptyView(R.id.widget_submission_list, R.id.empty_view);

      appWidgetManager.updateAppWidget(appWidgetId, views);


    }
    super.onUpdate(context, appWidgetManager, appWidgetIds);

  }

  @Override
  public void onEnabled(Context context) {
    Log.d(LOG_TAG, "onEnabled");
    super.onEnabled(context);
  }

  @Override
  public void onDisabled(Context context) {
    Log.d(LOG_TAG, "onDisabled");
    super.onDisabled(context);
  }

  @Override
  public void onDeleted(Context context, int[] appWidgetIds) {
    Log.d(LOG_TAG, "onDeleted");
    super.onDeleted(context, appWidgetIds);
  }


  @Override
  public void onReceive(Context context, Intent intent) {

    Log.d(LOG_TAG, "onReceive: " + intent.getAction());

    super.onReceive(context, intent);

    Log.d(LOG_TAG, intent.getAction());
    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()));
    Log.d(LOG_TAG, "Length: " + appWidgetIds.length);
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_submission_list);

  }

}

