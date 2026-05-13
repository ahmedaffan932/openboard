package org.dslul.openboard.translator.pro.interfaces

interface RewardedAdCallBack {
    fun onRewardEarned()
    fun onDismiss() {}
    fun onFailed() {}
    fun onAdDisplayed() {}
}
