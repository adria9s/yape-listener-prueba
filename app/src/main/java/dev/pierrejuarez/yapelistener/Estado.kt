package dev.pierrejuarez.yapelistener

import android.content.Context
import android.provider.Settings

/** Estado pequeño y persistente del listener (SharedPreferences `yape_estado`). */
object Estado {
    private const val PREFS = "yape_estado"
    private const val K_PROCESADOS = "procesados"
    private const val K_ULTIMO_ERROR = "ultimo_error"
    private const val K_ULTIMO_OK = "ultimo_envio_ok"
    private const val RETENCION_MS = 48L * 60 * 60 * 1000

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun yaProcesado(ctx: Context, id: String): Boolean =
        prefs(ctx).getStringSet(K_PROCESADOS, emptySet())!!.any { it.substringBeforeLast('|') == id }

    @Synchronized
    fun marcarProcesado(ctx: Context, id: String, ahora: Long = System.currentTimeMillis()) {
        val p = prefs(ctx)
        val vigentes = p.getStringSet(K_PROCESADOS, emptySet())!!
            .filter { (it.substringAfterLast('|').toLongOrNull() ?: 0L) > ahora - RETENCION_MS }
            .toMutableSet()
        vigentes.add("$id|$ahora")
        p.edit().putStringSet(K_PROCESADOS, vigentes).commit()
    }

    fun registrarError(ctx: Context, mensaje: String) {
        prefs(ctx).edit().putString(K_ULTIMO_ERROR, "${System.currentTimeMillis()}|$mensaje".take(200)).apply()
    }

    fun registrarEnvioOk(ctx: Context) {
        prefs(ctx).edit().putLong(K_ULTIMO_OK, System.currentTimeMillis()).apply()
    }

    fun ultimoError(ctx: Context): String = prefs(ctx).getString(K_ULTIMO_ERROR, "") ?: ""
    fun ultimoEnvioOk(ctx: Context): Long = prefs(ctx).getLong(K_ULTIMO_OK, 0L)

    fun dispositivoId(ctx: Context): String =
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
}
