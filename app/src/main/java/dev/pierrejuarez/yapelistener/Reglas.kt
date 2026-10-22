package dev.pierrejuarez.yapelistener

import java.util.UUID

enum class ResultadoEnvio { EXITO, REINTENTAR, DESCARTAR }

object Reglas {
    const val VENTANA_REPROCESO_MS = 6L * 60 * 60 * 1000
    const val VIDA_MAX_EVENTO_MS = 48L * 60 * 60 * 1000

    /** Misma notificación (postTime + texto) = mismo id: reprocesarla no crea un pago nuevo. */
    fun idEnvio(postTime: Long, mensaje: String): String =
        UUID.nameUUIDFromBytes("$postTime|$mensaje".toByteArray(Charsets.UTF_8)).toString()

    fun esReciente(postTime: Long, ahora: Long, ventanaMs: Long = VENTANA_REPROCESO_MS): Boolean =
        postTime >= ahora - ventanaMs && postTime <= ahora + 60_000

    fun expirado(timestamp: Long, ahora: Long): Boolean = ahora - timestamp > VIDA_MAX_EVENTO_MS

    /** 2xx = listo; 400 = formato inválido (no se reintenta); el resto se reintenta. */
    fun clasificarHttp(codigo: Int): ResultadoEnvio = when {
        codigo in 200..299 -> ResultadoEnvio.EXITO
        codigo == 400 -> ResultadoEnvio.DESCARTAR
        else -> ResultadoEnvio.REINTENTAR
    }
}
