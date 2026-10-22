package dev.pierrejuarez.yapelistener

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONObject

object PagoProcesador {
    private const val TAG = "YapeListener"
    const val YAPE_PACKAGE = "com.bcp.innovacxion.yapeapp"

    fun procesar(ctx: Context, sbn: StatusBarNotification, reproceso: Boolean) {
        val prefs = ctx.getSharedPreferences("yape_config", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("service_enabled", true)) {
            Log.d(TAG, "⏸️ Servicio pausado - Notificación ignorada")
            return
        }
        if (sbn.packageName != YAPE_PACKAGE) return
        if (reproceso && !Reglas.esReciente(sbn.postTime, System.currentTimeMillis())) {
            Log.d(TAG, "↩️ Notificación activa demasiado antigua - no se reprocesa")
            return
        }

        val extras = sbn.notification.extras
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val mensaje = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""
        Log.d(TAG, "📱 Yape detectado - Título: $titulo | Mensaje: $mensaje")

        val pago = YapeParser.parsear(mensaje)
        val codigoLog = if (pago.codigo.isEmpty()) "no viene" else "presente"
        Log.d(TAG, "💰 Monto: ${pago.monto} | 👤 Persona: ${pago.pagador} | 📊 Tipo: ${pago.tipo} | 🔐 Código: $codigoLog")

        if (pago.tipo != YapeParser.TIPO_INGRESO) {
            Log.d(TAG, "↩️ No es INGRESO - no se envía")
            return
        }

        val idEnvio = Reglas.idEnvio(sbn.postTime, mensaje)
        if (Estado.yaProcesado(ctx, idEnvio)) {
            Log.d(TAG, "↩️ Ya procesado (id=$idEnvio) - se ignora")
            return
        }

        val payload = JSONObject(
            mapOf<String, Any>(
                "id_envio" to idEnvio,
                "monto" to pago.monto,
                "pagador" to pago.pagador,
                "codigo_seguridad" to pago.codigo,
                "tipo" to pago.tipo,
                "timestamp" to sbn.postTime,
                "mensaje_completo" to mensaje,
                "dispositivo_id" to Estado.dispositivoId(ctx)
            )
        ).toString()

        EnvioPagoWorker.encolar(ctx, idEnvio, payload, sbn.postTime)
        Estado.marcarProcesado(ctx, idEnvio)
        Log.d(TAG, "📥 Pago en cola (id=$idEnvio, reproceso=$reproceso)")
    }
}
