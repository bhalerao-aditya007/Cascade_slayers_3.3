package com.cascade.attentiontwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.cascade.attentiontwin.ui.screens.HeldScreen
import com.cascade.attentiontwin.ui.screens.InsightsScreen
import com.cascade.attentiontwin.ui.screens.NowScreen
import com.cascade.attentiontwin.ui.screens.OnboardingScreen
import com.cascade.attentiontwin.ui.screens.PrivacyScreen
import com.cascade.attentiontwin.ui.screens.ReceiptsScreen
import com.cascade.attentiontwin.ui.screens.SimulatorScreen
import com.cascade.attentiontwin.ui.screens.SettingsScreen
import com.cascade.attentiontwin.ui.screens.TwinScreen
import com.cascade.attentiontwin.ui.theme.AttentionTwinTheme
import com.cascade.attentiontwin.ui.theme.Bg
import com.cascade.attentiontwin.ui.theme.Cyan
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
                    OnboardingScreen(
                        viewModel = viewModel,
                        onFinished = { onboardedState = true }
                    )
                } else {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}

enum class MainTab(val title: String, val icon: ImageVector) {
    NOW("Now", Icons.Rounded.Home),
    HELD("Held", Icons.Rounded.Inbox),
    TWIN("Twin", Icons.Rounded.AutoAwesome),
    RECEIPTS("Receipts", Icons.Rounded.ReceiptLong),
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
    var selectedMoreSubTab by remember { mutableStateOf(MoreSubTab.INSIGHTS) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        containerColor = Bg,
        bottomBar = {
            NavigationBar(
                containerColor = Surface1,
                tonalElevation = 8.dp
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_content"
            ) { targetTab ->
                when (targetTab) {
                    MainTab.NOW -> NowScreen(viewModel = viewModel)
                    MainTab.HELD -> HeldScreen(viewModel = viewModel)
                    MainTab.TWIN -> TwinScreen(viewModel = viewModel)
                    MainTab.RECEIPTS -> ReceiptsScreen(viewModel = viewModel)
                    MainTab.MORE -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Sub-tabs bar for More
                            TabRow(
                                selectedTabIndex = selectedMoreSubTab.ordinal,
                                containerColor = Surface1,
                                contentColor = Cyan,
                                indicator = { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedMoreSubTab.ordinal]),
                                        color = Cyan
                                    )
                                }
                            ) {
                                MoreSubTab.values().forEach { subTab ->
                                    Tab(
                                        selected = selectedMoreSubTab == subTab,
                                        onClick = { selectedMoreSubTab = subTab },
                                        text = {
                                            Text(
                                                text = subTab.title,
                                                fontSize = 11.sp,
                                                color = if (selectedMoreSubTab == subTab) Cyan else TextLo
                                            )
                                        }
                                    )
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                when (selectedMoreSubTab) {
                                    MoreSubTab.INSIGHTS -> InsightsScreen(viewModel = viewModel)
                                    MoreSubTab.EVIDENCE -> EvidenceScreen(viewModel = viewModel)
                                    MoreSubTab.SETTINGS -> SettingsScreen(
                                        viewModel = viewModel,
                                        onNavigatePrivacy = { selectedMoreSubTab = MoreSubTab.PRIVACY }
                                    )
                                    MoreSubTab.SIMULATOR -> SimulatorScreen()
                                    MoreSubTab.PRIVACY -> PrivacyScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
