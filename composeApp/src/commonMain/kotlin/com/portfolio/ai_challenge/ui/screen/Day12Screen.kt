package com.portfolio.ai_challenge.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.portfolio.ai_challenge.data.CommunicationPreferences
import com.portfolio.ai_challenge.data.MemoryLayersDebug
import com.portfolio.ai_challenge.data.PsyUserProfileDto
import com.portfolio.ai_challenge.ui.formatAiResponse
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Day12Screen(onBack: () -> Unit) {
    val viewModel: Day12ViewModel = koinViewModel()
    val sessionId by viewModel.sessionId.collectAsState()
    val userId by viewModel.userId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val memoryDebug by viewModel.memoryDebug.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val profileUpdates by viewModel.profileUpdates.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val listState = rememberLazyListState()
    var showProfileSheet by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    if (showProfileSheet && userId != null) {
        ProfileBottomSheet(
            profile = profile,
            onDismiss = { showProfileSheet = false },
            onSave = { prefs -> viewModel.savePreferences(userId!!, prefs); showProfileSheet = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 12: Personalization") },
                navigationIcon = { TextButton(onClick = onBack) { Text("\u2190 Back") } },
                actions = {
                    if (sessionId != null) {
                        TextButton(onClick = { showProfileSheet = true }) { Text("Profile") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = {
            if (sessionId != null) {
                var inputText by remember { mutableStateOf("") }
                Column {
                    Day12MemoryCard(memoryDebug = memoryDebug, profile = profile)
                    if (profileUpdates.isNotEmpty()) ProfileUpdatesChips(profileUpdates)
                    Day12QuickReplyChips(isLoading = isLoading, onSend = { viewModel.sendMessage(it) })
                    Day12ChatInput(
                        inputText = inputText,
                        isLoading = isLoading,
                        onInputChange = { inputText = it },
                        onSend = { viewModel.sendMessage(inputText); inputText = "" },
                    )
                }
            }
        },
    ) { padding ->
        if (sessionId == null) {
            Day12SessionStartScreen(
                isLoading = isLoading,
                error = error,
                modifier = Modifier.fillMaxSize().padding(padding),
                onStart = viewModel::startSession,
            )
        } else if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 32.dp),
                ) {
                    Text("\uD83E\uDDE0 MindGuard", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Personalized AI support.\nTap 'Profile' to set your communication preferences.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Day12MessageList(
                messages = messages,
                isLoading = isLoading,
                listState = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@Composable
private fun Day12SessionStartScreen(
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    onStart: (String) -> Unit,
) {
    var userId by remember { mutableStateOf("") }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text("\uD83C\uDFA8 MindGuard — Personalized", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("Communication style adapts to your preferences.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            OutlinedTextField(value = userId, onValueChange = { userId = it }, label = { Text("Your name or ID") }, placeholder = { Text("e.g. alice") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true)
            if (error != null) Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Button(onClick = { onStart(userId) }, enabled = userId.isNotBlank() && !isLoading, modifier = Modifier.fillMaxWidth()) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Start Session")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileUpdatesChips(updates: List<String>) {
    Surface(tonalElevation = 1.dp) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Detected: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            updates.forEach { update ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(update, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@Composable
private fun Day12QuickReplyChips(isLoading: Boolean, onSend: (String) -> Unit) {
    // Groups: label + phrases. Separated visually for easy manual testing.
    val groups = listOf(
        "Profile" to listOf(
            "My name is Katya",
            "Call me Dmytro",
            "I feel really anxious lately",
            "I cant sleep at night",
            "I feel sad and hopeless",
            "Work deadlines stress me out",
            "Having issues with my partner",
            "I feel so lonely and isolated",
        ),
        "Style" to listOf(
            "Please respond formally",
            "Be casual with me",
            "Give me very detailed explanations",
            "Keep responses very short please",
            "Switch to Ukrainian language",
        ),
        "Chat" to listOf(
            "hi",
            "Tell me more",
            "Yes, lets try that technique",
            "I feel better now, thank you",
            "What do you recommend for anxiety?",
            "Do I have depression?",
            "What medication should I take?",
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        groups.forEach { (label, phrases) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 16.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(phrases) { phrase ->
                        SuggestionChip(
                            onClick = { onSend(phrase) },
                            label = { Text(phrase, style = MaterialTheme.typography.labelSmall) },
                            enabled = !isLoading,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Day12ChatInput(
    inputText: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown && !event.isShiftPressed) {
                        onSend(); true
                    } else false
                },
                placeholder = { Text("Share what's on your mind...") },
                enabled = !isLoading,
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
            )
            FilledIconButton(onClick = onSend, enabled = inputText.isNotBlank() && !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("\u2191")
            }
        }
    }
}

@Composable
private fun Day12MemoryCard(memoryDebug: MemoryLayersDebug?, profile: PsyUserProfileDto?) {
    if (memoryDebug == null && profile == null) return
    var expanded by remember { mutableStateOf(false) }
    val prefs = profile?.preferences
    val liveProfileText = buildString {
        append("name: ${profile?.preferredName ?: "—"}")
        append("  |  concerns: ${profile?.primaryConcerns?.joinToString() ?: "—"}")
        append("\nformality: ${prefs?.formality ?: "—"}")
        append("  |  length: ${prefs?.responseLength ?: "—"}")
        append("  |  lang: ${prefs?.language ?: "—"}")
        if (prefs?.avoidTopics?.isNotEmpty() == true) append("\navoid: ${prefs.avoidTopics.joinToString()}")
    }
    Surface(tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("\uD83D\uDDC4 Memory", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show", style = MaterialTheme.typography.labelSmall)
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Live profile (always up to date, reflects preference changes immediately)
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Text(
                            text = liveProfileText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    // Raw debug from last LLM call (turn + session context)
                    if (memoryDebug != null) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Text(
                                text = "turn: ${memoryDebug.turn}\nsession: ${memoryDebug.session}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Day12MessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages) { message -> PsyChatBubble(message) }
        if (isLoading) {
            item {
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("MindGuard is thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PsyChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        ElevatedCard(
            modifier = Modifier.padding(start = if (isUser) 48.dp else 0.dp, end = if (isUser) 0.dp else 48.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        ) {
            SelectionContainer {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = if (isUser) "You" else "\uD83E\uDDE0 MindGuard", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f))
                    Text(text = if (isUser) message.text else formatAiResponse(message.text), style = MaterialTheme.typography.bodyMedium, color = textColor)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileBottomSheet(
    profile: PsyUserProfileDto?,
    onDismiss: () -> Unit,
    onSave: (CommunicationPreferences) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var language by remember { mutableStateOf(profile?.preferences?.language ?: "en") }
    var formality by remember { mutableStateOf(profile?.preferences?.formality ?: "INFORMAL") }
    var responseLength by remember { mutableStateOf(profile?.preferences?.responseLength ?: "MEDIUM") }
    val avoidTopics = remember { mutableStateListOf<String>().also { list -> profile?.preferences?.avoidTopics?.let(list::addAll) } }
    var avoidInput by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Communication Preferences", style = MaterialTheme.typography.titleMedium)

            if (profile?.preferredName != null) {
                Text("Name: ${profile.preferredName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }

            OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Language (e.g. en, uk)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Text("Formality", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("FORMAL", "INFORMAL", "MIXED").forEach { option ->
                    FilterChip(selected = formality == option, onClick = { formality = option }, label = { Text(option.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }

            Text("Response Length", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SHORT", "MEDIUM", "DETAILED").forEach { option ->
                    FilterChip(selected = responseLength == option, onClick = { responseLength = option }, label = { Text(option.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }

            Text("Avoid Topics", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = avoidInput,
                    onValueChange = { avoidInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add topic...") },
                    singleLine = true,
                )
                TextButton(onClick = { if (avoidInput.isNotBlank()) { avoidTopics.add(avoidInput.trim()); avoidInput = "" } }) { Text("Add") }
            }
            if (avoidTopics.isNotEmpty()) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    avoidTopics.forEach { topic ->
                        InputChip(selected = false, onClick = { avoidTopics.remove(topic) }, label = { Text(topic) })
                    }
                }
            }

            HorizontalDivider()
            Button(
                onClick = { onSave(CommunicationPreferences(language = language, formality = formality, responseLength = responseLength, avoidTopics = avoidTopics.toList())) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save Preferences") }
        }
    }
}
