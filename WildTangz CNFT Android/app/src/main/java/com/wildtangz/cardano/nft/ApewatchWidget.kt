package com.wildtangz.cardano.nft

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.util.*


/**
 * Implementation of App Widget functionality.
 */
class ApewatchWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            val sharedPrefs = context.getSharedPreferences(context.getString(R.string.shared_config), Context.MODE_PRIVATE)
            val addressOrAsset = sharedPrefs.getString(context.getString(R.string.selection_key), "")
            UpdatePortfolioWidgetTask(context, appWidgetManager, appWidgetId).execute(addressOrAsset)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?
    ) {
        if (context != null) {
            val widgetIds = IntArray(1)
            widgetIds[0] = appWidgetId
            onUpdate(context, appWidgetManager, widgetIds)
        }
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

private class PortfolioInfo(
    val wallet: String,
    val numAssets: Int,
    val numProjects: Int,
    val adaValueEstimate: Double,
    val adaToUsd: Double,
    val isAuthorized: Boolean) {
}

private class UpdatePortfolioWidgetTask(
    var context: Context,
    var appWidgetManager: AppWidgetManager,
    var appWidgetId: Int
) : AsyncTask<String, Integer, PortfolioInfo>() {

    private val CARDANO_KEY = "cardano"
    private val USD_KEY = "usd"

    private val APEWATCH_API: String = "https://apewatch.app/address"
    private val APEWATCHAPP_STDOPTS: String = "?status=owned%2Clisted&show=nfts"
    private val APEWATCH_REQD_HEADERS: Map<String, String> = mapOf(
        Pair("Content-Type", "application/json"),
        Pair("x-inertia-partial-component", "address/show"),
        Pair("x-inertia", "true"),
        Pair("x-inertia-partial-data", "assets"),
        Pair("x-inertia-version", "096a8c34ea37cce3f200dba88000a3b0")
    )

    private val PROPERTIES_KEY = "props"
    private val ASSETS_KEY = "assets"
    private val NFTS_KEY = "nfts"
    private val NFTS_VALUE_KEY = "value"
    private val NFTS_ASSETS_KEY = "assets"
    private val NFTS_ASSETS_STATUS_KEY = "status"

    private val TRANSFERRED = "transferred"

    private val COINGECKO_ADA_API = "https://api.coingecko.com/api/v3/simple/price?ids=cardano&vs_currencies=usd"

    private val MIN_WIDTH_FOR_ASSETS = 250

    var blockfrost : Blockfrost
    var walletAuth : WalletAuth

    init {
        blockfrost = Blockfrost(context)
        walletAuth = WalletAuth(blockfrost)
    }

    override fun doInBackground(vararg params: String?): PortfolioInfo {
        val wallet = blockfrost.getWalletName(params[0]!!)
        if (!walletAuth.isAuthorizedForPortfolio(wallet)) {
            return PortfolioInfo(
                wallet = wallet,
                numAssets = 0,
                numProjects = 0,
                adaValueEstimate = 0.0,
                adaToUsd = 0.0,
                isAuthorized = false
            )
        }

        val currPrice = JSONObject(URL(COINGECKO_ADA_API).readText(StandardCharsets.UTF_8))
        val adaToUsd = currPrice.getJSONObject(CARDANO_KEY).getDouble(USD_KEY)

        with(URL("${APEWATCH_API}/${wallet}?${APEWATCHAPP_STDOPTS}").openConnection() as HttpURLConnection) {
            APEWATCH_REQD_HEADERS.forEach { (key, value) ->
                setRequestProperty(key, value)
            }

            val portfolioInfo = JSONObject(String(inputStream.readBytes(), StandardCharsets.UTF_8))
            val nfts = portfolioInfo.getJSONObject(PROPERTIES_KEY).getJSONObject(ASSETS_KEY).getJSONArray(NFTS_KEY)

            var numProjects = 0
            var adaValueEstimate = 0.0
            var numAssets = 0
            for (nftIndex in 0 until nfts.length()) {
                val nft = nfts.getJSONObject(nftIndex)
                numProjects++
                adaValueEstimate += nft.getDouble(NFTS_VALUE_KEY)
                val assets = nft.getJSONArray(NFTS_ASSETS_KEY)
                for (assetIndex in 0 until assets.length()) {
                    val asset = assets.getJSONObject(assetIndex)
                    if (asset.getString(NFTS_ASSETS_STATUS_KEY).contentEquals(TRANSFERRED, ignoreCase = true)) {
                        continue
                    }
                    numAssets++
                }
            }

            return PortfolioInfo(
                wallet = wallet,
                numAssets = numAssets,
                numProjects = numProjects,
                adaValueEstimate = adaValueEstimate,
                adaToUsd = adaToUsd,
                isAuthorized = true
            )
        }
    }

    override fun onPostExecute(portfolioInfo: PortfolioInfo?) {
        if (portfolioInfo == null) {
            return
        }

        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.apewatch_widget_wide)
        with(views) {
            setTextViewText(R.id.portfolioSelection, portfolioInfo.wallet)
            setTextViewText(R.id.portfolioUpdate,"As of ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())}")

            if (!portfolioInfo.isAuthorized) {
                setTextViewText(R.id.portfolioValueUsd, walletAuth.unauthorizedForPortfolioMsg())
                setTextViewText(R.id.portfolioValue, "")
                setTextViewText(R.id.portfolioNumAssets, "")
                setTextViewText(R.id.portfolioNumProjects, "")
                return@with
            }

            setTextViewText(R.id.portfolioValue, String.format("₳%,.2f", portfolioInfo.adaValueEstimate))
            setTextViewText(R.id.portfolioValueUsd, String.format("$%,.2f", portfolioInfo.adaValueEstimate * portfolioInfo.adaToUsd))

            // See the dimensions and only add assets/projects if wide enough
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            if (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) > MIN_WIDTH_FOR_ASSETS) {
                setTextViewText(R.id.portfolioNumAssets, String.format("%d Assets", portfolioInfo.numAssets))
                setTextViewText(R.id.portfolioNumProjects, String.format("%d Projects", portfolioInfo.numProjects))
            } else {
                setTextViewText(R.id.portfolioNumAssets, "")
                setTextViewText(R.id.portfolioNumProjects, "")
            }
        }

        val apewatchUri = Uri.parse("${APEWATCH_API}/${portfolioInfo.wallet}")
        val launchUrl = Intent(Intent.ACTION_VIEW, apewatchUri)
        val pendingIntent = PendingIntent.getActivity(context, appWidgetId, launchUrl, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.portfolioWidget, pendingIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

}