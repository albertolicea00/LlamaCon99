# Llama con 99

Código fuente de Llama con 99: una app mínima de contactos con identificador de llamadas para los contactos guardados.

## Qué hace

Muestra los contactos del dispositivo e identifica las llamadas entrantes de esos contactos, mostrando el nombre real en vez de solo el número.

## Plataformas

| Plataforma | Estado | Ruta |
| --- | --- | --- |
| iOS | Lista | [`ios/`](ios/) |
| Android | Lista | [`android/`](android/) |

### iOS

- `ContactsApp` — app en SwiftUI que lista los contactos con número cubano (+53, 8 dígitos), agrupados alfabéticamente con buscador. Al tocar un contacto se marca una llamada por cobrar (`*99`); la opción de deslizar para llamar con identificador oculto (`#31#`) se activa desde Ajustes (desactivada por defecto). Sigue el idioma del dispositivo (fuente en español, traducida al inglés).
- `CallerIDExtension` — una Call Directory Extension de CallKit que identifica las llamadas entrantes `*99` con el nombre real del contacto, leyendo una lista que la app principal escribe vía un App Group.

Instrucciones de compilación y arquitectura en [`ios/ARCHITECTURE.md`](ios/ARCHITECTURE.md).

### Android

App nativa en Kotlin y Jetpack Compose. Incluye lista de contactos cubanos, llamada anónima al deslizar e identificación de llamadas entrantes. Para compilar, probar y exportar el APK release optimizado, consulta [`android/ARCHITECTURE.md`](android/ARCHITECTURE.md) o ejecuta `make help` desde la raíz.

## Limitaciones conocidas

**Telemetría de Firebase en Android desde Cuba — comprobado el 22 de octubre de 2026.** La app
está dirigida a usuarios cubanos. En un Samsung SM-A155M conectado desde Cuba sin VPN, Analytics
envió correctamente el evento de prueba (HTTP 204), pero los envíos de Crashlytics y Performance
Monitoring fueron rechazados (HTTP 403). Al activar la VPN, ambos servicios enviaron correctamente
sus datos de prueba (HTTP 200), y Analytics siguió funcionando. Por tanto, pueden faltar informes
de errores y rendimiento de usuarios que se conecten desde Cuba sin VPN. Estos resultados
corresponden a la conexión probada; no confirman el comportamiento en todas las redes cubanas.
Consulta los detalles de verificación en la
[arquitectura de Android](android/ARCHITECTURE.md#verified-delivery-and-cuba-network-limitation-2026-10-22).

## Estructura del repo

```
code/
├── ios/        # App de iOS + extensión de CallerID (proyecto XcodeGen)
├── android/    # App, arquitectura y compilación Android
└── Makefile    # Comandos de compilación Android
```
