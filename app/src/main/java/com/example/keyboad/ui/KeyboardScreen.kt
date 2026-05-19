package com.example.keyboad.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboad.KeyboardLayoutType
import com.example.keyboad.KeyboardUiState
import com.example.keyboad.ShiftState
import kotlinx.coroutines.delay

private val KeyboardBg = Color(0xFF17191D)
private val NormalKeyBg = Color(0xFF303134)
private val ActionKeyBg = Color(0xFF3C4043)
private val PressedKeyBg = Color(0xFF4D5157)
private val HintColor = Color(0xFF9BA0A8)
private val KeyTextColor = Color.White
private val VoiceActiveColor = Color(0xFFEA4335)

private val letterRows = listOf(
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
    listOf("z", "x", "c", "v", "b", "n", "m")
)

private val symbolRows1 = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"),
    listOf("=", "\\", "<", "*", "\"", "'", ":", ";", "!", "?")
)

private val symbolRows2 = listOf(
    listOf("~", "`", "|", "•", "√", "π", "÷", "×", "§", "∆"),
    listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\"),
    listOf("%", "©", "®", "™", "✓", "[", "]")
)

private val hints = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

@Composable
fun KeyboardScreen(
    state: KeyboardUiState,
    onKeyClick: (String) -> Unit,
    onDeleteClick: (Boolean) -> Unit,
    onSpaceClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onTranslateClick: () -> Unit,
    onShiftClick: () -> Unit,
    onLayoutToggle: () -> Unit,
    onSymbols1Toggle: () -> Unit = {},
    onSymbols2Toggle: () -> Unit = {},
    onNumpadToggle: () -> Unit = {},
    onEmojiToggle: () -> Unit = {},
    onEmojiClicked: (String) -> Unit = {},
    onCursorMove: (Int) -> Unit,
    onSwipeDelete: (Int) -> Unit,
    onClipboardToggle: () -> Unit,
    onClipboardItemClick: (String) -> Unit,
    onDownloadDictionary: () -> Unit,
    onVoiceInputClick: () -> Unit
) {
    val gesturePoints = remember { mutableStateListOf<Offset>() }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(258.dp)
            .background(KeyboardBg)
            .pointerInput(Unit) {
                // Use PointerEventPass.Initial to track gestures even if children consume them
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.first()
                        
                        if (change.changedToDown()) {
                            gesturePoints.clear()
                            gesturePoints.add(change.position)
                        } else if (change.pressed) {
                            gesturePoints.add(change.position)
                            if (gesturePoints.size > 25) gesturePoints.removeAt(0)
                        } else if (change.changedToUp()) {
                            gesturePoints.clear()
                        }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SuggestionBar(
                suggestions = state.suggestions,
                emojiSuggestions = state.emojiSuggestions,
                isLoading = state.isLoading,
                isListening = state.isListening,
                isDictionaryDownloaded = state.isDictionaryDownloaded,
                errorMessage = state.errorMessage,
                onSuggestionClick = onSuggestionClick,
                onTranslateClick = onTranslateClick,
                onClipboardToggle = onClipboardToggle,
                onDownloadDictionary = onDownloadDictionary,
                onVoiceInputClick = onVoiceInputClick,
                accentColor = state.themeColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (state.isClipboardOpen) {
                    ClipboardView(
                        history = state.clipboardHistory,
                        onItemClick = onClipboardItemClick,
                        onClose = onClipboardToggle
                    )
                } else if (state.isEmojiPickerOpen) {
                    EmojiPickerView(
                        categorizedEmojis = state.categorizedEmojis,
                        recentEmojis = state.recentEmojis,
                        onEmojiClick = onEmojiClicked,
                        onDeleteClick = onDeleteClick,
                        onClose = onEmojiToggle,
                        accentColor = state.themeColor
                    )
                } else {
                    KeyboardGrid(
                        layoutType = state.layoutType,
                        shiftState = state.shiftState,
                        onKeyClick = onKeyClick,
                        onDeleteClick = onDeleteClick,
                        onShiftClick = onShiftClick,
                        onLayoutToggle = onLayoutToggle,
                        onSymbols1Toggle = onSymbols1Toggle,
                        onSymbols2Toggle = onSymbols2Toggle,
                        onNumpadToggle = onNumpadToggle,
                        onEmojiToggle = onEmojiToggle,
                        onSpaceClick = onSpaceClick,
                        onEnterClick = onEnterClick,
                        onCursorMove = onCursorMove,
                        onSwipeDelete = onSwipeDelete,
                        accentColor = state.themeColor
                    )
                }

                // Gesture Trail Overlay (Always on top)
                if (gesturePoints.isNotEmpty()) {
                    GestureTrail(points = gesturePoints, color = state.themeColor)
                }
            }
        }
    }
}

