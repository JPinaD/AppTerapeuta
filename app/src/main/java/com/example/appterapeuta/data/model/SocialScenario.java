package com.example.appterapeuta.data.model;

/** Escenario social con descripción, 2 opciones e indicación de cuál es la correcta. */
public class SocialScenario {
    public final String id;
    public final String description;
    public final String optionA;
    public final String optionB;
    public final String outcomeA;
    public final String outcomeB;
    public final String correctOption; // "A" o "B"

    public SocialScenario(String id, String description,
                          String optionA, String optionB,
                          String outcomeA, String outcomeB,
                          String correctOption) {
        this.id             = id;
        this.description    = description;
        this.optionA        = optionA;
        this.optionB        = optionB;
        this.outcomeA       = outcomeA;
        this.outcomeB       = outcomeB;
        this.correctOption  = correctOption;
    }
}
