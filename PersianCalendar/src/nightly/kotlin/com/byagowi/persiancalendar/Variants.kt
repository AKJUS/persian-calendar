package com.byagowi.persiancalendar

import android.app.Application
import android.content.Context
import android.util.Log

@Suppress("UNUSED_PARAMETER")
object Variants {
    fun mainApplication(app: Application?) {} // Nothing here
    fun startLynxListenerIfIsDebug(context: Context?) {} // Nothing here
    fun logDebug(tag: String, msg: String) = Log.d(tag, msg)
    inline val <T> T.debugAssertNotNull: T
        inline get() = this ?: throw NullPointerException("A debug only assert has happened")
    inline val enableDevelopmentFeatures get() = true
}
