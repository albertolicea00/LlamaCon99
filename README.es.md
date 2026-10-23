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

### Temas de Android

Puedes elegir el estilo visual desde **Ajustes → Personalización**. Todos los temas tienen modo claro y oscuro y conservan el verde de la marca. Cambian la estructura, los controles y la navegación visual.

| Tema | Inspirado en | Estilo |
| --- | --- | --- |
| SamUI | Samsung One UI | Títulos grandes, grupos redondeados y controles al alcance del pulgar. |
| ManzanaUI | Apple iOS | Lista continua, cabecera que se recoge al desplazar, buscador inferior e índice A–Z lateral. |
| MaterialUI | Google Pixel / Material You (Material 3) | Buscador superior en forma de pastilla y filas abiertas con avatares. |
| HorizonUI | Huawei HarmonyOS | Buscador superior y contactos agrupados en módulos. |
| HyperUI | Xiaomi HyperOS | Cabecera grande, búsqueda superior y lista compacta con índice lateral. |
| MonoUI | Nothing OS | Base monocromática con el acento verde de la app, tipografía técnica, trama de puntos e índice lateral. |
| FluentUI | Microsoft Fluent Design | Barra de comandos, buscador superior, panel de lista y navegación lateral en pantallas amplias. |

## Limitaciones conocidas

**Telemetría de Firebase en Android desde Cuba — comprobado el 22 de octubre de 2026.** La app
está dirigida a usuarios cubanos. En un Samsung SM-A155M conectado desde Cuba sin VPN, Analytics
envió correctamente el evento de prueba (HTTP 204), pero los envíos de Crashlytics y Performance
Monitoring fueron rechazados (HTTP 403). Al activar la VPN, ambos servicios enviaron correctamente
sus datos de prueba (HTTP 200), y Analytics siguió funcionando. La app Android conserva
ahora únicamente Analytics; Crashlytics y Performance Monitoring quedan comentados para poder
reactivarlos cuando se compruebe el envío desde Cuba sin VPN. Mientras tanto, no se recopilan
informes de errores ni de rendimiento. Estos resultados
corresponden a la conexión probada; no confirman el comportamiento en todas las redes cubanas.
Consulta los detalles de verificación y los pasos para reactivarlos en la
[arquitectura de Android](android/ARCHITECTURE.md#verified-delivery-and-cuba-network-limitation-2026-10-22).

## Estructura del repo

```
code/
├── ios/        # App de iOS + extensión de CallerID (proyecto XcodeGen)
├── android/    # App, arquitectura y compilación Android
└── Makefile    # Comandos de compilación Android
```
