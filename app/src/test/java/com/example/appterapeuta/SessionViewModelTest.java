package com.example.appterapeuta;

import com.example.appterapeuta.data.model.ActivityEvent;
import com.example.appterapeuta.data.model.RobotSessionState;
import com.example.appterapeuta.util.AppConstants;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests unitarios para la lógica de transición de estados de sesión.
 * Replica la lógica de SessionViewModel.handleIncomingEvent sin depender
 * del framework Android (sin LiveData ni Application).
 */
public class SessionViewModelTest {

    /**
     * Replica la lógica de resolución de estado de SessionViewModel.handleIncomingEvent.
     * Devuelve null si el tipo de evento no produce cambio de estado.
     */
    private RobotSessionState resolveState(String eventType) {
        if (AppConstants.MSG_SESSION_READY.equals(eventType))      return RobotSessionState.READY;
        if (AppConstants.MSG_SESSION_ENDED.equals(eventType))      return RobotSessionState.ENDED;
        if (AppConstants.MSG_PICTOGRAM_SELECTED.equals(eventType)) return RobotSessionState.IN_ACTIVITY;
        if (AppConstants.MSG_SESSION_PAUSED.equals(eventType))     return RobotSessionState.PAUSED;
        if (AppConstants.MSG_SESSION_RESUMED.equals(eventType))    return RobotSessionState.IN_ACTIVITY;
        return null;
    }

    @Test
    public void sessionReady_producesEstadoReady() {
        RobotSessionState state = resolveState(AppConstants.MSG_SESSION_READY);
        assertEquals(RobotSessionState.READY, state);
    }

    @Test
    public void sessionPaused_producesEstadoPaused() {
        RobotSessionState state = resolveState(AppConstants.MSG_SESSION_PAUSED);
        assertEquals(RobotSessionState.PAUSED, state);
    }

    @Test
    public void sessionResumed_producesEstadoInActivity() {
        RobotSessionState state = resolveState(AppConstants.MSG_SESSION_RESUMED);
        assertEquals(RobotSessionState.IN_ACTIVITY, state);
    }

    @Test
    public void sessionEnded_producesEstadoEnded() {
        RobotSessionState state = resolveState(AppConstants.MSG_SESSION_ENDED);
        assertEquals(RobotSessionState.ENDED, state);
    }

    @Test
    public void tipoDesconocido_devuelveNull() {
        RobotSessionState state = resolveState("TIPO_DESCONOCIDO");
        assertNull(state);
    }
}
