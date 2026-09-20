package com.example.service

/**
 * State representing live AI cognitive analysis of user/contact history,
 * bio, status, and conversation context.
 */
data class AiAnalysisState(
    val isAnalyzing: Boolean = false,
    val sender: String = "",
    val contactName: String = "",
    val incomingMessage: String = "",
    val analysisStep: String = "",
    val historySummary: String = "",
    val personaInsight: String = "",
    val decisionReasoning: String = "",
    val scheduledSendTimeStr: String = "",
    val delayRemainingSeconds: Int = 0,
    val plannedReply: String = "",
    val isReadyToSend: Boolean = false,
    val timestamp: Long = 0L
)
