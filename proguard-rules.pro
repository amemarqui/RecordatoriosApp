# Reglas para la versión release (minify + shrink).

# Modelos que se serializan con Gson (servicio REST alternativo): R8 no debe renombrar sus campos
-keep class com.example.recordatorios.data.remote.** { *; }

# Enums de la app: se guardan por nombre en Room y DataStore (name / valueOf)
-keepclassmembers enum com.example.recordatorios.** { *; }
-keepnames enum com.example.recordatorios.** { *; }

# Retrofit usa la firma genérica de las funciones suspend
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Para leer los errores de los testers con número de línea (se puede quitar)
-keepattributes SourceFile,LineNumberTable
