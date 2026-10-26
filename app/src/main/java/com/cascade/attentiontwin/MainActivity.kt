package com.cascade.attentiontwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.screens.EvidenceScreen
import com.cascade.attentiontwin.ui.screens.HeldScreen
import com.cascade.attentiontwin.ui.screens.InsightsScreen
import com.cascade.attentiontwin.ui.screens.NowScreen
import com.cascade.attentiontwin.ui.screens.OnboardingScreen
import com.cascade.attentiontwin.ui.screens.PrivacyScreen
import com.cascade.attentiontwin.ui.screens.ReceiptsScreen
import com.cascade.attentiontwin.ui.screens.SettingsScreen
import com.cascade.attentiontwin.ui.screens.SimulatorScreen
import com.cascade.attentiontwin.ui.screens.TwinScreen
import com.cascade.attentiontwin.ui.theme.AttentionTwinTheme
import com.cascade.attentiontwin.ui.theme.Bg
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AttentionTwinTheme {
                val settings by viewModel.settingsFlow.collectAsState()
                var onboardedState by remember { mutableStateOf(settings.onboarded) }

                if (!onboardedState && !settings.onboarded) {
                    OnboardingScreen(viewModel = viewModel, onFinished = { onboardedState = true })
                } else {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}

enum class MainTab(val title: String, val icon: ImageVector) {
    NOW("Focus", Icons.Rounded.Home),
    TWIN("Twin", Icons.Rounded.AutoAwesome),
    ACTIVITY("Activity", Icons.Rounded.Inbox),
    MORE("More", Icons.Rounded.MoreHoriz)
}

enum class MoreSubTab(val title: String) {
    INSIGHTS("Insights"),
    EVIDENCE("Evidence"),
    SETTINGS("Settings"),
    SIMULATOR("Simulator"),
    PRIVACY("Privacy")
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(MainTab.NOW) }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Bg),
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Bg, tonalElevation = 0.dp) {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Cyan,
                            selectedTextColor = Cyan,
                            unselectedIconColor = TextLo,
                            unselectedTextColor = TextLo,
                            indicatorColor = Surface2
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_content"
            ) { tab ->
                when (tab) {
                    MainTab.NOW -> NowScreen(viewModel)
                    MainTab.TWIN -> TwinScreen(viewModel)
                    MainTab.ACTIVITY -> {
                        var seg by remember { mutableIntStateOf(0) }
                        Column(Modifier.fillMaxSize()) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Waiting", "History").forEachIndexed { i, t ->
                                    FilterChip(selected = seg == i, onClick = { seg = i }, label = { Text(t) })
                                }
                            }
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                if (seg == 0) HeldScreen(viewModel) else ReceiptsScreen(viewModel)
                            }
                        }
                    }
                    MainTab.MORE -> {
                        var page by remember { mutableStateOf<MoreSubTab?>(null) }
                        BackHandler(page != null) { page = null }
                        when (page) {
                            null -> LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                                items(MoreSubTab.values().toList()) { t ->
                                    Text(
                                        t.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextHi,
                                        modifier = Modifier.fillMaxWidth().clickable { page = t }.padding(vertical = 18.dp)
                                    )
                                }
                            }
                            MoreSubTab.INSIGHTS -> InsightsScreen(viewModel)
                            MoreSubTab.EVIDENCE -> EvidenceScreen(viewModel)
                            MoreSubTab.SETTINGS -> SettingsScreen(viewModel) { page = MoreSubTab.PRIVACY }
                            MoreSubTab.SIMULATOR -> SimulatorScreen()
                            MoreSubTab.PRIVACY -> PrivacyScreen()
                        }
                    }
                }
            }
        }
    }
}
