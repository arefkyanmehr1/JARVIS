package com.example.data.repository

import com.example.data.local.dao.RuleDao
import com.example.data.local.entity.RuleEntity
import kotlinx.coroutines.flow.Flow
import java.util.regex.Pattern

data class RuleEvaluationResult(
    val action: String, // "AUTO_REPLY", "IGNORE", "BLOCK", "FIXED_REPLY"
    val matchedRule: RuleEntity? = null,
    val fixedReplyText: String? = null,
    val customAiPrompt: String? = null
)

class RuleRepository(private val ruleDao: RuleDao) {

    val allRules: Flow<List<RuleEntity>> = ruleDao.getAllRules()

    suspend fun insertRule(rule: RuleEntity): Long = ruleDao.insertRule(rule)

    suspend fun updateRule(rule: RuleEntity) = ruleDao.updateRule(rule)

    suspend fun deleteRule(rule: RuleEntity) = ruleDao.deleteRule(rule)

    suspend fun deleteRuleById(id: Long) = ruleDao.deleteRuleById(id)

    /**
     * Evaluates incoming message against active rules ordered by priority DESC.
     * Highest priority matched rule takes precedence.
     */
    suspend fun evaluate(sender: String, messageText: String): RuleEvaluationResult {
        val activeRules = ruleDao.getActiveRules()

        for (rule in activeRules) {
            val senderMatches = rule.senderPattern.isBlank() ||
                    sender.contains(rule.senderPattern.trim()) ||
                    rule.senderPattern.trim() == sender.trim()

            if (!senderMatches) continue

            val textMatches = if (rule.keywordPatterns.isBlank()) {
                true
            } else if (rule.isRegex) {
                try {
                    Pattern.compile(rule.keywordPatterns, Pattern.CASE_INSENSITIVE)
                        .matcher(messageText).find()
                } catch (e: Exception) {
                    false
                }
            } else {
                val keywords = rule.keywordPatterns.split(",").map { it.trim() }.filter { it.isNotBlank() }
                keywords.any { keyword ->
                    messageText.contains(keyword, ignoreCase = true)
                }
            }

            if (textMatches) {
                ruleDao.recordMatch(rule.id)
                return RuleEvaluationResult(
                    action = rule.actionType,
                    matchedRule = rule,
                    fixedReplyText = rule.fixedReplyText,
                    customAiPrompt = rule.customAiPrompt
                )
            }
        }

        // Default behavior: Auto reply with standard prompt
        return RuleEvaluationResult(action = "AUTO_REPLY")
    }
}
