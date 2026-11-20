package com.mindguard.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mindguard.shared.policy.PolicyJson
import com.mindguard.shared.policy.PresentPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val POLICY_STORE_NAME = "present_policy"
private val Context.policyDataStore: DataStore<Preferences> by preferencesDataStore(name = POLICY_STORE_NAME)

class PolicyStore(private val context: Context) {

    private companion object {
        val ACTIVE_POLICY_JSON = stringPreferencesKey("active_policy_json")
    }

    // A stored policy that no longer decodes (e.g. after a schema change) reads as "no policy".
    val activePolicyFlow: Flow<PresentPolicy?> = context.policyDataStore.data
        .map { prefs -> prefs[ACTIVE_POLICY_JSON]?.let { PolicyJson.decode(it) } }
        .distinctUntilChanged()

    suspend fun activate(policy: PresentPolicy) {
        context.policyDataStore.edit { it[ACTIVE_POLICY_JSON] = PolicyJson.encode(policy) }
    }

    suspend fun clear() {
        context.policyDataStore.edit { it.remove(ACTIVE_POLICY_JSON) }
    }
}
