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
import java.nio.charset.StandardCharsets
import java.util.*
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
            UpdateViewerWidgetTask(context, appWidgetManager, appWidgetId).execute(addressOrAsset)
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

private class UpdateViewerWidgetTask(
    var context: Context,
    var appWidgetManager: AppWidgetManager,
    var appWidgetId: Int
) : AsyncTask<String, Integer, Bitmap>() {

    private val MAX_RETRIES : Int = 10
    private val MAX_BITMAP_SIZE : Double = 7500000.0
    private val MAX_WIDTH : Int = 1000
    private val MAX_HEIGHT : Int = 1000

    private val BASE64_ENCODING_MARKER : String = ";base64,"
    private val BASE64_ENCODED_PNG : String = "data:image/png"

    private var blockfrost : Blockfrost
    private var walletAuth : WalletAuth

    init {
        blockfrost = Blockfrost(context)
        walletAuth = WalletAuth(blockfrost)
    }

    override fun doInBackground(vararg addressOrAssets: String?): Bitmap? {
        val addressOrAsset = addressOrAssets[0]!!
        if (!walletAuth.isAuthorizedForViewer(addressOrAsset)) {
            return null
        }

        val eligibleImages = getNftUrls(addressOrAsset)
        for (attempt in 0 until MAX_RETRIES) {
            val nextAttemptIndex = Random().nextInt(eligibleImages.length())
            val nextAttempt = eligibleImages.getJSONObject(nextAttemptIndex)
            if (nextAttempt == null) {
                return null
            }

            try {
                val selectedImage = blockfrost.getNftMediaImage(nextAttempt)
                if (selectedImage != null) {
                    return when (selectedImage.first) {
                        "image/svg+xml" -> processSvg(selectedImage.second)
                        else -> processImage(selectedImage.second)
                    }
                }
            } catch (e: Exception) {
                // Ignore and continue
            }
        }
        return null
    }

    private fun getNftUrls(addressOrAsset: String): JSONArray {
        when (addressOrAsset.startsWith(PoolPm.ASSET_PREFIX)) {
            true -> {
                val jsonArray = JSONArray()
                jsonArray.put(PoolPm.convertToBlockfrost(addressOrAsset, blockfrost))
                return jsonArray
            }
            false -> {
                return blockfrost.getAddressTokens(addressOrAsset)
            }
        }
    }

    private fun processSvg(selectedImage: Any) : Bitmap {
        val imageBodyBytes : ByteArray = if (selectedImage is JSONArray) {
            convertJsonArrayToByteArray(selectedImage)
        } else {
            blockfrost.getDataMaybeFromIpfs(selectedImage.toString()).readBytes()
        }

        val imageBody = String(imageBodyBytes, StandardCharsets.UTF_8)
        val svg = SVG.getFromString(imageBody)
        val bitmap = Bitmap.createBitmap(MAX_WIDTH, MAX_HEIGHT, Bitmap.Config.ARGB_8888)
        val bmCanvas = Canvas(bitmap)
        bmCanvas.drawRGB(255, 255, 255) // Clear background to white
        svg.renderToCanvas(bmCanvas)
        return bitmap
    }

    private fun convertJsonArrayToByteArray(jsonArray: JSONArray) : ByteArray {
        val jsonAsString = StringBuilder()
        for (index in 0 until jsonArray.length()) {
            jsonAsString.append(jsonArray.get(index))
        }
        val dataString = jsonAsString.toString()
        if (dataString.contains(BASE64_ENCODING_MARKER)) {
            val rawData = dataString.substring(dataString.indexOf(BASE64_ENCODING_MARKER) + BASE64_ENCODING_MARKER.length)
            return Base64.decode(rawData, Base64.DEFAULT)
        } else {
            return ByteArray(0)
        }
    }

    private fun processImage(selectedImage: Any) : Bitmap? {
        if (selectedImage is JSONArray) {
            val decodedRawImage = convertJsonArrayToByteArray(selectedImage)
            return BitmapFactory.decodeByteArray(decodedRawImage, 0, decodedRawImage.size)
        } else {
            val imageInputStream = blockfrost.getDataMaybeFromIpfs(selectedImage.toString())
            return BitmapFactory.decodeStream(imageInputStream)
        }
    }

    override fun onPostExecute(image: Bitmap?) {
        val scaledImage : Bitmap
        if (image == null) {
            scaledImage = BitmapFactory.decodeResource(context.resources, R.mipmap.example_appwidget_preview_foreground)
        } else if (image.byteCount > MAX_BITMAP_SIZE) {
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