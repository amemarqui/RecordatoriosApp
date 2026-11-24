# Recordatorios — Programación Mobile

App Android de recordatorios hecha con **Kotlin**, **Jetpack Compose** y **Material 3**.

- **Actividad 1:** modelo de datos, validaciones, colecciones y una primera pantalla con datos simulados.
- **Actividad 2:** pantallas de listado, creación y detalle, componentes reutilizables, formulario validado, ViewModel + UiState + StateFlow, repositorio en memoria y Navigation Compose.
- **Actividad 3:** persistencia con **Room**, preferencias con **DataStore**, repositorio **local-first**, servicio remoto con **Retrofit + OkHttp**, sincronización controlada (sin duplicados) y funcionamiento **offline**.

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
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0) |
| Dependencias | `gradle/libs.versions.toml` |

## Configuración del servicio remoto
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
├── RecordatoriosApplication.kt        # Crea el AppContainer y arranca la sincronización automática
├── MainActivity.kt                    # Aplica el tema guardado y muestra el AppNavHost
├── model/                             # Modelo de dominio y validaciones
├── data/
│   ├── AppContainer.kt                # Instancias únicas: Room, DataStore, Retrofit, sincronizador, repositorio
│   ├── RecordatoriosRepository.kt     # Interfaz que usan los ViewModel
│   ├── RepositorioRecordatoriosLocalFirst.kt
│   ├── local/                         # Room: RecordatorioEntity, RecordatorioDao, RecordatoriosDatabase
│   ├── preferencias/                  # DataStore: PreferenciasUsuario, PreferenciasRepository, PreferenciasDataStore
│   ├── remote/                        # Retrofit: RecordatorioDto, RecordatoriosApi, ResultadoRed, ClienteRed
│   │   └── simulado/                  # Servidor simulado (interceptor de OkHttp)
│   ├── mapeo/                         # Mapeos DTO <-> Entity <-> dominio
│   ├── sync/                          # SincronizadorRecordatorios, MonitorConectividad
│   └── RepositorioSimulado.kt         # Categorías, estilos y datos iniciales del servidor simulado
└── ui/
    ├── components/                    # Tarjeta, banner de sincronización, campos, fecha, indicador de estado
    ├── listado/  formulario/  detalle/  ajustes/   # Screen + ViewModel + UiState
    ├── navigation/                    # Rutas + AppNavHost
    └── theme/
```

## Rutas
| Ruta | Pantalla | Argumentos |
|---|---|---|
| `listado` (inicial) | Listado | — |
| `crear` | Formulario (nuevo) | — |
| `editar/{id}` | Formulario (edición) | `id: String` (UUID) |
| `detalle/{id}` | Detalle | `id: String` (UUID) |
| `ajustes` | Ajustes | — |

## Cómo probar
- **Persistencia:** crear recordatorios, cerrar la app por completo (sacarla de recientes) y volver a abrirla.
- **Offline:** activar el modo avión en el emulador, crear/editar/eliminar: los cambios se guardan y aparecen como "Sin sincronizar". Al desactivar el modo avión se sincronizan solos.
- **Errores de red:** Ajustes → Servidor simulado → Timeout / Error 500 / Rechaza los datos (400), y después tocar ⟳ en el listado.
- **Preferencias:** cambiar tema, orden y "mostrar completados", cerrar y abrir la app. "Guardar un valor inválido" prueba la respuesta ante un dato desconocido.

## Ejecutar
1. (Opcional) Copiar `api.properties.example` como `api.properties` y ajustar los valores.
1. **File → Open** y elegir la carpeta del proyecto (la que contiene `settings.gradle.kts`).
2. Esperar a que termine **Gradle Sync** (la primera vez descarga dependencias).
3. Elegir el emulador o teléfono y presionar **Run ▶**.
4. Tests: clic derecho en la carpeta `test` → **Run 'Tests in ...'**.
