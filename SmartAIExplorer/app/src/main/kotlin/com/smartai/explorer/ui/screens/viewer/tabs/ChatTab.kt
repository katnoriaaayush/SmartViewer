package com.smartai.explorer.ui.screens.viewer.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.ChatMessage
import com.smartai.explorer.domain.model.MessageRole
import com.smartai.explorer.ui.components.StreamingBubble
import com.smartai.explorer.ui.theme.ChatBlueSoft
import kotlinx.coroutines.launch

@Composable
fun ChatTab(
    messages:      List<ChatMessage>,
    streamingText: String,
    onSend:        (String) -> Unit,
    modifier:      Modifier = Modifier,
) {
    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()
    var input      by remember { mutableStateOf("") }
    val isBusy     = streamingText.isNotEmpty()

    LaunchedEffect(messages.size, streamingText) {
        val target = messages.size + if (streamingText.isNotEmpty()) 1 else 0
        if (target > 0) scope.launch { listState.animateScrollToItem(target - 1) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (messages.isEmpty() && streamingText.isEmpty()) {
            ChatEmptyState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                state               = listState,
                modifier            = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding      = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(message = msg)
                }
                if (streamingText.isNotEmpty()) {
                    item(key = "streaming") {
                        StreamingBubble(text = streamingText)
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Input bar
        Row(
            modifier              = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment     = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextField(
                value         = input,
                onValueChange = { input = it },
                placeholder   = {
                    Text("Ask about the document…", style = MaterialTheme.typography.bodyMedium)
                },
                modifier  = Modifier.weight(1f),
                shape     = RoundedCornerShape(24.dp),
                enabled   = !isBusy,
                maxLines  = 3,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors    = TextFieldDefaults.colors(
                    focusedContainerColor    = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor  = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor    = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor  = androidx.compose.ui.graphics.Color.Transparent,
                    disabledIndicatorColor   = androidx.compose.ui.graphics.Color.Transparent,
                ),
            )
            FilledIconButton(
                onClick  = { if (input.isNotBlank()) { onSend(input.trim()); input = "" } },
                enabled  = input.isNotBlank() && !isBusy,
                modifier = Modifier.size(52.dp),
                shape    = RoundedCornerShape(50),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ChatBlueSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Ask anything about this document",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text      = "Try “What are the key takeaways?” or select\ntext in the PDF to ask about it.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.user
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart    = 18.dp,
                topEnd      = 18.dp,
                bottomStart = if (isUser) 18.dp else 6.dp,
                bottomEnd   = if (isUser) 6.dp  else 18.dp,
            ),
            color    = if (isUser) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 360.dp),
        ) {
            Text(
                text     = message.content,
                style    = MaterialTheme.typography.bodyMedium,
                color    = if (isUser) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}
