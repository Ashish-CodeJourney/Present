package com.mindguard.ui.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mindguard.onboarding.OnboardingAnswers
import com.mindguard.onboarding.TrackedApp
import com.mindguard.onboarding.averageDailyMinutes
import com.mindguard.onboarding.hasUsageAccess
import com.mindguard.onboarding.intentFor
import com.mindguard.onboarding.projectionFor
import com.mindguard.shared.policy.PresentPolicy
import com.mindguard.ui.present.CompileUiState
import com.mindguard.ui.theme.PresentMotion
import com.mindguard.ui.theme.PresentPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val REELS_APPS = listOf(TrackedApp.INSTAGRAM, TrackedApp.YOUTUBE, TrackedApp.TIKTOK, TrackedApp.SNAPCHAT)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingHost(
    accessibilityEnabled: Boolean,
    needsRestrictedSettingsStep: Boolean,
    savedAnswers: OnboardingAnswers?,
    compileState: CompileUiState,
    onSaveAnswers: (OnboardingAnswers) -> Unit,
    onCompileIntent: (String) -> Unit,
    onActivatePolicy: (PresentPolicy) -> Unit,
    onResetCompile: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableStateOf(OnboardingStep.SPLASH) }
    var answers by remember { mutableStateOf(OnboardingAnswers()) }
    var restored by remember { mutableStateOf(false) }
    var usageGranted by remember { mutableStateOf(hasUsageAccess(context)) }
    var measuring by remember { mutableStateOf(false) }

    LaunchedEffect(savedAnswers) {
        if (!restored && savedAnswers != null) {
            answers = savedAnswers
            restored = true
        }
    }

    NightStatusBar()
    RefreshOnResume { usageGranted = hasUsageAccess(context) }

    LaunchedEffect(step, usageGranted) {
        if (step != OnboardingStep.USAGE_ACCESS || !usageGranted || answers.isMeasured) return@LaunchedEffect
        measuring = true
        val measured = withContext(Dispatchers.IO) { averageDailyMinutes(context, answers.apps) }
        measuring = false
        answers = answers.copy(measuredMinutesByApp = measured.filterValues { it > 0 })
    }

    LaunchedEffect(step) {
        if (step == OnboardingStep.REVEAL_MIRROR) onSaveAnswers(answers)
    }

    val goNext = { step = nextStep(step, answers, answers.isMeasured) }
    val goBack = {
        if (step == OnboardingStep.PLAN) onResetCompile()
        step = previousStep(step, answers, answers.isMeasured)
    }
    val canGoBack = previousStep(step, answers, answers.isMeasured) != step && step != OnboardingStep.BUILDING
    BackHandler(enabled = canGoBack) { goBack() }

    val sunrise by animateFloatAsState(
        targetValue = step.ordinal.toFloat() / OnboardingStep.DONE.ordinal,
        animationSpec = tween(900),
        label = "sunrise"
    )
    val projection = remember(answers) { projectionFor(answers) }

    NightSky(sunrise = sunrise) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (step != OnboardingStep.SPLASH) {
                ProgressHeader(
                    progress = progressFor(step),
                    showProgress = showsProgress(step),
                    canGoBack = canGoBack,
                    canSkip = isSkippable(step),
                    onBack = goBack,
                    onSkip = {
                        answers = when (step) {
                            OnboardingStep.AGE -> answers.copy(ageBand = null)
                            else -> answers.copy(measuredMinutesByApp = emptyMap())
                        }
                        step = nextStep(step, answers, measured = false)
                    }
                )
            }
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val direction = if (isForward(initialState, targetState)) 1 else -1
                    (slideInHorizontally(PresentMotion.gentleSpring()) { width -> direction * width / 3 } +
                        fadeIn(tween(PresentMotion.EnterMs))) togetherWith
                        (slideOutHorizontally(tween(PresentMotion.EnterMs)) { width -> -direction * width / 4 } +
                            fadeOut(tween(PresentMotion.EnterMs / 2)))
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = "onboarding-step"
            ) { current ->
                Box(modifier = Modifier.fillMaxSize()) {
                    StepContent(
                        step = current,
                        answers = answers,
                        projection = projection,
                        usageGranted = usageGranted,
                        measuring = measuring,
                        accessibilityEnabled = accessibilityEnabled,
                        needsRestrictedSettingsStep = needsRestrictedSettingsStep,
                        compileState = compileState,
                        onChange = { answers = it },
                        onNext = goNext,
                        goTo = { step = it },
                        onCompileIntent = onCompileIntent,
                        onActivatePolicy = onActivatePolicy,
                        onResetCompile = onResetCompile,
                        onOpenUsageAccessSettings = onOpenUsageAccessSettings,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        onOpenAppInfo = onOpenAppInfo,
                        onShare = { shareText(context, shareTextFor(projection)) },
                        onTryReels = { openReelsApp(context, answers.apps) },
                        onFinish = onFinish
                    )
                }
            }
        }
    }
}

