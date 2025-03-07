package com.test.messages.demo.ui.Utils


object ViewUtils {


    fun isOfferSender(sender: String): Boolean {
        return sender.matches(Regex("^[A-Z-]+$"))
    }


}