@Composable
private fun GestureTrail(points: List<Offset>, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    // Smooth quadratic curves would be better, but lineTo is faster for initial test
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = path,
                color = color.copy(alpha = 0.6f),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun EmojiPickerView(
    categorizedEmojis: Map<String, List<String>>,
    recentEmojis: List<String>,
    onEmojiClick: (String) -> Unit,
    onDeleteClick: (Boolean) -> Unit,
    onClose: () -> Unit,
    accentColor: Color
) {
    val categories = listOf(
        Icons.Default.History to "Recent",
        Icons.Default.SentimentSatisfiedAlt to "Smileys",
        Icons.Default.EmojiPeople to "People",
        Icons.Default.EmojiNature to "Nature",
        Icons.Default.Fastfood to "Food",
        Icons.Default.DirectionsCar to "Travel"
    )

    Column(modifier = Modifier.fillMaxSize().background(KeyboardBg)) {
        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = KeyTextColor,
                modifier = Modifier.size(24.dp).clickable { onClose() }
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(18.dp)).background(NormalKeyBg).padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = HintColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Search emojis", color = HintColor, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            categories.take(3).forEach { (icon, _) ->
                Icon(icon, null, tint = HintColor, modifier = Modifier.size(22.dp).padding(horizontal = 4.dp))
            }
        }

        // EMOJI LIST
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            if (recentEmojis.isNotEmpty()) {
                item {
                    Text("Recent Emoji", color = HintColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
                item {
                    EmojiGrid(emojis = recentEmojis, onEmojiClick = onEmojiClick, rows = (recentEmojis.size + 7) / 8)
                }
            }

            if (categorizedEmojis.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Loading emojis...", color = HintColor)
                    }
                }
            } else {
                categorizedEmojis.forEach { (category, emojis) ->
                    item {
                        Text(category, color = HintColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    item {
                        EmojiGrid(emojis = emojis, onEmojiClick = onEmojiClick, rows = (emojis.size + 7) / 8)
                    }
                }
            }
        }

        // BOTTOM BAR
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).background(KeyboardBg).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "ABC",
                color = KeyTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onClose() }.padding(horizontal = 12.dp)
            )
            
            Box(
                modifier = Modifier.size(70.dp, 36.dp).clip(RoundedCornerShape(18.dp)).background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SentimentSatisfiedAlt, null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            
            Text("GIF", color = HintColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.StickyNote2, null, tint = HintColor, modifier = Modifier.size(20.dp))
            Text(":-)", color = HintColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            
            RepeatingActionKey(
                icon = Icons.Default.Backspace,
                onAction = onDeleteClick,
                modifier = Modifier.width(48.dp).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun EmojiGrid(emojis: List<String>, onEmojiClick: (String) -> Unit, rows: Int) {
    val gridHeight = (rows * 40).dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = Modifier.height(gridHeight),
        userScrollEnabled = false
    ) {
        items(emojis) { emoji ->
            Box(
                modifier = Modifier.aspectRatio(1f).clickable { onEmojiClick(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun ClipboardView(
    history: List<String>,
    onItemClick: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(KeyboardBg).padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Clipboard History", color = HintColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = HintColor,
                modifier = Modifier.size(20.dp).clickable { onClose() }
            )
        }
        HorizontalDivider(color = HintColor.copy(alpha = 0.2f))
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your clipboard is empty", color = HintColor.copy(alpha = 0.5f), fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(history) { item ->
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(8.dp)).background(NormalKeyBg).clickable { onItemClick(item) }.padding(12.dp)
                    ) {
                        Text(text = item, color = KeyTextColor, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardGrid(
    layoutType: KeyboardLayoutType,
    shiftState: ShiftState,
    onKeyClick: (String) -> Unit,
    onDeleteClick: (Boolean) -> Unit,
    onShiftClick: () -> Unit,
    onLayoutToggle: () -> Unit,
    onSymbols1Toggle: () -> Unit,
    onSymbols2Toggle: () -> Unit,
    onNumpadToggle: () -> Unit,
    onEmojiToggle: () -> Unit,
    onSpaceClick: () -> Unit,
    onEnterClick: () -> Unit,
    onCursorMove: (Int) -> Unit,
    onSwipeDelete: (Int) -> Unit,
    accentColor: Color
) {
    if (layoutType == KeyboardLayoutType.NUMPAD) {
        NumpadLayout(onKeyClick, onDeleteClick, onLayoutToggle, onEnterClick, onSymbols1Toggle)
        return
    }

    val currentRows = when (layoutType) {
        KeyboardLayoutType.LETTERS -> letterRows
        KeyboardLayoutType.SYMBOLS_1 -> symbolRows1
        KeyboardLayoutType.SYMBOLS_2 -> symbolRows2
        else -> letterRows
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(54.dp), horizontalArrangement = Arrangement.Center) {
            currentRows[0].forEachIndexed { i, key ->
                val displayKey = if (shiftState != ShiftState.OFF && layoutType == KeyboardLayoutType.LETTERS) key.uppercase() else key
                KeyButton(text = displayKey, hint = if (layoutType == KeyboardLayoutType.LETTERS) hints[i] else null, onClick = { onKeyClick(displayKey) }, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
        Row(modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = if (layoutType == KeyboardLayoutType.LETTERS) 18.dp else 0.dp, vertical = 2.dp), horizontalArrangement = Arrangement.Center) {
            currentRows[1].forEach { key ->
                val displayKey = if (shiftState != ShiftState.OFF && layoutType == KeyboardLayoutType.LETTERS) key.uppercase() else key
                KeyButton(text = displayKey, onClick = { onKeyClick(displayKey) }, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
        Row(modifier = Modifier.fillMaxWidth().height(54.dp).padding(vertical = 1.dp), horizontalArrangement = Arrangement.Center) {
            if (layoutType == KeyboardLayoutType.LETTERS) {
                ActionKey(icon = if (shiftState == ShiftState.OFF) Icons.Outlined.ArrowUpward else Icons.Default.ArrowUpward, onClick = onShiftClick, modifier = Modifier.weight(1.5f).fillMaxHeight(), isActive = shiftState != ShiftState.OFF, isCapsLock = shiftState == ShiftState.CAPS_LOCK, accentColor = accentColor)
            } else {
                KeyButton(text = if (layoutType == KeyboardLayoutType.SYMBOLS_1) "=\\<" else "?123", onClick = if (layoutType == KeyboardLayoutType.SYMBOLS_1) onSymbols2Toggle else onSymbols1Toggle, modifier = Modifier.weight(1.5f).fillMaxHeight(), color = ActionKeyBg, fontSize = 14.sp)
            }
            currentRows[2].forEach { key ->
                val displayKey = if (shiftState != ShiftState.OFF && layoutType == KeyboardLayoutType.LETTERS) key.uppercase() else key
                KeyButton(text = displayKey, onClick = { onKeyClick(displayKey) }, modifier = Modifier.weight(1f).fillMaxHeight())
            }
            RepeatingActionKey(icon = Icons.Default.Backspace, onAction = onDeleteClick, onSwipe = onSwipeDelete, modifier = Modifier.weight(1.5f).fillMaxHeight())
        }
        Row(modifier = Modifier.fillMaxWidth().height(54.dp).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            KeyButton(text = if (layoutType == KeyboardLayoutType.LETTERS) "?123" else "ABC", onClick = onLayoutToggle, modifier = Modifier.weight(1.4f).fillMaxHeight(), color = ActionKeyBg, fontSize = 14.sp, shape = RoundedCornerShape(27.dp))
            if (layoutType == KeyboardLayoutType.LETTERS) {
                KeyButton(text = ",", hint = "🙂", onClick = { onKeyClick(",") }, onLongClick = onEmojiToggle, modifier = Modifier.weight(1.1f).fillMaxHeight(), color = KeyboardBg, fontSize = 18.sp)
                ActionKey(icon = Icons.Default.Language, onClick = { }, modifier = Modifier.weight(1.1f).fillMaxHeight(), color = KeyboardBg)
            } else {
                KeyButton(text = if (layoutType == KeyboardLayoutType.SYMBOLS_1) "," else "<", onClick = { if (layoutType == KeyboardLayoutType.SYMBOLS_1) onKeyClick(",") else onCursorMove(-1) }, modifier = Modifier.weight(1.1f).fillMaxHeight(), color = if (layoutType == KeyboardLayoutType.SYMBOLS_1) KeyboardBg else ActionKeyBg, shape = if (layoutType == KeyboardLayoutType.SYMBOLS_1) RoundedCornerShape(6.dp) else RoundedCornerShape(27.dp))
                KeyButton(text = "12\n34", onClick = onNumpadToggle, modifier = Modifier.weight(1.1f).fillMaxHeight(), color = ActionKeyBg, fontSize = 10.sp)
            }
            SpaceKey(onSpaceClick = onSpaceClick, onCursorMove = onCursorMove, modifier = Modifier.weight(4.2f).fillMaxHeight())
            if (layoutType == KeyboardLayoutType.SYMBOLS_2) {
                ActionKey(icon = Icons.Default.ChevronRight, onClick = { onCursorMove(1) }, modifier = Modifier.weight(1.1f).fillMaxHeight(), color = ActionKeyBg)
            } else {
                KeyButton(text = ".", onClick = { onKeyClick(".") }, modifier = Modifier.weight(1.1f).fillMaxHeight(), color = KeyboardBg)
            }
            ActionKey(icon = Icons.Default.KeyboardReturn, onClick = onEnterClick, modifier = Modifier.weight(1.4f).fillMaxHeight(), color = ActionKeyBg, shape = RoundedCornerShape(27.dp), accentColor = accentColor)
        }
    }
}

@Composable
private fun NumpadLayout(
    onKeyClick: (String) -> Unit,
    onDeleteClick: (Boolean) -> Unit,
    onLayoutToggle: () -> Unit,
    onEnterClick: () -> Unit,
    onSymbols1Toggle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(modifier = Modifier.weight(0.8f).fillMaxHeight()) {
                listOf("+", "-", "*", "/").forEach { op ->
                    KeyButton(text = op, onClick = { onKeyClick(op) }, modifier = Modifier.weight(1f).fillMaxWidth(), color = ActionKeyBg)
                }
            }
            Column(modifier = Modifier.weight(3f).fillMaxHeight()) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        row.forEach { num ->
                            KeyButton(text = num, onClick = { onKeyClick(num) }, modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(0.8f).fillMaxHeight()) {
                KeyButton(text = "%", onClick = { onKeyClick("%") }, modifier = Modifier.weight(1f).fillMaxWidth(), color = ActionKeyBg)
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(3.dp).clip(RoundedCornerShape(6.dp)).background(ActionKeyBg).clickable { onKeyClick(" ") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SpaceBar, null, tint = KeyTextColor, modifier = Modifier.size(20.dp))
                }
                RepeatingActionKey(icon = Icons.Default.Backspace, onAction = onDeleteClick, modifier = Modifier.weight(1f).fillMaxWidth())
            }
        }
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 2.dp)) {
            KeyButton("ABC", onClick = onLayoutToggle, modifier = Modifier.weight(1f).fillMaxHeight(), color = ActionKeyBg, fontSize = 14.sp, shape = RoundedCornerShape(24.dp))
            KeyButton(",", onClick = { onKeyClick(",") }, modifier = Modifier.weight(0.8f).fillMaxHeight(), color = ActionKeyBg)
            KeyButton("!?#", onClick = onSymbols1Toggle, modifier = Modifier.weight(1f).fillMaxHeight(), color = ActionKeyBg, fontSize = 12.sp)
            KeyButton("0", onClick = { onKeyClick("0") }, modifier = Modifier.weight(1.2f).fillMaxHeight())
            KeyButton("=", onClick = { onKeyClick("=") }, modifier = Modifier.weight(0.8f).fillMaxHeight(), color = ActionKeyBg)
            KeyButton(".", onClick = { onKeyClick(".") }, modifier = Modifier.weight(0.8f).fillMaxHeight(), color = ActionKeyBg)
            ActionKey(Icons.Default.KeyboardReturn, onClick = onEnterClick, modifier = Modifier.weight(1f).fillMaxHeight(), color = ActionKeyBg, shape = RoundedCornerShape(24.dp))
        }
    }
}

@Composable
private fun KeyButton(
    text: String,
    hint: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    color: Color = NormalKeyBg,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp)
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.padding(horizontal = 3.dp, vertical = 2.dp).clip(shape).background(if (isPressed) PressedKeyBg else color)
            .pointerInput(text) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isPressed = true
                    
                    var isUp = false
                    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                    
                    val result = withTimeoutOrNull(longPressTimeout) {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.none { it.pressed }) {
                                isUp = true
                                break
                            }
                        }
                    }
                    
                    if (result == null) {
                        onLongClick?.invoke()
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.none { it.pressed }) break
                        }
                    } else if (isUp) {
                        onClick()
                    }
                    
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (hint != null) {
            Text(text = hint, color = HintColor.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp))
        }
        Text(text = text, color = KeyTextColor, fontSize = fontSize, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center, lineHeight = fontSize)
    }
}

