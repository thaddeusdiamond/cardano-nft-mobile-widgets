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
    val fiatEstimate: Double,
    val fiatCurrencyStr: String,
    val isAuthorized: Boolean) {
}

private class UpdatePortfolioWidgetTask(
    var context: Context,
    var appWidgetManager: AppWidgetManager,
    var appWidgetId: Int
) : AsyncTask<String, Integer, PortfolioInfo>() {

    private val ADA_KEY = "ADA"
    private val USD_KEY = "USD"
    private val USD_SYMBOL = "$"

    private val APEWATCH_API = "https://apewatch.app/api/v1/account"

    private val ASSETS_KEY = "assets"
    private val COUNTS_KEY = "counts"
    private val DEFAULT_KEY = "default"
    private val POLICIES_KEY = "policies"
    private val TOTAL_KEY = "total"
    private val VALUES_KEY = "values"

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
                fiatEstimate = 0.0,
                fiatCurrencyStr = "",
                isAuthorized = false
            )
        }

        with(URL("${APEWATCH_API}/${wallet}").openConnection() as HttpURLConnection) {
            val portfolioInfo = JSONObject(String(inputStream.readBytes(), StandardCharsets.UTF_8))
            val nfts = portfolioInfo.getJSONObject(VALUES_KEY).getJSONObject(TOTAL_KEY).getJSONObject(DEFAULT_KEY)

            val nftValuations = nfts.getJSONObject(VALUES_KEY)
            val adaValueEstimate = nftValuations.getDouble(ADA_KEY)
            val localCurrency = Currency.getInstance(Locale.getDefault())
            val fiatEstimate : Double
            val fiatCurrencyStr : String
            if (nftValuations.has(localCurrency.currencyCode)) {
                fiatEstimate = nftValuations.getDouble(localCurrency.currencyCode)
                fiatCurrencyStr = localCurrency.symbol
            } else {
                fiatEstimate = nftValuations.getDouble(USD_KEY)
                fiatCurrencyStr = USD_SYMBOL
            }

            val counts = portfolioInfo.getJSONObject(COUNTS_KEY)
            val numProjects = counts.getInt(POLICIES_KEY)
            val numAssets = counts.getJSONObject(ASSETS_KEY).getInt(TOTAL_KEY)

            return PortfolioInfo(
                wallet = wallet,
                numAssets = numAssets,
                numProjects = numProjects,
                adaValueEstimate = adaValueEstimate,
                fiatEstimate = fiatEstimate,
                fiatCurrencyStr = fiatCurrencyStr,
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
            setTextViewText(R.id.portfolioValueUsd, String.format("${portfolioInfo.fiatCurrencyStr}%,.2f", portfolioInfo.fiatEstimate))

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