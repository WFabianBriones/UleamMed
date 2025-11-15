package com.example.uleammed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // ⬅️ IMPORTANTE

// ============================================
// VIEWMODEL DEL DASHBOARD
// ============================================

class DashboardViewModel : ViewModel() {
    private val repository = AuthRepository()
    private val predictionEngine = HealthPredictionEngine()

    private val _healthProfile = MutableStateFlow<HealthRiskProfile?>(null)
    val healthProfile: StateFlow<HealthRiskProfile?> = _healthProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadHealthProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Cargar cuestionario inicial
                val initialResult = repository.getQuestionnaire(userId)
                val initial = initialResult.getOrNull()

                if (initial == null) {
                    _healthProfile.value = null
                    _isLoading.value = false
                    return@launch
                }

                // Cargar cuestionarios específicos
                val ergonomia = loadErgonomia(userId)
                val sintomasMusculares = loadSintomasMusculares(userId)
                val sintomasVisuales = loadSintomasVisuales(userId)
                val cargaTrabajo = loadCargaTrabajo(userId)
                val estres = loadEstres(userId)
                val sueno = loadSueno(userId)
                val actividadFisica = loadActividadFisica(userId)
                val balance = loadBalance(userId)

                // Analizar con el motor de predicción
                val profile = predictionEngine.analyzeUserHealth(
                    initial = initial,
                    ergonomia = ergonomia,
                    sintomasMusculares = sintomasMusculares,
                    sintomasVisuales = sintomasVisuales,
                    cargaTrabajo = cargaTrabajo,
                    estres = estres,
                    sueno = sueno,
                    actividadFisica = actividadFisica,
                    balance = balance
                )

