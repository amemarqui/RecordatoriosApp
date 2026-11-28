# Recordatorios — Programación Mobile

App Android de recordatorios hecha con **Kotlin**, **Jetpack Compose** y **Material 3**.

- **Actividad 1:** modelo de datos, validaciones, colecciones y una primera pantalla con datos simulados.
- **Actividad 2:** pantallas de listado, creación y detalle, componentes reutilizables, formulario validado, ViewModel + UiState + StateFlow, repositorio en memoria y Navigation Compose.
- **Actividad 3:** persistencia con **Room**, preferencias con **DataStore**, repositorio **local-first**, servicio remoto con **Retrofit + OkHttp**, sincronización controlada (sin duplicados) y funcionamiento **offline**.
- **Actividad 4:** **Firebase** como BaaS: **Authentication** (email y contraseña) y **Cloud Firestore** en `users/{uid}/reminders/{reminderId}`, datos separados por usuario en Room, reglas de seguridad, variantes **dev / prod** y build **release firmado** (APK + AAB).

## Versión
| Dato | Valor |
|---|---|
| applicationId | `com.amemarqui.recordatorios` (dev: `com.amemarqui.recordatorios.dev`) |
| versionCode | 4 |
| versionName | `4.0.0-rc1` (dev: `4.0.0-rc1-dev`) |
| Variante de entrega | `prodRelease` |

## Requisitos
- Android Studio (Ladybug o posterior) con el JDK que trae incluido
- Android SDK Platform 35 (Android 15), instalado desde **SDK Manager**
- Un emulador (Device Manager, API 26 o superior) o un teléfono con depuración USB

## Configuración
| Elemento | Valor |
|---|---|
| Gradle | 8.11.1 (wrapper) |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.1.0 (+ plugin Compose Compiler) |
| Compose BOM | 2024.12.01 (Material 3) |
| Navigation Compose | 2.8.5 |
| Lifecycle (ViewModel / runtime-compose) | 2.8.7 |
| Room (+ KSP 2.1.0-1.0.29) | 2.6.1 |
| DataStore Preferences | 1.1.1 |
| Retrofit / converter-gson | 2.11.0 |
| OkHttp / logging-interceptor | 4.12.0 |
| Firebase BoM (Auth + Firestore) | 33.7.0 |
| Plugin google-services | 4.4.2 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0) |
| Dependencias | `gradle/libs.versions.toml` |

