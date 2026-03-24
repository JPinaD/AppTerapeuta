package com.example.appterapeuta.util;

import com.example.appterapeuta.data.model.Robot;
import com.example.appterapeuta.data.model.StudentProfile;
import com.example.appterapeuta.data.model.TherapyActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockDataProvider {

    public static List<Robot> getMockRobots() {

        List<Robot> robots = new ArrayList<>();

        robots.add(new Robot("1", "Robot Alpha", "00:11:22:33:44:55", true, 85));
        robots.add(new Robot("2", "Robot Beta", "66:77:88:99:AA:BB", false, 40));
        robots.add(new Robot("3", "Robot Gamma", "CC:DD:EE:FF:00:11", true, 60));
        robots.add(new Robot("4", "Robot Delta", "22:33:44:55:66:77", false, 15));

        return robots;
    }

    public static List<StudentProfile> getMockStudentProfiles(){

        List<StudentProfile> profiles = new ArrayList<>();

        profiles.add(new StudentProfile("s1", "Mario", "M", "Apoyo visual alto"));
        profiles.add(new StudentProfile("s2", "Lucía", "L", "Refuerzo sonoro"));
        profiles.add(new StudentProfile("s3", "Diego", "D", "Baja estimulación visual"));

        return profiles;
    }
    public static List<TherapyActivity> getMockActivities(){

        List<TherapyActivity> activities = new ArrayList<>();

        activities.add(new TherapyActivity("a1", "Reconocer emociones", "Identificar emociones básicas", 1));
        activities.add(new TherapyActivity("a2", "Turnos cooperativos", "Esperar y responder por turnos", 2));
        activities.add(new TherapyActivity("a3", "Asociación visual", "Relacionar imagen y concepto", 1));

        return activities;
    }
}
