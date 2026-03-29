package com.example.goodstart.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.goodstart.R
import com.example.goodstart.tracker.StudyTracker

class StudyWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        StudyListFactory(applicationContext)
}

private class StudyListFactory(private val ctx: Context) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<Pair<String, Boolean>> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items = StudyTracker.getTodayStatus(ctx)
    }

    override fun onDestroy() {}

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val (key, done) = items[position]
        val label = StudyTracker.STUDY_LABELS[key] ?: key

        val rv = RemoteViews(ctx.packageName, R.layout.widget_study_item)
        rv.setTextViewText(R.id.item_label, label)
        rv.setImageViewResource(
            R.id.item_check,
            if (done) R.drawable.ic_widget_check_on
            else R.drawable.ic_widget_check_off
        )

        // Fill-in intent carries the study key for the toggle action
        val fillIntent = Intent().apply {
            putExtra(StudyWidgetProvider.EXTRA_STUDY_KEY, key)
        }
        rv.setOnClickFillInIntent(R.id.item_root, fillIntent)

        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
