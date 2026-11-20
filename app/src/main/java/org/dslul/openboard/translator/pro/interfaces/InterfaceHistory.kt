package org.dslul.openboard.translator.pro.interfaces

interface InterfaceHistory {
    fun onHistoryCrash()

    /** The user deleted the list's last entry. */
    fun onHistoryEmptied() {}
}
