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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    val isTranslationActive by viewModel.isTranslationActive.collectAsStateWithLifecycle()
    val currentOriginalText by viewModel.currentOriginalText.collectAsStateWithLifecycle()
    val currentTranslatedText by viewModel.currentTranslatedText.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val currentSpeaker by viewModel.currentSpeaker.collectAsStateWithLifecycle()

    val callSessions by viewModel.callHistory.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val detailedMessages by viewModel.detailedMessages.collectAsStateWithLifecycle()
    val selectedSessionForDetails by viewModel.selectedSessionIdForDetails.collectAsStateWithLifecycle()

    var overlayChecked by remember { mutableStateOf(false) }

    // Check overlay permission regularly
    LaunchedEffect(Unit) {
        overlayChecked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

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
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
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
                    dialectsEnabled = settings.localDialectEnabled
                )
                1 -> HistoryScreen(
                    sessions = callSessions,
                    onSelectSession = { viewModel.selectSessionForDetails(it) },
                    onDeleteSession = { viewModel.deleteSession(it) }
                )
                2 -> SettingsScreen(
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
    dialectsEnabled: Boolean
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
                        label = { Text("Nom du contact") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberTeal,
                            focusedLabelColor = CyberTeal
                        ),
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
                Text("Gemini 3.5 Ready", fontSize = 10.sp, color = CyberTeal, fontWeight = FontWeight.Bold)
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
                "Soutien des Dialectes Locaux" to "Optimisé par Gemini 3.5, l'application comprend parfaitement les dialectes d'Afrique de l'Ouest (Fon, Yoruba, Wolof, Éwé) sans latence.",
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
