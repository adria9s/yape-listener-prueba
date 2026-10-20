package dev.pierrejuarez.yapelistener

import java.math.BigDecimal
import java.math.RoundingMode

/** Resultado de interpretar el texto de una notificación de Yape. */
data class PagoYape(
    val monto: String,    // siempre con dos decimales: "1.00", "2.50"
    val pagador: String,  // "Desconocido" si no se pudo extraer
    val codigo: String,   // 3 dígitos como TEXTO ("031"); "" si no viene
    val tipo: String      // INGRESO | EGRESO | PAGO | RETIRO | OTRO
)

/** Parser sin dependencias de Android: se prueba con JUnit en la JVM. */
object YapeParser {
    const val TIPO_INGRESO = "INGRESO"

    private val MONTO = Regex("""S/\.?\s*([0-9][0-9,]*(?:\.[0-9]+)?)""")
    private val PREFIJO = Regex("""^¡?Yape!\s+""", RegexOption.IGNORE_CASE)
    private val PERSONA_ENVIO = Regex("""^(.+?)\s+te\s+envi[oó]""", RegexOption.IGNORE_CASE)
    private val PERSONA_EGRESO =
        Regex("""(?:Enviaste|Pagaste)\s+(?:a\s+)?(.+?)(?:\s+por\s+S/)""", RegexOption.IGNORE_CASE)
    private val PERSONA_RECIBISTE =
        Regex("""Recibiste\s+(?:de\s+)?(.+?)(?:\s+por\s+S/|\s*$)""", RegexOption.IGNORE_CASE)
    private val CODIGO =
        Regex("""c[oó]d(?:igo)?\.?\s*de\s*seguridad\s*es:?\s*(\d{3})(?!\d)""", RegexOption.IGNORE_CASE)
    private val INICIAL = Regex("""[A-Z]\.?""")
    private val ESPACIOS = Regex("""\s+""")

    fun parsear(mensaje: String): PagoYape = PagoYape(
        monto = extraerMonto(mensaje),
        pagador = extraerPersona(mensaje),
        codigo = extraerCodigoSeguridad(mensaje),
        tipo = detectarTipo(mensaje)
    )

    /** "S/ 1." -> "1.00" · "S/ 2.5" -> "2.50" · "S/ 1,250.5" -> "1250.50" · sin monto -> "0.00". */
    fun extraerMonto(mensaje: String): String {
        val crudo = MONTO.find(mensaje)?.groupValues?.get(1)?.replace(",", "") ?: return "0.00"
        return try {
            BigDecimal(crudo).setScale(2, RoundingMode.HALF_UP).toPlainString()
        } catch (e: NumberFormatException) {
            "0.00"
        }
    }

    fun extraerPersona(mensaje: String): String {
        val limpio = mensaje.replace(PREFIJO, "")
        PERSONA_ENVIO.find(limpio)?.let { return formatearNombre(it.groupValues[1]) }
        PERSONA_EGRESO.find(limpio)?.let { return formatearNombre(it.groupValues[1]) }
        PERSONA_RECIBISTE.find(limpio)?.let { return formatearNombre(it.groupValues[1]) }
        return "Desconocido"
    }

    /** Descarta iniciales sueltas ("J.") y devuelve las dos primeras palabras. */
    fun formatearNombre(nombreCompleto: String): String {
        val partes = nombreCompleto.trim().split(ESPACIOS).filter { it.isNotEmpty() }
        val sinIniciales = partes.filterNot { INICIAL.matches(it) }
        val usar = if (sinIniciales.isEmpty()) partes else sinIniciales
        return usar.take(2).joinToString(" ")
    }

    /** Devuelve los 3 dígitos como texto (conserva ceros: "031") o "" si no viene. */
    fun extraerCodigoSeguridad(mensaje: String): String =
        CODIGO.find(mensaje)?.groupValues?.get(1) ?: ""

    fun detectarTipo(mensaje: String): String = when {
        mensaje.contains("Recibiste", ignoreCase = true) -> "INGRESO"
        mensaje.contains("te envió un pago", ignoreCase = true) -> "INGRESO"
        mensaje.contains("te enviaron", ignoreCase = true) -> "INGRESO"
        mensaje.contains("Enviaste", ignoreCase = true) -> "EGRESO"
        mensaje.contains("Pagaste", ignoreCase = true) -> "PAGO"
        mensaje.contains("Retiraste", ignoreCase = true) -> "RETIRO"
        else -> "OTRO"
    }
}
