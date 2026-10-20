# 💸 YapeListener - Monitor de Transacciones en Tiempo Real

Una aplicación Android que **escucha notificaciones de Yape** y envía cada ingreso a un **webhook HTTP** (n8n) como JSON.
¡Sin intervención manual, sin registros manuales! El parser normaliza el monto y el código de seguridad, y el listener solo reenvía los ingresos.

---

![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple)
![Android](https://img.shields.io/badge/Platform-Android-green)
![Webhook](https://img.shields.io/badge/Backend-Webhook%20HTTP-orange)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)

> Fork de [`adria9s/yape-listener-prueba`](https://github.com/adria9s/yape-listener-prueba), a su vez fork de [`Jozelom/yape-listener`](https://github.com/Jozelom/yape-listener), con el historial original de Pierre Juarez. Esta rama reemplaza Firebase por un webhook propio.

---

## 🚀 Características

- 📲 **Captura automática** de notificaciones de Yape.
- 💰 **Extracción inteligente** de montos, nombres y tipo de transacción.
- 🔢 **Monto normalizado** a dos decimales y código de 3 dígitos conservado como texto.
- ⚡ **Envío inmediato** por POST JSON al webhook configurado.
- 🔐 **Identificación única** por dispositivo para múltiples usuarios.
- 🎯 **Solo ingresos**: las notificaciones que no son `INGRESO` se ignoran.
- 🧹 **Procesamiento limpio** de nombres (solo primer nombre y primer apellido).
- 🎨 **UI moderna** con Jetpack Compose y Material Design 3.
- 🌗 **Tema claro y oscuro** adaptativo.
- ⏯️ **Control del servicio** - Activa/desactiva la captura de notificaciones.

---

## 🎥 Demo

📹 **Video de demostración:**

<div align="center">
  <video width="80%" controls>
    <source src="presentation/demo.mov" type="video/quicktime">
    Tu navegador no soporta la reproducción de video. <a href="presentation/demo.mov">Descargar video</a>
  </video>
</div>

---

## 📋 Requisitos Previos

- **Android 7.0 (API 24)** o superior
- **Permisos de acceso a notificaciones** habilitados
- **Un webhook HTTP** que reciba el JSON (por ejemplo, un workflow de n8n)
- **Android Studio Iguana** o superior (recomendado)

---

## 🛠 Instalación

### 1. Clona el repositorio

```bash
git clone https://github.com/adria9s/yape-listener-prueba.git
```

### 2. Entra al proyecto

```bash
cd yape-listener-prueba
```

### 3. Configura el webhook

Agrega la URL y el token a `local.properties` en la raíz del proyecto (ese archivo no se versiona):

```properties
webhook.url=http://192.168.1.10:5678/webhook/yape-listener
webhook.token=tu_token_local
```

El build inyecta ambos valores en `BuildConfig` (`WEBHOOK_URL` y `WEBHOOK_TOKEN`).
En debug se permite HTTP plano; el APK release exige HTTPS.

### 4. Sincroniza el proyecto

Abre el proyecto en Android Studio y sincroniza las dependencias:

```bash
./gradlew build
```

### 5. Ejecuta la aplicación

Conecta tu dispositivo Android o usa un emulador y ejecuta:

```bash
./gradlew installDebug
```

### 6. Habilita los permisos de notificación

1. Abre la app
2. Ve a **Configuración → Notificaciones → Acceso a notificaciones**
3. Activa **YapeListener**

---

## 📱 Uso

1. **Instala y configura** la aplicación siguiendo los pasos anteriores
2. **Abre la app** y activa el servicio desde el toggle principal
3. **Recibe un Yape** - El listener parsea la notificación y, si es `INGRESO`, hace POST al webhook
4. **Verifica el webhook** - El evento llega como JSON con los ocho campos del payload

### Payload del webhook

Cada ingreso se envía como un objeto JSON plano:

```json
{
  "id_envio": "uuid-generado",
  "monto": "100.50",
  "pagador": "Juan Perez",
  "codigo_seguridad": "031",
  "tipo": "INGRESO",
  "timestamp": 1730000000000,
  "mensaje_completo": "Yape! Juan Perez te envió un pago por S/ 100.50",
  "dispositivo_id": "android_id_del_dispositivo"
}
```

`codigo_seguridad` viaja como texto para conservar ceros iniciales; si el pago llega
por banca y no trae código, se envía como cadena vacía.

### Tipos de transacción detectados

- **INGRESO**: Cuando recibes dinero (único tipo que se envía al webhook)

---

## 🏗 Arquitectura del Proyecto

```
app/src/main/
├── java/dev/pierrejuarez/yapelistener/
│   ├── MainActivity.kt                    # Actividad principal con UI
│   ├── YapeNotificationListener.kt        # Servicio: filtra Yape e ingresos
│   ├── YapeParser.kt                      # Parser JVM: monto, pagador, código, tipo
│   ├── WebhookSender.kt                   # POST JSON con HttpURLConnection
│   └── ui/theme/                          # Tema y estilos de Compose
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── AndroidManifest.xml                    # Configuración de permisos y servicios
```

---

## 🔑 Permisos Requeridos

El proyecto declara los permisos de red en el manifiesto:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

`BIND_NOTIFICATION_LISTENER_SERVICE` **no** es un `uses-permission`: se declara como
atributo `android:permission` del servicio `YapeNotificationListener`, y el usuario lo
concede desde Ajustes → Acceso a notificaciones.

---

## 🧪 Tecnologías Utilizadas

| Tecnología               | Versión/Descripción                         |
| ------------------------ | ------------------------------------------- |
| **Kotlin**               | Lenguaje principal                          |
| **Jetpack Compose**      | UI moderna y declarativa                    |
| **Material Design 3**    | Sistema de diseño                           |
| **Webhook HTTP**         | POST JSON con `HttpURLConnection`           |
| **NotificationListener** | API de Android para escuchar notificaciones |
| **SharedPreferences**    | Almacenamiento local de configuración       |

---

## 🤝 Contribuciones

¡Las contribuciones son bienvenidas! Si deseas mejorar este proyecto:

1. Haz un fork del repositorio
2. Crea una rama para tu feature:

```bash
git checkout -b feature/nueva-caracteristica
```

3. Realiza tus cambios y haz commit:

```bash
git commit -m 'Agrega nueva característica'
```

4. Sube tus cambios:

```bash
git push origin feature/nueva-caracteristica
```

5. Abre un Pull Request 🚀

---

## ⚠️ Importante

- Esta aplicación está diseñada **únicamente con fines educativos y de gestión personal**.
- Respeta la privacidad y los términos de servicio de Yape.
- Los eventos se envían a tu propio webhook; no hay backend de terceros.
- **No compartas tu `local.properties`**: contiene la URL y el token del webhook.

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

## 👨‍💻 Autor

Desarrollado con ♥️ por [Pierre Juarez](https://www.linkedin.com/in/pierre-juarez/) 😊

---

## 📞 Contacto

¿Tienes preguntas o sugerencias? ¡Contáctame!

- **LinkedIn**: [Pierre Juarez](https://www.linkedin.com/in/pierre-juarez/)
- **GitHub**: [@pierre-juarez](https://github.com/pierre-juarez)

---

## ⭐ ¿Te gustó el proyecto?

Si este proyecto te fue útil, no olvides darle una ⭐ en GitHub. ¡Tu apoyo motiva a seguir creando!
