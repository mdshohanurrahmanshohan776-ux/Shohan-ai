package com.example

import com.example.data.device.AppLanguage
import com.example.data.gemini.GeminiRepository
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testActionTagParsing() {
    val repo = GeminiRepository()
    
    val photoResponse = repo.parseActionFromText("আপনার গ্যালারির সর্বশেষ ছবি নিয়ে আসছি। [ACTION:FIND_LATEST_PHOTO]")
    assertEquals("FIND_LATEST_PHOTO", photoResponse.actionType)
    assertEquals("আপনার গ্যালারির সর্বশেষ ছবি নিয়ে আসছি।", photoResponse.replyText)

    val msgResponse = repo.parseActionFromText("মেসেজ প্রস্তুত করা হয়েছে। [ACTION:SEND_MESSAGE|বাবা|দেরি হবে]")
    assertEquals("SEND_MESSAGE", msgResponse.actionType)
    assertEquals("বাবা|দেরি হবে", msgResponse.actionPayload)

    val callResponse = repo.parseActionFromText("কল করা হচ্ছে [ACTION:MAKE_CALL|01700000000]")
    assertEquals("MAKE_CALL", callResponse.actionType)
    assertEquals("01700000000", callResponse.actionPayload)

    val reminderResponse = repo.parseActionFromText("আমি রিমাইন্ডার যুক্ত করেছি। [ACTION:ADD_REMINDER|Team Meeting|10:30 AM]")
    assertEquals("ADD_REMINDER", reminderResponse.actionType)
    assertEquals("Team Meeting|10:30 AM", reminderResponse.actionPayload)
  }

  @Test
  fun testLanguagesAvailable() {
    val languages = AppLanguage.values()
    assertTrue(languages.any { it.code == "bn" })
    assertTrue(languages.any { it.code == "en" })
    assertTrue(languages.any { it.code == "hi" })
    assertTrue(languages.any { it.code == "es" })
  }
}
