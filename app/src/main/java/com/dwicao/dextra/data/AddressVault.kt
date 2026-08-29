package com.dwicao.dextra.data

import android.content.Context
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

data class StoredAddress(
    val id: String,
    val name: String,
    val givenName: String,
    val additionalName: String,
    val familyName: String,
    val organization: String,
    val streetAddress: String,
    val addressLevel1: String,
    val addressLevel2: String,
    val addressLevel3: String,
    val postalCode: String,
    val country: String,
    val tel: String,
    val email: String,
    val updatedAt: Long,
)

/** Stores contact/address autofill data encrypted at rest with Android Keystore. */
class AddressVault(private val context: Context) {
    private val file = File(context.filesDir, "addresses.vault")
    private val _addresses = MutableStateFlow<List<StoredAddress>>(emptyList())
    val addresses: StateFlow<List<StoredAddress>> = _addresses.asStateFlow()

    suspend fun load() {
        _addresses.value = runCatching { read() }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(address: StoredAddress) {
        val current = if (_addresses.value.isEmpty()) runCatching { read() }.getOrDefault(emptyList()) else _addresses.value
        val updated = (current.filterNot { it.id == address.id } + address)
            .sortedByDescending { it.updatedAt }
            .take(MAX_ADDRESSES)
        write(updated)
        _addresses.value = updated
    }

    @Synchronized
    fun delete(id: String) {
        val updated = _addresses.value.filterNot { it.id == id }
        write(updated)
        _addresses.value = updated
    }

    @Synchronized
    fun clear() {
        runCatching { file.delete() }
        _addresses.value = emptyList()
    }

    private fun read(): List<StoredAddress> {
        if (!file.isFile || file.length() == 0L) return emptyList()
        val envelope = JSONObject(file.readText(Charsets.UTF_8))
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(TAG_LENGTH_BITS, Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)),
            )
        }
        val payload = JSONArray(String(cipher.doFinal(Base64.decode(envelope.getString("data"), Base64.NO_WRAP)), Charsets.UTF_8))
        return (0 until payload.length()).mapNotNull { index ->
            val value = payload.optJSONObject(index) ?: return@mapNotNull null
            val name = value.optString("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
            StoredAddress(
                id = value.optString("id").ifBlank { UUID.randomUUID().toString() },
                name = name,
                givenName = value.optString("givenName"),
                additionalName = value.optString("additionalName"),
                familyName = value.optString("familyName"),
                organization = value.optString("organization"),
                streetAddress = value.optString("streetAddress"),
                addressLevel1 = value.optString("addressLevel1"),
                addressLevel2 = value.optString("addressLevel2"),
                addressLevel3 = value.optString("addressLevel3"),
                postalCode = value.optString("postalCode"),
                country = value.optString("country"),
                tel = value.optString("tel"),
                email = value.optString("email"),
                updatedAt = value.optLong("updatedAt", 0L),
            )
        }
    }

    private fun write(addresses: List<StoredAddress>) {
        val payload = JSONArray(addresses.map { address ->
            JSONObject().apply {
                put("id", address.id)
                put("name", address.name)
                put("givenName", address.givenName)
                put("additionalName", address.additionalName)
                put("familyName", address.familyName)
                put("organization", address.organization)
                put("streetAddress", address.streetAddress)
                put("addressLevel1", address.addressLevel1)
                put("addressLevel2", address.addressLevel2)
                put("addressLevel3", address.addressLevel3)
                put("postalCode", address.postalCode)
                put("country", address.country)
                put("tel", address.tel)
                put("email", address.email)
                put("updatedAt", address.updatedAt)
            }
        })
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val envelope = JSONObject()
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(cipher.doFinal(payload.toString().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP))
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(envelope.toString(), Charsets.UTF_8)
        if (!temporary.renameTo(file)) {
            temporary.copyTo(file, overwrite = true)
            temporary.delete()
        }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", ANDROID_KEYSTORE).apply {
            init(
                android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "dextra.addresses"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        const val MAX_ADDRESSES = 50
    }
}
