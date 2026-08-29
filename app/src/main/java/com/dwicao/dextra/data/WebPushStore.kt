package com.dwicao.dextra.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.WebPushSubscription
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class StoredWebPushSubscription(
    val scope: String,
    val origin: String,
    val endpoint: String,
    val appServerKey: ByteArray?,
    val browserPublicKey: ByteArray,
    val authSecret: ByteArray,
    val createdAt: Long,
)

/** Encrypted local storage for Web Push subscription material. */
class WebPushStore(private val context: Context) {
    private val file = File(context.filesDir, "web-push.vault")
    private val _subscriptions = MutableStateFlow<List<StoredWebPushSubscription>>(emptyList())
    val subscriptions: StateFlow<List<StoredWebPushSubscription>> = _subscriptions.asStateFlow()

    suspend fun load() {
        _subscriptions.value = runCatching { read() }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(subscription: StoredWebPushSubscription) {
        val updated = (_subscriptions.value.filterNot { it.scope == subscription.scope } + subscription)
            .sortedByDescending { it.createdAt }
            .take(MAX_SUBSCRIPTIONS)
        write(updated)
        _subscriptions.value = updated
    }

    @Synchronized
    fun delete(scope: String) {
        val updated = _subscriptions.value.filterNot { it.scope == scope }
        write(updated)
        _subscriptions.value = updated
    }

    @Synchronized
    fun clear() {
        runCatching { file.delete() }
        _subscriptions.value = emptyList()
    }

    fun get(scope: String): StoredWebPushSubscription? =
        _subscriptions.value.firstOrNull { it.scope == scope }

    private fun read(): List<StoredWebPushSubscription> {
        if (!file.isFile || file.length() == 0L) return emptyList()
        val envelope = JSONObject(file.readText(Charsets.UTF_8))
        val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
        val encrypted = Base64.decode(envelope.getString("data"), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val values = JSONArray(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        return (0 until values.length()).mapNotNull { index ->
            values.optJSONObject(index)?.let { value ->
                val scope = value.optString("scope").takeIf(String::isNotBlank) ?: return@let null
                val endpoint = value.optString("endpoint").takeIf(String::isNotBlank) ?: return@let null
                val publicKey = value.optString("browserPublicKey").takeIf(String::isNotBlank)
                    ?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return@let null
                val auth = value.optString("authSecret").takeIf(String::isNotBlank)
                    ?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return@let null
                StoredWebPushSubscription(
                    scope = scope,
                    origin = value.optString("origin"),
                    endpoint = endpoint,
                    appServerKey = value.optString("appServerKey").takeIf(String::isNotBlank)
                        ?.let { Base64.decode(it, Base64.NO_WRAP) },
                    browserPublicKey = publicKey,
                    authSecret = auth,
                    createdAt = value.optLong("createdAt", 0L),
                )
            }
        }
    }

    private fun write(subscriptions: List<StoredWebPushSubscription>) {
        runCatching {
            val values = JSONArray(subscriptions.map { subscription ->
                JSONObject().apply {
                    put("scope", subscription.scope)
                    put("origin", subscription.origin)
                    put("endpoint", subscription.endpoint)
                    put("appServerKey", subscription.appServerKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) })
                    put("browserPublicKey", Base64.encodeToString(subscription.browserPublicKey, Base64.NO_WRAP))
                    put("authSecret", Base64.encodeToString(subscription.authSecret, Base64.NO_WRAP))
                    put("createdAt", subscription.createdAt)
                }
            })
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val envelope = JSONObject()
                .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .put("data", Base64.encodeToString(cipher.doFinal(values.toString().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP))
            val temporary = File(file.parentFile, "${file.name}.tmp")
            temporary.writeText(envelope.toString(), Charsets.UTF_8)
            if (!temporary.renameTo(file)) {
                temporary.copyTo(file, overwrite = true)
                temporary.delete()
            }
        }.getOrThrow()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    fun StoredWebPushSubscription.toGeckoSubscription(): WebPushSubscription = WebPushSubscription(
        scope,
        endpoint,
        appServerKey,
        browserPublicKey,
        authSecret,
    )

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "dextra.webpush"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        const val MAX_SUBSCRIPTIONS = 100
    }
}
