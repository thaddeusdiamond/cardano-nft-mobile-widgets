package com.wildtangz.cardano.nft

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
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
            bodyContent(sharedPreferences, getString(R.string.selection_key))
        }
    }
}

@Composable
fun bodyContent(sharedPreferences: SharedPreferences, selectionSubkey: String) {
    val currentSelection : String = sharedPreferences.getString(selectionSubkey, "")!!
    val inputState : MutableState<String> = rememberSaveable { mutableStateOf(currentSelection) }
    return WildTangzCardanoNFTTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Header()
                CardanoLookup(sharedPreferences = sharedPreferences, selectionSubkey = selectionSubkey, savingState = inputState)
                Spacer(modifier = Modifier.height(16.dp))
                CardanoSelection(savedSelection = inputState)
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
fun ThemedElevatedButton(text: String, onClick: () -> Unit) {
    val buttonColors = ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.tertiary)
    ElevatedButton(onClick = onClick, colors = buttonColors) {
        PrimaryText(text = text, style = MaterialTheme.typography.bodyMedium, weight = FontWeight.Bold)
    }
}

@Composable
fun Header() {
    PrimaryText(text = "Cardano NFT Viewer", style = MaterialTheme.typography.titleLarge, weight = FontWeight.Bold)
}

@Composable
fun CardanoLookup(sharedPreferences: SharedPreferences, selectionSubkey: String, savingState: MutableState<String>) {
    var temporaryInputState by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = temporaryInputState,
            onValueChange = { temporaryInputState = it },
            label = { TertiaryText("Enter handle, address, or asset ID", style = MaterialTheme.typography.bodySmall) },
            colors = textFieldColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        ThemedElevatedButton(text = "Update", onClick = {
            savingState.value = temporaryInputState
            temporaryInputState = ""
            with (sharedPreferences.edit()) {
                putString(selectionSubkey, savingState.value)
                apply()
            }
            // TODO: Refresh the widget
        })
    }
}

@Composable
fun CardanoSelection(savedSelection: MutableState<String>) {
    PrimaryText(text = "Current Selection", weight = FontWeight.Bold)
    PrimaryText(text = savedSelection.value)
    ThemedElevatedButton(text = "Refresh", onClick = {
        // TODO: Update Widget
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