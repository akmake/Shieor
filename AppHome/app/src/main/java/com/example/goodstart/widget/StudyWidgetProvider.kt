package com.example.goodstart.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.goodstart.R
import com.example.goodstart.tracker.StudyTracker

class StudyWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.example.goodstart.ACTION_STUDY_TOGGLE"
        const val EXTRA_STUDY_KEY = "extra_study_key"

        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, StudyWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_study_list)
                ids.forEach { updateWidget(context, mgr, it) }
            }
        }

        private fun updateWidget(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_study_checklist)

            // Progress text
            val done = StudyTracker.completedCount(context)
            val total = StudyTracker.totalEnabled(context)
            views.setTextViewText(R.id.widget_progress, "$done/$total")

            // Set up the list adapter
            val serviceIntent = Intent(context, StudyWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_study_list, serviceIntent)

            // Template for click on list items → toggle completion
            val toggleIntent = Intent(context, StudyWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
            }
            val togglePi = PendingIntent.getBroadcast(
                context, 0, toggleIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setPendingIntentTemplate(R.id.widget_study_list, togglePi)

            mgr.updateAppWidget(widgetId, views)
        }
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { updateWidget(context, mgr, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val key = intent.getStringExtra(EXTRA_STUDY_KEY) ?: return
            StudyTracker.toggleCompleted(context, key)
            updateAll(context)
        }
    }
}
