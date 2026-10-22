package dev.pierrejuarez.yapelistener

import android.app.Notification
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.UUID

class YapeNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "YapeListener"
        private const val YAPE_PACKAGE = "com.bcp.innovacxion.yapeapp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servicio iniciado")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences("yape_config", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("service_enabled", true)) {
            Log.d(TAG, "⏸️ Servicio pausado - Notificación ignorada")
            return
        }
        if (sbn.packageName != YAPE_PACKAGE) return

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

        val payload = mapOf<String, Any>(
            "id_envio" to UUID.randomUUID().toString(),
            "monto" to pago.monto,
            "pagador" to pago.pagador,
            "codigo_seguridad" to pago.codigo,
            "tipo" to pago.tipo,
            "timestamp" to sbn.postTime,
            "mensaje_completo" to mensaje,
            "dispositivo_id" to dispositivoId()
        )
        WebhookSender.enviar(payload)
    }

    private fun dispositivoId(): String =
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: ""
}
