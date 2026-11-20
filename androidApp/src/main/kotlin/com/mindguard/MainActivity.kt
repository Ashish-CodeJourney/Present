package com.mindguard

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.delay
import com.mindguard.ui.present.CompileUiState
import com.mindguard.ui.present.QuestAction
import com.mindguard.ui.present.SocialQuest
import com.mindguard.ui.present.pauseCountdownSeconds
import com.mindguard.ui.present.requiresTurnOffGuard
import com.mindguard.ui.present.turnOffCountdownSeconds
import com.mindguard.ui.screens.HomeScreen
import com.mindguard.ui.screens.IntentScreen
import com.mindguard.ui.screens.PauseCardScreen
import com.mindguard.ui.screens.ReviewScreen
import com.mindguard.ui.screens.PermissionsScreen
import com.mindguard.ui.screens.SettingsScreen
import com.mindguard.ui.screens.StatsScreen
import com.mindguard.ui.screens.TurnOffGuardScreen
import com.mindguard.onboarding.usageAccessSettingsIntent
import com.mindguard.ui.onboarding.OnboardingHost
import com.mindguard.ui.theme.MindGuardTheme
import com.mindguard.ui.theme.PresentPalette
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_TURN_OFF_GUARD = "com.mindguard.OPEN_TURN_OFF_GUARD"
    }

    private var turnOffGuardRequests by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleLaunchIntent(intent)
        setContent {
            MindGuardTheme {
                MindGuardApp(
                    turnOffGuardRequests = turnOffGuardRequests,
                    onStartQuest = ::startQuest,
                    checkAccessibilityEnabled = { isAccessibilityServiceEnabled() },
                    needsRestrictedSettingsStep = needsRestrictedSettingsStep(),
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenUsageAccessSettings = ::openUsageAccessSettings,
                    onOpenAppInfo = {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        )
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_TURN_OFF_GUARD, false) == true) turnOffGuardRequests += 1
    }

    private fun openUsageAccessSettings() {
        try {
            startActivity(usageAccessSettingsIntent())
        } catch (e: android.content.ActivityNotFoundException) {
            android.util.Log.w("Present", "No usage access settings screen")
        }
    }

    private fun startQuest(quest: SocialQuest) {
        val questIntent = when (quest.action) {
            QuestAction.DIAL    -> Intent(Intent.ACTION_DIAL)
            QuestAction.MESSAGE -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
                .putExtra("sms_body", "Hey! Free for a chai or coffee later? ☕")
            QuestAction.NONE    -> return
        }
        try {
            startActivity(questIntent)
        } catch (e: android.content.ActivityNotFoundException) {
            android.util.Log.w("Present", "No app for quest ${quest.action}")
        }
    }

    /**
     * Android 13+ restricts sideloaded apps from enabling accessibility services until
     * the user explicitly allows "restricted settings" in App Info. This check detects
     * that scenario so the UI can show the extra step.
     */
    private fun needsRestrictedSettingsStep(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return try {
            val source = packageManager.getInstallSourceInfo(packageName)
            source.installingPackageName != "com.android.vending"
        } catch (e: Exception) {
            true  // assume restricted if we can't determine the install source
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.name ==
                "com.mindguard.accessibility.MindGuardAccessibilityService" }
    }
}

private enum class Screen { PERMISSIONS, HOME, STATS, SETTINGS, INTENT, REVIEW, PAUSE, TURN_OFF_GUARD }

