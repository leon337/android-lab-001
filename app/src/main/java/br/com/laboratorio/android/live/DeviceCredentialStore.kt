package br.com.laboratorio.android.live

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class DeviceCredential(
    val token: String,
    val expiresAtEpochSeconds: Long
)

/**
 * Persiste a credencial bearer cifrada com uma chave AES mantida pelo Android Keystore.
 * O código de ativação nunca é armazenado. O UUID identifica esta instalação, não é segredo.
 */
class DeviceCredentialStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun getOrCreateDeviceId(): String {
        val current = preferences.getString(KEY_DEVICE_ID, null)
        if (!current.isNullOrBlank()) return current
        val generated = UUID.randomUUID().toString()
        check(preferences.edit().putString(KEY_DEVICE_ID, generated).commit()) {
            "Não foi possível persistir o identificador do dispositivo."
        }
        return generated
    }

    @Synchronized
    fun load(): DeviceCredential? {
        val encrypted = preferences.getString(KEY_ENCRYPTED_TOKEN, null) ?: return null
        val expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L)
        return try {
            val token = decrypt(encrypted)
            if (token.isBlank() || expiresAt <= 0L) {
                clear()
                null
            } else {
                DeviceCredential(token, expiresAt)
            }
        } catch (_: Exception) {
            clear()
            null
        }
    }

    @Synchronized
    fun save(credential: DeviceCredential) {
        require(credential.token.isNotBlank())
        require(credential.expiresAtEpochSeconds > 0L)
        val encrypted = encrypt(credential.token)
        check(
            preferences.edit()
                .putString(KEY_ENCRYPTED_TOKEN, encrypted)
                .putLong(KEY_EXPIRES_AT, credential.expiresAtEpochSeconds)
                .commit()
        ) {
            "Não foi possível salvar a credencial protegida."
        }
    }

    @Synchronized
    fun clear() {
        check(
            preferences.edit()
                .remove(KEY_ENCRYPTED_TOKEN)
                .remove(KEY_EXPIRES_AT)
                .commit()
        ) {
            "Não foi possível limpar a credencial do dispositivo."
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        require(combined.size > GCM_IV_BYTES)
        val iv = combined.copyOfRange(0, GCM_IV_BYTES)
        val encrypted = combined.copyOfRange(GCM_IV_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        try {
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing
        } catch (_: Exception) {
            keyStore.deleteEntry(KEY_ALIAS)
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val PREFERENCES_NAME = "gemini_device_auth"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ENCRYPTED_TOKEN = "encrypted_device_token"
        private const val KEY_EXPIRES_AT = "device_token_expires_at"
        private const val KEY_ALIAS = "android_lab_gemini_device_credential_v1"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
    }
}
