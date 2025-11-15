package com.example.uleammed

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

// ============================================
// MODELOS DE RIESGO Y PREDICCIÓN
// ============================================

/**
 * Perfil de riesgo completo del usuario
 */
data class HealthRiskProfile(
    val userId: String = "",
    val overallRiskLevel: RiskLevel = RiskLevel.UNKNOWN,
    val overallScore: Int = 0, // 0-100

    // Scores por categoría
    val musculoskeletalScore: CategoryScore? = null,
    val visualScore: CategoryScore? = null,
    val burnoutScore: CategoryScore? = null,
    val psychosocialScore: CategoryScore? = null,
    val sleepScore: CategoryScore? = null,
    val lifestyleScore: CategoryScore? = null,
    val balanceScore: CategoryScore? = null,
    val ergonomicScore: CategoryScore? = null,

    // Riesgos identificados
    val criticalRisks: List<HealthRisk> = emptyList(),
    val highRisks: List<HealthRisk> = emptyList(),
    val moderateRisks: List<HealthRisk> = emptyList(),

    // Recomendaciones
    val recommendations: List<Recommendation> = emptyList(),
    val urgentActions: List<UrgentAction> = emptyList(),

    // Métricas generales
    val bmi: Float = 0f,
    val bmiCategory: String = "",
    val hasChronicConditions: Boolean = false,
    val cardiovascularRisk: RiskLevel = RiskLevel.LOW,
    val metabolicRisk: RiskLevel = RiskLevel.LOW,

    val lastAnalysisDate: Long = System.currentTimeMillis(),
    val completedQuestionnaires: Int = 0,
    val totalQuestionnaires: Int = 9
)

/**
 * Score por categoría
 */
data class CategoryScore(
    val category: String,
    val score: Int, // 0-100
    val riskLevel: RiskLevel,
    val mainIssues: List<String> = emptyList(),
    val improvementAreas: List<String> = emptyList()
)

/**
 * Riesgo de salud identificado
 */
data class HealthRisk(
    val id: String = System.currentTimeMillis().toString(),
    val condition: String,
    val description: String,
    val riskLevel: RiskLevel,
    val probability: Int, // 0-100%
    val affectedSystems: List<String> = emptyList(),
    val symptoms: List<String> = emptyList(),
    val preventiveMeasures: List<String> = emptyList()
)

/**
 * Recomendación personalizada
 */
data class Recommendation(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val description: String,
    val category: RecommendationCategory,
    val priority: Int, // 1=máxima, 5=mínima
    val icon: String = "💡",
    val actionable: Boolean = true
)

/**
 * Acción urgente requerida
 */
data class UrgentAction(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val description: String,
    val deadline: String, // "Inmediato", "7 días", etc.
    val icon: String = "🚨",
    val actionType: ActionType
)

/**
 * Niveles de riesgo
 */
enum class RiskLevel {
    UNKNOWN,
    LOW,       // Verde - Todo bien
    MODERATE,  // Amarillo - Precaución
    HIGH,      // Naranja - Atención
    CRITICAL   // Rojo - Acción inmediata
}

/**
 * Categorías de recomendación
 */
enum class RecommendationCategory {
    ERGONOMIC,
    EXERCISE,
    NUTRITION,
    MENTAL_HEALTH,
    SLEEP,
    MEDICAL,
    WORK_LIFE_BALANCE,
    VISUAL_HEALTH
}

/**
 * Tipos de acción urgente
 */
enum class ActionType {
    MEDICAL_CONSULTATION,
    ERGONOMIC_ADJUSTMENT,
    LIFESTYLE_CHANGE,
    PSYCHOLOGICAL_SUPPORT,
    IMMEDIATE_REST
}

// ============================================
// MOTOR DE PREDICCIÓN
// ============================================

class HealthPredictionEngine {

    /**
     * Analiza todos los cuestionarios y genera perfil de riesgo
     */
    fun analyzeUserHealth(
        initial: HealthQuestionnaire?,
        ergonomia: ErgonomiaQuestionnaire?,
        sintomasMusculares: SintomasMuscularesQuestionnaire?,
        sintomasVisuales: SintomasVisualesQuestionnaire?,
        cargaTrabajo: CargaTrabajoQuestionnaire?,
        estres: EstresSaludMentalQuestionnaire?,
        sueno: HabitosSuenoQuestionnaire?,
        actividadFisica: ActividadFisicaQuestionnaire?,
        balance: BalanceVidaTrabajoQuestionnaire?
    ): HealthRiskProfile {

        if (initial == null) {
            return HealthRiskProfile(overallRiskLevel = RiskLevel.UNKNOWN)
        }

        val completedCount = listOfNotNull(
            initial, ergonomia, sintomasMusculares, sintomasVisuales,
            cargaTrabajo, estres, sueno, actividadFisica, balance
        ).size

        // Análisis del cuestionario inicial
        val bmiRisk = analyzeBMI(initial.bmi, initial.bmiCategory)
        val cardiovascularRisk = analyzeCardiovascularRisk(initial)
        val metabolicRisk = analyzeMetabolicRisk(initial)

        // Análisis de cuestionarios específicos
        val ergonomicScore = ergonomia?.let { analyzeErgonomics(it) }
        val musculoskeletalScore = sintomasMusculares?.let { analyzeMuscularSymptoms(it, ergonomia) }
        val visualScore = sintomasVisuales?.let { analyzeVisualSymptoms(it, ergonomia) }
        val burnoutScore = estres?.let { analyzeBurnout(it, cargaTrabajo, balance) }
        val psychosocialScore = cargaTrabajo?.let { analyzePsychosocial(it) }
        val sleepScore = sueno?.let { analyzeSleep(it, estres) }
        val lifestyleScore = actividadFisica?.let { analyzeLifestyle(it, initial) }
        val balanceScore = balance?.let { analyzeWorkLifeBalance(it) }

        // Identificar riesgos
        val allRisks = identifyHealthRisks(
            initial, ergonomia, sintomasMusculares, sintomasVisuales,
            cargaTrabajo, estres, sueno, actividadFisica, balance
        )

        val criticalRisks = allRisks.filter { it.riskLevel == RiskLevel.CRITICAL }
        val highRisks = allRisks.filter { it.riskLevel == RiskLevel.HIGH }
        val moderateRisks = allRisks.filter { it.riskLevel == RiskLevel.MODERATE }

        // Generar recomendaciones
        val recommendations = generateRecommendations(
            initial, ergonomicScore, musculoskeletalScore, visualScore,
            burnoutScore, psychosocialScore, sleepScore, lifestyleScore, balanceScore
        )

        // Generar acciones urgentes
        val urgentActions = generateUrgentActions(criticalRisks, highRisks)

        // Calcular score general
        val scores = listOfNotNull(
            ergonomicScore?.score,
            musculoskeletalScore?.score,
            visualScore?.score,
            burnoutScore?.score,
            psychosocialScore?.score,
            sleepScore?.score,
            lifestyleScore?.score,
            balanceScore?.score
        )

        val overallScore = if (scores.isNotEmpty()) {
            100 - (scores.average().roundToInt()) // Invertir: menor score = mejor salud
        } else 50

        val overallRiskLevel = when {
            criticalRisks.isNotEmpty() -> RiskLevel.CRITICAL
            highRisks.size >= 3 -> RiskLevel.HIGH
            highRisks.isNotEmpty() -> RiskLevel.MODERATE
            overallScore >= 70 -> RiskLevel.LOW
            overallScore >= 50 -> RiskLevel.MODERATE
            overallScore >= 30 -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }

        return HealthRiskProfile(
            userId = initial.userId,
            overallRiskLevel = overallRiskLevel,
            overallScore = overallScore.coerceIn(0, 100),
            musculoskeletalScore = musculoskeletalScore,
            visualScore = visualScore,
            burnoutScore = burnoutScore,
            psychosocialScore = psychosocialScore,
            sleepScore = sleepScore,
            lifestyleScore = lifestyleScore,
            balanceScore = balanceScore,
            ergonomicScore = ergonomicScore,
            criticalRisks = criticalRisks,
            highRisks = highRisks,
            moderateRisks = moderateRisks,
            recommendations = recommendations.take(8),
            urgentActions = urgentActions,
            bmi = initial.bmi,
            bmiCategory = initial.bmiCategory,
            hasChronicConditions = initial.preexistingConditions.isNotEmpty() &&
                    !initial.preexistingConditions.contains("Ninguna"),
            cardiovascularRisk = cardiovascularRisk,
            metabolicRisk = metabolicRisk,
            completedQuestionnaires = completedCount,
            totalQuestionnaires = 9
        )
    }

