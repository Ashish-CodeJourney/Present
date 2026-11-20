package com.mindguard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindguard.network.CompilerClient
import com.mindguard.network.InsightsRequest
import com.mindguard.onboarding.OnboardingAnswers
import com.mindguard.shared.policy.PresentPolicy
import com.mindguard.storage.OnboardingProfileStore
import com.mindguard.storage.PolicyStore
import com.mindguard.storage.SettingsDataStore
import com.mindguard.ui.present.CompileUiState
import com.mindguard.ui.present.GUARD_PAUSE_DURATION_MS
import com.mindguard.ui.present.InsightsLoader
import com.mindguard.ui.present.InsightsUiState
import com.mindguard.ui.present.IntentCompiler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val PAUSE_DURATION_MS = 5 * 60 * 1000L

class MainViewModel(
    private val store: SettingsDataStore,
    private val policyStore: PolicyStore,
    compilerClient: CompilerClient,
    private val onboardingProfileStore: OnboardingProfileStore
) : ViewModel() {

    private val intentCompiler = IntentCompiler(compilerClient)
    private val insightsLoader = InsightsLoader(compilerClient)

    val activePolicy: StateFlow<PresentPolicy?> = policyStore.activePolicyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val compileState: StateFlow<CompileUiState> = intentCompiler.state

    val insightsState: StateFlow<InsightsUiState> = insightsLoader.state

    private val _draftPrompt = MutableStateFlow("")
    val draftPrompt: StateFlow<String> = _draftPrompt

    val protectionEnabled: StateFlow<Boolean> = store.protectionEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val onboardingStatus: StateFlow<Boolean?> = store.onboardingCompleteFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val onboardingAnswers: StateFlow<OnboardingAnswers?> = onboardingProfileStore.answersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val blockCountToday: StateFlow<Long> = store.blockCountTodayFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val attemptCountToday: StateFlow<Long> = store.attemptCountTodayFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val totalBlocks: StateFlow<Long> = store.totalBlocksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val totalAttempts: StateFlow<Long> = store.totalAttemptsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val currentStreak: StateFlow<Long> = store.currentStreakFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val bestStreak: StateFlow<Long> = store.bestStreakFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val instagramEnabled: StateFlow<Boolean> = store.instagramEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val youtubeEnabled: StateFlow<Boolean> = store.youtubeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val tiktokEnabled: StateFlow<Boolean> = store.tiktokEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val snapchatEnabled: StateFlow<Boolean> = store.snapchatEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val focusScheduleEnabled: StateFlow<Boolean> = store.focusScheduleEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val focusStartHour: StateFlow<Int> = store.focusStartHourFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 9)

    val focusEndHour: StateFlow<Int> = store.focusEndHourFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 17)

    val pauseUntil: StateFlow<Long> = store.pauseUntilFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val instagramBlocks: StateFlow<Long> = store.instagramBlocksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val youtubeBlocks: StateFlow<Long> = store.youtubeBlocksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val tiktokBlocks: StateFlow<Long> = store.tiktokBlocksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val snapchatBlocks: StateFlow<Long> = store.snapchatBlocksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val turnOffsToday: StateFlow<Long> = store.turnOffsTodayFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val questsAccepted: StateFlow<Long> = store.questsAcceptedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    init {
        viewModelScope.launch { store.resetDailyCountsIfNeeded() }
    }

    fun toggleProtection(enabled: Boolean) {
        viewModelScope.launch { store.setProtectionEnabled(enabled) }
    }

    fun turnOffFromGuard() {
        viewModelScope.launch {
            store.recordTurnOff()
            store.setProtectionEnabled(false)
        }
    }

    fun pauseFromGuard() {
        viewModelScope.launch {
            store.recordTurnOff()
            store.pauseProtection(GUARD_PAUSE_DURATION_MS)
        }
    }

    fun acceptQuest() {
        viewModelScope.launch { store.recordQuestAccepted() }
    }

    fun completeOnboarding() {
        viewModelScope.launch { store.setOnboardingComplete() }
    }

    fun saveOnboardingAnswers(answers: OnboardingAnswers) {
        viewModelScope.launch { onboardingProfileStore.save(answers) }
    }

    fun compileOnboardingIntent(text: String) {
        updateDraftPrompt(text)
        compileIntent()
    }

    fun setInstagramEnabled(v: Boolean) { viewModelScope.launch { store.setInstagramEnabled(v) } }
    fun setYoutubeEnabled(v: Boolean)   { viewModelScope.launch { store.setYoutubeEnabled(v) } }
    fun setTiktokEnabled(v: Boolean)    { viewModelScope.launch { store.setTiktokEnabled(v) } }
    fun setSnapchatEnabled(v: Boolean)  { viewModelScope.launch { store.setSnapchatEnabled(v) } }

    fun setFocusSchedule(enabled: Boolean, startHour: Int, endHour: Int) {
        viewModelScope.launch { store.setFocusSchedule(enabled, startHour, endHour) }
    }

    fun pauseProtection(durationMs: Long) {
        viewModelScope.launch { store.pauseProtection(durationMs) }
    }

    fun resumeFromPause() {
        viewModelScope.launch { store.resumeFromPause() }
    }

    fun updateDraftPrompt(text: String) {
        _draftPrompt.value = text
    }

    fun compileIntent() {
        viewModelScope.launch { intentCompiler.compile(_draftPrompt.value, activePolicy.value) }
    }

    fun backToIntent() {
        intentCompiler.reset()
    }

    fun activatePolicy(policy: PresentPolicy) {
        viewModelScope.launch {
            policyStore.activate(policy)
            intentCompiler.reset()
            _draftPrompt.value = ""
        }
    }

    fun pauseForFiveMinutes() {
        pauseProtection(PAUSE_DURATION_MS)
    }

    fun requestWeeklyInsight() {
        val byApp = mapOf(
            "instagram" to instagramBlocks.value,
            "youtube"   to youtubeBlocks.value,
            "tiktok"    to tiktokBlocks.value,
            "snapchat"  to snapchatBlocks.value
        )
        val request = InsightsRequest(
            interceptsByApp = byApp,
            streakDays = currentStreak.value,
            totalIntercepts = maxOf(totalBlocks.value, byApp.values.sum())
        )
        viewModelScope.launch { insightsLoader.load(request) }
    }
}
