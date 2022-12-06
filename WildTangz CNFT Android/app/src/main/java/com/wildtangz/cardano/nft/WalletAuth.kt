package com.wildtangz.cardano.nft

import android.content.SharedPreferences
import android.os.AsyncTask
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray

class WalletAuth(var blockfrost : Blockfrost) {

    val NFTS_FOR_VIEWER = mapOf(
        "33568ad11f93b3e79ae8dee5ad928ded72adcea719e92108caf1521b" to 1,
        "33566617519280305e147975f80914cea1c93e8049567829f7370fca" to 1,
        "335695f7771bb789083b8a985308310ab8f0a4bbf8cd0687bbdb26b1" to 1
    )
    val NFTS_FOR_PORTFOLIO = mapOf(
        "33568ad11f93b3e79ae8dee5ad928ded72adcea719e92108caf1521b" to 3,
        "33566617519280305e147975f80914cea1c93e8049567829f7370fca" to 1
    )

    private fun isAuthorized(ownedTokens: JSONArray, authMap: Map<String, Int>): Boolean {
        for (entry in authMap.entries) {
            if (blockfrost.numPolicyTokens(ownedTokens, entry.key) >= entry.value) {
                return true
            }
        }
        return false
    }

    fun isAuthorizedForViewer(assetOrAddress: String) : Boolean {
        val ownedTokens = blockfrost.getAddressTokens(blockfrost.getWalletName(assetOrAddress))
        return isAuthorized(ownedTokens, NFTS_FOR_VIEWER)
    }

    fun unauthorizedForViewerMsg() : String {
        return "Owner of NFT needs to have at least 1 Wild Tangz"
    }

    fun isAuthorizedForPortfolio(assetOrAddress: String) : Boolean {
        val ownedTokens = blockfrost.getAddressTokens(blockfrost.getWalletName(assetOrAddress))
        return isAuthorized(ownedTokens, NFTS_FOR_PORTFOLIO)
    }

    fun unauthorizedForPortfolioMsg() : String {
        return "Wallet needs at least 3 Wild Tangz"
    }

}

class AuthorizeConfigTask(
    var appContext: MainActivity,
    var sharedPreferences: SharedPreferences,
    var selectionSubkey: String,
    var savingState: MutableState<String>,
    var buttonState: MutableState<Boolean>
) : AsyncTask<MutableState<String>, Int, Pair<Boolean, MutableState<String>>>() {

    val ILLEGAL_STATE = "An unknown error occurred"

    var blockfrost : Blockfrost
    var walletAuth : WalletAuth

    init {
        blockfrost = Blockfrost(appContext)
        walletAuth = WalletAuth(blockfrost)
    }

    override fun doInBackground(vararg selections: MutableState<String>?): Pair<Boolean, MutableState<String>> {
        if (selections.size == 0) {
            return Pair(false, mutableStateOf(""))
        }

        buttonState.value = false

        val selection = selections[0]!!
        val isAuthorized = walletAuth.isAuthorizedForViewer(selection.value)
        return Pair(isAuthorized, selection)
    }

    override fun onPostExecute(result: Pair<Boolean, MutableState<String>>?) {
        buttonState.value = true

        if (result == null) {
            Toast.makeText(appContext, ILLEGAL_STATE, Toast.LENGTH_LONG).show()
            return
        }

        if (!result.first) {
            Toast.makeText(appContext, walletAuth.unauthorizedForViewerMsg(), Toast.LENGTH_LONG).show()
            return
        }

        savingState.value = result.second.value
        result.second.value = ""
        with(sharedPreferences.edit()) {
            putString(selectionSubkey, savingState.value)
            apply()
        }

        appContext.sendBroadcast()
    }
}

