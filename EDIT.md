# Aqui empezara la fase de motificacion de este proyecto para poder aplicarlo en un sistema

Esto de aqui empieza el 16/08/26

## 20/10/2026 — Día 15A
- Parser movido a `YapeParser.kt` con pruebas JVM (muestras reales del Día 14).
- Monto normalizado a dos decimales; código de 3 dígitos como texto; prefijo `¡Yape!` tolerado.
- Firebase eliminado; envío por POST JSON a un webhook (`WebhookSender`), URL y token en `local.properties` → `BuildConfig`.
- HTTP plano permitido solo en debug (se quita en el Día 24).

