package com.example.appterapeuta.data.model;

import java.util.List;
import java.util.Map;

public class Session {
    public final String sessionId;
    public final List<String> participatingRobotIds;
    public final Map<String, String> robotToStudentProfileId; // robotId → profileId
    public final String activityId;
    public SessionState state;

    public Session(String sessionId, List<String> participatingRobotIds,
                   Map<String, String> robotToStudentProfileId, String activityId) {
        this.sessionId = sessionId;
        this.participatingRobotIds = participatingRobotIds;
        this.robotToStudentProfileId = robotToStudentProfileId;
        this.activityId = activityId;
        this.state = SessionState.PREPARING;
    }
}
