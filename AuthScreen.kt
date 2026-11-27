package com.example.recordatorios.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.BuildConfig
import com.example.recordatorios.ui.components.CampoTextoValidado
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/**
 * motivoSesionPerdida: si la sesión guardada dejó de ser válida, la raíz lo pasa acá
 * para explicarle al usuario por qué tiene que volver a entrar.
 */
@Composable
fun AuthRoute(
    motivoSesionPerdida: String?,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    AuthScreen(
        estado = estado,
        motivoSesionPerdida = motivoSesionPerdida,
        onModoChange = viewModel::onModoChange,
        onEmailChange = viewModel::onEmailChange,
        onContraseniaChange = viewModel::onContraseniaChange,
        onConfirmacionChange = viewModel::onConfirmacionChange,
        onEnviar = viewModel::onEnviar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    estado: AuthUiState,
    motivoSesionPerdida: String?,
    onModoChange: (ModoAuth) -> Unit,
    onEmailChange: (String) -> Unit,
    onContraseniaChange: (String) -> Unit,
    onConfirmacionChange: (String) -> Unit,
    onEnviar: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Mostrar / ocultar la contraseña es estado de UI puro, por eso vive en la pantalla
    var verContrasenia by rememberSaveable { mutableStateOf(false) }
    val registro = estado.modo == ModoAuth.REGISTRARSE

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text("Recordatorios", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Tus recordatorios se guardan en el teléfono y se sincronizan con tu cuenta.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (BuildConfig.AMBIENTE != "produccion") {
                Text(
                    "Ambiente: ${BuildConfig.AMBIENTE.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (motivoSesionPerdida != null) {
                Aviso(motivoSesionPerdida)
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ModoAuth.entries.forEachIndexed { i, modo ->
                    SegmentedButton(
                        selected = estado.modo == modo,
                        onClick = { onModoChange(modo) },
                        enabled = !estado.enCurso,
                        shape = SegmentedButtonDefaults.itemShape(i, ModoAuth.entries.size)
                    ) {
                        Text(if (modo == ModoAuth.INICIAR_SESION) "Iniciar sesión" else "Crear cuenta")
                    }
                }
            }

            CampoTextoValidado(
                valor = estado.email,
                onValorChange = onEmailChange,
                etiqueta = "Email",
                mensajeError = estado.errorEmail,
                habilitado = !estado.enCurso,
                opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            CampoTextoValidado(
                valor = estado.contrasenia,
                onValorChange = onContraseniaChange,
                etiqueta = "Contraseña",
                mensajeError = estado.errorContrasenia,
                habilitado = !estado.enCurso,
                opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Password),
                transformacion = if (verContrasenia) VisualTransformation.None else PasswordVisualTransformation()
            )
            if (registro) {
                CampoTextoValidado(
                    valor = estado.confirmacion,
                    onValorChange = onConfirmacionChange,
                    etiqueta = "Repetir contraseña",
                    mensajeError = estado.errorConfirmacion,
                    habilitado = !estado.enCurso,
                    opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Password),
                    transformacion = if (verContrasenia) VisualTransformation.None else PasswordVisualTransformation()
                )
            }
            TextButton(onClick = { verContrasenia = !verContrasenia }, modifier = Modifier.align(Alignment.End)) {
                Text(if (verContrasenia) "Ocultar contraseña" else "Mostrar contraseña")
            }

            if (estado.errorGeneral != null) {
                Aviso(estado.errorGeneral)
            }

            Button(
                onClick = onEnviar,
                enabled = !estado.enCurso,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (estado.enCurso) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(if (registro) "Creando cuenta…" else "Iniciando sesión…")
                } else {
                    Text(if (registro) "Crear cuenta" else "Iniciar sesión")
                }
            }
        }
    }
}

/** Mensaje de error / aviso con ícono y texto (no depende solo del color). */
@Composable
private fun Aviso(texto: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AuthPreview() {
    RecordatoriosTheme {
        AuthScreen(
            estado = AuthUiState(modo = ModoAuth.REGISTRARSE, email = "ana@mail.com", errorGeneral = "Ya existe una cuenta con ese email."),
            motivoSesionPerdida = null,
            onModoChange = {}, onEmailChange = {}, onContraseniaChange = {}, onConfirmacionChange = {}, onEnviar = {}
        )
    }
}
