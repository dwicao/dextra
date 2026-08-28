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
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class StoredCredential(
    val id: String,
    val origin: String,
    val username: String,
    val password: String,
    val formActionOrigin: String?,
    val httpRealm: String?,
    val updatedAt: Long,
)

/** Small local vault for browser logins. The file never contains plaintext credentials. */
class CredentialVault(private val context: Context) {
    private val file = File(context.filesDir, "credentials.vault")
    private val _credentials = MutableStateFlow<List<StoredCredential>>(emptyList())
    val credentials: StateFlow<List<StoredCredential>> = _credentials.asStateFlow()

    suspend fun load() {
        _credentials.value = runCatching { read() }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(credential: StoredCredential) {
        val current = if (_credentials.value.isEmpty()) runCatching { read() }.getOrDefault(emptyList()) else _credentials.value
        val updated = (current.filterNot {
            it.origin == credential.origin && it.username == credential.username
        } + credential).sortedByDescending { it.updatedAt }.take(MAX_CREDENTIALS)
        write(updated)
        _credentials.value = updated
    }

    @Synchronized
    fun delete(id: String) {
        val updated = _credentials.value.filterNot { it.id == id }
        write(updated)
        _credentials.value = updated
    }

    @Synchronized
    fun deleteOrigin(origin: String) {
        val updated = _credentials.value.filterNot { it.origin == origin }
        write(updated)
        _credentials.value = updated
    }

    @Synchronized
    fun clear() {
        runCatching { file.delete() }
        _credentials.value = emptyList()
    }

    private fun read(): List<StoredCredential> {
        if (!file.isFile || file.length() == 0L) return emptyList()
        val envelope = JSONObject(file.readText(Charsets.UTF_8))
        val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
        val encrypted = Base64.decode(envelope.getString("data"), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val payload = JSONArray(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        return (0 until payload.length()).mapNotNull { index ->
            payload.optJSONObject(index)?.let { value ->
                val origin = value.optString("origin").takeIf(String::isNotBlank) ?: return@let null
                StoredCredential(
                    id = value.optString("id").ifBlank { UUID.randomUUID().toString() },
                    origin = origin,
                    username = value.optString("username"),
                    password = value.optString("password"),
                    formActionOrigin = value.optString("formActionOrigin").takeIf(String::isNotBlank),
                    httpRealm = value.optString("httpRealm").takeIf(String::isNotBlank),
                    updatedAt = value.optLong("updatedAt", 0L),
                )
            }
        }
    }

    private fun write(credentials: List<StoredCredential>) {
        runCatching {
            val payload = JSONArray(credentials.map { credential ->
                JSONObject().apply {
                    put("id", credential.id)
                    put("origin", credential.origin)
                    put("username", credential.username)
                    put("password", credential.password)
                    put("formActionOrigin", credential.formActionOrigin)
                    put("httpRealm", credential.httpRealm)
                    put("updatedAt", credential.updatedAt)
                }
            })
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val envelope = JSONObject()
                .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .put("data", Base64.encodeToString(cipher.doFinal(payload.toString().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP))
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

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "dextra.credentials"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        const val MAX_CREDENTIALS = 200
    }
}
