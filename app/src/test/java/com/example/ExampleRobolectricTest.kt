package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.AiRepository
import com.example.data.repository.SmsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("JARVIS", appName)
  }

  @Test
  fun `test message hash computation`() {
    val hash1 = SmsRepository.computeMessageHash("09121234567", "سلام")
    val hash2 = SmsRepository.computeMessageHash("09121234567", "سلام")
    assertNotNull(hash1)
    assertEquals(hash1, hash2)
  }

  @Test
  fun `test api key masking`() {
    val masked = AiRepository.maskKey("AIzaSyTEST1234567890abcdef")
    assertEquals("AIzaSy...cdef", masked)
    val dsMasked = AiRepository.maskKey("sk-test-1234567890abcdef")
    assertEquals("sk-tes...cdef", dsMasked)
  }

  @Test
  fun `test deepseek provider detection`() {
    val key = com.example.data.local.entity.ApiKeyEntity(
      name = "DeepSeek Key",
      apiKey = "sk-test-1234567890abcdef",
      maskedKey = "sk-tes...cdef",
      provider = "DEEPSEEK"
    )
    org.junit.Assert.assertTrue(key.isDeepSeek())

    val geminiKey = com.example.data.local.entity.ApiKeyEntity(
      name = "Gemini Key",
      apiKey = "AIzaSyTEST1234567890abcdef",
      maskedKey = "AIzaSy...cdef",
      provider = "GEMINI"
    )
    org.junit.Assert.assertFalse(geminiKey.isDeepSeek())
    org.junit.Assert.assertTrue(geminiKey.isGemini())

    val openAiKey = com.example.data.local.entity.ApiKeyEntity(
      name = "OpenAI Key",
      apiKey = "sk-proj-test-openai-key",
      maskedKey = "sk-pro...-key",
      provider = "OPENAI"
    )
    org.junit.Assert.assertTrue(openAiKey.isOpenAi())

    val aimlKey = com.example.data.local.entity.ApiKeyEntity(
      name = "AIML Key",
      apiKey = "aiml-test-key-1234",
      maskedKey = "aiml-test...1234",
      provider = "AIML"
    )
    org.junit.Assert.assertTrue(aimlKey.isAiml())
  }
}

