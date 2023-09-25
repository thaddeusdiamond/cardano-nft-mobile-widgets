package com.wildtangz.cardano.nft

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults.textFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wildtangz.cardano.nft.ui.theme.WildTangzCardanoNFTTheme

class MainActivity : ComponentActivity() {

    private val APPLICATION_WIDGETS = listOf(NftViewerWidget::class.java, TapToolsWidget::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sharedPreferences =
                getSharedPreferences(getString(R.string.shared_config), Context.MODE_PRIVATE)
            BodyContent(this, sharedPreferences, getString(R.string.selection_key))
        }
    }

    override fun onResume() {
        super.onResume()
        sendBroadcast()
    }

    fun sendBroadcast() {
        APPLICATION_WIDGETS.forEach { widgetClazz ->
            val intent = Intent(applicationContext, widgetClazz)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val widgetIds = ComponentName(applicationContext, widgetClazz)
            val ids = AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(widgetIds)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            applicationContext.sendBroadcast(intent)
        }
    }

    @Composable
    fun BodyContent(
        appContext: MainActivity,
        sharedPreferences: SharedPreferences,
        selectionSubkey: String
    ) {
        val currentSelection: String = sharedPreferences.getString(selectionSubkey, "")!!
        val inputState: MutableState<String> = rememberSaveable { mutableStateOf(currentSelection) }
        val enablingState: MutableState<Boolean> = rememberSaveable { mutableStateOf(true) }
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
                    CardanoSelection(savedSelection = inputState)
                }
                Footer()
            }
        }
    }

    @Composable
    fun ThemeText(text: CharSequence, textColor: Color, style: TextStyle?) {
        val themedStyle: TextStyle =
            if (style != null) style else MaterialTheme.typography.bodyMedium
        when (text) {
            is AnnotatedString -> Text(text = text, style = themedStyle, color = textColor)
            is String -> Text(text = text, style = themedStyle, color = textColor)
        }
    }

    @Composable
    fun PrimaryText(text: CharSequence, style: TextStyle? = null) {
        ThemeText(text = text, textColor = MaterialTheme.colorScheme.secondary, style = style)
    }

    @Composable
    fun TertiaryText(text: CharSequence, style: TextStyle? = null) {
        ThemeText(text = text, textColor = MaterialTheme.colorScheme.tertiary, style = style)
    }

    @Composable
    fun ThemedElevatedButton(
        text: CharSequence,
        enabled: MutableState<Boolean>,
        onClick: () -> Unit
    ) {
        val buttonColors =
            ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ElevatedButton(onClick = onClick, enabled = enabled.value, colors = buttonColors) {
            PrimaryText(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }

    @Composable
    fun Header() {
        PrimaryText(text = "Cardano NFT Viewer", style = MaterialTheme.typography.titleLarge)
        PrimaryText(text = "Instructions", style = MaterialTheme.typography.titleMedium)
        val instructions = listOf(
            "Enter an address, handle ($), or single asset ID (asset1...) then click 'Update'. The selection will display under 'Current Selection'",
            "Exit app, perform a 'long press' on the home screen, then select 'Widgets' to add a widget to the home screen (NFT viewer and portfolio view available)",
            "Wait patiently as an NFT loads into the widget viewer (1-2 minutes).  The widget will refresh on its own every 30 minutes."
        )
        for (index in 0 until instructions.size) {
            PrimaryText(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("${index + 1}. ")
                    }
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(instructions[index])
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    @Composable
    fun CardanoLookup(
        appContext: MainActivity,
        sharedPreferences: SharedPreferences,
        selectionSubkey: String,
        savingState: MutableState<String>,
        enablingState: MutableState<Boolean>
    ) {
        val BUTTON_LABEL = "Enter handle, address, or asset ID"

        var temporaryInput: MutableState<String> = rememberSaveable { mutableStateOf("") }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(
                value = temporaryInput.value,
                onValueChange = { temporaryInput.value = it },
                label = { TertiaryText(BUTTON_LABEL, style = MaterialTheme.typography.bodyMedium) },
                colors = textFieldColors(containerColor = MaterialTheme.colorScheme.secondary),
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
    fun CardanoSelection(savedSelection: MutableState<String>) {
        PrimaryText(text = "Current Selection", style = MaterialTheme.typography.titleMedium)
        PrimaryText(text = savedSelection.value)
    }

    @Composable
    fun Footer() {
        Image(
            painterResource(R.drawable.banner),
            "Wild Tangz Banner",
            alignment = Alignment.BottomCenter
        )
    }
}