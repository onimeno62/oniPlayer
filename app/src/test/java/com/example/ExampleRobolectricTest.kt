package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.formatDuration
import org.junit.Assert.assertEquals
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
    assertEquals("OniPlayer", appName)
  }

  @Test
  fun `formatDuration formats milliseconds correctly`() {
    // 3 minutes and 25 seconds = 205000 milliseconds
    assertEquals("3:25", formatDuration(205000L))
    
    // 1 hour, 5 minutes, 3 seconds = 3903000 milliseconds
    assertEquals("1:05:03", formatDuration(3903000L))
    
    // 0 seconds
    assertEquals("0:00", formatDuration(0L))
  }
}

