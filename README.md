# Recordatorios — Programación Mobile, Actividad 1

Primera versión de una app Android de recordatorios contextuales, hecha con **Kotlin**, **Jetpack Compose** y **Material 3**. Muestra datos simulados; todavía no tiene formularios, navegación, persistencia ni sincronización (eso llega en los próximos módulos).

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
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0) |
| Dependencias | `gradle/libs.versions.toml` |

## Estructura
```
app/src/main/java/com/example/recordatorios/
├── MainActivity.kt                 # Punto de entrada; carga datos y escribe diagnóstico en Logcat
├── model/
│   ├── Modelos.kt                  # Consigna 2: data classes y sealed classes
│   └── Validacion.kt               # Reglas de validación (ResultadoValidacion)
├── data/
│   └── RepositorioSimulado.kt      # Consigna 3: List, Set, Map y filtros
└── ui/
    ├── theme/                      # Tema Material 3 (Color, Type, Theme)
    └── screens/
        └── PantallaRecordatorios.kt # Consigna 4: pantalla inicial con Compose
app/src/test/.../ModeloTest.kt       # Tests unitarios del modelo y las colecciones
```

## Ejecutar
1. **File → Open** y elegir la carpeta `RecordatoriosApp`.
2. Esperar a que termine **Gradle Sync** (la primera vez descarga dependencias).
3. Elegir el emulador o teléfono y presionar **Run ▶**.
4. En **Logcat**, filtrar por `tag:Recordatorios` para ver el diagnóstico de colecciones y validaciones.
5. Tests: clic derecho en `ModeloTest` → **Run 'ModeloTest'** (9 tests).
