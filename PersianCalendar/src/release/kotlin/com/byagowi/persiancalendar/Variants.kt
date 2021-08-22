package com.byagowi.persiancalendar

import android.app.Application
import android.content.Context

@Suppress("UNUSED_PARAMETER")
object Variants {
    fun mainApplication(app: Application?) {} // Nothing here
    fun startLynxListenerIfIsDebug(context: Context?) {} // Nothing here
    fun logDebug(tag: String, msg: String) {} // Nothing Here
    inline val <T> T.debugAssertNotNull: T inline get() = this // Nothing here
}
