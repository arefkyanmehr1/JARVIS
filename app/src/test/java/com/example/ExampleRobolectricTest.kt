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
@Config(sdk = [36])
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
    val masked = AiRepository.maskKey("AIzaSyB1234567890abcdef")
    assertEquals("AIzaSy...cdef", masked)
    val dsMasked = AiRepository.maskKey("sk-d0833a00b40d4b3ebf6b1e55adf569b0")
    assertEquals("sk-d08...69b0", dsMasked)
  }

  @Test
  fun `test deepseek provider detection`() {
    val key = com.example.data.local.entity.ApiKeyEntity(
      name = "DeepSeek Key",
      apiKey = "sk-d0833a00b40d4b3ebf6b1e55adf569b0",
      maskedKey = "sk-d08...69b0",
      provider = "DEEPSEEK"
    )
    org.junit.Assert.assertTrue(key.isDeepSeek())

    val geminiKey = com.example.data.local.entity.ApiKeyEntity(
      name = "Gemini Key",
      apiKey = "AIzaSyB1234567890abcdef",
      maskedKey = "AIzaSy...cdef",
      provider = "GEMINI"
    )
    org.junit.Assert.assertFalse(geminiKey.isDeepSeek())
    org.junit.Assert.assertTrue(geminiKey.isGemini())

    val openAiKey = com.example.data.local.entity.ApiKeyEntity(
      name = "OpenAI Key",
      apiKey = "sk-proj-cJS4Jxz3b0dHTydHWp2mGfpjz825DR8ZuV26AjIeQC5b1eHz",
      maskedKey = "sk-proj...1eHz",
      provider = "OPENAI"
    )
    org.junit.Assert.assertTrue(openAiKey.isOpenAi())

    val aimlKey = com.example.data.local.entity.ApiKeyEntity(
      name = "AIML Key",
      apiKey = "d92ac29de4dfc93541a87b3e779658e6",
      maskedKey = "d92ac29...58e6",
      provider = "AIML"
    )
    org.junit.Assert.assertTrue(aimlKey.isAiml())
  }
}