@Composable
private fun StepContent(
    step: OnboardingStep,
    answers: OnboardingAnswers,
    projection: com.mindguard.onboarding.Projection,
    usageGranted: Boolean,
    measuring: Boolean,
    accessibilityEnabled: Boolean,
    needsRestrictedSettingsStep: Boolean,
    compileState: CompileUiState,
    onChange: (OnboardingAnswers) -> Unit,
    onNext: () -> Unit,
    goTo: (OnboardingStep) -> Unit,
    onCompileIntent: (String) -> Unit,
    onActivatePolicy: (PresentPolicy) -> Unit,
    onResetCompile: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onShare: () -> Unit,
    onTryReels: () -> Unit,
    onFinish: () -> Unit
) {
    when (step) {
        OnboardingStep.SPLASH -> SplashStep(onDone = onNext)
        OnboardingStep.WELCOME -> WelcomeStep(onStart = onNext)
        OnboardingStep.APPS -> AppsStep(answers, onChange, onNext)
        OnboardingStep.DAILY_TIME -> DailyTimeStep(
            answers = answers,
            onChange = onChange,
            onMeasure = { goTo(OnboardingStep.USAGE_ACCESS) },
            onContinue = onNext
        )
        OnboardingStep.USAGE_ACCESS -> UsageAccessStep(
            granted = usageGranted,
            measuring = measuring,
            answers = answers,
            onOpenSettings = onOpenUsageAccessSettings,
            onContinue = onNext
        )
        OnboardingStep.SHORT_FORM -> ShortFormStep(answers, onChange, onNext)
        OnboardingStep.HARDEST_TIME -> HardestTimeStep(answers, onChange, onNext)
        OnboardingStep.STRUGGLES -> StrugglesStep(answers, onChange, onNext)
        OnboardingStep.MOTIVATIONS -> MotivationsStep(answers, onChange, onNext)
        OnboardingStep.KEEP -> KeepStep(answers, onChange, onNext)
        OnboardingStep.AGE -> AgeStep(answers, onChange, onNext)
        OnboardingStep.BUILDING -> BuildingStep(onDone = onNext)
        OnboardingStep.REVEAL_MIRROR -> MirrorStep(answers, projection, onShare, onNext)
        OnboardingStep.REVEAL_UPSIDE -> UpsideStep(projection, onNext)
        OnboardingStep.PLAN -> PlanStep(
            intent = remember(answers) { intentFor(answers) },
            compileState = compileState,
            onCompile = onCompileIntent,
            onActivate = { policy ->
                onActivatePolicy(policy)
                onNext()
            },
            onEdit = onResetCompile,
            onSkip = {
                onResetCompile()
                onNext()
            }
        )
        OnboardingStep.COMMIT -> CommitStep(onCommitted = onNext)
        OnboardingStep.PERMISSION -> PermissionStep(
            granted = accessibilityEnabled,
            needsRestrictedSettingsStep = needsRestrictedSettingsStep,
            onOpenAppInfo = onOpenAppInfo,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onContinue = onNext,
            onLater = onNext
        )
        OnboardingStep.DONE -> DoneStep(
            protected = accessibilityEnabled,
            quest = firstQuestFor(answers.motivations),
            canTryReels = answers.apps.any { it in REELS_APPS },
            onTryReels = onTryReels,
            onFinish = onFinish
        )
    }
}

@Composable
private fun RefreshOnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun NightStatusBar() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val previousStatusColor = window.statusBarColor
        val previousNavColor = window.navigationBarColor
        val previousLightStatus = controller.isAppearanceLightStatusBars
        val previousLightNav = controller.isAppearanceLightNavigationBars
        window.statusBarColor = PresentPalette.NightTop.toArgb()
        window.navigationBarColor = PresentPalette.NightBottom.toArgb()
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            window.statusBarColor = previousStatusColor
            window.navigationBarColor = previousNavColor
            controller.isAppearanceLightStatusBars = previousLightStatus
            controller.isAppearanceLightNavigationBars = previousLightNav
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    val chooser = Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(chooser)
    } catch (e: android.content.ActivityNotFoundException) {
        android.util.Log.w("Present", "No app to share with")
    }
}

private fun openReelsApp(context: Context, apps: Set<TrackedApp>) {
    val launch = REELS_APPS
        .filter { it in apps }
        .flatMap { it.packageNames }
        .firstNotNullOfOrNull { context.packageManager.getLaunchIntentForPackage(it) }
        ?: return
    try {
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: android.content.ActivityNotFoundException) {
        android.util.Log.w("Present", "Could not open ${launch.`package`}")
    }
}
