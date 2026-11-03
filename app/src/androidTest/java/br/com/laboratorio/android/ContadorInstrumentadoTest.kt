package br.com.laboratorio.android

import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContadorInstrumentadoTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun capturarTela(nome: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pasta = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "laboratorio")
        check(pasta.isDirectory || pasta.mkdirs()) { "Falha ao criar pasta de evidencias" }
        val destino = File(pasta, nome)
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(destino).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Falha ao salvar screenshot"
            }
        }
        check(destino.exists() && destino.length() > 0) { "Screenshot nao gerada" }
    }

    @Test
    fun contadorMostraEstadoInicialEIncrementaAposClique() {
        composeRule.onNodeWithText("Laboratório Android").assertIsDisplayed()
        composeRule.onNodeWithText("Interações: 0").assertIsDisplayed()
        capturarTela("contador-antes.png")

        composeRule.onNodeWithText("Testar interação").performClick()
        composeRule.onNodeWithText("Interações: 1").assertIsDisplayed()
        capturarTela("contador-depois.png")
    }
}
