package org.dslul.openboard.translator.pro.interfaces


interface TranslationInterface {
    fun onTranslate(translation: String)

    fun onFailed()
}