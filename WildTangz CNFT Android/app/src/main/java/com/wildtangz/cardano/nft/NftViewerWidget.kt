package com.wildtangz.cardano.nft

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.AsyncTask
import android.util.Base64
import android.widget.RemoteViews
import com.caverock.androidsvg.SVG
import org.json.JSONArray
import java.net.URL
import kotlin.math.roundToInt


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

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
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
    private val MAX_BITMAP_SIZE : Double = 18000000.0
    private val MAX_WIDTH : Int = 1800
    private val MAX_HEIGHT : Int = 1800
    private val BASE64_ENCODED_PNG : String = "data:image/png;base64"

    override fun doInBackground(vararg addressOrAssets: String?): Bitmap? {
        val eligibleImages = PoolPm.getNftUrls(addressOrAssets[0]!!)
        for (attempt in 0 until MAX_RETRIES) {
            val selectedImage = eligibleImages.randomOrNull()
            if (selectedImage == null) {
                return null
            }

            try {
                // TODO: If an array support (on-chain), parse SVG (switch on case using when)
                return when (selectedImage.first) {
                    "image/svg+xml" -> processSvg(selectedImage.second)
                    else -> processImage(selectedImage.second)
                }
            } catch (e: Exception) {
                // Ignore and continue
            }
        }
        return null
    }

    private fun processSvg(selectedImage: Any) : Bitmap {
        val imageBody = URL(PoolPm.convertedToWeb(selectedImage.toString())).readText()
        val svg = SVG.getFromString(imageBody)
        val bitmap = Bitmap.createBitmap(MAX_WIDTH, MAX_HEIGHT, Bitmap.Config.ARGB_8888)
        val bmCanvas = Canvas(bitmap)
        bmCanvas.drawRGB(255, 255, 255) // Clear background to white
        svg.renderToCanvas(bmCanvas)
        return bitmap
    }

    private fun processImage(selectedImage: Any) : Bitmap? {
        if (selectedImage is JSONArray) {
            val rawImage = StringBuilder()
            for (index in 0 until selectedImage.length()) {
                rawImage.append(selectedImage.get(index))
            }
            return parseStringToBitmap(rawImage.toString())
        } else {
            val imageUrl = URL(PoolPm.convertedToWeb(selectedImage.toString()))
            return BitmapFactory.decodeStream(imageUrl.openConnection().getInputStream())
        }
    }

    private fun parseStringToBitmap(rawImage: String) : Bitmap? {
        if (rawImage.startsWith(BASE64_ENCODED_PNG)) {
            val decodedRawImage : ByteArray = Base64.decode(rawImage.substring(BASE64_ENCODED_PNG.length + 1), Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedRawImage, 0, decodedRawImage.size)
        }
        return null
    }

    override fun onPostExecute(image: Bitmap?) {
        if (image == null) {
            return
        }

        val scaledImage : Bitmap
        if (image.byteCount > MAX_BITMAP_SIZE) {
            val scale = MAX_BITMAP_SIZE / image.byteCount
            scaledImage = Bitmap.createScaledBitmap(image, (scale * image.width).roundToInt(), (scale * image.height).roundToInt(), true)
            image.recycle()
        } else {
            scaledImage = image
        }

        val views = RemoteViews(context.packageName, R.layout.nft_viewer_widget)
        views.setImageViewBitmap(R.id.imageView, scaledImage)

        val widgetIds = IntArray(1)
        widgetIds[0] = appWidgetId

        val intent = Intent(context, NftViewerWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.relativeLayout, pendingIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

}