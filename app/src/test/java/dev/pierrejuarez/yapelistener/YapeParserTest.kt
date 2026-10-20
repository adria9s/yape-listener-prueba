package dev.pierrejuarez.yapelistener

import org.junit.Assert.assertEquals
import org.junit.Test

class YapeParserTest {

    private fun pago(mensaje: String) = YapeParser.parsear(mensaje)

    // --- Muestras reales del Día 14 (anonimizadas) ---

    @Test fun real1_montoEntero_conPuntoDeOracion() {
        val p = pago("NOMBRE A* te envió un pago por S/ 1. El cód. de seguridad es: 367")
        assertEquals("1.00", p.monto)
        assertEquals("NOMBRE A*", p.pagador)
        assertEquals("367", p.codigo)
        assertEquals("INGRESO", p.tipo)
    }

    @Test fun real2_decimalSinCeroFinal() {
        val p = pago("NOMBRE A* te envió un pago por S/ 2.5. El cód. de seguridad es: 925")
        assertEquals("2.50", p.monto)
        assertEquals("925", p.codigo)
    }

    @Test fun real3a_codigoConCeroInicial_seConservaComoTexto() {
        val p = pago("NOMBRE B* te envió un pago por S/ 10. El cód. de seguridad es: 031")
        assertEquals("10.00", p.monto)
        assertEquals("031", p.codigo)
    }

    @Test fun real3b_otroDecimal() {
        val p = pago("NOMBRE B* te envió un pago por S/ 1.5. El cód. de seguridad es: 593")
        assertEquals("1.50", p.monto)
        assertEquals("593", p.codigo)
    }

    @Test fun real4_banca_sinCodigo_nombreCompleto_prefijoYape() {
        val p = pago("Yape! ANA LUCIA PEREZ GOMEZ te envió un pago por S/ 1.50")
        assertEquals("1.50", p.monto)
        assertEquals("ANA LUCIA", p.pagador)
        assertEquals("", p.codigo)
        assertEquals("INGRESO", p.tipo)
    }

    @Test fun real7_enVivo_entero15() {
        val p = pago("NOMBRE C* te envió un pago por S/ 15. El cód. de seguridad es: 482")
        assertEquals("15.00", p.monto)
        assertEquals("482", p.codigo)
    }

    // --- Fragilidades documentadas ---

    @Test fun prefijoConSignoDeApertura() {
        val p = pago("¡Yape! ANA PEREZ te envió un pago por S/ 5")
        assertEquals("ANA PEREZ", p.pagador)
        assertEquals("5.00", p.monto)
    }

    @Test fun montoConPuntoDespuesDeSoles() {
        assertEquals("5.00", YapeParser.extraerMonto("ANA PEREZ te envió un pago por S/. 5"))
    }

    @Test fun montoConMiles() {
        assertEquals("1250.50", YapeParser.extraerMonto("te envió un pago por S/ 1,250.5"))
    }

    @Test fun codigoCompleto_palabraCodigo() {
        assertEquals("123", YapeParser.extraerCodigoSeguridad("El código de seguridad es: 123"))
    }

    @Test fun codigoDeCuatroDigitos_noSeInterpretaComoCodigo() {
        assertEquals("", YapeParser.extraerCodigoSeguridad("El cód. de seguridad es: 1234"))
    }

    // --- Sintéticas (sin muestra real) ---

    @Test fun promocion_esOtro_yNoSeEnvia() {
        val p = pago("Yape te regala un descuento en tu próxima recarga")
        assertEquals("OTRO", p.tipo)
        assertEquals("0.00", p.monto)
        assertEquals("Desconocido", p.pagador)
        assertEquals("", p.codigo)
    }

    @Test fun egreso() {
        val p = pago("Enviaste a ANA PEREZ por S/ 20")
        assertEquals("EGRESO", p.tipo)
        assertEquals("ANA PEREZ", p.pagador)
        assertEquals("20.00", p.monto)
    }

    @Test fun recibisteDe() {
        val p = pago("Recibiste de ANA PEREZ por S/ 7.50")
        assertEquals("INGRESO", p.tipo)
        assertEquals("ANA PEREZ", p.pagador)
        assertEquals("7.50", p.monto)
    }

    @Test fun nombreConTilde_dosApellidos_sintetica() {
        assertEquals("JOSÉ QUISPE", pago("JOSÉ QUISPE MAMANI te envió un pago por S/ 3").pagador)
    }

    @Test fun inicialSuelta_seDescarta() {
        assertEquals("CARLOS GOMEZ", YapeParser.formatearNombre("J. CARLOS GOMEZ"))
    }

    @Test fun mensajeVacio() {
        val p = pago("")
        assertEquals("0.00", p.monto)
        assertEquals("Desconocido", p.pagador)
        assertEquals("", p.codigo)
        assertEquals("OTRO", p.tipo)
    }
}
