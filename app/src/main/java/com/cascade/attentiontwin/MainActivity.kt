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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsPaused
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.screens.EvidenceScreen
import com.cascade.attentiontwin.ui.screens.FocusScreen
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
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.ElectricViolet
import com.cascade.attentiontwin.ui.theme.MidnightNavy
import com.cascade.attentiontwin.ui.theme.NeonBlue
import com.cascade.attentiontwin.ui.theme.Surface1
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
    HOME("Home", Icons.Rounded.Home),
    FOCUS("Sanctuary", Icons.Rounded.SelfImprovement),
    INSIGHTS("Insights", Icons.Rounded.Insights),
    ACTIVITY("Alerts", Icons.Rounded.NotificationsPaused),
    TWIN("Twin", Icons.Rounded.AutoAwesome)
}

enum class MoreSubTab(val title: String) {
    WHAT_IF("What-If Simulator"),
    EVIDENCE("Causal Evidence & Pilot"),
    SETTINGS("Settings & Preferences"),
    SIMULATOR("Developer Test Harness"),
    PRIVACY("Privacy Guarantees")
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(MidnightNavy),
        containerColor = MidnightNavy,
        bottomBar = {
            NavigationBar(
                containerColor = Surface1,
                tonalElevation = 0.dp,
                modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricViolet,
                            selectedTextColor = ElectricViolet,
                            unselectedIconColor = TextLo,
                            unselectedTextColor = TextLo,
                            indicatorColor = ElectricViolet.copy(alpha = 0.15f)
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
                    MainTab.HOME -> NowScreen(viewModel)
                    MainTab.FOCUS -> FocusScreen(viewModel)
                    MainTab.INSIGHTS -> InsightsScreen(viewModel)
                    MainTab.ACTIVITY -> {
                        // Smart Notifications: LATER (Held), NOW (Allowed), QUIET (History)
                        var seg by remember { mutableIntStateOf(0) }
                        Column(Modifier.fillMaxSize()) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text(
                                    text = "Smart Notifications",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextHi
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Your attention. Your rules.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextLo
                                )
                                Spacer(Modifier.height(10.dp))

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("LATER (Held)", "NOW (Allowed)", "QUIET (History)").forEachIndexed { i, t ->
                                        FilterChip(
                                            selected = seg == i,
                                            onClick = { seg = i },
                                            label = { Text(t, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                                                selectedLabelColor = ElectricViolet,
                                                containerColor = Surface2,
                                                labelColor = TextLo
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = seg == i,
                                                borderColor = if (seg == i) ElectricViolet else CardBorder
                                            )
                                        )
                                    }
                                }
                            }

                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                when (seg) {
                                    0 -> HeldScreen(viewModel)
                                    1 -> ReceiptsScreen(viewModel, initialFilter = "SHOW")
                                    else -> ReceiptsScreen(viewModel, initialFilter = null)
                                }
                            }
                        }
                    }
                    MainTab.TWIN -> {
                        var page by remember { mutableStateOf<MoreSubTab?>(null) }
                        BackHandler(page != null) { page = null }
                        when (page) {
                            null -> {
                                Column(Modifier.fillMaxSize()) {
                                    TwinScreen(viewModel)
                                }
                            }
                            MoreSubTab.WHAT_IF -> TwinScreen(viewModel)
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
