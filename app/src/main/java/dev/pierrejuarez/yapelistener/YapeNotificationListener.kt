package dev.pierrejuarez.yapelistener

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class YapeNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "YapeListener"

        /** Verdadero solo mientras el sistema tiene enlazado este servicio (se reinicia con el proceso). */
        @Volatile
        var conectado: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servicio iniciado")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        conectado = true
        Log.d(TAG, "🔌 Listener conectado")
        reprocesarActivas()
        LatidoWorker.iniciar(this, inmediato = true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        conectado = false
        Log.w(TAG, "🔌 Listener desconectado - se pide re-enlace")
        try {
            NotificationListenerService.requestRebind(ComponentName(this, YapeNotificationListener::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "❌ requestRebind falló: ${e.message}")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        PagoProcesador.procesar(this, sbn, reproceso = false)
    }

    private fun reprocesarActivas() {
        val activas = try {
            activeNotifications
        } catch (e: Exception) {
            Log.e(TAG, "❌ No se pudieron leer las notificaciones activas: ${e.message}")
            null
        } ?: return
        val yape = activas.filter { it.packageName == PagoProcesador.YAPE_PACKAGE }
        Log.d(TAG, "🔁 Reprocesando ${yape.size} notificación(es) activa(s) de Yape")
        yape.forEach { PagoProcesador.procesar(this, it, reproceso = true) }
    }
}
