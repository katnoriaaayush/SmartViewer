package com.smartai.explorer.ui.screens.viewer.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.ChatMessage
import com.smartai.explorer.domain.model.MessageRole
import com.smartai.explorer.ui.components.StreamingBubble
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
        LazyColumn(
            state   = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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

        HorizontalDivider()

        Row(
            modifier              = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value         = input,
                onValueChange = { input = it },
                placeholder   = { Text("Ask about the document…") },
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(16.dp),
                enabled       = !isBusy,
                maxLines      = 3,
                textStyle     = MaterialTheme.typography.bodyLarge,
            )
            IconButton(
                onClick  = { if (input.isNotBlank()) { onSend(input.trim()); input = "" } },
                enabled  = input.isNotBlank() && !isBusy,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
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
            shape  = RoundedCornerShape(
                topStart    = if (isUser) 16.dp else 4.dp,
                topEnd      = if (isUser) 4.dp  else 16.dp,
                bottomStart = 16.dp,
                bottomEnd   = 16.dp,
            ),
            color          = if (isUser) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier       = Modifier.widthIn(max = 360.dp),
        ) {
            Text(
                text     = message.content,
                style    = MaterialTheme.typography.bodyLarge,
                color    = if (isUser) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}
