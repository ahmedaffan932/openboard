package com.example.translatorguru.classes

data class TranslatedItem(
    val lngFrom: String,
    val lngTo: String,
    val originalText: String,
    val translatedText: String
)