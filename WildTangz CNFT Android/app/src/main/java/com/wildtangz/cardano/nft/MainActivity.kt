package com.wildtangz.cardano.nft

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults.textFieldColors
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wildtangz.cardano.nft.ui.theme.WildTangzCardanoNFTTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sharedPreferences = getSharedPreferences(getString(R.string.shared_config), Context.MODE_PRIVATE)
            bodyContent(this, sharedPreferences, getString(R.string.selection_key))
        }
    }

    override fun onResume() {
        super.onResume()
        sendBroadcastFrom(this)
    }

}

private fun sendBroadcastFrom(context: Context) {
    val intent = Intent(context, NftViewerWidget::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    val widgetIds = ComponentName(context.applicationContext, NftViewerWidget::class.java)
    val ids = AppWidgetManager.getInstance(context.applicationContext).getAppWidgetIds(widgetIds)
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    context.sendBroadcast(intent)
}

@Composable
fun bodyContent(appContext: Context, sharedPreferences: SharedPreferences, selectionSubkey: String) {
    val currentSelection : String = sharedPreferences.getString(selectionSubkey, "")!!
    val inputState : MutableState<String> = rememberSaveable { mutableStateOf(currentSelection) }
    val enablingState : MutableState<Boolean> = rememberSaveable { mutableStateOf(true) }
    return WildTangzCardanoNFTTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Header()
                CardanoLookup(
                    appContext = appContext,
                    sharedPreferences = sharedPreferences,
                    selectionSubkey = selectionSubkey,
                    savingState = inputState,
                    enablingState = enablingState
                )
                Spacer(modifier = Modifier.height(16.dp))
                CardanoSelection(
                    context = appContext,
                    savedSelection = inputState,
                    enablingState = enablingState
                )
            }
            Footer()
        }
    }
}

@Composable
fun ThemeText(text: String, textColor: Color, style: TextStyle?, weight: FontWeight?) {
    var themedStyle: TextStyle = if (style != null) style else MaterialTheme.typography.bodyLarge
    Text(text = text, fontSize = themedStyle.fontSize, fontWeight = weight, color = textColor)
}

@Composable
fun PrimaryText(text: String, style: TextStyle? = null, weight: FontWeight? = null) {
    ThemeText(text = text, textColor = MaterialTheme.colorScheme.secondary, style = style, weight = weight)
}

@Composable
fun InverseText(text: String, style: TextStyle? = null, weight: FontWeight? = null) {
    ThemeText(text = text, textColor = MaterialTheme.colorScheme.inversePrimary, style = style, weight = weight)
}

@Composable
fun TertiaryText(text: String, style: TextStyle? = null, weight: FontWeight? = null) {
    ThemeText(text = text, textColor = MaterialTheme.colorScheme.tertiary, style = style, weight = weight)
}

@Composable
fun ThemedElevatedButton(text: String, enabled: MutableState<Boolean>, onClick: () -> Unit) {
    val buttonColors = ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.tertiary)
    ElevatedButton(onClick = onClick, enabled = enabled.value, colors = buttonColors) {
        PrimaryText(text = text, style = MaterialTheme.typography.bodyMedium, weight = FontWeight.Bold)
    }
}

@Composable
fun Header() {
    PrimaryText(text = "Cardano NFT Viewer", style = MaterialTheme.typography.titleLarge, weight = FontWeight.Bold)
}

@Composable
fun CardanoLookup(
    appContext: Context,
    sharedPreferences: SharedPreferences,
    selectionSubkey: String,
    savingState: MutableState<String>,
    enablingState: MutableState<Boolean>
) {
    val BUTTON_LABEL = "Enter handle, address, or asset ID"

    var temporaryInput : MutableState<String> = rememberSaveable { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = temporaryInput.value,
            onValueChange = { temporaryInput.value = it },
            label = { TertiaryText(BUTTON_LABEL, style = MaterialTheme.typography.bodySmall) },
            colors = textFieldColors(containerColor = Color.White),
            enabled = enablingState.value,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )
        ThemedElevatedButton(
            text = "Update",
            enabled = enablingState,
            onClick = {
                if (!temporaryInput.value.isEmpty()) {
                    val task = AuthorizeConfigTask(
                        appContext,
                        sharedPreferences,
                        selectionSubkey,
                        savingState,
                        enablingState
                    )
                    task.execute(temporaryInput)
                }
            }
        )
    }
}

@Composable
fun CardanoSelection(
    context: Context,
    savedSelection: MutableState<String>,
    enablingState: MutableState<Boolean>
) {
    PrimaryText(text = "Current Selection", weight = FontWeight.Bold)
    PrimaryText(text = savedSelection.value)
    ThemedElevatedButton(text = "Refresh", enabled = enablingState, onClick = {
        sendBroadcastFrom(context)
    })
}

@Composable
fun Footer() {
    Image(
        painterResource(R.drawable.banner),
        "Wild Tangz Banner",
        alignment = Alignment.BottomCenter
    )
}

private class AuthorizeConfigTask(
    var appContext: Context,
    var sharedPreferences: SharedPreferences,
    var selectionSubkey: String,
    var savingState: MutableState<String>,
    var buttonState: MutableState<Boolean>
) : AsyncTask<MutableState<String>, Int, Pair<Boolean, MutableState<String>>>() {

    val POLICY_ID = "33568ad11f93b3e79ae8dee5ad928ded72adcea719e92108caf1521b"
    val MIN_NFTS = 1
    val UNAUTHORIZED_MSG = "Owner of NFT needs to have at least ${MIN_NFTS} WildTangz"
    val ILLEGAL_STATE = "An unknown error occurred"

    override fun doInBackground(vararg selections: MutableState<String>?): Pair<Boolean, MutableState<String>> {
        if (selections.size == 0) {
            return Pair(false, mutableStateOf(""))
        }

        buttonState.value = false

        val selection = selections[0]!!
        val isAuthorized = PoolPm.hasAmountPolicyTokens(selection.value, POLICY_ID, MIN_NFTS)
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

        sendBroadcastFrom(appContext)
    }

}