package io.tolgee.unit.formats.escaping

import io.tolgee.formats.escaping.IcuMessageEscapeRemover
import io.tolgee.formats.escaping.IcuMessageEscaper
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class TwoWayEscapingTest {
  @Test
  fun `it works`() {
    testString("'What ' complex '' { string # ", false)
    testString("''", false)
    testString("'#'", false)
    testString("{}", false)
    testString("{aa}", false)
    testString("'{", false)
    testString("'{ }", false)
    testString("'{ }}", false)
    testString("{", false)
    testString("''", false)
    testString("Another ''' more complex ' '{ string }' with many weird } cases '", false)
    testString("Another ''' more complex ' '{ string }' with many weird } cases '}", false)
  }

  fun testString(
    string: String,
    plural: Boolean,
  ) {
    val escaped = IcuMessageEscaper(string, plural).escaped
    val unescaped = IcuMessageEscapeRemover(escaped, plural).escapeRemoved
    unescaped.assert.describedAs(
      "\n\nInput:\n$string \n\nEscaped:\n$escaped \n\nUnescpaed: \n$unescaped",
    ).isEqualTo(string)
  }
}
