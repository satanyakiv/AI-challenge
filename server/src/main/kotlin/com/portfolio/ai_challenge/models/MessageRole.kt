package com.portfolio.ai_challenge.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MessageRole(val displayName: String) {
    @SerialName("system") SYSTEM("system"),
    @SerialName("user") USER("user"),
    @SerialName("assistant") ASSISTANT("assistant"),
}
