package com.wildtangz.cardano.nft

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.URL
import java.nio.charset.Charset


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

    private val BASE_URL : String = "https://pool.pm/wallet"
    private val CHARSET : String = "UTF-8"

    private val IPFS_GATEWAY : String = "https://infura-ipfs.io/ipfs"
    private val IPFS_PROTOCOL : String = "ipfs://"
    private val IPFS_V1_START : String = "Q"
    private val IPFS_V2 : String = "bafy"

    private val MAX_RETRIES : Int = 10

    override fun doInBackground(vararg addressOrAssets: String?): Bitmap? {
        // TODO: Support individual assets
        val addressOrAsset = addressOrAssets[0]

        val userWalletText = URL("${BASE_URL}/${addressOrAsset}").readText(Charset.forName(CHARSET))
        val userWallet = JSONObject(userWalletText)
        val tokens = userWallet.getJSONArray("tokens")
        val eligibleImages = mutableListOf<Any>()
        for (index in 0 until tokens.length()) {
            val token = tokens.getJSONObject(index)
            if (token.getInt("quantity") == 1 && token.has("metadata")) {
                val tokenMetadata = token.getJSONObject("metadata")
                if (tokenMetadata.has("image")) {
                    eligibleImages.add(tokenMetadata.get("image"))
                }
            }
        }

        // TODO: Gatekeep on owning a Tangz

        for (attempt in 0 until MAX_RETRIES) {
            val selectedImage = eligibleImages.randomOrNull()
            if (selectedImage == null) {
                return null
            }

            try {
                // TODO: If an array support (on-chain), parse SVG (switch on case using when)

                val imageUrl = URL(convertedToWeb(selectedImage.toString()))
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

    private fun convertedToWeb(imageUrl: String): String {
        if (imageUrl.startsWith(IPFS_PROTOCOL)) {
            val cidv0 = imageUrl.substring(imageUrl.indexOf(IPFS_V1_START))
            return "${IPFS_GATEWAY}/${cidv0}"
        }

        if (imageUrl.startsWith(IPFS_V2)) {
            return "${IPFS_GATEWAY}/${imageUrl}"
        }

        return imageUrl
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