@Composable
private fun SpaceKey(
    onSpaceClick: () -> Unit,
    onCursorMove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    // Efficiency 1: Avoid `mutableFloatStateOf` for high-frequency updates.
    // The Compose snapshot system incurs overhead tracking State. 
    // Since this value is only needed between the pointer event loop and LaunchedEffect 
    // (and doesn't directly drive UI composition), a primitive array is much faster.
    val dragTracker = remember { floatArrayOf(0f) }
    val dragThreshold = 35f

    LaunchedEffect(isPressed) {
        if (isPressed) {
            while (isPressed) {
                val currentDragX = dragTracker[0]
                if (Math.abs(currentDragX) > 60f) {
                    val steps = if (currentDragX > 0) 1 else -1
                    onCursorMove(steps)
                    delay(100L)
                } else { 
                    delay(50L) 
                }
            }
        }
    }

    Box(
        modifier = modifier
            .padding(horizontal = 3.dp, vertical = 2.dp)
            .background(if (isPressed) PressedKeyBg else NormalKeyBg, RoundedCornerShape(27.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isPressed = true
                    dragTracker[0] = 0f
                    var dragEvent = down

                    while (true) {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.pressed }
                        if (!anyPressed) { 
                            isPressed = false
                            break 
                        }

                        val move = event.changes.first()
                        dragTracker[0] += move.position.x - dragEvent.position.x
                        dragEvent = move
                        
                        val currentDrag = dragTracker[0]
                        if (Math.abs(currentDrag) > dragThreshold) {
                            // Efficiency 2: Consume the event once we exceed the threshold.
                            // This signals to standard Modifiers (like .clickable) that we 
                            // have taken over the gesture, saving them from redundant processing.
                            move.consume()
                            
                            val steps = (currentDrag / dragThreshold).toInt()
                            if (steps != 0) {
                                onCursorMove(steps)
                                dragTracker[0] -= steps * dragThreshold
                            }
                        }
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, 
                indication = null, 
                onClick = onSpaceClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "English", color = KeyTextColor, fontSize = 14.sp, fontWeight = FontWeight.Normal)
    }
}

@Suppress("DEPRECATION")
@Composable
private fun RepeatingActionKey(
    icon: ImageVector,
    onAction: (Boolean) -> Unit,
    onSwipe: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var totalDrag by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = -60f
    LaunchedEffect(isPressed) {
        if (isPressed) {
            onAction(false)
            delay(400)
            while (isPressed) {
                onAction(false)
                delay(60L)
            }
            onAction(true)
        }
    }
    Box(
        modifier = modifier.padding(horizontal = 3.dp, vertical = 2.dp).background(if (isPressed) PressedKeyBg else ActionKeyBg, RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isPressed = true
                    totalDrag = 0f
                    var lastX = down.position.x
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.pressed }
                        if (!anyPressed) { isPressed = false; break }
                        val move = event.changes.first()
                        totalDrag += move.position.x - lastX
                        lastX = move.position.x
                        if (totalDrag < swipeThreshold) { onSwipe?.invoke(1); totalDrag = 0f }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = KeyTextColor, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ActionKey(
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = ActionKeyBg,
    isActive: Boolean = false,
    isCapsLock: Boolean = false,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp),
    accentColor: Color = Color(0xFF8AB4F8)
) {
    var isPressed by remember { mutableStateOf(false) }
    val backgroundColor = when {
        isCapsLock -> accentColor
        isActive -> accentColor.copy(alpha = 0.7f)
        isPressed -> PressedKeyBg
        else -> color
    }
    Box(
        modifier = modifier.padding(horizontal = 3.dp, vertical = 2.dp).background(backgroundColor, shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; try { awaitRelease() } finally { isPressed = false } },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isActive || isCapsLock) Color.Black else KeyTextColor, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SuggestionBar(
    suggestions: List<String>,
    emojiSuggestions: List<String>,
    isLoading: Boolean,
    isListening: Boolean,
    isDictionaryDownloaded: Boolean,
    errorMessage: String?,
    onSuggestionClick: (String) -> Unit,
    onTranslateClick: () -> Unit,
    onClipboardToggle: () -> Unit,
    onDownloadDictionary: () -> Unit,
    onVoiceInputClick: () -> Unit,
    accentColor: Color
) {
    Row(modifier = Modifier.fillMaxWidth().height(42.dp).background(KeyboardBg), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(42.dp).clickable { onClipboardToggle() }, contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Outlined.ContentPaste, contentDescription = "Clipboard", tint = HintColor, modifier = Modifier.size(20.dp))
        }
        Box(modifier = Modifier.size(42.dp).clickable { onTranslateClick() }, contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Translate", tint = if (isLoading) accentColor else HintColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
        Row(modifier = Modifier.weight(1f)) {
            if (isListening) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(VoiceActiveColor))
                    Spacer(Modifier.width(8.dp))
                    Text("Listening...", color = VoiceActiveColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else if (isLoading) {
                Text("Loading...", color = accentColor, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            } else if (!isDictionaryDownloaded) {
                Row(modifier = Modifier.fillMaxSize().clickable { onDownloadDictionary() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.CloudDownload, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download Dictionary", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                val allSuggestions = suggestions + emojiSuggestions
                if (allSuggestions.isNotEmpty()) {
                    allSuggestions.forEach { suggestion ->
                        Text(text = suggestion, color = KeyTextColor, fontSize = 17.sp, modifier = Modifier.weight(1f).clickable { onSuggestionClick(suggestion) }.padding(vertical = 10.dp), textAlign = TextAlign.Center, maxLines = 1)
                    }
                } else if (errorMessage != null) {
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                } else {
                    Text("SwiftMind", color = HintColor.copy(alpha = 0.2f), fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
        Box(modifier = Modifier.size(42.dp).clickable { onVoiceInputClick() }, contentAlignment = Alignment.Center) {
            Icon(imageVector = if (isListening) Icons.Default.Mic else Icons.Outlined.Mic, contentDescription = "Voice Input", tint = if (isListening) VoiceActiveColor else HintColor, modifier = Modifier.size(20.dp))
        }
    }
}
