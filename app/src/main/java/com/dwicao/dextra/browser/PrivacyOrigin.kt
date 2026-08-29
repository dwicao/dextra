package com.dwicao.dextra.browser

import com.dwicao.dextra.data.SitePermission
import com.dwicao.dextra.data.SiteSetting
import org.mozilla.geckoview.GeckoSession

data class PrivacyOrigin(
    val origin: String,
    val permissionCount: Int,
    val hasSiteOverrides: Boolean,
    val blockedCount: Int,
    val updatedAt: Long,
)

fun buildPrivacyOrigins(
    permissions: List<SitePermission>,
    settings: List<SiteSetting>,
    blockedByOrigin: Map<String, Int> = emptyMap(),
): List<PrivacyOrigin> {
    val grouped = linkedMapOf<String, PrivacyOrigin>()
    permissions.forEach { permission ->
        val current = grouped[permission.origin]
        grouped[permission.origin] = PrivacyOrigin(
            origin = permission.origin,
            permissionCount = (current?.permissionCount ?: 0) + 1,
            hasSiteOverrides = current?.hasSiteOverrides ?: false,
            blockedCount = maxOf(current?.blockedCount ?: 0, blockedByOrigin[permission.origin].orZero()),
            updatedAt = maxOf(current?.updatedAt ?: 0L, permission.updatedAt),
        )
    }
    settings.forEach { setting ->
        val current = grouped[setting.origin]
        grouped[setting.origin] = PrivacyOrigin(
            origin = setting.origin,
            permissionCount = current?.permissionCount ?: 0,
            hasSiteOverrides = setting.desktopSites != null ||
                setting.adBlockingEnabled != null ||
                setting.userScriptsEnabled != null ||
                setting.zoomPercent != null ||
                setting.translationTarget != null ||
                setting.httpsOnly != null ||
                setting.cookieBannerMode != null,
            blockedCount = maxOf(current?.blockedCount ?: 0, blockedByOrigin[setting.origin].orZero()),
            updatedAt = maxOf(current?.updatedAt ?: 0L, setting.updatedAt),
        )
    }
    blockedByOrigin.forEach { (origin, count) ->
        if (count <= 0) return@forEach
        val current = grouped[origin]
        grouped[origin] = PrivacyOrigin(
            origin = origin,
            permissionCount = current?.permissionCount ?: 0,
            hasSiteOverrides = current?.hasSiteOverrides ?: false,
            blockedCount = count,
            updatedAt = current?.updatedAt ?: 0L,
        )
    }
    return grouped.values.sortedByDescending { it.updatedAt }
}

private fun Int?.orZero(): Int = this ?: 0

fun sitePermissionLabel(permission: String): String = when (permission.toIntOrNull()) {
    GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> "Location"
    GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> "Notifications"
    GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE -> "Autoplay media"
    else -> "Additional access ($permission)"
}