@Composable
fun MindGuardApp(
    turnOffGuardRequests: Int,
    onStartQuest: (SocialQuest) -> Unit,
    checkAccessibilityEnabled: () -> Boolean,
    needsRestrictedSettingsStep: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    val onboardingStatus    by viewModel.onboardingStatus.collectAsState()
    val onboardingAnswers   by viewModel.onboardingAnswers.collectAsState()
    val protectionEnabled   by viewModel.protectionEnabled.collectAsState()
    val blockCountToday     by viewModel.blockCountToday.collectAsState()
    val attemptCountToday   by viewModel.attemptCountToday.collectAsState()
    val totalBlocks         by viewModel.totalBlocks.collectAsState()
    val totalAttempts       by viewModel.totalAttempts.collectAsState()
    val currentStreak       by viewModel.currentStreak.collectAsState()
    val bestStreak          by viewModel.bestStreak.collectAsState()
    val instagramEnabled    by viewModel.instagramEnabled.collectAsState()
    val youtubeEnabled      by viewModel.youtubeEnabled.collectAsState()
    val tiktokEnabled       by viewModel.tiktokEnabled.collectAsState()
    val snapchatEnabled     by viewModel.snapchatEnabled.collectAsState()
    val focusScheduleEnabled by viewModel.focusScheduleEnabled.collectAsState()
    val focusStartHour      by viewModel.focusStartHour.collectAsState()
    val focusEndHour        by viewModel.focusEndHour.collectAsState()
    val pauseUntil          by viewModel.pauseUntil.collectAsState()
    val instagramBlocks     by viewModel.instagramBlocks.collectAsState()
    val youtubeBlocks       by viewModel.youtubeBlocks.collectAsState()
    val tiktokBlocks        by viewModel.tiktokBlocks.collectAsState()
    val snapchatBlocks      by viewModel.snapchatBlocks.collectAsState()
    val activePolicy        by viewModel.activePolicy.collectAsState()
    val compileState        by viewModel.compileState.collectAsState()
    val draftPrompt         by viewModel.draftPrompt.collectAsState()
    val insightsState       by viewModel.insightsState.collectAsState()
    val turnOffsToday       by viewModel.turnOffsToday.collectAsState()
    val questsAccepted      by viewModel.questsAccepted.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    var accessibilityEnabled by remember { mutableStateOf(checkAccessibilityEnabled()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) accessibilityEnabled = checkAccessibilityEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // Active poll every 500ms while waiting for the user to grant permission.
    // ON_RESUME alone is unreliable when the system commits the accessibility setting
    // slightly after the activity resumes.
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            val current = checkAccessibilityEnabled()
            if (current != accessibilityEnabled) accessibilityEnabled = current
        }
    }

    when (onboardingStatus) {
        null -> {
            Box(modifier = Modifier.fillMaxSize().background(PresentPalette.nightGradient))
            return
        }
        false -> {
            OnboardingHost(
                accessibilityEnabled        = accessibilityEnabled,
                needsRestrictedSettingsStep = needsRestrictedSettingsStep,
                savedAnswers                = onboardingAnswers,
                compileState                = compileState,
                onSaveAnswers               = viewModel::saveOnboardingAnswers,
                onCompileIntent             = viewModel::compileOnboardingIntent,
                onActivatePolicy            = viewModel::activatePolicy,
                onResetCompile              = viewModel::backToIntent,
                onOpenUsageAccessSettings   = onOpenUsageAccessSettings,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onOpenAppInfo               = onOpenAppInfo,
                onFinish                    = viewModel::completeOnboarding
            )
            return
        }
        true -> Unit
    }

    val startScreen = if (accessibilityEnabled) Screen.HOME else Screen.PERMISSIONS
    var currentScreen by remember(accessibilityEnabled) {
        mutableStateOf(startScreen)
    }

    // Quick Settings tile asks to switch off: route through the guard instead of pausing silently.
    var handledGuardRequests by remember { mutableIntStateOf(0) }
    LaunchedEffect(turnOffGuardRequests, startScreen) {
        if (turnOffGuardRequests > handledGuardRequests && startScreen == Screen.HOME) {
            handledGuardRequests = turnOffGuardRequests
            currentScreen = Screen.TURN_OFF_GUARD
        }
    }

    when (currentScreen) {
        Screen.PERMISSIONS -> PermissionsScreen(
            isGranted = accessibilityEnabled,
            needsRestrictedSettingsStep = needsRestrictedSettingsStep,
            onOpenAppInfo = onOpenAppInfo,
            onOpenSettings = onOpenAccessibilitySettings,
            onContinue = { currentScreen = Screen.HOME }
        )
        Screen.HOME -> HomeScreen(
            protectionEnabled  = protectionEnabled,
            blockCount         = blockCountToday,
            attemptCount       = attemptCountToday,
            currentStreak      = currentStreak,
            pauseUntilMs       = pauseUntil,
            onToggleProtection = { requested ->
                if (requiresTurnOffGuard(protectionEnabled, requested)) currentScreen = Screen.TURN_OFF_GUARD
                else viewModel.toggleProtection(requested)
            },
            onResumeFromPause  = viewModel::resumeFromPause,
            onViewStats        = { currentScreen = Screen.STATS },
            onOpenSettings     = { currentScreen = Screen.SETTINGS },
            activePolicySummary = activePolicy?.summary,
            onEditIntent       = { currentScreen = Screen.INTENT },
            onNeedFiveMinutes  = { currentScreen = Screen.PAUSE }
        )
        Screen.INTENT -> {
            val leaveIntent = {
                viewModel.backToIntent()
                currentScreen = Screen.HOME
            }
            BackHandler(enabled = compileState != CompileUiState.Loading) { leaveIntent() }
            LaunchedEffect(compileState) {
                if (compileState is CompileUiState.Review) currentScreen = Screen.REVIEW
            }
            IntentScreen(
                prompt          = draftPrompt,
                compileState    = compileState,
                hasActivePolicy = activePolicy != null,
                onPromptChange  = viewModel::updateDraftPrompt,
                onCompile       = viewModel::compileIntent,
                onBack          = leaveIntent
            )
        }
        Screen.REVIEW -> {
            val backToEdit = {
                viewModel.backToIntent()
                currentScreen = Screen.INTENT
            }
            BackHandler { backToEdit() }
            when (val state = compileState) {
                is CompileUiState.Review -> ReviewScreen(
                    policy     = state.policy,
                    onActivate = {
                        viewModel.activatePolicy(state.policy)
                        currentScreen = Screen.HOME
                    },
                    onEdit     = backToEdit
                )
                else -> LaunchedEffect(Unit) { currentScreen = Screen.INTENT }
            }
        }
        Screen.PAUSE -> {
            BackHandler { currentScreen = Screen.HOME }
            PauseCardScreen(
                countdownSeconds = pauseCountdownSeconds(activePolicy?.pauseCardSettings),
                onUnlock = {
                    viewModel.pauseForFiveMinutes()
                    currentScreen = Screen.HOME
                },
                onDismiss = { currentScreen = Screen.HOME }
            )
        }
        Screen.TURN_OFF_GUARD -> {
            val keepOn = {
                viewModel.acceptQuest()
                currentScreen = Screen.HOME
            }
            BackHandler { currentScreen = Screen.HOME }
            TurnOffGuardScreen(
                countdownSeconds = turnOffCountdownSeconds(turnOffsToday),
                turnOffsToday    = turnOffsToday,
                questsAccepted   = questsAccepted,
                onKeepOn         = keepOn,
                onStartQuest     = { quest ->
                    keepOn()
                    onStartQuest(quest)
                },
                onPauseInstead   = {
                    viewModel.pauseFromGuard()
                    currentScreen = Screen.HOME
                },
                onTurnOff        = {
                    viewModel.turnOffFromGuard()
                    currentScreen = Screen.HOME
                }
            )
        }
        Screen.STATS -> {
            BackHandler { currentScreen = Screen.HOME }
            StatsScreen(
                blockCountToday   = blockCountToday,
                attemptCountToday = attemptCountToday,
                totalBlocks       = totalBlocks,
                totalAttempts     = totalAttempts,
                currentStreak     = currentStreak,
                bestStreak        = bestStreak,
                instagramBlocks   = instagramBlocks,
                youtubeBlocks     = youtubeBlocks,
                tiktokBlocks      = tiktokBlocks,
                snapchatBlocks    = snapchatBlocks,
                insightsState     = insightsState,
                onRequestInsight  = viewModel::requestWeeklyInsight,
                onBack            = { currentScreen = Screen.HOME }
            )
        }
        Screen.SETTINGS -> {
            BackHandler { currentScreen = Screen.HOME }
            SettingsScreen(
            instagramEnabled     = instagramEnabled,
            youtubeEnabled       = youtubeEnabled,
            tiktokEnabled        = tiktokEnabled,
            snapchatEnabled      = snapchatEnabled,
            focusScheduleEnabled = focusScheduleEnabled,
            focusStartHour       = focusStartHour,
            focusEndHour         = focusEndHour,
            currentStreak        = currentStreak,
            bestStreak           = bestStreak,
            onInstagramToggle    = viewModel::setInstagramEnabled,
            onYoutubeToggle      = viewModel::setYoutubeEnabled,
            onTiktokToggle       = viewModel::setTiktokEnabled,
            onSnapchatToggle     = viewModel::setSnapchatEnabled,
            onFocusScheduleChange = viewModel::setFocusSchedule,
            onBack               = { currentScreen = Screen.HOME }
            )
        }
    }
}
