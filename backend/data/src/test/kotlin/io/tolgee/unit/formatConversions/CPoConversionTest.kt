package io.tolgee.unit.formatConversions

import com.ibm.icu.util.ULocale
import io.tolgee.formats.po.SupportedFormat
import io.tolgee.formats.po.`in`.PoToICUConverter
import io.tolgee.formats.po.out.c.ToCPoMessageConverter
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class CPoConversionTest {
  @Test
  fun `it transforms`() {
    testString("Hello %s")
    testString("Hello %d")
    testString("Hello %.2f")
    testString("Hello %f")
    testString("Hello %e")
    testString("Hello %2\$e, hello %1\$s")
    testString("Hello %.50f")
    testString("Hello %.50f")
  }

  @Test
  fun `it limits precision`() {
    val precisionString = (1..50).joinToString("") { "0" }
    convertToIcu("Hello %.51f").assert.isEqualTo("Hello {0, number, .$precisionString}")
  }

  private fun testString(string: String) {
    val icuString = convertToIcu(string)
    val cString = ToCPoMessageConverter(icuString).convert().singleResult
    cString.assert
      .describedAs("Input:\n${string}\nICU:\n$icuString\nC String:\n$cString")
      .isEqualTo(string)
  }

  private fun convertToIcu(string: String) = PoToICUConverter(ULocale.ENGLISH, SupportedFormat.C).convert(string)
}