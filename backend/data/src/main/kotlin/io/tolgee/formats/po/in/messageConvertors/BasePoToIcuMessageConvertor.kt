package io.tolgee.formats.po.`in`.messageConvertors

import com.ibm.icu.text.PluralRules
import com.ibm.icu.util.ULocale
import io.tolgee.formats.FormsToIcuPluralConvertor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.ToIcuParamConvertor
import io.tolgee.formats.convertMessage
import io.tolgee.formats.escapeIcu
import io.tolgee.formats.escaping.ForceIcuEscaper
import io.tolgee.formats.getULocaleFromTag
import io.tolgee.formats.pluralData.PluralData

class BasePoToIcuMessageConvertor(private val paramConvertorFactory: () -> ToIcuParamConvertor) {
  fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean,
    forceEscapePluralForms: Boolean,
  ): MessageConvertorResult {
    val stringValue = rawData as? String ?: (rawData as? Map<*, *>)?.get("_stringValue") as? String

    if (stringValue is String) {
      val converted = convert(stringValue, false, convertPlaceholders, false)
      return MessageConvertorResult(converted, false)
    }

    if (rawData is Map<*, *>) {
      val converted = convertPoPlural(rawData, languageTag, convertPlaceholders, forceEscapePluralForms)
      return MessageConvertorResult(converted, true)
    }

    return MessageConvertorResult(null, false)
  }

  private fun convertPoPlural(
    possiblePluralForms: Map<*, *>,
    languageTag: String,
    convertPlaceholders: Boolean,
    forceEscape: Boolean,
  ): String {
    val forms =
      possiblePluralForms.entries.associate { (formNumPossibleString, value) ->
        val formNumber = (formNumPossibleString as? Int) ?: (formNumPossibleString as? String)?.toIntOrNull()
        if (formNumber !is Int || value !is String) {
          throw IllegalArgumentException("Plural forms must be a map of Int to String")
        }
        val locale = getULocaleFromTag(languageTag)
        val example = findSuitableExample(formNumber, locale)
        val keyword = PluralRules.forLocale(locale).select(example.toDouble())
        keyword to (convert(value, true, convertPlaceholders, forceEscape))
      }
    return FormsToIcuPluralConvertor(forms, forceEscape = false, addNewLines = true, argName = "0").convert()
  }

  private fun findSuitableExample(
    key: Int,
    locale: ULocale,
  ): Int {
    val examples = PluralData.DATA[locale.language]?.examples ?: PluralData.DATA["en"]!!.examples
    return examples.find { it.plural == key }?.sample ?: examples[0].sample
  }

  private fun convert(
    message: String,
    isInPlural: Boolean = false,
    convertPlaceholders: Boolean,
    forceEscapePluralForms: Boolean,
  ): String {
    if (forceEscapePluralForms)
      {
        return ForceIcuEscaper(message).escaped
      }
    if (!convertPlaceholders) return message.escapeIcu(isInPlural)
    return convertMessage(message, isInPlural, paramConvertorFactory)
  }
}
