# Aqui empezara la fase de motificacion de este proyecto para poder aplicarlo en un sistema

Esto de aqui empieza el 16/08/26

## 20/10/2026 — Día 15A
- Parser movido a `YapeParser.kt` con pruebas JVM (muestras reales del Día 14).
- Monto normalizado a dos decimales; código de 3 dígitos como texto; prefijo `¡Yape!` tolerado.
- Firebase eliminado; envío por POST JSON a un webhook (`WebhookSender`), URL y token en `local.properties` → `BuildConfig`.
- HTTP plano permitido solo en debug (se quita en el Día 24).


## 22/10/2026 — Día 15C
- `WorkManager 2.10.0`: cola persistente `EnvioPagoWorker` con espera creciente (30 s) y reintentos aunque el proceso muera.
- `Reglas.kt` puro: `idEnvio` determinista (`postTime + mensaje`), ventana de reproceso de 6 h, vida máxima de 48 h y clasificación HTTP (2xx éxito, 400 descartar, resto reintentar).
- Listener: reprocesa las notificaciones activas al reconectar y pide `requestRebind` al desconectarse; `onListenerConnected` arranca el latido.
- `LatidoWorker`: POST a `yape-latido` cada 15 min con `listener_conectado`, `rebind_solicitado`, `pendientes` y último error.
- n8n: workflow `Canelú - Latido Yape` (`1iglzMiD7pUf0xqX`) con `yape-latido` y `yape-latido-estado`; «Save successful production executions» en «Do not save».
- Pruebas (a)(b)(c) verdes: reproceso con la app caída, 4 intentos con el webhook en 404 sobreviviendo a 3 muertes del proceso, y silencio detectado con umbral de 3 min.
- Hallazgo: `am force-stop` solo no detiene la app (el sistema re-enlaza el listener); para simular muerte hay que `disallow_listener` + `force-stop`.
- Hallazgo: el teléfono no tiene datos móviles; el reloj está 5 h atrasado.
