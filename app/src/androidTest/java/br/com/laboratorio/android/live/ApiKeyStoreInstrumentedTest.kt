package br.com.laboratorio.android.live

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApiKeyStoreInstrumentedTest {
    @Test
    fun chaveApiFicaCifradaEPermaneceDisponivelAposRecriarStore() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ApiKeyStore(context)
        val fakeKey = "AIzaFakeInstrumentationKey_NotForRealUse_000000000000"

        store.clear()
        store.save(fakeKey)

        val rawValues = context.getSharedPreferences(
            "gemini_live_secure_settings",
            Context.MODE_PRIVATE,
        ).all.values.joinToString("|")
        assertFalse("A chave não pode ser persistida em texto puro.", rawValues.contains(fakeKey))
        assertEquals(fakeKey, ApiKeyStore(context).load())

        store.clear()
        assertNull(store.load())
    }
}
