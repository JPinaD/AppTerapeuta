package com.example.appterapeuta.util;

public class AppConstants {
    public static final String NSD_SERVICE_TYPE  = "_approbot._tcp.";
    public static final String NSD_ATTR_ROBOT_ID = "robotId";
    public static final int    NSD_DEFAULT_PORT  = 9000;
    public static final String MSG_PING = "PING";
    public static final String MSG_PONG = "PONG";

    // Actividades
    public static final String MSG_ACTIVITY_START     = "ACTIVITY_START";
    public static final String MSG_ACTIVITY_RESULT    = "ACTIVITY_RESULT";
    public static final String MSG_PICTOGRAM_SELECTED = "PICTOGRAM_SELECTED";
    public static final String MSG_ROBOT_FEEDBACK     = "ROBOT_FEEDBACK";
    public static final String MSG_TURN_SIGNAL        = "TURN_SIGNAL";
    public static final String MSG_TURN_DONE          = "TURN_DONE";

    // IDs de actividades
    public static final String ACTIVITY_PICTOGRAM        = "activity_pictogram";
    public static final String ACTIVITY_PICTOGRAM_LEGACY = "pictogram_v1";
    public static final String ACTIVITY_EMOTION          = "activity_emotion";
    public static final String ACTIVITY_SOCIAL           = "activity_social";
    public static final String ACTIVITY_SEQUENCE         = "activity_sequence";
    public static final String ACTIVITY_CALM             = "activity_calm";
    public static final String ACTIVITY_TURNS            = "activity_turns";

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
}
