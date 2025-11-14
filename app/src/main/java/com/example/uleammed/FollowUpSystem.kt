package com.example.uleammed

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

// ============================================
// MODELOS DE NOTIFICACIONES Y SEGUIMIENTO
// ============================================

/**
 * Estado de cada cuestionario para el usuario
 */
data class QuestionnaireStatus(
    val type: String = "", // Usamos String para Firestore
    val hasCompletedInitial: Boolean = false,
    val lastCompletedDate: Long = 0L, // Timestamp en milisegundos
    val totalCompletions: Int = 0,
    val followUpEnabled: Boolean = true,
    val followUpIntervalDays: Int = 7
) {
    fun getQuestionnaireType(): QuestionnaireType? {
        return try {
            QuestionnaireType.valueOf(type)
        } catch (e: Exception) {
            null
        }
    }

    fun getLastCompletedDateTime(): LocalDateTime? {
        return if (lastCompletedDate > 0) {
            LocalDateTime.ofEpochSecond(
                lastCompletedDate / 1000,
                0,
                java.time.ZoneOffset.UTC
            )
        } else null
    }
}

/**
 * Notificación de seguimiento pendiente
 */
data class FollowUpNotification(
    val id: String = System.currentTimeMillis().toString(),
    val questionnaireType: QuestionnaireType,
    val title: String,
    val message: String,
    val dueDate: LocalDateTime,
    val priority: NotificationPriority,
    val isOverdue: Boolean = false,
    val daysSinceLastCompletion: Int,
    val route: String
)

enum class NotificationPriority {
    HIGH,    // Más de 14 días sin responder
    MEDIUM,  // Entre 7-14 días sin responder
    LOW      // Menos de 7 días
}

/**
 * Configuración del usuario para seguimientos
 */
data class FollowUpConfig(
    val userId: String = "",
    val globalIntervalDays: Int = 7,
    val enabledQuestionnaires: Map<String, Boolean> = mapOf(
        QuestionnaireType.ERGONOMIA.name to true,
        QuestionnaireType.SINTOMAS_MUSCULARES.name to true,
        QuestionnaireType.SINTOMAS_VISUALES.name to true,
        QuestionnaireType.CARGA_TRABAJO.name to true,
        QuestionnaireType.ESTRES_SALUD_MENTAL.name to true,
        QuestionnaireType.HABITOS_SUENO.name to true,
        QuestionnaireType.ACTIVIDAD_FISICA.name to true,
        QuestionnaireType.BALANCE_VIDA_TRABAJO.name to true
    ),
    val customIntervals: Map<String, Int> = emptyMap(),
    val notificationsEnabled: Boolean = true
)

// ============================================
// GESTOR DE NOTIFICACIONES DE SEGUIMIENTO
// ============================================

