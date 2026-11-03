package br.com.laboratorio.android.live

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceCredentialStoreInstrumentedTest {
    @Test
    fun credencialFicaCifradaEPermaneceDisponivelNaProximaInstancia() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = DeviceCredentialStore(context)
        val testToken = "test-device-token-that-must-not-be-plain-text"
        val expiry = 2_000_000_000L

        store.clear()
        val deviceId = store.getOrCreateDeviceId()
        store.save(DeviceCredential(testToken, expiry))

        val rawValues = context.getSharedPreferences(
            "gemini_device_auth",
            Context.MODE_PRIVATE
        ).all.values.joinToString("|")
        assertFalse(rawValues.contains(testToken))

        val restored = DeviceCredentialStore(context).load()
        assertNotNull(restored)
        assertEquals(testToken, restored?.token)
        assertEquals(expiry, restored?.expiresAtEpochSeconds)
        assertEquals(deviceId, DeviceCredentialStore(context).getOrCreateDeviceId())

        store.clear()
        assertNull(store.load())
    }
}
