package io.tolgee.formats

enum class ExportFormat(val extension: String, val mediaType: String) {
  JSON("json", "application/json"),
  XLIFF("xlf", "application/x-xliff+xml"),
  PO("po", "text/x-gettext-translation"),
  IOS_STRINGS_STRINGSDICT("", ""),
  APPLE_XLIFF("xliff", "application/x-xliff+xml"),
}