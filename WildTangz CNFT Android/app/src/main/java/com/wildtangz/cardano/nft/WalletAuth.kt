package com.wildtangz.cardano.nft

import android.content.SharedPreferences
import android.os.AsyncTask
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class AuthorizeConfigTask(
    var appContext: MainActivity,
    var sharedPreferences: SharedPreferences,
    var selectionSubkey: String,
    var savingState: MutableState<String>,
    var buttonState: MutableState<Boolean>
) : AsyncTask<MutableState<String>, Int, Pair<Boolean, MutableState<String>>>() {

    val POLICY_ID = "33568ad11f93b3e79ae8dee5ad928ded72adcea719e92108caf1521b"
    val MIN_NFTS = 1
    val UNAUTHORIZED_MSG = "Owner of NFT needs to have at least ${MIN_NFTS} WildTangz"
    val ILLEGAL_STATE = "An unknown error occurred"

    var blockfrost : Blockfrost

    init {
        blockfrost = Blockfrost(appContext)
    }

    override fun doInBackground(vararg selections: MutableState<String>?): Pair<Boolean, MutableState<String>> {
        if (selections.size == 0) {
            return Pair(false, mutableStateOf(""))
        }

        buttonState.value = false

        val selection = selections[0]!!
        val isAuthorized = blockfrost.numPolicyTokens(selection.value, POLICY_ID) >= MIN_NFTS
        return Pair(isAuthorized, selection)
    }

    override fun onPostExecute(result: Pair<Boolean, MutableState<String>>?) {
        buttonState.value = true

        if (result == null) {
            Toast.makeText(appContext, ILLEGAL_STATE, Toast.LENGTH_LONG).show()
            return
        }

        if (!result.first) {
            Toast.makeText(appContext, UNAUTHORIZED_MSG, Toast.LENGTH_LONG).show()
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

