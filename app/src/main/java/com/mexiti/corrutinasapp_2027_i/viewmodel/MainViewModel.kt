package com.mexiti.corrutinasapp_2027_i.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job

// Estado de la Interfaz de Usuario
data class UiState(
    val statusMessage: String = "Sistema Listo",
    val progress: Int = 0,
    val isBusy: Boolean = false,
    val tiempoContador1: Int = 0,
    val tiempoContador2: Int = 0,
    val errorMessage: String? = null
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    var countTime by mutableStateOf(0)
        private set

    // Mutex para controlar el acceso exclusivo al recurso
    private val mutex = Mutex()

    private var timerJob: Job? = null

    // ESCENARIO A: Intenta ejecutar un proceso; si hay otro en marcha, lanza un ERROR.
    fun ejecutarConErrorSiOcupado(nombreProceso: String) {
        if (_uiState.value.isBusy) {
            _uiState.update {
                it.copy(errorMessage = "ERROR: ¡El proceso actual no ha finalizado! Rechazado: $nombreProceso")
            }
            return
        }
    timerJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isBusy = true,
                    errorMessage = null,
                    statusMessage = "Ejecutando $nombreProceso...",
                    progress = 0,
                    tiempoContador1 = 0,
                    tiempoContador2 = 0)
            }

                for (i in 1..5) {
                    delay(1000)
                    _uiState.update { it.copy(progress = i * 20)
            }

            _uiState.update {
                it.copy(
                    isBusy = false,
                    statusMessage = "Proceso $nombreProceso finalizado con éxito.",
                    progress = 0
                )
                }
            cronometros()
            }
        }
    }

    // ESCENARIO B (SOLUCIÓN): Usa Mutex para hacer cola/sincronizar.
    // El segundo proceso ESPERA activamente a que el primero libere el recurso.
    fun ejecutarSincronizadoConMutex(nombreProceso: String) {
        viewModelScope.launch {
            if (mutex.isLocked) {
                _uiState.update {
                    it.copy(statusMessage = "Esperando turno para $nombreProceso...", errorMessage = null)
                }
            }

            // withLock suspende la corrutina hasta que el proceso anterior libera el Mutex
            timerJob = viewModelScope.launch {
            mutex.withLock {
                _uiState.update {
                    it.copy(
                        isBusy = true,
                        errorMessage = null,
                        statusMessage = "Ejecutando $nombreProceso...",
                        progress = 0,
                        tiempoContador1 = 0,
                        tiempoContador2 = 0
                    )
                }
                for (i in 1..5) {
                    delay(1000)
                    _uiState.update {
                        it.copy(progress = i * 20)
                    }
                }

                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusMessage = "Finalizado $nombreProceso",
                        progress = 0
                    )
                }
                cronometros()
                }
            }
        }
    }

    suspend fun cronometros(){
        for (j in 1..5){
            delay(1000)
            _uiState.update { it.copy(tiempoContador1 = j) }
        }

        _uiState.update {it.copy(progress = 0)}

        for (k in 1..5){
            delay(1000)
            _uiState.update { it.copy(tiempoContador2 = k) }
        }
    }

    fun cancelarProceso (){
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isBusy = false,
                statusMessage = "Proceso Cancelado",
                progress = 0
            )
        }
    }

    fun limpiarMensajeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}