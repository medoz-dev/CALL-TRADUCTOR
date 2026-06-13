package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CallSession
import com.example.data.TranslationMessage
import com.example.service.FloatingBubbleService
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallTranslatorUi(viewModel: CallTranslatorViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var showSplash by rememberSaveable { mutableStateOf(true) }

    val isTranslationActive by viewModel.isTranslationActive.collectAsStateWithLifecycle()
    val currentOriginalText by viewModel.currentOriginalText.collectAsStateWithLifecycle()
    val currentTranslatedText by viewModel.currentTranslatedText.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val currentSpeaker by viewModel.currentSpeaker.collectAsStateWithLifecycle()

    val callSessions by viewModel.callHistory.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val detailedMessages by viewModel.detailedMessages.collectAsStateWithLifecycle()
    val activeSessionMessages by viewModel.activeSessionMessages.collectAsStateWithLifecycle()
    val selectedSessionForDetails by viewModel.selectedSessionIdForDetails.collectAsStateWithLifecycle()

    var overlayChecked by remember { mutableStateOf(false) }

    // Check overlay permission regularly
    LaunchedEffect(Unit) {
        overlayChecked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SlateSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PhoneInTalk, contentDescription = "Console Live") },
                    label = { Text("Console Live", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberTeal,
                        selectedTextColor = CyberTeal,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = SlateCard
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Language, contentDescription = "Appel Internet") },
                    label = { Text("Appel Internet", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberTeal,
                        selectedTextColor = CyberTeal,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = SlateCard
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Historique") },
                    label = { Text("Historique", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberTeal,
                        selectedTextColor = CyberTeal,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = SlateCard
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Paramètres") },
                    label = { Text("Réglages", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberTeal,
                        selectedTextColor = CyberTeal,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = SlateCard
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SlateBackground, SlateSurface)
                    )
                )
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> LiveConsoleScreen(
                    viewModel = viewModel,
                    isTranslationActive = isTranslationActive,
                    currentOriginalText = currentOriginalText,
                    currentTranslatedText = currentTranslatedText,
                    isListening = isListening,
                    currentSpeaker = currentSpeaker,
                    clientLang = settings.clientLang,
                    partnerLang = settings.partnerLang,
                    dialectsEnabled = settings.localDialectEnabled,
                    activeSessionMessages = activeSessionMessages
                )
                1 -> InternetCallScreen(viewModel = viewModel)
                2 -> HistoryScreen(
                    sessions = callSessions,
                    onSelectSession = { viewModel.selectSessionForDetails(it) },
                    onDeleteSession = { viewModel.deleteSession(it) }
                )
                3 -> SettingsScreen(
                    settings = settings,
                    overlayEnabled = overlayChecked,
                    onUpdateSettings = { cl, pl, vt, sr, pt, ld, om, sh ->
                        viewModel.updateSettings(cl, pl, vt, sr, pt, ld, om, sh)
                    },
                    onToggleOverlay = { enabled ->
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                                Toast.makeText(context, "Veuillez autoriser la superposition pour VoxBridge", Toast.LENGTH_LONG).show()
                            } else {
                                overlayChecked = true
                                context.startService(Intent(context, FloatingBubbleService::class.java))
                            }
                        } else {
                            overlayChecked = false
                            context.stopService(Intent(context, FloatingBubbleService::class.java))
                        }
                    }
                )
            }

            // Historical chat logs detail dialog overlay
            if (selectedSessionForDetails != null) {
                SessionDetailDialog(
                    session = callSessions.find { it.id == selectedSessionForDetails },
                    messages = detailedMessages,
                    onDismiss = { viewModel.selectSessionForDetails(null) }
                )
            }
        }
    }

    AnimatedVisibility(
        visible = showSplash,
        exit = fadeOut(animationSpec = tween(600))
    ) {
        SplashScreen(onTimeout = { showSplash = false })
    }
}
}

