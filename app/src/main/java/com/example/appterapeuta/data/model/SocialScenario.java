package com.example.appterapeuta.data.model;

/** Escenario social con descripción y 2 opciones. */
public class SocialScenario {
    public final String id;
    public final String description;
    public final String optionA;
    public final String optionB;
    public final String outcomeA;
    public final String outcomeB;

    public SocialScenario(String id, String description,
                          String optionA, String optionB,
                          String outcomeA, String outcomeB) {
        this.id          = id;
        this.description = description;
        this.optionA     = optionA;
        this.optionB     = optionB;
        this.outcomeA    = outcomeA;
        this.outcomeB    = outcomeB;
    }
}
