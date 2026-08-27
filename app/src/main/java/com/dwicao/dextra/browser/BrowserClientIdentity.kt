package com.dwicao.dextra.browser

import com.dwicao.dextra.BuildConfig

/** Identifies native requests without pretending to be a different browser. */
object BrowserClientIdentity {
    val userAgent: String = "Dextra/${BuildConfig.VERSION_NAME}"
}
