# AppTerapeuta - Contexto Específico

## Tecnología
- Android (Java), Gradle, compileSdk 36, minSdk 26.
- Room para persistencia local.
- HiveMQ MQTT client (implementado, coexiste con TCP).
- Gson para serialización.
- AndroidX ViewModel/LiveData.

## Arquitectura
- MVVM con ViewModels compartidos en Application class.
- TCP client que se conecta a AppRobot (puerto 9000).
- NSD para descubrir robots en red local.
- MultiRobotConnectionManager: gestiona N conexiones TCP simultáneas.
- Auto-reconnect con 2s de delay.
- SessionOrchestrator para coordinación MQTT (no activo en flujo principal).
- Repository pattern con Room DB.

## Funcionalidades principales
- Dashboard con estado en tiempo real de todos los robots.
- Configuración de sesiones: selección de actividad, contenido, asignación alumno-robot.
- Sesión en vivo: pausa/reanuda individual o global, momento calma, parada de emergencia.
- Gestión de turnos entre robots.
- Envío de feedback textual a pantalla del robot.
- Historial de sesiones con resultados por alumno.
- Gestión de perfiles de alumnos (preferencias de accesibilidad).
- Sistema de login con roles (root/no-root).
- Gestión de terapeutas.

## Entidades Room
- SessionRecordEntity: registro de sesión completa.
- ActivityResultEntity: resultados por actividad.
- AlumnResultEntity: métricas por alumno (intentos, aciertos, tiempo medio).
- IncidentEntity: incidencias con motivo y timestamp.
- StudentProfileEntity: perfiles de alumnos.
- TherapistEntity: terapeutas registrados.
