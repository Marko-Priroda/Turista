package com.marko.turista

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The user's service key is encrypted with a non-exportable Android Keystore key. */
class RecognitionKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences("recognition_credentials", Context.MODE_PRIVATE)
    private val alias = "turista_recognition_key"
    private fun secret(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun get(): String = try {
        val encoded=prefs.getString("value",null)
        if(encoded == null) "" else {
            val data=Base64.decode(encoded,Base64.NO_WRAP)
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,secret(),GCMParameterSpec(128,data.copyOfRange(0,12)))
            String(cipher.doFinal(data.copyOfRange(12,data.size)),Charsets.UTF_8)
        }
    } catch(_: Exception) { "" }
    fun save(value: String) {
        if(value.isBlank()) { prefs.edit().clear().apply(); return }
        val cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,secret())
        prefs.edit().putString("value",Base64.encodeToString(cipher.iv+cipher.doFinal(value.toByteArray(Charsets.UTF_8)),Base64.NO_WRAP)).apply()
    }
}
