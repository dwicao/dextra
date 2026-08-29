package com.dwicao.dextra.browser

internal object SyncSessionPolicy {
    fun shouldRestore(
        restoredSavedTabs: Boolean,
        syncApplyToken: String?,
        lastAppliedSyncToken: String?,
        standalone: Boolean,
    ): Boolean = restoredSavedTabs && !standalone &&
        syncApplyToken != null && syncApplyToken != lastAppliedSyncToken

    fun hasConflict(remoteChanged: Boolean, localChanged: Boolean): Boolean =
        remoteChanged && localChanged
}