                _healthProfile.value = profile

            } catch (e: Exception) {
                _error.value = "Error al cargar perfil de salud: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadErgonomia(userId: String): ErgonomiaQuestionnaire? {
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("ergonomia_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(ErgonomiaQuestionnaire::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadSintomasMusculares(userId: String): SintomasMuscularesQuestionnaire? {
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("sintomas_musculares_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(SintomasMuscularesQuestionnaire::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadSintomasVisuales(userId: String): SintomasVisualesQuestionnaire? {
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("sintomas_visuales_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(SintomasVisualesQuestionnaire::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadCargaTrabajo(userId: String): CargaTrabajoQuestionnaire? {
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("carga_trabajo_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(CargaTrabajoQuestionnaire::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadEstres(userId: String): EstresSaludMentalQuestionnaire? {
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("estres_salud_mental_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(EstresSaludMentalQuestionnaire::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadSueno(userId: String): HabitosSuenoQuestionnaire? {
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("habitos_sueno_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(HabitosSuenoQuestionnaire::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadActividadFisica(userId: String): ActividadFisicaQuestionnaire? {
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("actividad_fisica_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(ActividadFisicaQuestionnaire::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadBalance(userId: String): BalanceVidaTrabajoQuestionnaire? {
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("balance_vida_trabajo_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(BalanceVidaTrabajoQuestionnaire::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

// ============================================
// UI DEL DASHBOARD (ACTUALIZACIÓN DE HomeContent)
// ============================================

@Composable
fun DashboardHomeContent(
    userName: String,
    viewModel: DashboardViewModel = viewModel()
) {
    val healthProfile by viewModel.healthProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { userId ->
            viewModel.loadHealthProfile(userId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Encabezado
        Text(
            text = "¡Bienvenido!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = userName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            // Estado de carga
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (healthProfile == null) {
            // Sin datos - Invitar a completar cuestionarios
            EmptyDashboardState()
        } else {
            // Dashboard completo
            HealthDashboard(profile = healthProfile!!)
        }
    }
}

@Composable
fun EmptyDashboardState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Completa el Cuestionario Inicial",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Para ver tu perfil de salud, completa primero el cuestionario inicial de salud.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun HealthDashboard(profile: HealthRiskProfile) {
    // 1. Estado General de Salud
    OverallHealthCard(profile)

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Acciones Urgentes
    if (profile.urgentActions.isNotEmpty()) {
        UrgentActionsSection(profile.urgentActions)
        Spacer(modifier = Modifier.height(16.dp))
    }

    // 3. Riesgos Identificados
    if (profile.criticalRisks.isNotEmpty() || profile.highRisks.isNotEmpty()) {
        HealthRisksSection(profile)
        Spacer(modifier = Modifier.height(16.dp))
    }

    // 4. Recomendaciones
    RecommendationsSection(profile.recommendations)

    Spacer(modifier = Modifier.height(16.dp))

    // 5. Métricas por Categoría
    CategoryMetricsSection(profile)
}

@Composable
fun OverallHealthCard(profile: HealthRiskProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (profile.overallRiskLevel) {
                RiskLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                RiskLevel.HIGH -> Color(0xFFFFE0B2) // Naranja claro
                RiskLevel.MODERATE -> Color(0xFFFFF9C4) // Amarillo claro
                RiskLevel.LOW -> Color(0xFFC8E6C9) // Verde claro
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de estado
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        when (profile.overallRiskLevel) {
                            RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
                            RiskLevel.HIGH -> Color(0xFFFF9800)
                            RiskLevel.MODERATE -> Color(0xFFFFC107)
                            RiskLevel.LOW -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.outline
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${profile.overallScore}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Estado de Salud",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (profile.overallRiskLevel) {
                        RiskLevel.CRITICAL -> "⚠️ Crítico - Acción Inmediata"
                        RiskLevel.HIGH -> "🔶 Alto Riesgo - Atención Requerida"
                        RiskLevel.MODERATE -> "⚡ Riesgo Moderado - Precaución"
                        RiskLevel.LOW -> "✅ Saludable - Mantén Hábitos"
                        else -> "Datos Insuficientes"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cuestionarios: ${profile.completedQuestionnaires}/${profile.totalQuestionnaires}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Progreso
                LinearProgressIndicator(
                    progress = profile.completedQuestionnaires.toFloat() / profile.totalQuestionnaires,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Información adicional
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HealthMetricChip(
                label = "IMC",
                value = String.format("%.1f", profile.bmi),
                icon = Icons.Filled.MonitorWeight
            )
            HealthMetricChip(
                label = "Riesgos",
                value = "${profile.criticalRisks.size + profile.highRisks.size}",
                icon = Icons.Filled.Warning
            )
            HealthMetricChip(
                label = "Recomend.",
                value = "${profile.recommendations.size}",
                icon = Icons.Filled.Lightbulb
            )
        }
    }
}

@Composable
fun HealthMetricChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UrgentActionsSection(actions: List<UrgentAction>) {
    Text(
        text = "🚨 Acciones Urgentes",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    actions.forEach { action ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = action.icon,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = action.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = action.deadline,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = action.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun HealthRisksSection(profile: HealthRiskProfile) {
    Text(
        text = "⚠️ Riesgos Identificados",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    profile.criticalRisks.forEach { risk ->
        HealthRiskCard(risk)
    }

    profile.highRisks.take(3).forEach { risk ->
        HealthRiskCard(risk)
    }
}

@Composable
fun HealthRiskCard(risk: HealthRisk) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (risk.riskLevel) {
                RiskLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                RiskLevel.HIGH -> Color(0xFFFFE0B2)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = risk.condition,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = CircleShape,
                    color = when (risk.riskLevel) {
                        RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
                        RiskLevel.HIGH -> Color(0xFFFF9800)
                        else -> Color(0xFFFFC107)
                    }
                ) {
                    Text(
                        text = "${risk.probability}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = risk.description,
                style = MaterialTheme.typography.bodyMedium
            )

            if (risk.preventiveMeasures.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Medidas preventivas:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                risk.preventiveMeasures.take(2).forEach { measure ->
                    Text(
                        text = "• $measure",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendationsSection(recommendations: List<Recommendation>) {
    Text(
        text = "💡 Recomendaciones",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    if (recommendations.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "¡Excelente! No hay recomendaciones urgentes en este momento.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        recommendations.take(6).forEach { recommendation ->
            RecommendationCard(recommendation)
        }
    }
}

@Composable
fun RecommendationCard(recommendation: Recommendation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = recommendation.icon,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun CategoryMetricsSection(profile: HealthRiskProfile) {
    Text(
        text = "📊 Métricas por Categoría",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
    )

    val categories = listOfNotNull(
        profile.ergonomicScore,
        profile.musculoskeletalScore,
        profile.visualScore,
        profile.burnoutScore,
        profile.psychosocialScore,
        profile.sleepScore,
        profile.lifestyleScore,
        profile.balanceScore
    )

    if (categories.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Completa más cuestionarios para ver métricas detalladas",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else {
        categories.forEach { category ->
            CategoryScoreCard(category)
        }
    }
}

@Composable
fun CategoryScoreCard(category: CategoryScore) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (category.riskLevel) {
                RiskLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                RiskLevel.HIGH -> Color(0xFFFFE0B2)
                RiskLevel.MODERATE -> Color(0xFFFFF9C4)
                RiskLevel.LOW -> Color(0xFFC8E6C9)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = when (category.riskLevel) {
                        RiskLevel.CRITICAL -> "🔴 Crítico"
                        RiskLevel.HIGH -> "🟠 Alto"
                        RiskLevel.MODERATE -> "🟡 Moderado"
                        RiskLevel.LOW -> "🟢 Bajo"
                        else -> "⚪ Desconocido"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barra de progreso
            LinearProgressIndicator(
                progress = (100 - category.score).toFloat() / 100,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when (category.riskLevel) {
                    RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
                    RiskLevel.HIGH -> Color(0xFFFF9800)
                    RiskLevel.MODERATE -> Color(0xFFFFC107)
                    RiskLevel.LOW -> Color(0xFF4CAF50)
                    else -> MaterialTheme.colorScheme.outline
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (category.mainIssues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Problemas principales:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                category.mainIssues.forEach { issue ->
                    Text(
                        text = "• $issue",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}