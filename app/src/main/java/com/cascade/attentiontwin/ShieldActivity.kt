package com.cascade.attentiontwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cascade.attentiontwin.ui.theme.AttentionTwinTheme
import com.cascade.attentiontwin.ui.theme.Bg
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo

class ShieldActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val pkg = intent.getStringExtra("pkg") ?: ""
        val label = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) { "This app" }
        setContent {
            AttentionTwinTheme {
                Column(
                    Modifier.fillMaxSize().background(Bg).padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Focus is on", style = MaterialTheme.typography.headlineLarge, color = TextHi)
                    Spacer(Modifier.height(8.dp))
                    Text("$label is paused until you end focus.", color = TextLo)
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { finish() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text("Back to focus")
                    }
                    TextButton(onClick = {
                        ShieldService.passUntil = System.currentTimeMillis() + 5 * 60_000L
                        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
                        finish()
                    }) { Text("Allow 5 minutes", color = TextLo) }
                }
            }
        }
    }
}
