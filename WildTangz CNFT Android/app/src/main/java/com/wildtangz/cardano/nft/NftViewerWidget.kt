package com.wildtangz.cardano.nft

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.widget.RemoteViews
import java.net.URL


/**
 * Implementation of App Widget functionality.
 */
class NftViewerWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            val sharedPrefs = context.getSharedPreferences(context.getString(R.string.shared_config), Context.MODE_PRIVATE)
            val addressOrAsset = sharedPrefs.getString(context.getString(R.string.selection_key), "")
            UpdateWidgetTask(context, appWidgetManager, appWidgetId).execute(addressOrAsset)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

private class UpdateWidgetTask(
    var context: Context,
    var appWidgetManager: AppWidgetManager,
    var appWidgetId: Int
) : AsyncTask<String, Integer, Bitmap>() {

    private val MAX_RETRIES : Int = 10

    override fun doInBackground(vararg addressOrAssets: String?): Bitmap? {
        val eligibleImages = PoolPm.getNftUrls(addressOrAssets[0]!!)
        for (attempt in 0 until MAX_RETRIES) {
            val selectedImage = eligibleImages.randomOrNull()
            if (selectedImage == null) {
                return null
            }

            try {
                // TODO: If an array support (on-chain), parse SVG (switch on case using when)
                val imageUrl = URL(PoolPm.convertedToWeb(selectedImage.toString()))
                return BitmapFactory.decodeStream(
                    imageUrl.openConnection().getInputStream(),
                    null,
                    getBitmapOptions()
                )
            } catch (e: Exception) {
                // Ignore and continue
            }
        }
        return null
    }

    private fun getBitmapOptions() : BitmapFactory.Options {
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inSampleSize = 4
        return bitmapOptions
    }

    override fun onPostExecute(image: Bitmap?) {
        if (image == null) {
            return
        }

        val views = RemoteViews(context.packageName, R.layout.nft_viewer_widget)
        views.setImageViewBitmap(R.id.imageView, image)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

}