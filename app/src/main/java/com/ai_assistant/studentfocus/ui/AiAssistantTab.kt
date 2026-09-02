package com.ai_assistant.studentfocus.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.ai_assistant.studentfocus.viewmodel.GemmaViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

// ─────────────────────────────────────────────────────────────────────────────
// Markdown Parser for Clean & Bold Typography
// ─────────────────────────────────────────────────────────────────────────────
fun parseMarkdownToAnnotatedString(text: String, isUser: Boolean): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd()
            
            when {
                line.startsWith("### ") -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isUser) Color.White else Color(0xFFC7D2FE)
                        )
                    ) {
                        append(line.removePrefix("### "))
                    }
                }
                line.startsWith("## ") -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isUser) Color.White else Color(0xFFA5B4FC)
                        )
                    ) {
                        append(line.removePrefix("## "))
                    }
                }
                line.startsWith("# ") -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = if (isUser) Color.White else Color(0xFF818CF8)
                        )
                    ) {
                        append(line.removePrefix("# "))
                    }
                }
                line.startsWith("---") -> {
                    withStyle(SpanStyle(color = Color(0xFF475569))) {
                        append("────────────────────")
                    }
                }
                else -> {
                    parseInlineMarkdown(line, isUser)
                }
            }

            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.parseInlineMarkdown(line: String, isUser: Boolean) {
    val regex = Regex("""(\*\*(?:[^*]|\*(?!\*))+\*\*|\*[^*]+\*|`[^`]+`|~~[^~]+~~)""")
    var lastIndex = 0
    val matches = regex.findAll(line)
    
    for (match in matches) {
        if (match.range.first > lastIndex) {
            append(line.substring(lastIndex, match.range.first))
        }
        val token = match.value
        when {
            token.startsWith("**") && token.endsWith("**") && token.length >= 4 -> {
                val content = token.substring(2, token.length - 2)
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) Color.White else Color(0xFFF8FAFC)
                    )
                ) {
                    append(content)
                }
            }
            token.startsWith("*") && token.endsWith("*") && token.length >= 2 -> {
                val content = token.substring(1, token.length - 1)
                withStyle(
                    SpanStyle(
                        fontStyle = FontStyle.Italic,
                        color = if (isUser) Color(0xFFE2E8F0) else Color(0xFFCBD5E1)
                    )
                ) {
                    append(content)
                }
            }
            token.startsWith("`") && token.endsWith("`") && token.length >= 2 -> {
                val content = token.substring(1, token.length - 1)
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x337C3AED),
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(" $content ")
                }
            }
            token.startsWith("~~") && token.endsWith("~~") && token.length >= 4 -> {
                val content = token.substring(2, token.length - 2)
                withStyle(
                    SpanStyle(
                        textDecoration = TextDecoration.LineThrough,
                        color = Color(0xFF94A3B8)
                    )
                ) {
                    append(content)
                }
            }
            else -> {
                append(token)
            }
        }
        lastIndex = match.range.last + 1
    }
    
    if (lastIndex < line.length) {
        append(line.substring(lastIndex))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chat message model
// ─────────────────────────────────────────────────────────────────────────────
data class ChatMessage(val text: String, val isUser: Boolean)

// ─────────────────────────────────────────────────────────────────────────────
// MAIN COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantTab(gemmaViewModel: GemmaViewModel) {

    val modelState     by gemmaViewModel.modelState.collectAsState()
    val dlProgress     by gemmaViewModel.downloadProgress.collectAsState()
    val dlMB           by gemmaViewModel.downloadedMB.collectAsState()
    val totalMB        by gemmaViewModel.totalMB.collectAsState()
    val isGenerating   by gemmaViewModel.isGenerating.collectAsState()

    // Animate between Download screen and Chat screen
    AnimatedContent(
        targetState = modelState,
        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
        label = "screen_switch"
    ) { state ->
        when (state) {
            GemmaViewModel.ModelState.NOT_DOWNLOADED ->
                DownloadScreen(onDownload = { gemmaViewModel.downloadModel() })

            GemmaViewModel.ModelState.DOWNLOADING ->
                DownloadProgressScreen(progress = dlProgress, downloadedMB = dlMB, totalMB = totalMB)

            GemmaViewModel.ModelState.LOADING ->
                ModelLoadingScreen()

            GemmaViewModel.ModelState.READY -> {
                val chatHistory by gemmaViewModel.chatHistory.collectAsState()
                GemmaChatScreen(
                    isGenerating = isGenerating,
                    chatHistory = chatHistory,
                    onSend = { message, onPartial, onDone ->
                        gemmaViewModel.generateResponse(message, onPartial, onDone)
                    },
                    onDeleteMessage = { index ->
                        gemmaViewModel.deleteChatMessageAt(index)
                    }
                )
            }

            GemmaViewModel.ModelState.ERROR -> {
                val errorText by gemmaViewModel.errorText.collectAsState()
                ErrorScreen(
                    errorMessage = errorText ?: "Download failed",
                    onRetry = { gemmaViewModel.downloadModel() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. DOWNLOAD SCREEN — shown before model is downloaded
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DownloadScreen(onDownload: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // Glowing model logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF7C3AED).copy(alpha = 0.4f), Color.Transparent)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF4F46E5))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", fontSize = 36.sp)
                }
            }

            Text(
                text = "StudentOS AI Tutor",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "StudentOS offline AI assistant — runs 100% offline on your device.",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            // Feature list
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                FeatureRow("🧠", "Understands your questions naturally")
                FeatureRow("📚", "Trained on Math, Science, CS & more")
                FeatureRow("📵", "Fully offline — no internet needed after download")
                FeatureRow("🔒", "Private — data never leaves your device")
                FeatureRow("⚡", "Streaming responses — word by word")
            }

            // Size warning
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1917)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 18.sp)
                    Column {
                        Text(
                            text = "One-time download: ~1.5 GB",
                            color = Color(0xFFFBBF24),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Use Wi-Fi. Model stays on device permanently.",
                            color = Color(0xFF78716C),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Download button
            Button(
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED)
                )
            ) {
                Text(
                    text = "⬇  Download Gemma Model",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(emoji: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(text, color = Color(0xFFCBD5E1), fontSize = 13.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. DOWNLOAD PROGRESS SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DownloadProgressScreen(progress: Float, downloadedMB: Float, totalMB: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            // Spinning gear animation
            val rotation by rememberInfiniteTransition(label = "dl_spin").animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
                label = "spin"
            )
            Text("⚙️", fontSize = 64.sp, modifier = Modifier.rotate(rotation))

            Text(
                text = "Downloading Gemma...",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Progress bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF7C3AED),
                    trackColor = Color(0xFF1E293B)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color(0xFF7C3AED),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${"%.0f".format(downloadedMB)} / ${"%.0f".format(totalMB)} MB",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = "Please keep the app open.\nThis is a one-time download.",
                color = Color(0xFF475569),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. MODEL LOADING SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ModelLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFF7C3AED), strokeWidth = 3.dp)
            Text("Loading Gemma into memory...", color = Color(0xFF94A3B8), fontSize = 14.sp)
            Text("This may take 10–20 seconds", color = Color(0xFF475569), fontSize = 12.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. ERROR SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ErrorScreen(errorMessage: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("❌", fontSize = 48.sp)
            Text("Download Failed", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                text = errorMessage,
                color = Color(0xFFF87171),
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            Text(
                text = "Check your internet connection (Wi-Fi recommended) and try again.",
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text("↺  Retry Download")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. GEMMA CHAT SCREEN — replaces rule-based tutor after model is ready
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GemmaChatScreen(
    isGenerating: Boolean,
    chatHistory: List<Pair<String, Boolean>>,
    onSend: (String, onPartial: (String) -> Unit, onDone: () -> Unit) -> Unit,
    onDeleteMessage: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState      = rememberLazyListState()
    var inputQuery     by remember { mutableStateOf(TextFieldValue("")) }
    val clipboardManager = LocalClipboardManager.current
    val context        = LocalContext.current
    var messageToDeleteIndex by remember { mutableStateOf<Int?>(null) }

    // Dialog for deleting message on double-click
    if (messageToDeleteIndex != null) {
        AlertDialog(
            onDismissRequest = { messageToDeleteIndex = null },
            title = {
                Text("Delete Message?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Do you want to delete this message from your chat history?", color = Color(0xFFCBD5E1), fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        messageToDeleteIndex?.let { idx ->
                            onDeleteMessage(idx)
                            Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                        }
                        messageToDeleteIndex = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDeleteIndex = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Auto-scroll to bottom when new content arrives
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) listState.animateScrollToItem(chatHistory.size - 1)
    }

    fun sendMessage(text: String) {
        val q = text.trim()
        if (q.isBlank() || isGenerating) return
        inputQuery = TextFieldValue("")

        onSend(
            q,
            { _ ->
                coroutineScope.launch {
                    if (chatHistory.isNotEmpty()) listState.animateScrollToItem(chatHistory.size - 1)
                }
            },
            { /* isGenerating already reset in ViewModel */ }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header — shows GEMMA ONLINE badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF4F46E5))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) { Text("✨", fontSize = 20.sp) }
            Spacer(Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("StudentOS AI", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF16A34A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("OFFLINE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("StudentOS AI · Running on-device", fontSize = 11.sp, color = Color(0xFF7C3AED))
            }
        }

        // Chat area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(chatHistory) { index, msgPair ->
                val (msgText, isUser) = msgPair
                val align  = if (isUser) Alignment.End else Alignment.Start
                val shape  = if (isUser)
                    RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                else
                    RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
                    if (!isUser) {
                        Text(
                            "✨ StudentOS AI",
                            fontSize = 10.sp,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    if (msgText.isEmpty() && !isUser) {
                        // Streaming not started yet — show dots
                        TypingIndicator()
                    } else {
                        val cleanText = remember(msgText) { msgText.replace(Regex("<action[^>]*/>"), "").trim() }
                        val annotatedText = remember(cleanText, isUser) { parseMarkdownToAnnotatedString(cleanText, isUser) }
                        
                        Surface(
                            shape = shape,
                            color = if (isUser) Color.Transparent else Color(0xFF1E293B),
                            border = if (!isUser) BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.6f)) else null,
                            modifier = Modifier
                                .widthIn(max = 330.dp)
                                .then(
                                    if (isUser)
                                        Modifier.background(
                                            Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF4F46E5))),
                                            shape
                                        )
                                    else Modifier
                                )
                                .pointerInput(msgText) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            messageToDeleteIndex = index
                                        },
                                        onLongPress = {
                                            clipboardManager.setText(AnnotatedString(cleanText))
                                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                        ) {
                            Text(
                                text       = annotatedText,
                                color      = if (isUser) Color.White else Color(0xFFF1F5F9),
                                fontSize   = 14.sp,
                                lineHeight = 22.sp,
                                modifier   = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Input row
        Row(
            modifier  = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = inputQuery,
                onValueChange = { newVal ->
                    val hadDate = inputQuery.text.contains("@date")
                    inputQuery = newVal
                    if (newVal.text.contains("@date") && !hadDate) {
                        val calendar = Calendar.getInstance()
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCal = Calendar.getInstance()
                                selectedCal.set(Calendar.YEAR, year)
                                selectedCal.set(Calendar.MONTH, month)
                                selectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCal.time)
                                
                                val curText = inputQuery.text
                                val atIndex = curText.indexOf("@date")
                                val newText = if (atIndex >= 0) {
                                    curText.replaceFirst("@date", formattedDate)
                                } else {
                                    "$curText $formattedDate"
                                }
                                val newCursorPos = if (atIndex >= 0) atIndex + formattedDate.length else newText.length
                                inputQuery = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newCursorPos)
                                )
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                },
                placeholder   = { Text("Ask AI or type @help, @task, @routine, @date...", color = Color(0xFF475569), fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    focusedBorderColor   = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    cursorColor          = Color(0xFF7C3AED)
                ),
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
                maxLines = 4,
                enabled  = !isGenerating
            )

            Button(
                onClick  = { sendMessage(inputQuery.text) },
                enabled  = inputQuery.text.isNotBlank() && !isGenerating,
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF7C3AED),
                    disabledContainerColor = Color(0xFF334155)
                ),
                modifier = Modifier.height(56.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text("Send ➤", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated typing dots indicator
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier
            .background(Color(0xFF1E293B), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (0..2).forEach { i ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(500, delayMillis = i * 150, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                Modifier
                    .size(7.dp)
                    .background(Color(0xFF7C3AED).copy(alpha = alpha), CircleShape)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text("StudentOS AI is thinking...", color = Color(0xFF475569), fontSize = 11.sp)
    }
}
