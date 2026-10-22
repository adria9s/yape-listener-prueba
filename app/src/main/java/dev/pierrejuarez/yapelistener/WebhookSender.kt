package dev.pierrejuarez.yapelistener

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** POST JSON al webhook de n8n. Se llama desde un hilo de fondo (los workers). */
object WebhookSender {
    fun urlPago(): String = BuildConfig.WEBHOOK_URL

    /** Mismo servidor y carpeta que el webhook de pagos, con el path `yape-latido`. */
    fun urlLatido(): String = BuildConfig.WEBHOOK_URL.substringBeforeLast('/') + "/yape-latido"

    /** Devuelve el código HTTP. Lanza IOException si falla la red o falta la configuración. */
    @Throws(IOException::class)
    fun postJson(url: String, cuerpoJson: String): Int {
        val token = BuildConfig.WEBHOOK_TOKEN
        if (url.isBlank() || token.isBlank()) {
            throw IOException("Webhook sin configurar (webhook.url / webhook.token en local.properties)")
        }
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
            conexion.outputStream.use { it.write(cuerpoJson.toByteArray(Charsets.UTF_8)) }
            val codigo = conexion.responseCode
            (if (codigo in 200..299) conexion.inputStream else conexion.errorStream)?.use { it.readBytes() }
            return codigo
        } finally {
            conexion?.disconnect()
        }
    }
}
