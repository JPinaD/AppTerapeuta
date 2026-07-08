package com.example.appterapeuta.util;

public class AppConstants {
    public static final String NSD_SERVICE_TYPE  = "_approbot._tcp.";
    public static final String NSD_ATTR_ROBOT_ID = "robotId";
    public static final int    NSD_DEFAULT_PORT  = 9000;
    public static final String MSG_PING = "PING";
    public static final String MSG_PONG = "PONG";

    // Actividad pictograma v1
    public static final String MSG_ACTIVITY_START     = "ACTIVITY_START";
    /** @deprecated Replaced by MSG_COMMUNICATOR_SEQUENCE */
    @Deprecated
    public static final String MSG_PICTOGRAM_SELECTED = "PICTOGRAM_SELECTED";
    public static final String MSG_ROBOT_FEEDBACK     = "ROBOT_FEEDBACK";

    // Sesiones
    public static final String MSG_SESSION_START  = "SESSION_START";
    public static final String MSG_SESSION_END    = "SESSION_END";
    public static final String MSG_SESSION_READY  = "SESSION_READY";
    public static final String MSG_SESSION_ENDED  = "SESSION_ENDED";

    // Parada de emergencia
    public static final String MSG_SESSION_PAUSE   = "SESSION_PAUSE";
    public static final String MSG_SESSION_PAUSED  = "SESSION_PAUSED";
    public static final String MSG_SESSION_RESUME  = "SESSION_RESUME";
    public static final String MSG_SESSION_RESUMED = "SESSION_RESUMED";

    // Telemetría
    public static final String MSG_ROBOT_STATUS = "ROBOT_STATUS";

    // Resultados de actividad
    public static final String MSG_ACTIVITY_RESULT = "ACTIVITY_RESULT";

    // Comunicador bidireccional (TCP)
    public static final String MSG_COMMUNICATOR_SEQUENCE       = "COMMUNICATOR_SEQUENCE";
    public static final String MSG_COMMUNICATOR_RESPONSE       = "COMMUNICATOR_RESPONSE";
    public static final String MSG_TERAPEUTA_PICTOGRAM_MESSAGE = "TERAPEUTA_PICTOGRAM_MESSAGE";
    public static final String MSG_STUDENT_PICTOGRAM_RESPONSE  = "STUDENT_PICTOGRAM_RESPONSE";

    // Turnos cooperativos
    public static final String MSG_TURN_SIGNAL = "TURN_SIGNAL";
    public static final String MSG_TURN_DONE   = "TURN_DONE";

    // Seguridad: detección de vuelco
    public static final String MSG_TILT_ALERT = "TILT_ALERT";
}
