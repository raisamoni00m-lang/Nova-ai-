package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.NovaPreferences
import com.example.domain.ai.LocalCommandParser
import com.example.domain.ai.Sanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Nova", appName)
  }

  @Test
  fun `test sensitive data sanitization`() {
    val promptWithOtp = "My secret verification OTP is 492812 please use it"
    val sanitized = Sanitizer.sanitizeUserPrompt(promptWithOtp)
    assertTrue(sanitized.contains("[REDACTED_SENSITIVE_CODE]"))
    assertTrue(!sanitized.contains("492812"))
  }

  @Test
  fun `test local command parsing for Bengali flashlight`() {
    val plan = LocalCommandParser.parseDeterministicCommand("ফ্ল্যাশলাইট অন করো")
    assertNotNull(plan)
    assertEquals("flashlight", plan?.toolCalls?.firstOrNull()?.toolName)
    assertEquals("on", plan?.toolCalls?.firstOrNull()?.arguments?.get("state"))
  }

  @Test
  fun `test local command parsing for Banglish volume`() {
    val plan = LocalCommandParser.parseDeterministicCommand("Volume barao")
    assertNotNull(plan)
    assertEquals("volume", plan?.toolCalls?.firstOrNull()?.toolName)
    assertEquals("up", plan?.toolCalls?.firstOrNull()?.arguments?.get("action"))
  }

  @Test
  fun `test preferences storage`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = NovaPreferences(context)
    prefs.language = "bn"
    prefs.voicePitch = 1.25f
    assertEquals("bn", prefs.language)
    assertEquals(1.25f, prefs.voicePitch, 0.01f)
  }
}
