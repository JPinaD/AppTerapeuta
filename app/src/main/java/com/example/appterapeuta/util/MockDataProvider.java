package com.example.appterapeuta.util;

import com.example.appterapeuta.data.model.Robot;
import com.example.appterapeuta.data.model.StudentProfile;
import com.example.appterapeuta.data.model.TherapyActivity;

import java.util.ArrayList;
import java.util.List;

public class MockDataProvider {

    public static List<Robot> getMockRobots() {
        List<Robot> robots = new ArrayList<>();
        robots.add(new Robot("robot-1", "Robot Alpha", true,  85));
        robots.add(new Robot("robot-2", "Robot Beta",  false, 40));
        robots.add(new Robot("robot-3", "Robot Gamma", true,  60));
        robots.add(new Robot("robot-4", "Robot Delta", false, 15));
        return robots;
    }

    public static List<StudentProfile> getMockStudentProfiles() {
        List<StudentProfile> profiles = new ArrayList<>();
        profiles.add(new StudentProfile("s1", "Mario", "M", "Apoyo visual alto"));
        profiles.add(new StudentProfile("s2", "Lucía", "L", "Refuerzo sonoro"));
        profiles.add(new StudentProfile("s3", "Diego", "D", "Baja estimulación visual"));
        return profiles;
    }

    public static List<TherapyActivity> getMockActivities() {
        List<TherapyActivity> activities = new ArrayList<>();
        activities.add(new TherapyActivity("a1", "Reconocer emociones",  "Identificar emociones básicas",          1));
        activities.add(new TherapyActivity("a2", "Turnos cooperativos",  "Esperar y responder por turnos",          2));
        activities.add(new TherapyActivity("a3", "Asociación visual",    "Relacionar imagen y concepto",            1));
        return activities;
    }
}
