package com.dwicao.dextra.browser

import com.dwicao.dextra.data.SitePermission
import com.dwicao.dextra.data.SiteSetting

data class PrivacyOrigin(
    val origin: String,
    val permissionCount: Int,
    val hasSiteOverrides: Boolean,
    val updatedAt: Long,
)

fun buildPrivacyOrigins(
    permissions: List<SitePermission>,
    settings: List<SiteSetting>,
): List<PrivacyOrigin> {
    val grouped = linkedMapOf<String, PrivacyOrigin>()
    permissions.forEach { permission ->
        val current = grouped[permission.origin]
        grouped[permission.origin] = PrivacyOrigin(
            origin = permission.origin,
            permissionCount = (current?.permissionCount ?: 0) + 1,
            hasSiteOverrides = current?.hasSiteOverrides ?: false,
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
                setting.zoomPercent != null,
            updatedAt = maxOf(current?.updatedAt ?: 0L, setting.updatedAt),
        )
    }
    return grouped.values.sortedByDescending { it.updatedAt }
}
