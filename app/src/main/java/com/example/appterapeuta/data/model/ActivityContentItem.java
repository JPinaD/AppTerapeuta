package com.example.appterapeuta.data.model;

/** Ítem del repositorio de contenido de actividades. */
public class ActivityContentItem {
    public final String id;
    public final String activityType;
    public final String label;
    public final String drawableRes; // nullable

    public ActivityContentItem(String id, String activityType, String label, String drawableRes) {
        this.id           = id;
        this.activityType = activityType;
        this.label        = label;
        this.drawableRes  = drawableRes;
    }
}
