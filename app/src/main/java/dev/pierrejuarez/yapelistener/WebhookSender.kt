package dev.pierrejuarez.yapelistener

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Envía un evento de pago al webhook de n8n. Sin reintentos todavía (Día 15C). */
object WebhookSender {
    private const val TAG = "YapeListener"

    fun enviar(payload: Map<String, Any>) {
        val url = BuildConfig.WEBHOOK_URL
        val token = BuildConfig.WEBHOOK_TOKEN
        if (url.isBlank() || token.isBlank()) {
            Log.e(TAG, "❌ Webhook sin configurar (webhook.url / webhook.token en local.properties)")
            return
        }
        Thread {
            var conexion: HttpURLConnection? = null
            try {
                conexion = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("X-Canelu-Token", token)
                }
                val cuerpo = JSONObject(payload).toString().toByteArray(Charsets.UTF_8)
                conexion.outputStream.use { it.write(cuerpo) }
                val codigo = conexion.responseCode
                if (codigo in 200..299) {
                    Log.d(TAG, "✅ Evento enviado al webhook (HTTP $codigo) id=${payload["id_envio"]}")
                } else {
                    Log.e(TAG, "❌ El webhook respondió HTTP $codigo")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al enviar: ${e.message}")
            } finally {
                conexion?.disconnect()
            }
        }.start()
    }
}