class FollowUpNotificationManager(
    private val repository: AuthRepository
) {

    /**
     * Obtiene todas las notificaciones pendientes para el usuario
     */
    suspend fun getPendingNotifications(userId: String): List<FollowUpNotification> {
        val config = repository.getFollowUpConfig(userId).getOrNull()
            ?: FollowUpConfig(userId = userId)

        if (!config.notificationsEnabled) {
            return emptyList()
        }

        val statuses = repository.getAllQuestionnaireStatuses(userId).getOrNull()
            ?: emptyList()

        val notifications = mutableListOf<FollowUpNotification>()
        val now = LocalDateTime.now()

        statuses.forEach { status ->
            val type = status.getQuestionnaireType() ?: return@forEach

            if (status.hasCompletedInitial &&
                status.followUpEnabled &&
                config.enabledQuestionnaires[type.name] == true) {

                val interval = config.customIntervals[type.name]
                    ?: config.globalIntervalDays

                val lastDate = status.getLastCompletedDateTime() ?: return@forEach
                val daysSince = ChronoUnit.DAYS.between(lastDate, now).toInt()

                if (daysSince >= interval) {
                    notifications.add(
                        createNotification(
                            type = type,
                            daysSince = daysSince,
                            interval = interval,
                            now = now
                        )
                    )
                }
            }
        }

        return notifications.sortedByDescending { it.priority }
    }

    /**
     * Obtiene el próximo seguimiento programado
     */
    suspend fun getNextScheduledFollowUp(userId: String): FollowUpNotification? {
        val config = repository.getFollowUpConfig(userId).getOrNull()
            ?: return null

        val statuses = repository.getAllQuestionnaireStatuses(userId).getOrNull()
            ?: return null

        val now = LocalDateTime.now()

        return statuses
            .mapNotNull { status ->
                val type = status.getQuestionnaireType() ?: return@mapNotNull null

                if (!status.hasCompletedInitial ||
                    !status.followUpEnabled ||
                    config.enabledQuestionnaires[type.name] != true) {
                    return@mapNotNull null
                }

                val interval = config.customIntervals[type.name]
                    ?: config.globalIntervalDays
                val lastDate = status.getLastCompletedDateTime() ?: return@mapNotNull null
                val dueDate = lastDate.plusDays(interval.toLong())

                if (dueDate.isAfter(now)) {
                    FollowUpNotification(
                        questionnaireType = type,
                        title = getQuestionnaireTitle(type),
                        message = "Próximo seguimiento programado",
                        dueDate = dueDate,
                        priority = NotificationPriority.LOW,
                        daysSinceLastCompletion = ChronoUnit.DAYS.between(lastDate, now).toInt(),
                        route = getQuestionnaireRoute(type)
                    )
                } else null
            }
            .minByOrNull { it.dueDate }
    }

    /**
     * Marca un cuestionario como completado
     */
    suspend fun markQuestionnaireCompleted(
        userId: String,
        type: QuestionnaireType,
        isInitial: Boolean = false
    ): Result<Unit> {
        val currentStatus = repository.getQuestionnaireStatus(userId, type).getOrNull()

        val updatedStatus = if (currentStatus == null) {
            QuestionnaireStatus(
                type = type.name,
                hasCompletedInitial = true,
                lastCompletedDate = System.currentTimeMillis(),
                totalCompletions = 1
            )
        } else {
            currentStatus.copy(
                hasCompletedInitial = currentStatus.hasCompletedInitial || isInitial,
                lastCompletedDate = System.currentTimeMillis(),
                totalCompletions = currentStatus.totalCompletions + 1
            )
        }

        return repository.saveQuestionnaireStatus(userId, updatedStatus)
    }

    /**
     * Verifica si el usuario puede responder un seguimiento
     */
    suspend fun canAccessFollowUp(
        userId: String,
        type: QuestionnaireType
    ): FollowUpAccessResult {
        val status = repository.getQuestionnaireStatus(userId, type).getOrNull()

        return when {
            status == null || !status.hasCompletedInitial -> {
                FollowUpAccessResult.MustCompleteInitial(
                    message = "Primero debes completar el cuestionario inicial"
                )
            }
            status.lastCompletedDate == 0L -> {
                FollowUpAccessResult.MustCompleteInitial(
                    message = "Primero debes completar el cuestionario inicial"
                )
            }
            else -> {
                val config = repository.getFollowUpConfig(userId).getOrNull()
                val interval = config?.customIntervals?.get(type.name)
                    ?: config?.globalIntervalDays
                    ?: 7

                val lastDate = status.getLastCompletedDateTime() ?: return FollowUpAccessResult.MustCompleteInitial("")
                val daysSince = ChronoUnit.DAYS.between(lastDate, LocalDateTime.now()).toInt()

                if (daysSince >= interval) {
                    FollowUpAccessResult.Allowed(
                        daysSinceLastCompletion = daysSince,
                        lastCompletionDate = lastDate
                    )
                } else {
                    FollowUpAccessResult.TooSoon(
                        daysRemaining = interval - daysSince,
                        nextAvailableDate = lastDate.plusDays(interval.toLong())
                    )
                }
            }
        }
    }

    private fun createNotification(
        type: QuestionnaireType,
        daysSince: Int,
        interval: Int,
        now: LocalDateTime
    ): FollowUpNotification {
        val priority = when {
            daysSince >= 14 -> NotificationPriority.HIGH
            daysSince >= interval -> NotificationPriority.MEDIUM
            else -> NotificationPriority.LOW
        }

        val isOverdue = daysSince > interval

        val message = when {
            isOverdue -> "Han pasado $daysSince días desde tu última evaluación. ¡Es momento de actualizar!"
            else -> "Tu seguimiento está disponible. Última evaluación hace $daysSince días."
        }

        return FollowUpNotification(
            questionnaireType = type,
            title = "Seguimiento: ${getQuestionnaireTitle(type)}",
            message = message,
            dueDate = now,
            priority = priority,
            isOverdue = isOverdue,
            daysSinceLastCompletion = daysSince,
            route = getQuestionnaireRoute(type)
        )
    }

    private fun getQuestionnaireTitle(type: QuestionnaireType): String = when (type) {
        QuestionnaireType.ERGONOMIA -> "Ergonomía"
        QuestionnaireType.SINTOMAS_MUSCULARES -> "Síntomas Musculares"
        QuestionnaireType.SINTOMAS_VISUALES -> "Síntomas Visuales"
        QuestionnaireType.CARGA_TRABAJO -> "Carga de Trabajo"
        QuestionnaireType.ESTRES_SALUD_MENTAL -> "Estrés y Salud Mental"
        QuestionnaireType.HABITOS_SUENO -> "Hábitos de Sueño"
        QuestionnaireType.ACTIVIDAD_FISICA -> "Actividad Física"
        QuestionnaireType.BALANCE_VIDA_TRABAJO -> "Balance Vida-Trabajo"
    }

    private fun getQuestionnaireRoute(type: QuestionnaireType): String = when (type) {
        QuestionnaireType.ERGONOMIA -> Screen.ErgonomiaQuestionnaire.route
        QuestionnaireType.SINTOMAS_MUSCULARES -> Screen.SintomasMuscularesQuestionnaire.route
        QuestionnaireType.SINTOMAS_VISUALES -> Screen.SintomasVisualesQuestionnaire.route
        QuestionnaireType.CARGA_TRABAJO -> Screen.CargaTrabajoQuestionnaire.route
        QuestionnaireType.ESTRES_SALUD_MENTAL -> Screen.EstresSaludMentalQuestionnaire.route
        QuestionnaireType.HABITOS_SUENO -> Screen.HabitosSuenoQuestionnaire.route
        QuestionnaireType.ACTIVIDAD_FISICA -> Screen.ActividadFisicaQuestionnaire.route
        QuestionnaireType.BALANCE_VIDA_TRABAJO -> Screen.BalanceVidaTrabajoQuestionnaire.route
    }
}

