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

        // ── Batería de Escenarios Sociales (15+) ──────────────────────────────
        SOCIAL_SCENARIOS.add(new SocialScenario("social_greet",
                "Ves a un amigo en el parque. ¿Qué haces?",
                "Le saludas y sonríes", "Miras hacia otro lado",
                "Tu amigo se alegra de verte", "Tu amigo sigue jugando solo", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_share",
                "Tienes un juguete y otro niño quiere jugar. ¿Qué haces?",
                "Le dejas el juguete un rato", "Sigues jugando tú solo",
                "Los dos jugáis juntos y os divertís", "El otro niño busca otro juguete", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_help",
                "Un compañero se ha caído. ¿Qué haces?",
                "Le preguntas si está bien", "Sigues caminando",
                "Tu compañero te da las gracias", "Tu compañero se levanta solo", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_wait",
                "Hay cola para el tobogán. ¿Qué haces?",
                "Esperas tu turno", "Te cuelas delante",
                "Todos esperan y llega tu turno", "Los demás niños se enfadan", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_thanks",
                "La profesora te da un regalo. ¿Qué haces?",
                "Le das las gracias", "Lo coges sin decir nada",
                "La profesora sonríe contenta", "La profesora se queda un poco triste", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_sorry",
                "Sin querer tiras el vaso de agua de un compañero. ¿Qué haces?",
                "Le pides perdón y le ayudas a limpiar", "Te vas corriendo",
                "Tu compañero dice que no pasa nada", "Tu compañero se enfada contigo", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_listen",
                "La profesora está explicando algo importante. ¿Qué haces?",
                "Escuchas con atención", "Hablas con tu compañero de al lado",
                "Entiendes bien la actividad", "No sabes qué hacer después", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_join",
                "Unos niños están jugando al balón. ¿Qué haces?",
                "Les preguntas si puedes jugar", "Les quitas el balón",
                "Te dicen que sí y juegas con ellos", "Se enfadan y no quieren jugar contigo", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_angry",
                "Estás enfadado porque perdiste en un juego. ¿Qué haces?",
                "Respiras hondo y dices que no pasa nada", "Tiras las piezas del juego",
                "Te sientes mejor y podéis jugar otra vez", "Los demás ya no quieren jugar contigo", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_door",
                "Llegas a la puerta al mismo tiempo que otra persona. ¿Qué haces?",
                "Le dejas pasar primero", "Empujas para pasar tú antes",
                "La otra persona te da las gracias", "La otra persona se molesta", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_noise",
                "Estás en la biblioteca y quieres hablar. ¿Qué haces?",
                "Hablas bajito o esperas a salir", "Hablas fuerte como siempre",
                "Todos pueden leer tranquilos", "Las demás personas se molestan", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_gift",
                "Un amigo te regala un dibujo que ha hecho. ¿Qué haces?",
                "Le dices que te gusta mucho", "Lo tiras a la basura",
                "Tu amigo se siente muy feliz", "Tu amigo se pone triste", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_new_kid",
                "Hay un niño nuevo en clase que no conoce a nadie. ¿Qué haces?",
                "Te acercas y le invitas a jugar", "Le ignoras",
                "El niño nuevo se siente bienvenido", "El niño nuevo se siente solo", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_interrupt",
                "Mamá está hablando por teléfono y quieres decirle algo. ¿Qué haces?",
                "Esperas a que termine", "Le interrumpes gritando",
                "Mamá te escucha después y te hace caso", "Mamá se enfada porque estaba ocupada", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_borrow",
                "Quieres usar los colores de un compañero. ¿Qué haces?",
                "Le preguntas si te los puede dejar", "Los coges sin decir nada",
                "Tu compañero te dice que sí", "Tu compañero se enfada porque no le has preguntado", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_sad_friend",
                "Tu amigo está llorando. ¿Qué haces?",
                "Le preguntas qué le pasa", "Te ríes de él",
                "Tu amigo se siente mejor", "Tu amigo se siente peor", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_clean",
                "Habéis terminado de jugar y hay juguetes por el suelo. ¿Qué haces?",
                "Ayudas a recoger", "Te vas sin recoger nada",
                "La clase queda ordenada y todos contentos", "La profesora se enfada", "A"));

        SOCIAL_SCENARIOS.add(new SocialScenario("social_goodbye",
                "Te vas de casa de un amigo después de jugar. ¿Qué haces?",
                "Dices adiós y gracias por invitarme", "Te vas sin decir nada",
                "Tu amigo y su familia se alegran", "Tu amigo no sabe si te lo has pasado bien", "A"));

        // Pictogramas de turno
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
