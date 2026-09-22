# Llama con 99

Código fuente de Llama con 99: una app mínima de contactos con identificador de llamadas para los contactos guardados.

## Qué hace

Muestra los contactos del dispositivo e identifica las llamadas entrantes de esos contactos, mostrando el nombre real en vez de solo el número.

## Plataformas

| Plataforma | Estado | Ruta |
| --- | --- | --- |
| iOS | Lista | [`ios/`](ios/) |
| Android | Sin empezar | [`android/`](android/) |

### iOS

- `ContactsApp` — app en SwiftUI que lista los contactos con número cubano (+53, 8 dígitos), agrupados alfabéticamente con buscador. Al tocar un contacto se marca una llamada por cobrar (`*99`); la opción de deslizar para llamar con identificador oculto (`#31#`) se activa desde Ajustes (desactivada por defecto). Sigue el idioma del dispositivo (fuente en español, traducida al inglés).
- `CallerIDExtension` — una Call Directory Extension de CallKit que identifica las llamadas entrantes `*99` con el nombre real del contacto, leyendo una lista que la app principal escribe vía un App Group.

Instrucciones de compilación y arquitectura en [`ios/ARCHITECTURE.md`](ios/ARCHITECTURE.md).

### Android

Todavía no empezada.

## Estructura del repo

```
code/
├── ios/        # App de iOS + extensión de CallerID (proyecto XcodeGen)
└── android/    # App de Android (pendiente)
```
