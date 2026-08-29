package com.dwicao.dextra

import com.dwicao.dextra.browser.SyncSessionPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSessionPolicyTest {
    @Test
    fun localTabChangesDoNotLookLikeRemoteRestore() {
        assertFalse(SyncSessionPolicy.shouldRestore(true, null, null, standalone = false))
        assertFalse(SyncSessionPolicy.shouldRestore(true, "same", "same", standalone = false))
    }

    @Test
    fun onlyAnewSyncTokenRestoresNormalTabs() {
        assertTrue(SyncSessionPolicy.shouldRestore(true, "new", "old", standalone = false))
        assertFalse(SyncSessionPolicy.shouldRestore(true, "new", "old", standalone = true))
    }

    @Test
    fun simultaneousRemoteAndLocalChangesBecomeAConflict() {
        assertTrue(SyncSessionPolicy.hasConflict(remoteChanged = true, localChanged = true))
        assertFalse(SyncSessionPolicy.hasConflict(remoteChanged = true, localChanged = false))
    }
}
