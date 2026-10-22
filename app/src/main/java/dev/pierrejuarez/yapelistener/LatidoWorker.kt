package dev.pierrejuarez.yapelistener

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Avisa a n8n que el celular está vivo e intenta re-enlazar el listener si está caído. */
class LatidoWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        val conectado = YapeNotificationListener.conectado
        var rebind = false
        if (!conectado) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(ctx, YapeNotificationListener::class.java)
                )
                rebind = true
            } catch (e: Exception) {
                Log.e(TAG, "❌ requestRebind falló: ${e.message}")
            }
        }
        val pendientes = try {
            WorkManager.getInstance(ctx).getWorkInfosByTag("pago").get().count { !it.state.isFinished }
        } catch (e: Exception) {
            -1
        }
        val cuerpo = JSONObject(
            mapOf<String, Any>(
                "tipo" to "latido",
                "timestamp" to System.currentTimeMillis(),
                "dispositivo_id" to Estado.dispositivoId(ctx),
                "listener_conectado" to conectado,
                "rebind_solicitado" to rebind,
                "pendientes" to pendientes,
                "ultimo_envio_ok" to Estado.ultimoEnvioOk(ctx),
                "ultimo_error" to Estado.ultimoError(ctx)
            )
        ).toString()
        try {
            val codigo = WebhookSender.postJson(WebhookSender.urlLatido(), cuerpo)
            Log.d(TAG, "💓 Latido enviado (HTTP $codigo) conectado=$conectado rebind=$rebind pendientes=$pendientes")
        } catch (e: IOException) {
            Estado.registrarError(ctx, "latido: ${e.message}")
            Log.e(TAG, "❌ Latido falló: ${e.message}")
        }
        return Result.success() // el siguiente periodo lo vuelve a intentar
    }

    companion object {
        private const val TAG = "YapeListener"

        fun iniciar(ctx: Context, inmediato: Boolean) {
            val wm = WorkManager.getInstance(ctx.applicationContext)
            wm.enqueueUniquePeriodicWork(
                "latido",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequest.Builder(LatidoWorker::class.java, 15, TimeUnit.MINUTES).build()
            )
            if (inmediato) {
                wm.enqueueUniqueWork(
                    "latido-ya",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequest.Builder(LatidoWorker::class.java).build()
                )
            }
        }
    }
}
