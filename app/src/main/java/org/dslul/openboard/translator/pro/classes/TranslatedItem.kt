package org.dslul.openboard.translator.pro.classes

data class TranslatedItem(
    val lngFrom: String,
    val lngTo: String,
    val originalText: String,
    val translatedText: String
)