    // ============================================
    // ANÁLISIS POR CATEGORÍA
    // ============================================

    private fun analyzeErgonomics(q: ErgonomiaQuestionnaire): CategoryScore {
        var score = 0
        val issues = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        // Silla (0-20 puntos)
        when (q.tipoSilla) {
            "Silla inadecuada (comedor, cocina, etc.)" -> {
                score += 20
                issues.add("Silla inadecuada para trabajo")
                improvements.add("Adquirir silla ergonómica ajustable")
            }
            "Silla básica sin ajustes" -> {
                score += 15
                issues.add("Silla sin ajustes")
            }
            "Silla con ajuste de altura solamente" -> score += 10
            "Silla con ajuste de altura y respaldo" -> score += 5
        }

        if (q.soporteLumbar == "No tiene") {
            score += 10
            issues.add("Sin soporte lumbar")
            improvements.add("Agregar cojín lumbar")
        }

        // Monitor (0-20 puntos)
        if (q.alturaMonitor.contains("Por encima") || q.alturaMonitor.contains("Más de 15cm por debajo")) {
            score += 15
            issues.add("Monitor mal posicionado")
            improvements.add("Ajustar altura del monitor a nivel de ojos")
        }

        if (q.distanciaMonitor.contains("Menos de 50 cm")) {
            score += 10
            issues.add("Monitor muy cerca")
            improvements.add("Alejar monitor a 50-70cm")
        }

        // Teclado y mouse (0-15 puntos)
        if (q.posicionTeclado.contains("Por encima") || q.posicionTeclado.contains("Por debajo")) {
            score += 10
            issues.add("Teclado mal posicionado")
        }

        if (q.usaAlmohadilla == "No uso") {
            score += 5
            issues.add("Sin soporte para muñecas")
            improvements.add("Usar almohadilla ergonómica")
        }

        // Iluminación (0-15 puntos)
        if (q.reflejosPantalla == "Constantemente" || q.reflejosPantalla == "Frecuentemente") {
            score += 10
            issues.add("Reflejos en pantalla")
            improvements.add("Ajustar posición monitor o iluminación")
        }

        if (q.iluminacionPrincipal == "Insuficiente/Tenue") {
            score += 5
            issues.add("Iluminación insuficiente")
        }

        // Ambiente (0-15 puntos)
        if (q.temperatura == "Varía mucho") {
            score += 5
            issues.add("Temperatura variable")
        }
        if (q.nivelRuido == "Ruidoso" || q.nivelRuido == "Muy ruidoso") {
            score += 5
            issues.add("Nivel de ruido alto")
            improvements.add("Usar auriculares con cancelación de ruido")
        }
        if (q.ventilacion == "Mala (aire viciado)") {
            score += 5
            issues.add("Mala ventilación")
        }

        // Pausas (0-15 puntos)
        when (q.pausasActivas) {
            "Nunca/Muy rara vez" -> {
                score += 15
                issues.add("No realiza pausas activas")
                improvements.add("Implementar pausas cada 50 minutos")
            }
            "Solo cuando voy al baño" -> score += 12
            "Sí, cada 3-4 horas" -> score += 8
            "Sí, cada 1-2 horas" -> score += 4
        }

        if (q.tiempoSentadoContinuo == "Más de 3 horas") {
            score += 10
            issues.add("Tiempo sentado excesivo")
            improvements.add("Levantarse cada hora")
        }

        val riskLevel = when {
            score >= 70 -> RiskLevel.CRITICAL
            score >= 50 -> RiskLevel.HIGH
            score >= 30 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        return CategoryScore(
            category = "Ergonomía",
            score = score.coerceIn(0, 100),
            riskLevel = riskLevel,
            mainIssues = issues.take(3),
            improvementAreas = improvements.take(3)
        )
    }

    private fun analyzeMuscularSymptoms(
        q: SintomasMuscularesQuestionnaire,
        ergonomia: ErgonomiaQuestionnaire?
    ): CategoryScore {
        var score = 0
        val issues = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        // Cuello (0-20 puntos)
        val cuelloScore = (q.dolorCuelloFrecuencia * 2) + (q.dolorCuelloIntensidad * 2)
        score += cuelloScore
        if (cuelloScore >= 15) {
            issues.add("Dolor cervical significativo")
            improvements.add("Ejercicios de estiramiento cervical")
        }

        val rigidezCuelloScore = (q.rigidezCuelloFrecuencia * 2) + (q.rigidezCuelloIntensidad * 2)
        score += rigidezCuelloScore
        if (rigidezCuelloScore >= 15) {
            issues.add("Rigidez cervical frecuente")
        }

        // Hombros (0-15 puntos)
        val hombrosScore = (q.dolorHombrosFrecuencia * 1.5).toInt() + (q.dolorHombrosIntensidad * 1.5).toInt()
        score += hombrosScore
        if (hombrosScore >= 12) {
            issues.add("Dolor en hombros")
            improvements.add("Ajustar altura de escritorio y monitor")
        }

        // Espalda (0-25 puntos)
        val espaldaAltaScore = (q.dolorEspaldaAltaFrecuencia * 1.5).toInt() + (q.dolorEspaldaAltaIntensidad * 1.5).toInt()
        val espaldaBajaScore = (q.dolorEspaldaBajaFrecuencia * 2) + (q.dolorEspaldaBajaIntensidad * 2)
        score += espaldaAltaScore + espaldaBajaScore

        if (espaldaBajaScore >= 15) {
            issues.add("Lumbalgia significativa")
            improvements.add("Mejorar soporte lumbar y postura")
        }

        // Muñecas y manos (0-20 puntos) - TÚNEL CARPIANO
        val munecasScore = (q.dolorMunecasFrecuencia * 2) + (q.dolorMunecasIntensidad * 2)
        score += munecasScore

        if (q.hormigueoManosFrecuencia >= 3 && q.hormigueoPorNoche.contains("Sí")) {
            score += 15 // FUERTE PREDICTOR DE TÚNEL CARPIANO
            issues.add("⚠️ Riesgo alto túnel carpiano")
            improvements.add("🚨 Consultar médico - Posible túnel carpiano")
        } else if (munecasScore >= 12) {
            issues.add("Dolor en muñecas")
            improvements.add("Usar almohadilla ergonómica")
        }

        // Impacto funcional (0-10 puntos)
        if (q.dolorImpidenActividades == "Sí, frecuentemente") {
            score += 10
            issues.add("Dolores limitan actividades")
        } else if (q.dolorImpidenActividades == "Sí, ocasionalmente") {
            score += 5
        }

        val riskLevel = when {
            score >= 70 -> RiskLevel.CRITICAL
            score >= 50 -> RiskLevel.HIGH
            score >= 30 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        return CategoryScore(
            category = "Síntomas Musculares",
            score = score.coerceIn(0, 100),
            riskLevel = riskLevel,
            mainIssues = issues.take(4),
            improvementAreas = improvements.take(3)
        )
    }

    private fun analyzeVisualSymptoms(
        q: SintomasVisualesQuestionnaire,
        ergonomia: ErgonomiaQuestionnaire?
    ): CategoryScore {
        var score = 0
        val issues = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        // Ojo seco (0-20 puntos)
        val ojoSecoScore = (q.ojosSecosFrecuencia * 2) + (q.ojosSecosIntensidad * 2)
        score += ojoSecoScore
        if (ojoSecoScore >= 15) {
            issues.add("Síndrome ojo seco")
            improvements.add("Usar lágrimas artificiales")
        }

        // Ardor (0-15 puntos)
        val ardorScore = (q.ardorOjosFrecuencia * 1.5).toInt() + (q.ardorOjosIntensidad * 1.5).toInt()
        score += ardorScore

        // Ojos rojos (0-10 puntos)
        score += q.ojosRojosFrecuencia * 2
        if (q.ojosRojosFrecuencia >= 4) {
            issues.add("Irritación ocular frecuente")
        }

        // Visión borrosa (0-15 puntos)
        score += q.visionBorrosaFrecuencia * 3
        if (q.visionBorrosaFrecuencia >= 4) {
            issues.add("Visión borrosa frecuente")
            improvements.add("Examen oftalmológico urgente")
        }

        // Dificultad enfocar (0-15 puntos)
        score += q.dificultadEnfocarFrecuencia * 3
        if (q.dificultadEnfocarFrecuencia >= 3) {
            issues.add("Dificultad para enfocar")
        }

        // Fatiga visual (0-15 puntos)
        if (q.ojosCansadosFinDia == "Siempre" || q.ojosCansadosFinDia == "Frecuentemente") {
            score += 15
            issues.add("Fatiga visual significativa")
            improvements.add("Aplicar regla 20-20-20")
        }

        // Cuidado visual (0-10 puntos)
        if (q.ultimoExamenVisual == "Hace más de 2 años" || q.ultimoExamenVisual == "Nunca") {
            score += 5
            improvements.add("Realizar examen visual")
        }

        if (q.aplicaRegla202020 == "No sé qué es/Nunca" || q.aplicaRegla202020 == "Rara vez") {
            score += 5
            improvements.add("Implementar pausas visuales (20-20-20)")
        }

        val riskLevel = when {
            score >= 70 -> RiskLevel.CRITICAL
            score >= 50 -> RiskLevel.HIGH
            score >= 30 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        return CategoryScore(
            category = "Salud Visual",
            score = score.coerceIn(0, 100),
            riskLevel = riskLevel,
            mainIssues = issues.take(3),
            improvementAreas = improvements.take(3)
        )
    }

    private fun analyzeBurnout(
        estres: EstresSaludMentalQuestionnaire,
        cargaTrabajo: CargaTrabajoQuestionnaire?,
        balance: BalanceVidaTrabajoQuestionnaire?
    ): CategoryScore {
        var score = 0
        val issues = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        // Nivel de estrés general (0-20 puntos)
        score += estres.nivelEstresGeneral * 2
        if (estres.nivelEstresGeneral >= 8) {
            issues.add("Nivel de estrés muy alto")
            improvements.add("Consultar psicólogo organizacional")
        }

        // Agotamiento emocional (0-25 puntos)
        val agotamiento = (estres.fatigaAgotamiento + estres.sensacionAbrumado +
                estres.dificultadDesconectar) * 2
        score += agotamiento
        if (agotamiento >= 20) {
            issues.add("⚠️ Agotamiento emocional severo")
        }

        // Despersonalización (0-20 puntos)
        val despersonalizacion = (estres.actitudNegativa + estres.sentimientoIneficacia) * 3
        score += despersonalizacion
        if (despersonalizacion >= 15) {
            issues.add("Síntomas de despersonalización")
        }

        // Síntomas cognitivos (0-15 puntos)
        val cognitivo = estres.dificultadConcentracion + estres.problemasMemoria
        score += cognitivo * 1.5.toInt()
        if (cognitivo >= 8) {
            issues.add("Deterioro cognitivo por estrés")
        }

        // Baja realización personal (0-10 puntos)
        val realizacion = estres.perdidaMotivacion + estres.sensacionInproductividad
        score += realizacion

        // Síndrome de Burnout completo
        val burnoutCriteria = (agotamiento >= 20) && (despersonalizacion >= 15) && (realizacion >= 8)
        if (burnoutCriteria) {
            score += 20
            issues.add("🚨 SÍNDROME DE BURNOUT DETECTADO")
            improvements.add("🚨 Intervención psicológica URGENTE")
        }

        // Considerando cambiar trabajo
        if (estres.consideraCambiarTrabajo.contains("estoy buscando") ||
            estres.consideraCambiarTrabajo.contains("decidí cambiar")) {
            score += 10
            issues.add("Contempla cambio laboral")
        }

        val riskLevel = when {
            burnoutCriteria -> RiskLevel.CRITICAL
            score >= 70 -> RiskLevel.HIGH
            score >= 50 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        return CategoryScore(
            category = "Burnout",
            score = score.coerceIn(0, 100),
            riskLevel = riskLevel,
            mainIssues = issues.take(3),
            improvementAreas = improvements.take(2)
        )
    }

    private fun analyzePsychosocial(q: CargaTrabajoQuestionnaire): CategoryScore {
        var score = 0
        val issues = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        // Demanda laboral (0-30 puntos)
        when (q.cargaTrabajoActual) {
            "Excesiva (no puedo con todo)" -> {
                score += 20
                issues.add("Carga de trabajo excesiva")
                improvements.add("Redistribuir tareas y delegar")
            }
            "Alta (requiere esfuerzo extra)" -> score += 15
            "Adecuada (equilibrada)" -> score += 0
            else -> score += 5
        }

        when (q.presionTiempoPlazos) {
            "Muy alta/Constante" -> score += 10
            "Alta" -> score += 7
            "Moderada" -> score += 3
        }

        // Horas extra (0-20 puntos)
        when (q.horasFueraHorario) {
            "Más de 12 horas" -> {
                score += 15
                issues.add("Horas extra excesivas")
            }
            "8-12 horas" -> score += 10
            "4-7 horas" -> score += 5
        }

        if (q.trabajaFinesSemana.contains("Todos") || q.trabajaFinesSemana.contains("3 fines")) {
            score += 5
            issues.add("Trabajo en fines de semana frecuente")
        }

        // Control y autonomía (0-20 puntos) - INVERSO
        when (q.puedeDecidirComoTrabajar) {
            "No, todo está muy controlado" -> {
                score += 15
                issues.add("Falta de autonomía laboral")
                improvements.add("Negociar mayor autonomía")
            }
            "Poco" -> score += 10
            "Parcialmente" -> score += 5
        }

        if (q.puedePlanificarPausas == "No puedo") {
            score += 5
            issues.add("No puede planificar pausas")
        }

        // Apoyo social (0-15 puntos) - INVERSO
        when (q.apoyoSuperior) {
            "Muy malo/Ninguno" -> {
                score += 10
                issues.add("Falta de apoyo del superior")
            }
            "Malo" -> score += 7
            "Regular" -> score += 3
        }

        if (q.relacionCompaneros == "Mala (conflictiva)") {
            score += 5
            issues.add("Relaciones conflictivas")
        }

        // Acoso laboral (0-15 puntos)
        when (q.acosoLaboral) {
            "Constantemente" -> {
                score += 15
                issues.add("🚨 ACOSO LABORAL CONSTANTE")
                improvements.add("🚨 Denunciar a RRHH inmediatamente")
            }
            "Frecuentemente" -> {
                score += 12
                issues.add("Acoso laboral frecuente")
            }
            "Ocasionalmente" -> score += 7
        }

        val riskLevel = when {
            score >= 70 -> RiskLevel.CRITICAL
            score >= 50 -> RiskLevel.HIGH
            score >= 30 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        return CategoryScore(
            category = "Factores Psicosociales",
            score = score.coerceIn(0, 100),
            riskLevel = riskLevel,
            mainIssues = issues.take(3),
            improvementAreas = improvements.take(2)
        )
    }

    private fun analyzeSleep(
        q: HabitosSuenoQuestionnaire,
        estres: EstresSaludMentalQuestionnaire?
    ): CategoryScore {
        var score = 0
        val issues = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        // Duración del sueño (0-25 puntos)
        when (q.horasSuenoSemana) {
            "Menos de 5 horas" -> {
                score += 25
                issues.add("Privación severa de sueño")
                improvements.add("Priorizar 7-8 horas de sueño")
            }
            "5-6 horas" -> {
                score += 15
                issues.add("Duración de sueño insuficiente")
            }
            "7-8 horas" -> score += 0
            "Más de 8 horas" -> score += 5 // Puede indicar hipersomnia
        }

        // Calidad del sueño (0-25 puntos)
        when (q.calidadSueno) {
            "Muy mala (nunca descansado)" -> {
                score += 25
                issues.add("Calidad de sueño muy mala")
                improvements.add("Evaluación médica del sueño")
            }
            "Mala (rara vez descansado)" -> {
                score += 20
                issues.add("Calidad de sueño mala")
            }
            "Regular (a veces descansado)" -> score += 12
            "Buena (generalmente descansado)" -> score += 5
        }

        // Problemas para dormir (0-30 puntos)
        val dificultadConciliar = q.dificultadConciliarFrecuencia * 3
        val despertaresNocturnos = q.despertaresNocturnosFrecuencia * 3
        val despertarTemprano = q.despertarTempranoFrecuencia * 3

        val problemasTotal = dificultadConciliar + despertaresNocturnos + despertarTemprano
        score += (problemasTotal / 3).coerceAtMost(30)

        if (dificultadConciliar >= 12) {
            issues.add("Insomnio de conciliación")
        }
        if (despertaresNocturnos >= 12) {
            issues.add("Insomnio de mantenimiento")
        }
        if (despertarTemprano >= 12) {
            issues.add("Despertar precoz (posible depresión)")
        }

        // Higiene del sueño (0-20 puntos)
        if (q.usaDispositivosAntesDormir.contains("hasta el momento de dormir")) {
            score += 10
            issues.add("Uso pantallas antes de dormir")
            improvements.add("Evitar pantallas 1h antes de dormir")
        }

        if (q.piensaProblemasTrabajoAntesDormir.contains("Siempre") ||
            q.piensaProblemasTrabajoAntesDormir.contains("Frecuentemente")) {
            score += 10
            issues.add("Rumiación laboral nocturna")
            improvements.add("Técnicas de relajación antes de dormir")
        }

        val riskLevel = when {
            score >= 70 -> RiskLevel.CRITICAL
            score >= 50 -> RiskLevel.HIGH
            score >= 30 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        return CategoryScore(
            category = "Sueño",
            score = score.coerceIn(0, 100),
            riskLevel = riskLevel,
            mainIssues = issues.take(3),
            improvementAreas = improvements.take(2)
        )
    }

    private fun analyzeLifestyle(
        q: ActividadFisicaQuestionnaire,
        initial: HealthQuestionnaire
    ): CategoryScore {
        var score = 0
        val issues = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        // Ejercicio (0-30 puntos)
        when (q.frecuenciaEjercicio) {
            "Ninguna (sedentario)" -> {
                score += 30
                issues.add("Sedentarismo completo")
                improvements.add("Iniciar actividad física gradual")
            }
            "1 vez por semana" -> {
                score += 20
                issues.add("Actividad física insuficiente")
            }
            "2-3 veces por semana" -> score += 10
            "4-5 veces por semana" -> score += 0
            "Diariamente" -> score += 0
        }

        when (q.duracionEjercicio) {
            "No hago ejercicio" -> score += 5
            "Menos de 20 minutos" -> {
                score += 10
                improvements.add("Aumentar duración a 30-60 min")
            }
            "20-30 minutos" -> score += 5
        }

        // Alimentación (0-25 puntos)
        when (q.frecuenciaComidasDia) {
            "Irregular/Sin horario" -> {
                score += 15
                issues.add("Horarios alimentación irregulares")
                improvements.add("Establecer horarios regulares")
            }
            "1-2 comidas" -> {
                score += 10
                issues.add("Pocas comidas al día")
            }
        }

        if (q.saltaDesayuno.contains("Siempre") || q.saltaDesayuno.contains("Frecuentemente")) {
            score += 10
            issues.add("Salta el desayuno frecuentemente")
        }

        // Hábitos laborales alimenticios (0-15 puntos)
        if (q.comeEnEscritorio.contains("Siempre") || q.comeEnEscritorio.contains("Frecuentemente")) {
            score += 5
            issues.add("Come en escritorio habitualmente")
        }

        when (q.consumoAguaDiario) {
            "Menos de 1 litro" -> {
                score += 10
                issues.add("Hidratación insuficiente")
                improvements.add("Aumentar consumo agua a 2L/día")
            }
            "1-1.5 litros" -> score += 5
        }

        // Sustancias estimulantes (0-20 puntos)
        when (q.consumoCafeTe) {
            "Más de 5 tazas al día" -> {
                score += 15
                issues.add("Consumo excesivo cafeína")
                improvements.add("Reducir cafeína gradualmente")
            }
            "4-5 tazas al día" -> score += 10
            "2-3 tazas al día" -> score += 5
        }

        when (q.consumeBebidasEnergizantes) {
            "Diariamente" -> {
                score += 10
                issues.add("Consumo diario bebidas energizantes")
                improvements.add("Eliminar bebidas energizantes")
            }
            "Frecuentemente (3+ por semana)" -> score += 7
            "Regularmente (1-2 por semana)" -> score += 3
        }

        // IMC del cuestionario inicial (0-10 puntos adicionales)
        if (initial.bmi >= 30) {
            score += 10
            issues.add("Obesidad")
        } else if (initial.bmi >= 25) {
            score += 5
            issues.add("Sobrepeso")
        }

        val riskLevel = when {
            score >= 70 -> RiskLevel.CRITICAL
            score >= 50 -> RiskLevel.HIGH
            score >= 30 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        return CategoryScore(
            category = "Estilo de Vida",
            score = score.coerceIn(0, 100),
            riskLevel = riskLevel,
            mainIssues = issues.take(3),
            improvementAreas = improvements.take(3)
        )
    }

    private fun analyzeWorkLifeBalance(q: BalanceVidaTrabajoQuestionnaire): CategoryScore {
        var score = 0
        val issues = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        // Equilibrio percibido (0-25 puntos)
        when (q.equilibrioTrabajoVida) {
            "El trabajo domina completamente mi vida" -> {
                score += 25
                issues.add("Desequilibrio severo vida-trabajo")
                improvements.add("Establecer límites laborales claros")
            }
            "Más trabajo que vida personal" -> {
                score += 20
                issues.add("Trabajo domina vida personal")
            }
            "Parcialmente equilibrado" -> score += 10
        }

        // Tiempo libre (0-20 puntos)
        when (q.tiempoLibreCalidad) {
            "Menos de 5 horas" -> {
                score += 20
                issues.add("Tiempo libre muy limitado")
                improvements.add("Aumentar tiempo libre a 10h/semana mínimo")
            }
            "5-10 horas" -> {
                score += 15
                issues.add("Tiempo libre insuficiente")
            }
            "10-15 horas" -> score += 8
        }

        // Actividades recreativas (0-15 puntos)
        when (q.actividadesRecreativas) {
            "Nunca, no tengo tiempo" -> {
                score += 15
                issues.add("Sin actividades recreativas")
                improvements.add("Programar hobbies regulares")
            }
            "Rara vez" -> score += 10
            "Ocasionalmente (1-2 veces al mes)" -> score += 5
        }

        // Impacto relaciones (0-15 puntos)
        when (q.trabajoAfectaRelaciones) {
            "Severamente" -> {
                score += 15
                issues.add("Trabajo afecta severamente relaciones")
            }
            "Bastante" -> {
                score += 12
                issues.add("Trabajo afecta relaciones personales")
            }
            "Moderadamente" -> score += 7
        }

        // Desconexión laboral (0-20 puntos)
        when (q.puedeDesconectarseDiasLibres) {
            "Nunca puedo desconectar" -> {
                score += 15
                issues.add("Incapacidad para desconectar")
                improvements.add("Activar modo 'No molestar' en días libres")
            }
            "Rara vez" -> score += 12
            "Con dificultad" -> score += 8
        }

        if (q.revisaCorreosVacaciones.contains("Constantemente") ||
            q.revisaCorreosVacaciones.contains("Frecuentemente")) {
            score += 5
            issues.add("Revisa trabajo en vacaciones")
        }

        // Vacaciones (0-5 puntos)
        when (q.ultimasVacaciones) {
            "Nunca/No recuerdo" -> {
                score += 5
                issues.add("Sin vacaciones recientes")
                improvements.add("Planificar vacaciones próximas")
            }
            "Hace más de 2 años" -> score += 3
        }

        val riskLevel = when {
            score >= 70 -> RiskLevel.CRITICAL
            score >= 50 -> RiskLevel.HIGH
            score >= 30 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        return CategoryScore(
            category = "Balance Vida-Trabajo",
            score = score.coerceIn(0, 100),
            riskLevel = riskLevel,
            mainIssues = issues.take(3),
            improvementAreas = improvements.take(2)
        )
    }

    // ============================================
    // ANÁLISIS DE RIESGOS ESPECÍFICOS
    // ============================================

    private fun analyzeBMI(bmi: Float, category: String): RiskLevel {
        return when {
            bmi >= 35 -> RiskLevel.CRITICAL // Obesidad mórbida
            bmi >= 30 -> RiskLevel.HIGH     // Obesidad
            bmi >= 25 -> RiskLevel.MODERATE // Sobrepeso
            bmi < 18.5 -> RiskLevel.MODERATE // Bajo peso
            else -> RiskLevel.LOW
        }
    }

    private fun analyzeCardiovascularRisk(q: HealthQuestionnaire): RiskLevel {
        var riskFactors = 0

        if (q.bmi >= 30) riskFactors++
        if (q.bloodPressure.contains("Hipertensión")) riskFactors++
        if (q.cholesterolLevel.contains("Alto")) riskFactors++
        if (q.smokingStatus.contains("Sí")) riskFactors++
        if (q.bloodGlucose.contains("Diabetes") || q.bloodGlucose.contains("Prediabetes")) riskFactors++
        if (q.familyHistory.contains("Enfermedades cardíacas")) riskFactors++

        return when {
            riskFactors >= 4 -> RiskLevel.CRITICAL
            riskFactors >= 3 -> RiskLevel.HIGH
            riskFactors >= 2 -> RiskLevel.MODERATE
            riskFactors >= 1 -> RiskLevel.LOW
            else -> RiskLevel.LOW
        }
    }

    private fun analyzeMetabolicRisk(q: HealthQuestionnaire): RiskLevel {
        var riskFactors = 0

        if (q.bmi >= 30) riskFactors++
        if (q.bloodGlucose.contains("Diabetes")) riskFactors += 2
        if (q.bloodGlucose.contains("Prediabetes")) riskFactors++
        if (q.cholesterolLevel.contains("Alto")) riskFactors++
        if (q.bloodPressure.contains("Hipertensión")) riskFactors++

        return when {
            riskFactors >= 4 -> RiskLevel.CRITICAL
            riskFactors >= 3 -> RiskLevel.HIGH
            riskFactors >= 2 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }
    }

    private fun identifyHealthRisks(
        initial: HealthQuestionnaire,
        ergonomia: ErgonomiaQuestionnaire?,
        sintomasMusculares: SintomasMuscularesQuestionnaire?,
        sintomasVisuales: SintomasVisualesQuestionnaire?,
        cargaTrabajo: CargaTrabajoQuestionnaire?,
        estres: EstresSaludMentalQuestionnaire?,
        sueno: HabitosSuenoQuestionnaire?,
        actividadFisica: ActividadFisicaQuestionnaire?,
        balance: BalanceVidaTrabajoQuestionnaire?
    ): List<HealthRisk> {
        val risks = mutableListOf<HealthRisk>()

        // TÚNEL CARPIANO
        if (sintomasMusculares != null) {
            if (sintomasMusculares.hormigueoManosFrecuencia >= 3 &&
                sintomasMusculares.hormigueoPorNoche.contains("Sí")) {
                risks.add(HealthRisk(
                    condition = "Síndrome del Túnel Carpiano",
                    description = "Alta probabilidad de compresión del nervio mediano",
                    riskLevel = RiskLevel.HIGH,
                    probability = 85,
                    affectedSystems = listOf("Sistema nervioso periférico", "Manos y muñecas"),
                    symptoms = listOf("Hormigueo nocturno", "Dolor muñecas", "Debilidad al agarrar"),
                    preventiveMeasures = listOf(
                        "Consultar médico urgente",
                        "Usar férula nocturna",
                        "Mejorar ergonomía de teclado/mouse",
                        "Pausas cada 30 minutos"
                    )
                ))
            }
        }

        // BURNOUT
        // BURNOUT
        if (estres != null && cargaTrabajo != null) {
            val agotamiento = estres.fatigaAgotamiento + estres.sensacionAbrumado
            val despersonalizacion = estres.actitudNegativa + estres.sentimientoIneficacia
            val bajaRealizacion = estres.perdidaMotivacion + estres.sensacionInproductividad
            // ↑ SIN ESPACIO - era "baja realizacion"

            if (agotamiento >= 8 && despersonalizacion >= 6 && bajaRealizacion >= 6) {
                risks.add(HealthRisk(
                    condition = "Síndrome de Burnout",
                    description = "Agotamiento emocional crónico relacionado con el trabajo",
                    riskLevel = RiskLevel.CRITICAL,
                    probability = 90,
                    affectedSystems = listOf("Salud mental", "Sistema nervioso", "Sistema inmune"),
                    symptoms = listOf("Agotamiento", "Despersonalización", "Cinismo", "Baja productividad"),
                    preventiveMeasures = listOf(
                        "🚨 Consulta psicológica URGENTE",
                        "Considerar tiempo sabático",
                        "Reducir carga laboral",
                        "Apoyo de RRHH"
                    )
                ))
            }
        }

        // LUMBALGIA CRÓNICA
        if (sintomasMusculares != null && ergonomia != null) {
            val dolorLumbar = (sintomasMusculares.dolorEspaldaBajaFrecuencia * 2) +
                    (sintomasMusculares.dolorEspaldaBajaIntensidad * 2)
            if (dolorLumbar >= 15 && ergonomia.soporteLumbar == "No tiene") {
                risks.add(HealthRisk(
                    condition = "Lumbalgia Crónica",
                    description = "Dolor persistente en zona lumbar por postura inadecuada",
                    riskLevel = RiskLevel.HIGH,
                    probability = 75,
                    affectedSystems = listOf("Sistema músculo-esquelético", "Columna lumbar"),
                    symptoms = listOf("Dolor espalda baja", "Rigidez matutina", "Limitación movimiento"),
                    preventiveMeasures = listOf(
                        "Mejorar soporte lumbar silla",
                        "Ejercicios fortalecimiento core",
                        "Fisioterapia",
                        "Pausas activas cada hora"
                    )
                ))
            }
        }

        // SÍNDROME VISUAL INFORMÁTICO
        if (sintomasVisuales != null && ergonomia != null) {
            val scoreVisual = (sintomasVisuales.ojosSecosFrecuencia * 2) +
                    (sintomasVisuales.visionBorrosaFrecuencia * 3) +
                    (sintomasVisuales.dificultadEnfocarFrecuencia * 3)
            if (scoreVisual >= 25) {
                risks.add(HealthRisk(
                    condition = "Síndrome Visual Informático",
                    description = "Fatiga ocular por uso prolongado de pantallas",
                    riskLevel = if (scoreVisual >= 35) RiskLevel.HIGH else RiskLevel.MODERATE,
                    probability = 80,
                    affectedSystems = listOf("Sistema visual", "Ojos"),
                    symptoms = listOf("Ojos secos", "Visión borrosa", "Fatiga ocular", "Cefalea"),
                    preventiveMeasures = listOf(
                        "Regla 20-20-20",
                        "Lágrimas artificiales",
                        "Ajustar brillo pantalla",
                        "Examen oftalmológico"
                    )
                ))
            }
        }

        // INSOMNIO CRÓNICO
        if (sueno != null && estres != null) {
            val problemaSueno = sueno.dificultadConciliarFrecuencia +
                    sueno.despertaresNocturnosFrecuencia +
                    sueno.despertarTempranoFrecuencia
            if (problemaSueno >= 12 && estres.nivelEstresGeneral >= 7) {
                risks.add(HealthRisk(
                    condition = "Trastorno de Insomnio Crónico",
                    description = "Dificultad persistente para dormir o mantener el sueño",
                    riskLevel = RiskLevel.HIGH,
                    probability = 70,
                    affectedSystems = listOf("Sistema nervioso", "Salud mental", "Sistema inmune"),
                    symptoms = listOf("Dificultad conciliar sueño", "Despertares nocturnos", "Fatiga diurna"),
                    preventiveMeasures = listOf(
                        "Higiene del sueño",
                        "Terapia cognitivo-conductual",
                        "Evitar pantallas 1h antes dormir",
                        "Consultar especialista sueño"
                    )
                ))
            }
        }

        // RIESGO CARDIOVASCULAR
        if (initial.bmi >= 30 && initial.bloodPressure.contains("Hipertensión") &&
            actividadFisica?.frecuenciaEjercicio == "Ninguna (sedentario)") {
            risks.add(HealthRisk(
                condition = "Riesgo Cardiovascular Elevado",
                description = "Múltiples factores de riesgo cardiovascular presentes",
                riskLevel = RiskLevel.HIGH,
                probability = 65,
                affectedSystems = listOf("Sistema cardiovascular", "Corazón", "Arterias"),
                symptoms = listOf("Puede ser asintomático", "Fatiga", "Palpitaciones"),
                preventiveMeasures = listOf(
                    "🚨 Evaluación cardiológica",
                    "Control presión arterial",
                    "Iniciar ejercicio supervisado",
                    "Dieta cardiosaludable",
                    "Reducir sodio"
                )
            ))
        }

        // SÍNDROME METABÓLICO
        if (initial.bmi >= 30 && initial.bloodGlucose.contains("Prediabetes") &&
            initial.bloodPressure.contains("Elevada")) {
            risks.add(HealthRisk(
                condition = "Síndrome Metabólico",
                description = "Conjunto de factores que aumentan riesgo diabetes y enfermedad cardiovascular",
                riskLevel = RiskLevel.HIGH,
                probability = 70,
                affectedSystems = listOf("Sistema metabólico", "Sistema cardiovascular"),
                symptoms = listOf("Obesidad abdominal", "Presión alta", "Glucosa elevada"),
                preventiveMeasures = listOf(
                    "Control médico regular",
                    "Pérdida de peso gradual",
                    "Ejercicio 150min/semana",
                    "Dieta mediterránea"
                )
            ))
        }

        // DEPRESIÓN LABORAL
        if (estres != null && balance != null) {
            val depresionScore = estres.perdidaMotivacion + estres.sentimientoIneficacia +
                    estres.actitudNegativa
            if (depresionScore >= 12 && balance.trabajoAfectaRelaciones.contains("Severamente")) {
                risks.add(HealthRisk(
                    condition = "Depresión Laboral",
                    description = "Estado depresivo relacionado con el entorno laboral",
                    riskLevel = RiskLevel.HIGH,
                    probability = 65,
                    affectedSystems = listOf("Salud mental", "Sistema nervioso"),
                    symptoms = listOf("Pérdida motivación", "Tristeza", "Aislamiento", "Baja autoestima"),
                    preventiveMeasures = listOf(
                        "Consulta psicológica",
                        "Terapia cognitivo-conductual",
                        "Apoyo social",
                        "Considerar cambios laborales"
                    )
                ))
            }
        }

        return risks.sortedByDescending {
            when (it.riskLevel) {
                RiskLevel.CRITICAL -> 4
                RiskLevel.HIGH -> 3
                RiskLevel.MODERATE -> 2
                RiskLevel.LOW -> 1
                else -> 0
            }
        }
    }

    // ============================================
    // GENERACIÓN DE RECOMENDACIONES
    // ============================================

    private fun generateRecommendations(
        initial: HealthQuestionnaire,
        ergonomic: CategoryScore?,
        muscular: CategoryScore?,
        visual: CategoryScore?,
        burnout: CategoryScore?,
        psychosocial: CategoryScore?,
        sleep: CategoryScore?,
        lifestyle: CategoryScore?,
        balance: CategoryScore?
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        // Recomendaciones según scores
        ergonomic?.let {
            if (it.riskLevel >= RiskLevel.MODERATE) {
                recommendations.add(Recommendation(
                    title = "Mejora tu Espacio de Trabajo",
                    description = "Ajusta tu silla, monitor y teclado según principios ergonómicos",
                    category = RecommendationCategory.ERGONOMIC,
                    priority = if (it.riskLevel == RiskLevel.CRITICAL) 1 else 2,
                    icon = "🪑"
                ))
            }
        }

        muscular?.let {
            if (it.riskLevel >= RiskLevel.MODERATE) {
                recommendations.add(Recommendation(
                    title = "Pausas Activas Cada Hora",
                    description = "Levántate y realiza estiramientos de cuello, hombros y espalda",
                    category = RecommendationCategory.EXERCISE,
                    priority = if (it.riskLevel == RiskLevel.CRITICAL) 1 else 2,
                    icon = "🧘"
                ))
            }
        }

        visual?.let {
            if (it.riskLevel >= RiskLevel.MODERATE) {
                recommendations.add(Recommendation(
                    title = "Regla 20-20-20 para tus Ojos",
                    description = "Cada 20 minutos, mira 20 pies (6m) de distancia por 20 segundos",
                    category = RecommendationCategory.VISUAL_HEALTH,
                    priority = 2,
                    icon = "👁️"
                ))
            }
        }

        burnout?.let {
            if (it.riskLevel >= RiskLevel.HIGH) {
                recommendations.add(Recommendation(
                    title = "Busca Apoyo Psicológico",
                    description = "Contacta con un psicólogo especializado en salud ocupacional",
                    category = RecommendationCategory.MENTAL_HEALTH,
                    priority = 1,
                    icon = "🧠"
                ))
            }
        }

        sleep?.let {
            if (it.riskLevel >= RiskLevel.MODERATE) {
                recommendations.add(Recommendation(
                    title = "Mejora tu Higiene del Sueño",
                    description = "Establece horarios regulares y evita pantallas antes de dormir",
                    category = RecommendationCategory.SLEEP,
                    priority = 2,
                    icon = "😴"
                ))
            }
        }

        lifestyle?.let {
            if (it.riskLevel >= RiskLevel.MODERATE) {
                recommendations.add(Recommendation(
                    title = "Incrementa tu Actividad Física",
                    description = "Realiza al menos 30 minutos de ejercicio 5 días a la semana",
                    category = RecommendationCategory.EXERCISE,
                    priority = 2,
                    icon = "💪"
                ))

                if (initial.bmi >= 25) {
                    recommendations.add(Recommendation(
                        title = "Plan Nutricional Personalizado",
                        description = "Consulta con un nutricionista para alcanzar peso saludable",
                        category = RecommendationCategory.NUTRITION,
                        priority = 2,
                        icon = "🥗"
                    ))
                }
            }
        }

        balance?.let {
            if (it.riskLevel >= RiskLevel.HIGH) {
                recommendations.add(Recommendation(
                    title = "Establece Límites Laborales",
                    description = "Define horarios claros y desconéctate en días libres",
                    category = RecommendationCategory.WORK_LIFE_BALANCE,
                    priority = 1,
                    icon = "⚖️"
                ))
            }
        }

        // Recomendaciones generales
        recommendations.add(Recommendation(
            title = "Mantén Hidratación Adecuada",
            description = "Bebe al menos 2 litros de agua durante tu jornada laboral",
            category = RecommendationCategory.NUTRITION,
            priority = 3,
            icon = "💧"
        ))

        return recommendations.sortedBy { it.priority }
    }

    private fun generateUrgentActions(
        criticalRisks: List<HealthRisk>,
        highRisks: List<HealthRisk>
    ): List<UrgentAction> {
        val actions = mutableListOf<UrgentAction>()

        criticalRisks.forEach { risk ->
            when (risk.condition) {
                "Síndrome de Burnout" -> {
                    actions.add(UrgentAction(
                        title = "Evaluación Psicológica Urgente",
                        description = "Burnout severo detectado. Programa consulta con psicólogo en las próximas 48 horas.",
                        deadline = "48 horas",
                        icon = "🚨",
                        actionType = ActionType.MEDICAL_CONSULTATION
                    ))
                }
                "Lumbalgia Crónica" -> {
                    actions.add(UrgentAction(
                        title = "Ajuste Ergonómico Inmediato",
                        description = "Dolor lumbar persistente. Mejora soporte de silla y consulta fisioterapeuta.",
                        deadline = "7 días",
                        icon = "🪑",
                        actionType = ActionType.ERGONOMIC_ADJUSTMENT
                    ))
                }
                "Trastorno de Insomnio Crónico" -> {
                    actions.add(UrgentAction(
                        title = "Especialista en Sueño",
                        description = "Problemas crónicos de sueño. Consulta con especialista para evaluación.",
                        deadline = "14 días",
                        icon = "😴",
                        actionType = ActionType.MEDICAL_CONSULTATION
                    ))
                }
                "Síndrome Visual Informático" -> {
                    actions.add(UrgentAction(
                        title = "Examen Oftalmológico",
                        description = "Fatiga visual severa. Programa examen con oftalmólogo.",
                        deadline = "14 días",
                        icon = "👁️",
                        actionType = ActionType.MEDICAL_CONSULTATION
                    ))
                }
                "Depresión Laboral" -> {
                    actions.add(UrgentAction(
                        title = "Apoyo Psicológico",
                        description = "Síntomas depresivos detectados. Busca apoyo profesional.",
                        deadline = "7 días",
                        icon = "🧠",
                        actionType = ActionType.PSYCHOLOGICAL_SUPPORT
                    ))
                }
                "Riesgo Cardiovascular Elevado" -> {
                    actions.add(UrgentAction(
                        title = "Control Médico Cardiovascular",
                        description = "Múltiples factores de riesgo. Evaluación cardiológica necesaria.",
                        deadline = "14 días",
                        icon = "❤️",
                        actionType = ActionType.MEDICAL_CONSULTATION
                    ))
                }
                "Síndrome Metabólico" -> {
                    actions.add(UrgentAction(
                        title = "Control Metabólico",
                        description = "Síndrome metabólico detectado. Consulta médico y nutricionista.",
                        deadline = "14 días",
                        icon = "🩺",
                        actionType = ActionType.MEDICAL_CONSULTATION
                    ))
                }
            }
        }

        // Acciones adicionales según contexto
        if (criticalRisks.any { it.condition.contains("Burnout") || it.condition.contains("Depresión") }) {
            if (actions.none { it.actionType == ActionType.PSYCHOLOGICAL_SUPPORT }) {
                actions.add(UrgentAction(
                    title = "Apoyo de Recursos Humanos",
                    description = "Considera hablar con RRHH sobre tu carga laboral y apoyo disponible.",
                    deadline = "7 días",
                    icon = "🤝",
                    actionType = ActionType.LIFESTYLE_CHANGE
                ))
            }
        }

        return actions.take(3) // Máximo 3 acciones urgentes para no abrumar
    }
}

// ============================================
// EXTENSIONES Y UTILIDADES
// ============================================

/**
 * Obtiene el color según nivel de riesgo
 */
fun RiskLevel.toColor(): Color {
    return when (this) {
        RiskLevel.CRITICAL -> Color(0xFFD32F2F) // Rojo
        RiskLevel.HIGH -> Color(0xFFFF9800)     // Naranja
        RiskLevel.MODERATE -> Color(0xFFFFC107) // Amarillo
        RiskLevel.LOW -> Color(0xFF4CAF50)      // Verde
        else -> Color(0xFF9E9E9E)               // Gris
    }
}

/**
 * Obtiene el icono según nivel de riesgo
 */
fun RiskLevel.toIcon(): String {
    return when (this) {
        RiskLevel.CRITICAL -> "🔴"
        RiskLevel.HIGH -> "🟠"
        RiskLevel.MODERATE -> "🟡"
        RiskLevel.LOW -> "🟢"
        else -> "⚪"
    }
}

/**
 * Obtiene la descripción según nivel de riesgo
 */
fun RiskLevel.toDescription(): String {
    return when (this) {
        RiskLevel.CRITICAL -> "Crítico - Acción Inmediata"
        RiskLevel.HIGH -> "Alto Riesgo - Atención Requerida"
        RiskLevel.MODERATE -> "Riesgo Moderado - Precaución"
        RiskLevel.LOW -> "Bajo Riesgo - Saludable"
        else -> "Datos Insuficientes"
    }
}

/**
 * Obtiene el mensaje de salud según score
 */
fun getHealthMessage(score: Int): String {
    return when {
        score >= 80 -> "¡Excelente! Tu salud ocupacional está en muy buen estado."
        score >= 60 -> "Bien. Algunas áreas necesitan atención preventiva."
        score >= 40 -> "Precaución. Es importante abordar varios aspectos de tu salud."
        score >= 20 -> "Atención requerida. Múltiples factores de riesgo identificados."
        else -> "Situación crítica. Acción inmediata necesaria."
    }
}

/**
 * Obtiene recomendaciones según IMC
 */
fun getBMIRecommendations(bmi: Float): List<String> {
    return when {
        bmi < 18.5 -> listOf(
            "Tu IMC indica bajo peso",
            "Consulta con nutricionista",
            "Aumenta ingesta calórica saludable"
        )
        bmi < 25 -> listOf(
            "Tu IMC está en rango saludable",
            "Mantén tus hábitos alimenticios",
            "Continúa con actividad física regular"
        )
        bmi < 30 -> listOf(
            "Tu IMC indica sobrepeso",
            "Considera plan nutricional",
            "Incrementa actividad física a 150min/semana"
        )
        bmi < 35 -> listOf(
            "Tu IMC indica obesidad",
            "Consulta médico y nutricionista",
            "Plan de ejercicio supervisado",
            "Evaluación de factores de riesgo"
        )
        else -> listOf(
            "Tu IMC indica obesidad mórbida",
            "🚨 Atención médica urgente requerida",
            "Evaluación integral de salud",
            "Programa multidisciplinario necesario"
        )
    }
}

/**
 * Calcula el riesgo de síndrome metabólico
 */
fun calculateMetabolicSyndromeRisk(
    bmi: Float,
    bloodPressure: String,
    bloodGlucose: String,
    cholesterol: String
): Pair<Int, RiskLevel> {
    var score = 0

    // Obesidad abdominal (IMC como proxy)
    if (bmi >= 30) score += 25
    else if (bmi >= 25) score += 15

    // Presión arterial
    if (bloodPressure.contains("Hipertensión moderada/severa")) score += 25
    else if (bloodPressure.contains("Hipertensión leve")) score += 20
    else if (bloodPressure.contains("Elevada")) score += 10

    // Glucosa
    if (bloodGlucose.contains("Diabetes")) score += 30
    else if (bloodGlucose.contains("Prediabetes")) score += 20

    // Colesterol
    if (cholesterol.contains("Alto")) score += 20
    else if (cholesterol.contains("Límite alto")) score += 10

    val riskLevel = when {
        score >= 70 -> RiskLevel.CRITICAL
        score >= 50 -> RiskLevel.HIGH
        score >= 30 -> RiskLevel.MODERATE
        else -> RiskLevel.LOW
    }

    return Pair(score, riskLevel)
}

/**
 * Genera sugerencias de ejercicio según perfil
 */
fun getExerciseSuggestions(
    lifestyle: CategoryScore?,
    muscular: CategoryScore?,
    cardiovascularRisk: RiskLevel
): List<String> {
    val suggestions = mutableListOf<String>()

    when (cardiovascularRisk) {
        RiskLevel.CRITICAL, RiskLevel.HIGH -> {
            suggestions.add("⚠️ Consulta médico antes de iniciar ejercicio")
            suggestions.add("Ejercicio supervisado recomendado")
            suggestions.add("Inicio gradual con caminatas de 10-15 min")
        }
        else -> {
            suggestions.add("Objetivo: 150 minutos/semana actividad moderada")
            suggestions.add("O 75 minutos/semana actividad vigorosa")
        }
    }

    if (muscular != null && muscular.riskLevel >= RiskLevel.MODERATE) {
        suggestions.add("Prioriza ejercicios de bajo impacto:")
        suggestions.add("• Natación o actividades acuáticas")
        suggestions.add("• Ciclismo estacionario")
        suggestions.add("• Yoga o Pilates para flexibilidad")
    } else {
        suggestions.add("Combina ejercicio cardiovascular con fuerza")
        suggestions.add("Incluye estiramientos y flexibilidad")
    }

    return suggestions
}

/**
 * Genera plan de pausas activas según ergonomía
 */
fun getActivePausesPlan(ergonomic: CategoryScore?): List<String> {
    return if (ergonomic != null && ergonomic.score >= 50) {
        listOf(
            "🚨 URGENTE: Pausas cada 30 minutos",
            "Duración mínima: 5 minutos",
            "Levántate y camina por la oficina",
            "Estiramientos de cuello y hombros",
            "Ejercicios de muñecas y manos",
            "Mirar a lo lejos (6 metros) por 20 segundos"
        )
    } else {
        listOf(
            "Pausas cada 50-60 minutos",
            "Duración: 2-5 minutos",
            "Levántate y estira",
            "Cambia de posición frecuentemente",
            "Regla 20-20-20 para los ojos"
        )
    }
}

/**
 * Formato de fecha para última actualización
 */
fun formatLastUpdate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutes = diff / 1000 / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Hace menos de 1 minuto"
        minutes < 60 -> "Hace $minutes minutos"
        hours < 24 -> "Hace ${hours} horas"
        days < 7 -> "Hace ${days} días"
        days < 30 -> "Hace ${days / 7} semanas"
        else -> "Hace más de un mes"
    }
}