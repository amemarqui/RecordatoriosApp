# Recordatorios — Programación Mobile

App Android de recordatorios hecha con **Kotlin**, **Jetpack Compose** y **Material 3**.

- **Actividad 1:** modelo de datos, validaciones, colecciones y una primera pantalla con datos simulados.
- **Actividad 2:** pantallas de listado, creación y detalle, componentes reutilizables, formulario validado, ViewModel + UiState + StateFlow, repositorio en memoria y Navigation Compose.

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
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0) |
| Dependencias | `gradle/libs.versions.toml` |

## Estructura
```
app/src/main/java/com/example/recordatorios/
├── MainActivity.kt                    # Solo arranca el tema y el AppNavHost
├── model/                             # Modelo y validaciones (Act. 1, ampliado)
├── data/
│   ├── RecordatoriosRepository.kt     # Interfaz del repositorio
│   ├── RepositorioEnMemoria.kt        # Implementación en memoria (con demoras simuladas)
│   ├── SimuladorEscenarios.kt         # Escenarios: normal, vacío, error de carga, error al guardar
│   ├── AppContainer.kt                # Instancia única del repositorio
│   └── RepositorioSimulado.kt         # Datos de ejemplo, List/Set/Map (Act. 1)
└── ui/
    ├── components/                    # Tarjeta, indicador de estado, campos del formulario
    ├── listado/                       # ListadoScreen + ListadoViewModel + ListadoUiState
    ├── crear/                         # CrearRecordatorioScreen + ViewModel + CrearUiState
    ├── detalle/                       # DetalleScreen + DetalleViewModel + DetalleUiState
    ├── navigation/                    # Rutas + AppNavHost
    └── theme/
```

## Rutas
| Ruta | Pantalla | Argumentos |
|---|---|---|
| `listado` (inicial) | Listado | — |
| `crear` | Creación | — |
| `detalle/{id}` | Detalle | `id: Int` |

## Probar los estados
En el listado, el menú **⋮ → Simular escenario** permite elegir:
- **Sin recordatorios:** muestra el estado vacío.
- **Error al cargar:** falla una vez; "Reintentar" vuelve a cargar bien.
- **Error al guardar:** el próximo guardado falla una vez.
- **Datos de ejemplo:** restaura los datos iniciales.

## Ejecutar
1. **File → Open** y elegir la carpeta `RecordatoriosApp`.
2. Esperar a que termine **Gradle Sync** (la primera vez descarga dependencias).
3. Elegir el emulador o teléfono y presionar **Run ▶**.
4. Tests: clic derecho en la carpeta `test` → **Run 'Tests in ...'**.