## Configuración de Firebase (Actividad 4)
El archivo **`app/google-services.json` no está en el repositorio** (está en `.gitignore`). Para compilar:
1. En [console.firebase.google.com](https://console.firebase.google.com) crear un proyecto.
2. Agregar **dos apps Android**: `com.amemarqui.recordatorios` y `com.amemarqui.recordatorios.dev`.
3. Descargar `google-services.json` (después de registrar las dos apps) y copiarlo en `app/`.
4. **Authentication → Sign-in method →** habilitar **Correo electrónico/contraseña**.
5. **Firestore Database → Crear base de datos** (modo producción) y en **Reglas** pegar el contenido de [`firestore.rules`](firestore.rules) → **Publicar**.

Las dos variantes usan el mismo proyecto de Firebase, así que se pueden instalar las dos en el mismo emulador y funcionan como **dos dispositivos distintos** (sirve para probar la recuperación de datos en otro dispositivo).

## Variantes (build types × product flavors)
| | `debug` | `release` |
|---|---|---|
| **`dev`** | `devDebug`: para desarrollar y probar. Nombre "Recordatorios DEV", id `.dev` | `devRelease` |
| **`prod`** | `prodDebug` | **`prodRelease`**: la que se entrega. Minificada (R8), recursos reducidos y **firmada** |

`BuildConfig.AMBIENTE` indica la variante; en `dev` la pantalla de login muestra "Ambiente: DESARROLLO".

## Firma del release
La clave de firma **no se sube al repositorio**. `app/build.gradle.kts` lee los datos de un archivo **`keystore.properties`** (ignorado por git) en la raíz del proyecto. Hay un ejemplo sin datos reales en [`keystore.properties.example`](keystore.properties.example):

```properties
storeFile=keystore/recordatorios-release.jks
storePassword=CAMBIAR
keyAlias=recordatorios
keyPassword=CAMBIAR
```

Si `keystore.properties` no existe, el proyecto compila igual, pero el release sale **sin firmar**. Los archivos `*.jks` / `*.keystore` también están en `.gitignore`.

Generar los artefactos (desde la terminal de Android Studio, en la raíz del proyecto):
```
.\gradlew assembleProdRelease bundleProdRelease
```
- APK: `app/build/outputs/apk/prod/release/app-prod-release.apk`
- AAB: `app/build/outputs/bundle/prodRelease/app-prod-release.aab`

## Configuración del servicio remoto REST (Actividad 3)
Desde la Actividad 4 la fuente remota es **Firestore**. El servicio REST de la Actividad 3 sigue en el código detrás de la misma interfaz (`FuenteRemotaRecordatorios`) y se puede volver a usar cambiando `FUENTE_REMOTA` a `"rest"` en `app/build.gradle.kts`.

La URL del servicio se configura en un archivo **`api.properties`** en la raíz del proyecto (no se sube al repositorio).
Hay un ejemplo sin credenciales en **`api.properties.example`**:

```properties
api.baseUrl=https://servidor-simulado.local/api/
api.usarServidorSimulado=true
```

- Si `api.properties` no existe, se usan esos valores: la app trae un **servidor simulado** (un interceptor de OkHttp) que responde como un servicio REST real y guarda sus datos en un archivo aparte de Room. No necesita internet, pero **respeta el estado de la red del dispositivo**: con el modo avión activado responde "sin conexión".
- Para usar un servicio real: copiar el ejemplo como `api.properties`, poner su URL en `api.baseUrl` y `api.usarServidorSimulado=false`. El servicio tiene que ofrecer `GET /recordatorios`, `PUT /recordatorios/{id}` (crear o actualizar) y `DELETE /recordatorios/{id}`.

## Estructura
```
app/src/main/java/com/example/recordatorios/
├── RecordatoriosApplication.kt        # Crea el AppContainer, verifica la sesión y arranca la sincronización
├── MainActivity.kt                    # Aplica el tema guardado y muestra RaizApp
├── model/                             # Modelo de dominio y validaciones
├── data/
│   ├── AppContainer.kt                # Instancias únicas: Room, DataStore, Firebase, sincronizador, repositorio
│   ├── auth/                          # AutenticacionRepository (interfaz) + AutenticacionFirebase
│   ├── firebase/                      # FuenteRemotaFirestore (users/{uid}/reminders)
│   ├── RecordatoriosRepository.kt     # Interfaz que usan los ViewModel
│   ├── RepositorioRecordatoriosLocalFirst.kt
│   ├── local/                         # Room: RecordatorioEntity, RecordatorioDao, RecordatoriosDatabase
│   ├── preferencias/                  # DataStore: PreferenciasUsuario, PreferenciasRepository, PreferenciasDataStore
│   ├── remote/                        # FuenteRemotaRecordatorios (interfaz), DTO, ResultadoRed, REST (Retrofit)
│   │   └── simulado/                  # Servidor simulado (interceptor de OkHttp)
│   ├── mapeo/                         # Mapeos DTO <-> Entity <-> dominio
│   ├── sync/                          # SincronizadorRecordatorios, MonitorConectividad
│   └── RepositorioSimulado.kt         # Categorías, estilos y datos iniciales del servidor simulado
└── ui/
    ├── components/                    # Tarjeta, banner de sincronización, campos, fecha, indicador de estado
    ├── auth/                          # Login / registro: AuthScreen + AuthViewModel
    ├── listado/  formulario/  detalle/  ajustes/   # Screen + ViewModel + UiState
    ├── navigation/                    # RaizApp (según la sesión), Rutas, AppNavHost
    └── theme/
```

## Arquitectura (Actividad 4)
```
Compose (Screen) → ViewModel (StateFlow<UiState>) → RecordatoriosRepository
                                                     ├── Room (fuente local, filtrada por uid)
                                                     └── SincronizadorRecordatorios → FuenteRemotaFirestore → Firestore
AuthScreen → AuthViewModel → AutenticacionRepository → AutenticacionFirebase → Firebase Auth
```
Diagrama: [`docs/flujo-actividad4.png`](docs/flujo-actividad4.png).

Ningún `@Composable` usa clases de Firebase: la interfaz solo ve `UiState`, `EstadoSesion` y `ResultadoOperacion`.

## Rutas
Antes del grafo, `RaizApp` muestra *Verificando sesión* → *Login / registro* → o el grafo (con `key(uid)`, así cambiar de cuenta crea pantallas nuevas).

| Ruta | Pantalla | Argumentos |
|---|---|---|
| `listado` (inicial) | Listado | — |
| `crear` | Formulario (nuevo) | — |
| `editar/{id}` | Formulario (edición) | `id: String` (UUID) |
| `detalle/{id}` | Detalle | `id: String` (UUID) |
| `ajustes` | Ajustes | — |

## Cómo probar
- **Sesión:** crear una cuenta, cerrar la app y volver a abrirla (sigue la sesión). Cerrar sesión desde Ajustes → vuelve al login. Deshabilitar el usuario en la consola de Firebase y reabrir la app → "Tu sesión ya no es válida".
- **Separación de cuentas:** con la cuenta A crear recordatorios, cerrar sesión y entrar con la cuenta B: no se ve nada de A.
- **Otro dispositivo:** instalar `devDebug` y `prodRelease` (o usar dos emuladores) con la misma cuenta: lo creado en uno aparece en el otro.
- **Seguridad:** Ajustes → Prueba de seguridad → pegar el uid de otra cuenta → las dos operaciones tienen que salir **DENEGADA**.
- **Persistencia:** crear recordatorios, cerrar la app por completo (sacarla de recientes) y volver a abrirla.
- **Offline:** activar el modo avión en el emulador, crear/editar/eliminar: los cambios se guardan y aparecen como "Sin sincronizar". Al desactivar el modo avión se sincronizan solos.
- **Preferencias:** cambiar tema, orden y "mostrar completados", cerrar y abrir la app. "Guardar un valor inválido" prueba la respuesta ante un dato desconocido.

## Ejecutar
1. Copiar `google-services.json` en `app/` (ver *Configuración de Firebase*).
1. **File → Open** y elegir la carpeta del proyecto (la que contiene `settings.gradle.kts`).
2. Esperar a que termine **Gradle Sync** (la primera vez descarga dependencias).
3. Elegir el emulador o teléfono y presionar **Run ▶**.
4. Elegir la variante en **Build → Select Build Variant** (`devDebug` para probar).
5. Tests: en la terminal `.\gradlew testDevDebugUnitTest`.
