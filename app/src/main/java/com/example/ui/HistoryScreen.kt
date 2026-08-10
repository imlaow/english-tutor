package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.R
import com.example.ui.theme.Accent100
import com.example.ui.theme.Neutral200
import com.example.ui.theme.Neutral500
import com.example.ui.theme.Neutral600
import com.example.ui.theme.Neutral700
import java.text.SimpleDateFormat
import java.util.*

/** `--radius-lg`: the history cards use the same 28dp as the topic cards. */
private val HistoryCardShape = RoundedCornerShape(28.dp)

/** `--shadow-sm` (`0 1 2` @14%) as a Compose elevation. */
private val ShadowSm = 2.dp

/** `line-height: 1.45` on the two 14sp card lines, tighter than the 1.55 body default. */
private val CardLineHeight = 20.3.sp

@Composable
fun HistoryScreen(viewModel: ChatViewModel, navController: NavController) {
    val history by viewModel.history.collectAsStateWithLifecycle(emptyList())

    // One entry per conversation, newest first. groupBy keeps the oldest-first
    // message order inside each conversation.
    val conversations = history.groupBy { it.sessionId }.values.toList().asReversed()

    // Session id of the conversation awaiting delete confirmation, if any.
    var sessionPendingDelete by remember { mutableStateOf<String?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    sessionPendingDelete?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { sessionPendingDelete = null },
            title = { Text("Delete conversation?") },
            text = { Text("This conversation will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(sessionId)
                    sessionPendingDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Delete all history?") },
            text = { Text("All practice conversations will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllHistory()
                    showClearAllDialog = false
                }) {
                    Text("Delete all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    HistoryContent(
        conversations = conversations,
        onBack = { navController.safePopBackStack() },
        // Both destructive actions only arm a dialog; the delete itself happens in
        // the confirm button above.
        onClearAll = { showClearAllDialog = true },
        onDeleteSession = { sessionId -> sessionPendingDelete = sessionId },
        onOpenSession = { sessionId ->
            viewModel.resumeSession(sessionId)
            navController.safePopBackStack()
        }
    )
}

/**
 * The screen without its ViewModel: the warm top bar over either the saved
 * conversations or the empty state.
 *
 * Visible to the module (not private) for the same reason [TopicSuggestions] and
 * [MessageStream] are — the screenshot specimens render it directly, and it takes
 * plain values and lambdas.
 *
 * @param conversations grouped by session, newest first, each still in oldest-first
 *   message order.
 */
@Composable
internal fun HistoryContent(
    conversations: List<List<ChatMessage>>,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenSession: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WarmTopBar(
            title = "Practice history",
            navigation = {
                IconButton44(
                    icon = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Back",
                    onClick = onBack
                )
            },
            actions = {
                if (conversations.isNotEmpty()) {
                    IconButton44(
                        icon = painterResource(R.drawable.ic_trash),
                        contentDescription = "Delete all history",
                        onClick = onClearAll,
                        iconSize = 20.dp,
                        tint = Neutral600
                    )
                }
            }
        )

        if (conversations.isEmpty()) {
            EmptyHistory(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(conversations, key = { it.first().sessionId }) { conversation ->
                    val firstMessage = conversation.first()
                    HistoryCard(
                        date = dateFormat.format(Date(firstMessage.timestamp)),
                        exchangeLabel = if (conversation.size == 1) {
                            "1 exchange"
                        } else {
                            "${conversation.size} exchanges"
                        },
                        userText = firstMessage.userText,
                        tutorText = conversation.last().aiResponse,
                        onClick = { onOpenSession(firstMessage.sessionId) },
                        onDelete = { onDeleteSession(firstMessage.sessionId) }
                    )
                }
            }
        }
    }
}

/**
 * One saved conversation: when it happened and how long it ran, then its opening
 * learner line and the tutor's latest reply.
 *
 * The design dropped the "You: " / "Tutor: " text prefixes in favour of the two
 * lettered circles, so the speaker is carried by [AvatarBadge] alone.
 */
@Composable
private fun HistoryCard(
    date: String,
    exchangeLabel: String,
    userText: String,
    tutorText: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(ShadowSm, HistoryCardShape)
            .background(
                color = if (pressed) Accent100 else MaterialTheme.colorScheme.surface,
                shape = HistoryCardShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Pill(
                text = date,
                backgroundColor = Neutral200,
                contentColor = Neutral700,
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = exchangeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = Neutral600
            )
            IconButton44(
                icon = painterResource(R.drawable.ic_trash),
                contentDescription = "Delete conversation",
                onClick = onDelete,
                iconSize = 15.dp,
                tint = Neutral500,
                size = 26.dp
            )
        }
        Spacer(modifier = Modifier.height(11.dp))
        // A conversation opened from a topic has no learner utterance in its first
        // turn, which is the design's `sc-if s.hasUser`.
        if (userText.isNotBlank()) {
            SpeakerLine(
                letter = "Y",
                badgeBackground = MaterialTheme.colorScheme.primaryContainer,
                badgeContent = MaterialTheme.colorScheme.onPrimaryContainer,
                text = userText,
                textColor = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(7.dp))
        }
        SpeakerLine(
            letter = "T",
            badgeBackground = MaterialTheme.colorScheme.secondaryContainer,
            badgeContent = MaterialTheme.colorScheme.onSecondaryContainer,
            text = tutorText,
            textColor = Neutral700,
            fontWeight = FontWeight.Normal,
            maxLines = 2
        )
    }
}

/** An 18dp speaker circle beside one clamped line of what was said. */
@Composable
private fun SpeakerLine(
    letter: String,
    badgeBackground: Color,
    badgeContent: Color,
    text: String,
    textColor: Color,
    fontWeight: FontWeight,
    maxLines: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AvatarBadge(
            letter = letter,
            backgroundColor = badgeBackground,
            contentColor = badgeContent,
            size = 18.dp,
            // The circle sits on the text's first line rather than its line box.
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = CardLineHeight,
            fontWeight = fontWeight,
            color = textColor,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Shown until the first conversation is saved. The heading keeps the wording the
 * screen has always used; the line under it says where sessions come from, since
 * the handoff's own sentence there is placeholder copy in another language.
 */
@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = null,
                tint = Neutral500,
                modifier = Modifier.size(30.dp)
            )
        }
        // The 10dp column gap plus the heading's own 6dp top margin.
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No history yet.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}