/**
 * Resultado de verificar acceso a seguimiento
 */
sealed class FollowUpAccessResult {
    data class Allowed(
        val daysSinceLastCompletion: Int,
        val lastCompletionDate: LocalDateTime
    ) : FollowUpAccessResult()

    data class TooSoon(
        val daysRemaining: Int,
        val nextAvailableDate: LocalDateTime
    ) : FollowUpAccessResult()

    data class MustCompleteInitial(
        val message: String
    ) : FollowUpAccessResult()
}

/**
 * Tracker simplificado para usar en ViewModels
 */
class QuestionnaireTracker(
    private val repository: AuthRepository
) {
    private val notificationManager = FollowUpNotificationManager(repository)

    suspend fun onQuestionnaireCompleted(
        userId: String,
        type: QuestionnaireType,
        isInitial: Boolean = false
    ): Result<Unit> {
        return try {
            notificationManager.markQuestionnaireCompleted(
                userId = userId,
                type = type,
                isInitial = isInitial
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun canAccessFollowUp(
        userId: String,
        type: QuestionnaireType
    ): FollowUpAccessResult {
        return notificationManager.canAccessFollowUp(userId, type)
    }
}

// ============================================
// EXTENSIONES DEL REPOSITORIO
// ============================================

suspend fun AuthRepository.getQuestionnaireStatus(
    userId: String,
    type: QuestionnaireType
): Result<QuestionnaireStatus?> {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        val doc = firestore.collection("users")
            .document(userId)
            .collection("questionnaire_statuses")
            .document(type.name)
            .get()
            .await()

        val status = doc.toObject(QuestionnaireStatus::class.java)
        Result.success(status)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun AuthRepository.getAllQuestionnaireStatuses(
    userId: String
): Result<List<QuestionnaireStatus>> {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        val docs = firestore.collection("users")
            .document(userId)
            .collection("questionnaire_statuses")
            .get()
            .await()

        val statuses = docs.documents.mapNotNull {
            it.toObject(QuestionnaireStatus::class.java)
        }
        Result.success(statuses)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun AuthRepository.saveQuestionnaireStatus(
    userId: String,
    status: QuestionnaireStatus
): Result<Unit> {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .document(userId)
            .collection("questionnaire_statuses")
            .document(status.type)
            .set(status)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun AuthRepository.getFollowUpConfig(
    userId: String
): Result<FollowUpConfig> {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        val doc = firestore.collection("users")
            .document(userId)
            .collection("settings")
            .document("follow_up_config")
            .get()
            .await()

        val config = doc.toObject(FollowUpConfig::class.java)
            ?: FollowUpConfig(userId = userId)
        Result.success(config)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun AuthRepository.saveFollowUpConfig(
    config: FollowUpConfig
): Result<Unit> {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .document(config.userId)
            .collection("settings")
            .document("follow_up_config")
            .set(config)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}