@Composable
fun LanguageSelectDropdown(
    label: String,
    selectedCode: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.find { it.first == selectedCode }?.second ?: selectedCode

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedOption,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = "Menu déroulant",
                    tint = CyberTeal,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(SlateCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .widthIn(min = 140.dp)
            ) {
                options.forEach { (code, name) ->
                    val isSelected = code == selectedCode
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CyberTeal else TextPrimary
                            )
                        },
                        onClick = {
                            onSelected(code)
                            expanded = false
                        },
                        modifier = Modifier.background(
                            if (isSelected) CyberTeal.copy(alpha = 0.08f) else Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun LiveConsoleScreen(
    viewModel: CallTranslatorViewModel,
    isTranslationActive: Boolean,
    currentOriginalText: String,
    currentTranslatedText: String,
    isListening: Boolean,
    currentSpeaker: String,
    clientLang: String,
    partnerLang: String,
    dialectsEnabled: Boolean,
    activeSessionMessages: List<TranslationMessage>
) {
    val context = LocalContext.current
    var inputContactName by remember { mutableStateOf("Sarah") }
    var inputAppName by remember { mutableStateOf("WhatsApp") }
    var showStartCallDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderSection()
        }

        if (!isTranslationActive) {
            // Idle Mode state
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CyberTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = "Pret",
                                tint = CyberTeal,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "Prêt pour la traduction d'appel",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Configurez vos langues, puis lancez pour intercepter et traduire vos communications en temps réel.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        // Beautiful, Clean Minimalist Dropdown Language selectors
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LanguageSelectDropdown(
                                label = "Ma langue (Moi)",
                                selectedCode = clientLang,
                                options = listOf(
                                    "fr" to "Français 🇫🇷",
                                    "en" to "Anglais 🇺🇸",
                                    "es" to "Espagnol 🇪🇸"
                                ),
                                onSelected = { code ->
                                    viewModel.updateSettings(clientLang = code)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            LanguageSelectDropdown(
                                label = "Correspondant",
                                selectedCode = partnerLang,
                                options = listOf(
                                    "en" to "Anglais 🇺🇸",
                                    "es" to "Espagnol 🇪🇸",
                                    "de" to "Allemand 🇩🇪",
                                    "fon" to "Fon (Bénin) 🇧🇯",
                                    "yo" to "Yoruba 🇳🇬",
                                    "wo" to "Wolof 🇸🇳"
                                ),
                                onSelected = { code ->
                                    viewModel.updateSettings(partnerLang = code)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = { showStartCallDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = SlateBackground)
                            Spacer(Modifier.width(8.dp))
                            Text("Démarrer Traducteur", color = SlateBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                TranslationFeaturesHint()
            }
        } else {
            // Live Call Translation Console
            item {
                ActiveCallConsoleCard(
                    isListening = isListening,
                    currentSpeaker = currentSpeaker,
                    clientLang = clientLang,
                    partnerLang = partnerLang,
                    onToggleSpeaker = { viewModel.toggleSpeaker() },
                    onToggleMic = { viewModel.toggleMicrophone() },
                    onStop = { viewModel.stopCallTranslation() }
                )
            }

            item {
                LiveTranscriptCard(
                    currentSpeaker = currentSpeaker,
                    originalText = currentOriginalText,
                    translatedText = currentTranslatedText,
                    clientLang = clientLang,
                    partnerLang = partnerLang
                )
            }

            item {
                RealTimeTranscriptTimeline(
                    messages = activeSessionMessages,
                    clientLang = clientLang,
                    partnerLang = partnerLang
                )
            }

            // SIMULATOR PANEL (CRITICAL FOR TESTING CALL INTERCEPTIONS ON ONLINE EMULATORS)
            item {
                SimulatorTriggerPanel(
                    partnerLang = partnerLang,
                    dialectsEnabled = dialectsEnabled,
                    onTriggerText = { text ->
                        viewModel.simulateSpeech(text, "INTERLOCUTEUR")
                    },
                    onTriggerMyText = { text ->
                        viewModel.simulateSpeech(text, "ME")
                    }
                )
            }
        }
    }

    // Modal dialog to configure custom simulated call names
    if (showStartCallDialog) {
        Dialog(onDismissRequest = { showStartCallDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Configurer l'appel traducteur",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = inputContactName,
                        onValueChange = { inputContactName = it },
                        label = { Text("Nom du contact", color = TextSecondary) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberTeal,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = CyberTeal,
                            unfocusedLabelColor = TextSecondary,
                            focusedContainerColor = SlateCard,
                            unfocusedContainerColor = SlateCard
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Application d'appel",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Platform buttons selectors (WhatsApp vs phone Zoom etc)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("WhatsApp", "Téléphone", "Zoom", "Messenger").forEach { app ->
                            val isSelected = inputAppName == app
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) CyberTeal.copy(alpha = 0.2f) else SlateCard)
                                    .border(
                                        1.dp,
                                        if (isSelected) CyberTeal else BorderColor,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { inputAppName = app }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app,
                                    fontSize = 11.sp,
                                    color = if (isSelected) CyberTeal else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showStartCallDialog = false }) {
                            Text("Annuler", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                showStartCallDialog = false
                                viewModel.startCallTranslation(inputContactName, inputAppName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Lancer la Traduction", color = SlateBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "VoxBridge AI",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = CyberTeal,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "Traduction Vocale d'Appels Live",
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CyberTeal.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CyberTeal)
                )
                Text("Moteur VoxBridge Actif", fontSize = 10.sp, color = CyberTeal, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActiveCallConsoleCard(
    isListening: Boolean,
    currentSpeaker: String,
    clientLang: String,
    partnerLang: String,
    onToggleSpeaker: () -> Unit,
    onToggleMic: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live wave pulse animation
            AudioWaveVisualizer(isListening = isListening)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speaker select block
                InteractiveSpeakerButton(
                    speakerType = "ME",
                    lang = clientLang,
                    isSelected = currentSpeaker == "ME",
                    onClick = onToggleSpeaker
                )

                InteractiveSpeakerButton(
                    speakerType = "CORRESPONDANT",
                    lang = partnerLang,
                    isSelected = currentSpeaker == "INTERLOCUTEUR",
                    onClick = onToggleSpeaker
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Custom toggle mic action with rounded pulse UI
                IconButton(
                    onClick = onToggleMic,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isListening) CyberTeal else SlateCard)
                        .border(1.dp, if (isListening) Color.Transparent else BorderColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Mic toggle",
                        tint = if (isListening) SlateBackground else TextPrimary
                    )
                }

                // Stop session
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CyberRose)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Stop call",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = if (isListening) "En cours de capture et traduction vocale..." else "Traduction en pause. Activez le micro pour reprendre.",
                fontSize = 11.sp,
                color = if (isListening) CyberTeal else TextSecondary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun InteractiveSpeakerButton(
    speakerType: String,
    lang: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val displayTitle = if (speakerType == "ME") "Moi (Locuteur)" else "Correspondant"
    val displayLang = lang.uppercase()
    val indicatorColor = if (speakerType == "ME") CyberTeal else CyberRose

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) indicatorColor.copy(alpha = 0.15f) else SlateCard)
            .border(
                2.dp,
                if (isSelected) indicatorColor else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                displayTitle,
                fontSize = 10.sp,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                displayLang,
                fontSize = 16.sp,
                color = if (isSelected) indicatorColor else TextPrimary,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (isSelected) "Parle 🗣️" else "Muet 🔇",
                fontSize = 10.sp,
                color = if (isSelected) indicatorColor else TextSecondary
            )
        }
    }
}

@Composable
fun LiveTranscriptCard(
    currentSpeaker: String,
    originalText: String,
    translatedText: String,
    clientLang: String,
    partnerLang: String
) {
    val sourceTitle = if (currentSpeaker == "ME") "Moi (${clientLang.uppercase()})" else "Lui/Elle (${partnerLang.uppercase()})"
    val targetTitle = if (currentSpeaker == "ME") "Lui/Elle (${partnerLang.uppercase()})" else "Moi (${clientLang.uppercase()})"
    val speakerIcon = if (currentSpeaker == "ME") Icons.Default.Person else Icons.Default.RecordVoiceOver

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = speakerIcon,
                    contentDescription = "Speaker type",
                    tint = CyberTeal,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Phrase en cours",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Original phrase
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateBackground)
                    .padding(14.dp)
            ) {
                Text(sourceTitle, fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (originalText.isBlank()) "Écoute de la voix en direct..." else originalText,
                    color = if (originalText.isBlank()) TextSecondary else TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp
                )
            }

            // Translated phrase
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberTeal.copy(alpha = 0.05f))
                    .border(1.dp, CyberTeal.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(targetTitle, fontSize = 9.sp, color = CyberTeal, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (translatedText.isBlank()) "La traduction en direct s'affichera ici..." else translatedText,
                    color = if (translatedText.isBlank()) TextSecondary else CyberTeal,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun RealTimeTranscriptTimeline(
    messages: List<TranslationMessage>,
    clientLang: String,
    partnerLang: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = "Transcript icon",
                        tint = CyberTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Journal de la conversation",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                // Pulsing real-time badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(CyberTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CyberTeal)
                    )
                    Text(
                        text = "EN DIRECT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberTeal
                    )
                }
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateBackground)
                        .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = "En attente",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "En attente de parole...",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Show last 6 messages for compact, neat display
                    messages.takeLast(6).forEach { msg ->
                        val isMe = msg.speakerType == "ME"
                        val bubbleColor = if (isMe) CyberTeal.copy(alpha = 0.08f) else SlateCard
                        val alignment = if (isMe) Alignment.End else Alignment.Start
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = alignment
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isMe) 16.dp else 4.dp,
                                            bottomEnd = if (isMe) 4.dp else 16.dp
                                        )
                                    )
                                    .background(bubbleColor)
                                    .border(
                                        1.dp,
                                        if (isMe) CyberTeal.copy(alpha = 0.2f) else BorderColor.copy(alpha = 0.7f),
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isMe) 16.dp else 4.dp,
                                            bottomEnd = if (isMe) 4.dp else 16.dp
                                        )
                                    )
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isMe) "Moi (${msg.sourceLanguage.uppercase()})" else "Correspondant (${msg.sourceLanguage.uppercase()})",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMe) CyberTeal else CyberRose
                                        )
                                        
                                        Text(
                                            text = "${getLanguageName(msg.sourceLanguage)} ➔ ${getLanguageName(msg.targetLanguage)}",
                                            fontSize = 8.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    
                                    // Original speech text
                                    Text(
                                        text = msg.textOriginal,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                    
                                    Spacer(modifier = Modifier.height(2.dp))
                                    
                                    // Translation text
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = "Translated",
                                            tint = CyberTeal,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = msg.textTranslated,
                                            fontSize = 13.sp,
                                            color = if (isMe) CyberTeal else TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatorTriggerPanel(
    partnerLang: String,
    dialectsEnabled: Boolean,
    onTriggerText: (String) -> Unit,
    onTriggerMyText: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = "Simulate", tint = AmberAccent)
                Text(
                    "Simulateur de Phrases d'Appels",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Text(
                "Le simulateur permet de tester la reconnaissance et la traduction de messages en direct sans devoir passer de vrais appels ou utiliser votre microphone physique.",
                fontSize = 11.sp,
                color = TextSecondary
            )

            // Dynamic layout of script sentences based on target languages
            Text(
                "L'Interlocuteur dit (${partnerLang.uppercase()}) :",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyberRose
            )

            val phrases = when (partnerLang.lowercase()) {
                "en" -> listOf(
                    "Hello! Can you hear me clearly?",
                    "We need to schedule the Zoom meeting this next Monday.",
                    "The translation sounds wonderful! Thanks for building this app!"
                )
                "es" -> listOf(
                    "¡Hola! Estoy muy feliz de hablar contigo hoy.",
                    "¿Necesitamos enviar los informes finales esta tarde?",
                    "Me encanta usar esta tecnología para traducir en directo."
                )
                "de" -> listOf(
                    "Hallo! Kannst du mich gut hören?",
                    "Wir müssen das Treffen für nächsten Montag organisieren."
                )
                "yo" -> listOf(
                    "Bawo ni oṣe nṣe? Ire oṣe po!",
                    "Ẹṣé gan ni ire ooo!"
                )
                "fon" -> listOf(
                    "E ɖo te gbe? Mi na d'alɔ mɛ tɔn kaka sɔ.",
                    "Ku d'avɔ mɛ! Gbe bɛ gbe na nyɔ sɔ̃!"
                )
                else -> listOf(
                    "Hello, how is the call going?",
                    "Please confirm you heard my message."
                )
            }

            phrases.forEach { phrase ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateBackground)
                        .clickable { onTriggerText(phrase) }
                        .padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(phrase, fontSize = 12.sp, color = TextPrimary)
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = CyberRose, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Divider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))

            Text(
                "Moi, je réponds (FRANÇAIS) :",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTeal
            )

            val myReplies = listOf(
                "Oui, je t'entends très bien ! Tu as fait de l'excellent travail.",
                "Je suis d'accord, organisons cela au plus vite.",
                "Est-ce que tu as compris ma voix ?"
            )

            myReplies.forEach { reply ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateBackground)
                        .clickable { onTriggerMyText(reply) }
                        .padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(reply, fontSize = 12.sp, color = TextPrimary)
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = CyberTeal, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AudioWaveVisualizer(isListening: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Wave pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Sub wave bars lengths
    val height1 by infiniteTransition.animateFloat(
        initialValue = 15.dp.value,
        targetValue = 45.dp.value,
        animationSpec = infiniteRepeatable(animation = tween(400), repeatMode = RepeatMode.Reverse)
    )
    val height2 by infiniteTransition.animateFloat(
        initialValue = 20.dp.value,
        targetValue = 55.dp.value,
        animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse)
    )
    val height3 by infiniteTransition.animateFloat(
        initialValue = 10.dp.value,
        targetValue = 35.dp.value,
        animationSpec = infiniteRepeatable(animation = tween(300), repeatMode = RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            // Wave pulse glow
            Box(
                modifier = Modifier
                    .size((70 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(CyberTeal.copy(alpha = 0.05f))
                    .border(1.dp, CyberTeal.copy(alpha = 0.2f), CircleShape)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(height3, height1, height2, height1, height3).forEach { ht ->
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(ht.dp)
                            .clip(RoundedCornerShape(30))
                            .background(CyberTeal)
                    )
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(30))
                            .background(TextSecondary.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

@Composable
fun TranslationFeaturesHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("💎 Services Exceptionnels", fontWeight = FontWeight.Bold, color = CyberTeal)
            
            val infoItems = listOf(
                "Superposition multi-applications" to "Rendez-vous dans l'onglet Réglages pour lancer la bulle de superposition. Elle flottera au-dessus de WhatsApp, Zoom, ou Messenger.",
                "Soutien des Dialectes Locaux" to "Grâce à notre intelligence embarquée VoxBridge, l'application comprend parfaitement les dialectes d'Afrique de l'Ouest (Fon, Yoruba, Wolof, Éwé) sans latence.",
                "Synthèse Vocale Naturelle" to "Intègre la lecture en temps réel de la traduction pour que vous n'ayez jamais à regarder votre écran."
            )

            infoItems.forEach { (title, text) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Check", tint = CyberTeal, modifier = Modifier.size(16.dp))
                    Column {
                        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text, fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

private fun getLanguageName(code: String): String {
    return when (code.lowercase()) {
        "fr" -> "Français"
        "en" -> "Anglais"
        "es" -> "Espagnol"
        "de" -> "Allemand"
        "it" -> "Italien"
        "pt" -> "Portugais"
        "fon" -> "Fon (Bénin)"
        "yo" -> "Yoruba (Nigeria)"
        "wo" -> "Wolof (Sénégal)"
        else -> code.uppercase()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    sessions: List<CallSession>,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredSessions = remember(sessions, searchQuery) {
        if (searchQuery.isBlank()) {
            sessions
        } else {
            val query = searchQuery.lowercase().trim()
            sessions.filter { session ->
                session.contactName.lowercase().contains(query) ||
                session.appName.lowercase().contains(query) ||
                (session.keyPhrasesSummary?.lowercase()?.contains(query) == true) ||
                getLanguageName(session.sourceLanguage).lowercase().contains(query) ||
                getLanguageName(session.targetLanguage).lowercase().contains(query)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Historique d'Appels",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Fluent Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().testTag("history_search_input"),
            placeholder = { Text("Rechercher un contact, une appli ou un mot clé...", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Recherche", tint = CyberTeal) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = CyberTeal,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = SlateCard,
                unfocusedContainerColor = SlateCard
            ),
            shape = RoundedCornerShape(14.dp)
        )

        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneCallback,
                        contentDescription = "Aucun appel",
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Aucun résultat trouvé" else "Aucun appel enregistré",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Essayez d'autres mots-clés." else "Vos appels traduits apparaîtront ici.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSessions) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSession(session.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Top Row: Contact Name, App, Delete Action, Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (session.appName == "WhatsApp") Color(0xFF25D366).copy(alpha = 0.15f)
                                                else CyberTeal.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (session.appName == "WhatsApp") Icons.Default.Phone else Icons.Default.Call,
                                            contentDescription = "Call Type",
                                            tint = if (session.appName == "WhatsApp") Color(0xFF25D366) else CyberTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = session.contactName,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Via ${session.appName}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val dateText = java.text.DateFormat.getDateTimeInstance(
                                        java.text.DateFormat.SHORT, java.text.DateFormat.SHORT
                                    ).format(java.util.Date(session.timestamp))
                                    
                                    Text(
                                        text = dateText,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )

                                    IconButton(onClick = { onDeleteSession(session.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CyberRose.copy(alpha = 0.7f))
                                    }
                                }
                            }

                            // Second Row: Languages capsule
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlateCard)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = getLanguageName(session.sourceLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTeal
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowRightAlt,
                                    contentDescription = "vers",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = getLanguageName(session.targetLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberRose
                                )
                                if (session.durationSeconds > 0) {
                                    Text(
                                        text = " • ${session.durationSeconds}s",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Third Row: Key Phrase Summary
                            if (!session.keyPhrasesSummary.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Summary",
                                        tint = CyberTeal.copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                    )
                                    Text(
                                        text = session.keyPhrasesSummary,
                                        fontSize = 12.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = TextPrimary.copy(alpha = 0.9f),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: com.example.data.AppSettings,
    overlayEnabled: Boolean,
    onUpdateSettings: (clientLang: String?, partnerLang: String?, voiceType: String?, speechRate: Float?, speechPitch: Float?, localDialectEnabled: Boolean?, offlineModeEnabled: Boolean?, saveCallHistory: Boolean?) -> Unit,
    onToggleOverlay: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Paramètres de Superposition",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Overlay trigger
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "Overlay", tint = CyberTeal)
                        Column {
                            Text("Bulle de Traduction Superposée", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Affiche un widget flottant par-dessus WhatsApp, Zoom et Messenger pendant votre appel.", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Switch(
                        checked = overlayEnabled,
                        onCheckedChange = { onToggleOverlay(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberTeal, checkedTrackColor = CyberTeal.copy(alpha = 0.4f))
                    )
                }
            }
        }

        item {
            Text(
                text = "Personnalisation Vocale AI",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Config Languages Dropdown/Choice
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Langues Déclenchées", fontWeight = FontWeight.Bold, color = TextPrimary)
                    
                    // Client Primary Select
                    LanguageSelectDropdown(
                        label = "Ma langue principale (Moi)",
                        selectedCode = settings.clientLang,
                        options = listOf(
                            "fr" to "Français 🇫🇷",
                            "en" to "Anglais 🇺🇸",
                            "es" to "Espagnol 🇪🇸",
                            "de" to "Allemand 🇩🇪"
                        ),
                        onSelected = { code ->
                            onUpdateSettings(code, null, null, null, null, null, null, null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Partner Secondary Select
                    LanguageSelectDropdown(
                        label = "Langue de mon correspondant",
                        selectedCode = settings.partnerLang,
                        options = listOf(
                            "fr" to "Français 🇫🇷",
                            "en" to "Anglais 🇺🇸",
                            "es" to "Espagnol 🇪🇸",
                            "de" to "Allemand 🇩🇪",
                            "fon" to "Fon (Bénin) 🇧🇯",
                            "yo" to "Yoruba 🇳🇬",
                            "wo" to "Wolof 🇸🇳"
                        ),
                        onSelected = { code ->
                            val isAfricanDialect = code in listOf("fon", "yo", "wo")
                            onUpdateSettings(null, code, null, null, null, isAfricanDialect, null, null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // TTS voice customizer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Type de voix de l'IA", fontWeight = FontWeight.Bold, color = TextPrimary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("FEMALE" to "👩 Féminin", "MALE" to "👨 Masculin").forEach { (gender, label) ->
                            val selected = settings.voiceType == gender
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) CyberTeal.copy(alpha = 0.15f) else SlateCard)
                                    .border(1.dp, if (selected) CyberTeal else BorderColor, RoundedCornerShape(12.dp))
                                    .clickable { onUpdateSettings(null, null, gender, null, null, null, null, null) }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontSize = 13.sp, color = if (selected) CyberTeal else TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Pitch
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Tonalité (Pitch)", fontSize = 11.sp, color = TextSecondary)
                            Text(String.format("%.1fx", settings.speechPitch), fontSize = 11.sp, color = CyberTeal, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = settings.speechPitch,
                            onValueChange = { onUpdateSettings(null, null, null, null, it, null, null, null) },
                            valueRange = 0.5f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal)
                        )
                    }

                    // Speed Rate
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Vitesse de lecture (Rate)", fontSize = 11.sp, color = TextSecondary)
                            Text(String.format("%.1fx", settings.speechRate), fontSize = 11.sp, color = CyberTeal, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = settings.speechRate,
                            onValueChange = { onUpdateSettings(null, null, null, it, null, null, null, null) },
                            valueRange = 0.5f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal)
                        )
                    }
                }
            }
        }

        // Toggles
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // West african dialect optimization toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dialectes Ouest-Africains", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Optimise la reconnaissance phonétique et linguistique de la traduction pour le Yoruba, Fon, Wolof.", fontSize = 11.sp, color = TextSecondary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = settings.localDialectEnabled,
                            onCheckedChange = { onUpdateSettings(null, null, null, null, null, it, null, null) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberTeal, checkedTrackColor = CyberTeal.copy(alpha = 0.4f))
                        )
                    }

                    Divider(color = BorderColor)

                    // Offline translation toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mode Hors Ligne", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Utiliser les dictionnaires embarqués hors ligne si aucune connexion n'est active.", fontSize = 11.sp, color = TextSecondary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = settings.offlineModeEnabled,
                            onCheckedChange = { onUpdateSettings(null, null, null, null, null, null, it, null) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberTeal, checkedTrackColor = CyberTeal.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionDetailDialog(
    session: CallSession?,
    messages: List<TranslationMessage>,
    onDismiss: () -> Unit
) {
    if (session == null) return
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Topic title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(session.contactName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Text("Appel enregistré via ${session.appName}", fontSize = 11.sp, color = TextSecondary)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }

                // Session metrics & Languages capsule
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getLanguageName(session.sourceLanguage) + " ➔ " + getLanguageName(session.targetLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTeal,
                        modifier = Modifier
                            .background(SlateCard, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    
                    if (session.durationSeconds > 0) {
                        Text(
                            text = "Durée: ${session.durationSeconds}s",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier
                                .background(SlateCard, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!session.keyPhrasesSummary.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, CyberTeal.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Summary",
                                tint = CyberTeal,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Résumé & Mots clés :",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTeal
                                )
                                Text(
                                    text = session.keyPhrasesSummary,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Divider(color = BorderColor)

                // Conversation bubbles list
                if (messages.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Aucun message enregistré pour cet appel.", fontSize = 11.sp, color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(messages) { msg ->
                            val isMe = msg.speakerType == "ME"
                            val bubbleColor = if (isMe) CyberTeal.copy(alpha = 0.15f) else SlateCard
                            val alignment = if (isMe) Alignment.End else Alignment.Start

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = alignment
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 4.dp,
                                                bottomEnd = if (isMe) 4.dp else 16.dp
                                            )
                                        )
                                        .background(bubbleColor)
                                        .border(
                                            1.dp,
                                            if (isMe) CyberTeal.copy(alpha = 0.3f) else BorderColor,
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 4.dp,
                                                bottomEnd = if (isMe) 4.dp else 16.dp
                                            )
                                        )
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (isMe) "Moi (${msg.sourceLanguage.uppercase()})" else "${session.contactName} (${msg.sourceLanguage.uppercase()})",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMe) CyberTeal else CyberRose
                                        )
                                        Text(msg.textOriginal, fontSize = 13.sp, color = TextPrimary)
                                        
                                        Divider(color = BorderColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                                        
                                        Text("Traduction (${msg.targetLanguage.uppercase()}) :", fontSize = 9.sp, color = TextSecondary)
                                        Text(msg.textTranslated, fontSize = 14.sp, color = if (isMe) CyberTeal else TextPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InternetCallScreen(viewModel: CallTranslatorViewModel) {
    val connectionState by viewModel.internetConnectionState.collectAsStateWithLifecycle()
    val roomCode by viewModel.internetRoomCode.collectAsStateWithLifecycle()
    val myName by viewModel.internetMyName.collectAsStateWithLifecycle()
    val peerName by viewModel.internetPeerName.collectAsStateWithLifecycle()
    val isCallIncoming by viewModel.internetIsCallIncoming.collectAsStateWithLifecycle()
    val isCallActive by viewModel.internetIsCallActive.collectAsStateWithLifecycle()
    val callerName by viewModel.internetCallerName.collectAsStateWithLifecycle()
    val activeMessages by viewModel.internetActiveCallMessages.collectAsStateWithLifecycle()
    val isListening by viewModel.internetIsListening.collectAsStateWithLifecycle()
    val originalText by viewModel.internetCurrentOriginalText.collectAsStateWithLifecycle()
    val translatedText by viewModel.internetCurrentTranslatedText.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var inputRoomCode by remember { mutableStateOf("") }
    var inputMyName by remember { mutableStateOf("Moi") }

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        when (connectionState) {
            com.example.service.InternetCallManager.ConnectionStatus.DISCONNECTED -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(CyberTeal.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Contact direct via internet",
                                        tint = CyberTeal,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Text(
                                    text = "Salon d'Appel Internet Traduit",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Connectez-vous à un salon éphémère sécurisé. L'appli traduit réciproquement vos paroles en arrière-plan et lit la traduction directement à haute voix.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )

                                // EXPLICATION PEDAGOGIQUE SANS CODE NI BANNIERE LOURDE
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "💡 Comment ça marche sans s'inscrire ?",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberTeal
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Pas besoin de compte e-mail ni de mot de passe. Vous et votre interlocuteur décidez simplement d'un code secret de salon pour vous connecter au canal crypté ultra-sécurisé de notre passerelle VoxBridge. C'est immédiat et anonyme !",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = inputMyName,
                                    onValueChange = { inputMyName = it },
                                    label = { Text("Votre Pseudo (pour votre ami)", color = TextSecondary) },
                                    placeholder = { Text("Ex: Michel", color = TextSecondary.copy(alpha = 0.6f)) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyberTeal) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = CyberTeal,
                                        unfocusedBorderColor = BorderColor,
                                        focusedLabelColor = CyberTeal,
                                        unfocusedLabelColor = TextSecondary,
                                        focusedContainerColor = SlateCard,
                                        unfocusedContainerColor = SlateCard
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = inputRoomCode,
                                    onValueChange = { inputRoomCode = it },
                                    label = { Text("Code Secret du Salon", color = TextSecondary) },
                                    placeholder = { Text("Ex: 8X3Y2K7P ou personnalisé", color = TextSecondary.copy(alpha = 0.6f)) },
                                    leadingIcon = { Icon(Icons.Default.Dialpad, contentDescription = null, tint = CyberTeal) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                inputRoomCode = generateSecureRoomCode()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Générer un code aléatoire",
                                                tint = CyberTeal
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = CyberTeal,
                                        unfocusedBorderColor = BorderColor,
                                        focusedLabelColor = CyberTeal,
                                        unfocusedLabelColor = TextSecondary,
                                        focusedContainerColor = SlateCard,
                                        unfocusedContainerColor = SlateCard
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SlateCard, RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Vos langues d'écoute active (Réglages)", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                        Text("Moi: ${settings.clientLang.uppercase()} ➔ Ami: ${settings.partnerLang.uppercase()}", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }

                                Button(
                                    onClick = { viewModel.connectToInternetRoom(inputRoomCode, inputMyName) },
                                    enabled = inputRoomCode.isNotBlank() && inputMyName.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal)
                                ) {
                                    Icon(Icons.Default.Power, contentDescription = null, tint = SlateBackground)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Rejoindre le Salon", color = SlateBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            com.example.service.InternetCallManager.ConnectionStatus.CONNECTING -> {
                Card(
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = CyberTeal)
                        Text(
                            text = "Connexion sécurisée au salon...",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            com.example.service.InternetCallManager.ConnectionStatus.ERROR -> {
                Card(
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Erreur", tint = CyberRose, modifier = Modifier.size(48.dp))
                        Text("Échec de connexion au salon", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Vérifiez votre connexion internet.", color = TextSecondary, textAlign = TextAlign.Center)
                        Button(
                            onClick = { viewModel.disconnectFromInternetRoom() },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCard)
                        ) {
                            Text("Retour")
                        }
                    }
                }
            }

            com.example.service.InternetCallManager.ConnectionStatus.CONNECTED -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Salon status header
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Green)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("🟢 Salon: #${roomCode.uppercase()}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Connecté en tant que: $myName", color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            IconButton(
                                onClick = { viewModel.disconnectFromInternetRoom() }
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "Quitter", tint = CyberRose)
                            }
                        }
                    }

                    if (!isCallActive) {
                        // Lobby state - Connection is OK, waiting to dial or receive a call
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(CyberTeal.copy(alpha = 0.15f * pulseAlpha)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Pret",
                                        tint = CyberTeal,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                
                                Spacer(Modifier.height(24.dp))

                                Text(
                                    text = if (peerName != null) "Ami en ligne : ${peerName}" else "En attente de votre correspondant...",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "Donnez le code secret (#$roomCode) de ce salon à votre correspondant pour qu'il le rejoigne sur son propre téléphone. Une fois en ligne, lancez l'appel !",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(Modifier.height(16.dp))

                                val context = androidx.compose.ui.platform.LocalContext.current
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clipData = android.content.ClipData.newPlainText("VoxBridge Room Code", roomCode)
                                            clipboardManager.setPrimaryClip(clipData)
                                            android.widget.Toast.makeText(context, "Code #${roomCode.uppercase()} copié !", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, BorderColor),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Copier", fontSize = 12.sp, color = CyberTeal)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val sendIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_TEXT, "Rejoins mon appel traduit sur VoxBridge ! Copie simplement ce code de salon secret : #${roomCode.uppercase()} et entre-le après avoir ouvert le menu Appel Internet.")
                                                type = "text/plain"
                                            }
                                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Partager le salon d'appel")
                                            context.startActivity(shareIntent)
                                        },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, BorderColor),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Partager", fontSize = 12.sp, color = CyberTeal)
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Button(
                                    onClick = { viewModel.startInternetCallDial() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = SlateBackground)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Lancer l'Appel Sécurisé", color = SlateBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // ACTIVE CALL SCREEN PANEL
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Call Wave/Status header
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(CyberTeal.copy(alpha = pulseAlpha))
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Appel Vocal Traduit Actif", color = CyberTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Text("Ami: ${peerName ?: "Correspondant"}", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Interactive Transcript Bubble area
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(SlateCard, RoundedCornerShape(16.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                        .padding(8.dp)
                                ) {
                                    if (activeMessages.isEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Default.Hearing, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                                            Spacer(Modifier.height(8.dp))
                                            Text("Prêt pour l'écoute mutuelle", fontSize = 11.sp, color = TextSecondary)
                                            Text("Appuyez sur 'Mic' et parlez !", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            reverseLayout = true
                                        ) {
                                            items(activeMessages.reversed()) { msg ->
                                                val isMe = msg.speakerType == "ME"
                                                val bubbleColor = if (isMe) SlateSurface else CyberTeal.copy(alpha = 0.1f)
                                                val borderLineColor = if (isMe) BorderColor else CyberTeal

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                                ) {
                                                    Card(
                                                        shape = RoundedCornerShape(
                                                            topStart = 16.dp, topEnd = 16.dp,
                                                            bottomStart = if (isMe) 16.dp else 4.dp,
                                                            bottomEnd = if (isMe) 4.dp else 16.dp
                                                        ),
                                                        border = BorderStroke(1.dp, borderLineColor),
                                                        colors = CardDefaults.cardColors(containerColor = bubbleColor),
                                                        modifier = Modifier.widthIn(max = 240.dp)
                                                    ) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Text(
                                                                text = if (isMe) "Vous" else (peerName ?: "Ami"),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isMe) TextSecondary else CyberTeal
                                                            )
                                                            Text(msg.textOriginal, fontSize = 13.sp, color = TextPrimary)
                                                            Divider(color = BorderColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                                                            Text(msg.textTranslated, fontSize = 14.sp, color = if (isMe) CyberTeal else TextPrimary, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Interactive voice output live feedback
                                if (originalText.isNotEmpty() || translatedText.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SlateCard.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("En cours de traitement :", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                        if (originalText.isNotEmpty()) {
                                            Text("A dit: \"$originalText\"", fontSize = 11.sp, color = TextPrimary, fontStyle = FontStyle.Italic)
                                        }
                                        if (translatedText.isNotEmpty()) {
                                            Text("Traduction: \"$translatedText\"", fontSize = 12.sp, color = CyberTeal, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                // Active calling options row with mic toggle and hang up
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Mic toggle button (Active or Muted)
                                    Button(
                                        onClick = { viewModel.triggerInternetMic() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isListening) CyberRose else SlateCard
                                        ),
                                        shape = CircleShape,
                                        modifier = Modifier.size(56.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                            contentDescription = "Microphone",
                                            tint = if (isListening) Color.White else TextPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Direct Simulator input trigger for testing on silent platforms / emulators
                                    Button(
                                        onClick = {
                                            // Trigger simulation text box entry directly
                                            viewModel.startInternetCallDial() // Keep active
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Simuler Voix", fontSize = 11.sp, color = TextPrimary)
                                    }

                                    // Hang up button
                                    Button(
                                        onClick = { viewModel.rejectInternetCall() },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberRose),
                                        shape = CircleShape,
                                        modifier = Modifier.size(56.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CallEnd,
                                            contentDescription = "Raccrocher",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // POPUP MODAL SCREEN FOR INCOMING CALL SIMULATION / REAL-TIME DETECTION
        if (isCallIncoming) {
            Dialog(onDismissRequest = { viewModel.rejectInternetCall() }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(2.dp, CyberTeal)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(CyberTeal.copy(alpha = 0.2f * pulseAlpha)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneCallback,
                                contentDescription = null,
                                tint = CyberTeal,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Text(
                            text = "Appel Vocal Entrant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "Votre correspondant \"$callerName\" vous appelle en ligne pour commencer un échange traduit !",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.rejectInternetCall() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberRose),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Refuser", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.acceptInternetCall() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = SlateBackground)
                                Spacer(Modifier.width(8.dp))
                                Text("Décrocher", color = SlateBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var startAnim by remember { mutableStateOf(false) }
    
    val alphaText by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "AlphaText"
    )
    
    val scaleLogo by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ScaleLogo"
    )
    
    val alphaMadeInBenin by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 1200, easing = LinearOutSlowInEasing),
        label = "AlphaMadeInBenin"
    )

    val translationYMadeInBenin by animateFloatAsState(
        targetValue = if (startAnim) 0f else 40f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 1200, easing = FastOutSlowInEasing),
        label = "TranslationYMadeInBenin"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        playStartupChime()
        kotlinx.coroutines.delay(3200) // Beautiful 3.2s splash presentation
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SlateBackground, SlateSurface, BorderColor)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // High-fidelity pulsing voice circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer(scaleX = scaleLogo, scaleY = scaleLogo)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(BorderStroke(2.dp, CyberTeal.copy(alpha = 0.6f)), CircleShape)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = CyberTeal,
                    modifier = Modifier.size(54.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "VoxBridge",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 38.sp,
                color = CyberTeal,
                letterSpacing = 2.sp,
                modifier = Modifier.graphicsLayer(alpha = alphaText)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Live Call Translator",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.graphicsLayer(alpha = alphaText)
            )
        }

        // Beautiful proud Benin visual signifier
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .graphicsLayer(alpha = alphaMadeInBenin, translationY = translationYMadeInBenin)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF008751)))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFCD116)))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE8112D)))
                    }
                    
                    Text(
                        text = "Made in Benin 🇧🇯",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

private fun playStartupChime() {
    Thread {
        try {
            val sampleRate = 44100
            val duration1 = 0.22 // seconds
            val duration2 = 0.45 // seconds
            val numSamples1 = (duration1 * sampleRate).toInt()
            val numSamples2 = (duration2 * sampleRate).toInt()
            val totalSamples = numSamples1 + numSamples2
            val generatedSnd = ShortArray(totalSamples)

            // Tone 1: E5 (659.25 Hz)
            for (i in 0 until numSamples1) {
                val t = i.toDouble() / sampleRate
                val angle = 2.0 * Math.PI * 659.25 * t
                val envelope = if (i > numSamples1 - 1500) (numSamples1 - i).toDouble() / 1500.0 else 1.0
                generatedSnd[i] = (Math.sin(angle) * 14000.0 * envelope).toInt().toShort()
            }

            // Tone 2: A5 (880.0 Hz)
            for (i in 0 until numSamples2) {
                val t = i.toDouble() / sampleRate
                val angle = 2.0 * Math.PI * 880.0 * t
                val envelope = if (i > numSamples2 - 4000) (numSamples2 - i).toDouble() / 4000.0 else 1.0
                generatedSnd[numSamples1 + i] = (Math.sin(angle) * 18000.0 * envelope).toInt().toShort()
            }

            val audioTrack = android.media.AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                totalSamples * 2,
                android.media.AudioTrack.MODE_STATIC
            )
            audioTrack.write(generatedSnd, 0, totalSamples)
            audioTrack.play()
            Thread.sleep(1000)
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}

private fun generateSecureRoomCode(): String {
    val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" // Highly distinct alphanumeric chars, avoiding 1, 0, I, O to stop human confusion
    return (1..8).map { chars.random() }.joinToString("")
}

