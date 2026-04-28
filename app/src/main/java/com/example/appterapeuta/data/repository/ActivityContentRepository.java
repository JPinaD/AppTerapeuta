package com.example.appterapeuta.data.repository;

import com.example.appterapeuta.data.model.ActivityContentItem;
import com.example.appterapeuta.data.model.SocialScenario;
import com.example.appterapeuta.util.AppConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de contenido de actividades. Datos hardcodeados, no editables por el terapeuta.
 */
public class ActivityContentRepository {

    private static final List<ActivityContentItem> EMOTIONS = new ArrayList<>();
    private static final List<SocialScenario> SOCIAL_SCENARIOS = new ArrayList<>();
    private static final List<ActivityContentItem> TURN_PICTOGRAMS = new ArrayList<>();

    static {
        EMOTIONS.add(new ActivityContentItem("emotion_happy",   AppConstants.ACTIVITY_EMOTION, "Feliz",      "emotion_happy"));
        EMOTIONS.add(new ActivityContentItem("emotion_sad",     AppConstants.ACTIVITY_EMOTION, "Triste",     "emotion_sad"));
        EMOTIONS.add(new ActivityContentItem("emotion_angry",   AppConstants.ACTIVITY_EMOTION, "Enfadado",   "emotion_angry"));
        EMOTIONS.add(new ActivityContentItem("emotion_surprised", AppConstants.ACTIVITY_EMOTION, "Sorprendido", "emotion_surprised"));
        EMOTIONS.add(new ActivityContentItem("emotion_scared",  AppConstants.ACTIVITY_EMOTION, "Asustado",   "emotion_scared"));

        SOCIAL_SCENARIOS.add(new SocialScenario(
                "social_greet",
                "Ves a un amigo en el parque. ¿Qué haces?",
                "Le saludas y sonríes",
                "Miras hacia otro lado",
                "Tu amigo se alegra de verte",
                "Tu amigo sigue jugando solo"));

        SOCIAL_SCENARIOS.add(new SocialScenario(
                "social_share",
                "Tienes un juguete y otro niño quiere jugar. ¿Qué haces?",
                "Le dejas el juguete un rato",
                "Sigues jugando tú solo",
                "Los dos jugáis juntos y os divertís",
                "El otro niño busca otro juguete"));

        SOCIAL_SCENARIOS.add(new SocialScenario(
                "social_help",
                "Un compañero se ha caído. ¿Qué haces?",
                "Le preguntas si está bien",
                "Sigues caminando",
                "Tu compañero te da las gracias",
                "Tu compañero se levanta solo"));

        SOCIAL_SCENARIOS.add(new SocialScenario(
                "social_wait",
                "Hay cola para el tobogán. ¿Qué haces?",
                "Esperas tu turno",
                "Te cuelas delante",
                "Todos esperan y llega tu turno",
                "Los demás niños se enfadan"));

        // Pictogramas de turno (reutiliza los de activity_pictogram)
        TURN_PICTOGRAMS.add(new ActivityContentItem("pic_agua",   AppConstants.ACTIVITY_TURNS, "Agua",   "pic_agua"));
        TURN_PICTOGRAMS.add(new ActivityContentItem("pic_jugar",  AppConstants.ACTIVITY_TURNS, "Jugar",  "pic_jugar"));
        TURN_PICTOGRAMS.add(new ActivityContentItem("pic_comer",  AppConstants.ACTIVITY_TURNS, "Comer",  "pic_comer"));
        TURN_PICTOGRAMS.add(new ActivityContentItem("pic_ayuda",  AppConstants.ACTIVITY_TURNS, "Ayuda",  "pic_ayuda"));
    }

    public List<ActivityContentItem> getItemsForActivity(String activityId) {
        switch (activityId) {
            case AppConstants.ACTIVITY_EMOTION:   return new ArrayList<>(EMOTIONS);
            case AppConstants.ACTIVITY_TURNS:     return new ArrayList<>(TURN_PICTOGRAMS);
            default:                              return new ArrayList<>();
        }
    }

    public List<SocialScenario> getSocialScenarios() {
        return new ArrayList<>(SOCIAL_SCENARIOS);
    }
}
