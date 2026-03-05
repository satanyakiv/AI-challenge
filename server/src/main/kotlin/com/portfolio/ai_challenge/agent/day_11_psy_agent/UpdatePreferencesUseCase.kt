package com.portfolio.ai_challenge.agent.day_11_psy_agent

import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.CommunicationPreferences

class UpdatePreferencesUseCase(private val contextStore: ContextStore) {

    fun execute(userId: String, preferences: CommunicationPreferences) {
        val profile = contextStore.loadProfile(userId)
        contextStore.saveProfile(profile.copy(preferences = preferences))
    }
}
