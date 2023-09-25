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
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.util.*

/**
 * Implementation of App Widget functionality.
 */
class TapToolsWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            val sharedPrefs = context.getSharedPreferences(context.getString(R.string.shared_config), Context.MODE_PRIVATE)
            val addressOrAsset = sharedPrefs.getString(context.getString(R.string.selection_key), "")
            UpdateTapToolsWidgetTask(context, appWidgetManager, appWidgetId).execute(addressOrAsset)
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

private class UpdateTapToolsWidgetTask(
    var context: Context,
    var appWidgetManager: AppWidgetManager,
    var appWidgetId: Int
) : AsyncTask<String, Integer, PortfolioInfo>() {

    private val ADA_KEY = "ADA"
    private val USD_KEY = "USD"
    private val USD_SYMBOL = "$"

    private val COINMARKETCAP_API = "https://pro-api.coinmarketcap.com"

    private val TAPTOOLS_SITE = "https://taptools.io"
    private val TAPTOOLS_API = "https://openapi.taptools.io/api/v1"

    private val POSITIONS_NFTS_KEY = "positionsNft"
    private val NUM_NFTS_KEY = "numNFTs"
    private val ADA_VALUE_KEY = "adaValue"

    private val MIN_WIDTH_FOR_ASSETS = 250

    var blockfrost : Blockfrost
    var walletAuth : WalletAuth
    var tapToolsKey : String
    var coinMarketKey : String

    init {
        blockfrost = Blockfrost(context)
        walletAuth = WalletAuth(blockfrost)
        tapToolsKey = context.getString(R.string.taptools_api_key)
        coinMarketKey = context.getString(R.string.coinmarket_api_key)
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

        val address = if (wallet.startsWith(Blockfrost.HANDLE_PREFIX)) blockfrost.lookupHandle(wallet) else wallet;

        with(URL("${TAPTOOLS_API}/wallet/portfolio/positions?address=${address}").openConnection() as HttpURLConnection) {
            setRequestProperty("x-api-key", tapToolsKey)

            val portfolioInfo = JSONObject(String(inputStream.readBytes(), StandardCharsets.UTF_8))
            val adaValueEstimate = portfolioInfo.getDouble(ADA_VALUE_KEY)
            val localCurrency = Currency.getInstance(Locale.getDefault())

            var fiatCurrencyStr = USD_SYMBOL
            var adaConversionRate : Double
            with(URL("${COINMARKETCAP_API}/v2/cryptocurrency/quotes/latest?symbol=${ADA_KEY}&convert=${localCurrency.currencyCode}").openConnection() as HttpURLConnection) {
                setRequestProperty("X-CMC_PRO_API_KEY", coinMarketKey)

                val coinMarketData = JSONObject(String(inputStream.readBytes(), StandardCharsets.UTF_8))
                val quotes = coinMarketData.getJSONObject("data").getJSONArray(ADA_KEY).getJSONObject(0).getJSONObject("quote")
                try {
                    adaConversionRate = quotes.getJSONObject(localCurrency.currencyCode).getDouble("price")
                    fiatCurrencyStr = localCurrency.symbol
                } catch (e : JSONException) {
                    // Could not find currency code, default to USD
                    adaConversionRate = quotes.getJSONObject(USD_KEY).getDouble("price")
                }
            }
            val fiatEstimate = adaValueEstimate * adaConversionRate

            val numProjects = portfolioInfo.getInt(NUM_NFTS_KEY)
            val nftPositions = portfolioInfo.getJSONArray(POSITIONS_NFTS_KEY)
            var numAssets = 0
            for (idx in 0 until nftPositions.length()) {
                numAssets += nftPositions.getJSONObject(idx).getInt("balance")
            }

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
        val views = RemoteViews(context.packageName, R.layout.taptools_widget_wide)
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

        val taptoolsUri = Uri.parse("${TAPTOOLS_SITE}/${portfolioInfo.wallet}")
        val launchUrl = Intent(Intent.ACTION_VIEW, taptoolsUri)
        val pendingIntent = PendingIntent.getActivity(context, appWidgetId, launchUrl, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.portfolioWidget, pendingIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

}