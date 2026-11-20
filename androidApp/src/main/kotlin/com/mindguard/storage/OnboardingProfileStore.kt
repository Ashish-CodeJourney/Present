package com.mindguard.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mindguard.onboarding.AgeBand
import com.mindguard.onboarding.HardestTime
import com.mindguard.onboarding.KeepSurface
import com.mindguard.onboarding.Motivation
import com.mindguard.onboarding.OnboardingAnswers
import com.mindguard.onboarding.ShortFormShare
import com.mindguard.onboarding.Struggle
import com.mindguard.onboarding.TrackedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private const val ONBOARDING_STORE_NAME = "present_onboarding"
private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = ONBOARDING_STORE_NAME)

class OnboardingProfileStore(private val context: Context) {

    private companion object {
        val ANSWERS_JSON = stringPreferencesKey("onboarding_answers_json")
    }

    val answersFlow: Flow<OnboardingAnswers?> = context.onboardingDataStore.data
        .map { prefs -> prefs[ANSWERS_JSON]?.let(::decodeAnswers) }
        .distinctUntilChanged()

    suspend fun save(answers: OnboardingAnswers) {
        context.onboardingDataStore.edit { it[ANSWERS_JSON] = encodeAnswers(answers) }
    }
}

private const val APPS = "apps"
private const val DAILY_MINUTES = "dailyMinutes"
private const val MEASURED = "measuredMinutesByApp"
private const val SHARE = "shortFormShare"
private const val HARDEST_TIME = "hardestTime"
private const val STRUGGLES = "struggles"
private const val MOTIVATIONS = "motivations"
private const val KEEP = "keep"
private const val AGE_BAND = "ageBand"

private fun names(values: Set<Enum<*>>): JSONArray = JSONArray(values.map { it.name })

internal fun encodeAnswers(answers: OnboardingAnswers): String = JSONObject()
    .put(APPS, names(answers.apps))
    .put(DAILY_MINUTES, answers.dailyMinutes)
    .put(MEASURED, JSONObject(answers.measuredMinutesByApp.mapKeys { it.key.name }))
    .put(SHARE, answers.shortFormShare.name)
    .put(HARDEST_TIME, answers.hardestTime?.name ?: JSONObject.NULL)
    .put(STRUGGLES, names(answers.struggles))
    .put(MOTIVATIONS, names(answers.motivations))
    .put(KEEP, names(answers.keep))
    .put(AGE_BAND, answers.ageBand?.name ?: JSONObject.NULL)
    .toString()

private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
    enumValues<E>().firstOrNull { it.name == name }

private inline fun <reified E : Enum<E>> JSONObject.enumSet(key: String, fallback: Set<E>): Set<E> {
    val array = optJSONArray(key) ?: return fallback
    return (0 until array.length()).mapNotNull { enumOrNull<E>(array.optString(it)) }.toSet()
}

private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else optString(key)

private fun JSONObject.measuredMinutes(): Map<TrackedApp, Int> {
    val obj = optJSONObject(MEASURED) ?: return emptyMap()
    return obj.keys().asSequence()
        .mapNotNull { key -> enumOrNull<TrackedApp>(key)?.let { it to obj.optInt(key) } }
        .toMap()
}

internal fun decodeAnswers(json: String): OnboardingAnswers? = runCatching {
    val defaults = OnboardingAnswers()
    val obj = JSONObject(json)
    OnboardingAnswers(
        apps = obj.enumSet(APPS, defaults.apps),
        dailyMinutes = obj.optInt(DAILY_MINUTES, defaults.dailyMinutes),
        measuredMinutesByApp = obj.measuredMinutes(),
        shortFormShare = enumOrNull<ShortFormShare>(obj.optString(SHARE)) ?: defaults.shortFormShare,
        hardestTime = enumOrNull<HardestTime>(obj.nullableString(HARDEST_TIME)),
        struggles = obj.enumSet<Struggle>(STRUGGLES, defaults.struggles),
        motivations = obj.enumSet<Motivation>(MOTIVATIONS, defaults.motivations),
        keep = obj.enumSet<KeepSurface>(KEEP, defaults.keep),
        ageBand = enumOrNull<AgeBand>(obj.nullableString(AGE_BAND))
    )
}.getOrNull()
