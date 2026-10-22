package dev.pierrejuarez.yapelistener

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Envía un pago al webhook; WorkManager lo reintenta con espera creciente aunque el proceso muera. */
class EnvioPagoWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val payload = inputData.getString(K_PAYLOAD) ?: return Result.failure()
        val id = inputData.getString(K_ID) ?: "?"
        val timestamp = inputData.getLong(K_TIMESTAMP, 0L)
        val intento = runAttemptCount + 1

        if (Reglas.expirado(timestamp, System.currentTimeMillis())) {
            Estado.registrarError(applicationContext, "descartado por antiguedad id=$id")
            Log.e(TAG, "❌ Evento descartado por antigüedad (>48 h) id=$id")
            return Result.failure()
        }
        return try {
            val codigo = WebhookSender.postJson(WebhookSender.urlPago(), payload)
            when (Reglas.clasificarHttp(codigo)) {
                ResultadoEnvio.EXITO -> {
                    Estado.registrarEnvioOk(applicationContext)
                    Log.d(TAG, "✅ Evento enviado al webhook (HTTP $codigo) id=$id intento=$intento")
                    Result.success()
                }
                ResultadoEnvio.DESCARTAR -> {
                    Estado.registrarError(applicationContext, "HTTP $codigo descartado id=$id")
                    Log.e(TAG, "❌ El webhook rechazó el evento (HTTP $codigo) id=$id: no se reintenta")
                    Result.failure()
                }
                ResultadoEnvio.REINTENTAR -> {
                    Estado.registrarError(applicationContext, "HTTP $codigo id=$id")
                    Log.e(TAG, "❌ El webhook respondió HTTP $codigo id=$id intento=$intento: se reintentará")
                    Result.retry()
                }
            }
        } catch (e: IOException) {
            Estado.registrarError(applicationContext, "red: ${e.message} id=$id")
            Log.e(TAG, "❌ Error al enviar: ${e.message} id=$id intento=$intento: se reintentará")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "YapeListener"
        private const val K_PAYLOAD = "payload"
        private const val K_ID = "id"
        private const val K_TIMESTAMP = "timestamp"

        fun encolar(ctx: Context, id: String, payloadJson: String, timestamp: Long) {
            val datos = Data.Builder()
                .putString(K_PAYLOAD, payloadJson)
                .putString(K_ID, id)
                .putLong(K_TIMESTAMP, timestamp)
                .build()
            val pedido = OneTimeWorkRequest.Builder(EnvioPagoWorker::class.java)
                .setInputData(datos)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("pago")
                .build()
            WorkManager.getInstance(ctx.applicationContext)
                .enqueueUniqueWork("pago-$id", ExistingWorkPolicy.KEEP, pedido)
        }
    }
}
