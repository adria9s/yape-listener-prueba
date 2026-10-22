package dev.pierrejuarez.yapelistener

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasTest {
    private val ahora = 10_000_000_000L

    @Test fun idEnvio_esDeterminista() =
        assertEquals(Reglas.idEnvio(1000L, "texto"), Reglas.idEnvio(1000L, "texto"))

    @Test fun idEnvio_cambiaConPostTime() =
        assertNotEquals(Reglas.idEnvio(1000L, "texto"), Reglas.idEnvio(1001L, "texto"))

    @Test fun idEnvio_cambiaConTexto() =
        assertNotEquals(Reglas.idEnvio(1000L, "a"), Reglas.idEnvio(1000L, "b"))

    @Test fun idEnvio_tieneFormatoUuid() =
        assertTrue(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-3[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
            .matches(Reglas.idEnvio(1000L, "texto")))

    @Test fun esReciente_dentroDeLaVentana() = assertTrue(Reglas.esReciente(ahora - 1_000, ahora))

    @Test fun esReciente_enElBordeDeLaVentana() =
        assertTrue(Reglas.esReciente(ahora - Reglas.VENTANA_REPROCESO_MS, ahora))

    @Test fun esReciente_fueraDeLaVentana() =
        assertFalse(Reglas.esReciente(ahora - Reglas.VENTANA_REPROCESO_MS - 1, ahora))

    @Test fun esReciente_futuroLejano_noCuenta() = assertFalse(Reglas.esReciente(ahora + 120_000, ahora))

    @Test fun expirado() {
        assertFalse(Reglas.expirado(ahora - Reglas.VIDA_MAX_EVENTO_MS, ahora))
        assertTrue(Reglas.expirado(ahora - Reglas.VIDA_MAX_EVENTO_MS - 1, ahora))
    }

    @Test fun clasificarHttp() {
        assertEquals(ResultadoEnvio.EXITO, Reglas.clasificarHttp(200))
        assertEquals(ResultadoEnvio.EXITO, Reglas.clasificarHttp(201))
        assertEquals(ResultadoEnvio.DESCARTAR, Reglas.clasificarHttp(400))
        for (c in listOf(403, 404, 408, 429, 500, 503)) {
            assertEquals(ResultadoEnvio.REINTENTAR, Reglas.clasificarHttp(c))
        }
    }
}
