package com.wildtangz.cardano.nft

import android.content.SharedPreferences
import android.os.AsyncTask
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class WalletAuth(var blockfrost : Blockfrost) {

    val NFTS_FOR_VIEWER = 1
    val NFTS_FOR_PORTFOLIO = 5
    val POLICY_ID = "33568ad11f93b3e79ae8dee5ad928ded72adcea719e92108caf1521b"

    fun isAuthorizedForViewer(assetOrAddress: String) : Boolean {
        return blockfrost.numPolicyTokens(assetOrAddress, POLICY_ID) >= NFTS_FOR_VIEWER
    }

    fun unauthorizedForViewerMsg() : String {
        return "Owner of NFT needs to have at least ${NFTS_FOR_VIEWER} Wild Tangz"
    }

    fun isAuthorizedForPortfolio(assetOrAddress: String) : Boolean {
        return blockfrost.numPolicyTokens(assetOrAddress, POLICY_ID) >= NFTS_FOR_PORTFOLIO
    }

    fun unauthorizedForPortfolioMsg() : String {
        return "Wallet needs at least ${NFTS_FOR_PORTFOLIO} Wild Tangz"
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

