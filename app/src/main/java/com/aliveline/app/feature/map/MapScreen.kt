package com.aliveline.app.feature.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliveline.app.domain.TimeBlockLayout
import com.aliveline.app.domain.TimeFormat
import com.aliveline.app.feature.focus.StartFocusDialog
import com.aliveline.app.feature.focus.StartFocusRequest
import com.aliveline.app.ui.TaskItem
import com.aliveline.app.ui.palette
import com.aliveline.app.ui.theme.AliveColors
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private val HourHeight = 64.dp
private val Gutter = 52.dp

private data class TrayDrag(val item: TaskItem, val pointer: Offset)

private fun minuteOfDayNow(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

@Composable
fun MapScreen(
    onTaskClick: (Long) -> Unit,
    onFocusStarted: () -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val hourPx = with(density) { HourHeight.toPx() }
    val scroll = rememberScrollState()

    var nowMinute by remember { mutableIntStateOf(minuteOfDayNow()) }
    var focusRequest by remember { mutableStateOf<StartFocusRequest?>(null) }
    var pickerFor by remember { mutableStateOf<TaskItem?>(null) }
    var gridBounds by remember { mutableStateOf(Rect.Zero) }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    var drag by remember { mutableStateOf<TrayDrag?>(null) }

    LaunchedEffect(Unit) {
        // Start the view a little before the current time, once the grid has been measured.
        snapshotFlow { scroll.maxValue }.first { it in 1 until Int.MAX_VALUE }
        scroll.scrollTo((((nowMinute - 90).coerceAtLeast(0)) / 60f * hourPx).roundToInt())
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMinute = minuteOfDayNow()
        }
    }

    /** Which minute of the day is under a finger at [pointer] (screen coordinates)? Null if outside the grid. */
    fun minuteAt(pointer: Offset, durationMin: Int): Int? {
        if (!gridBounds.contains(pointer)) return null
        val raw = (pointer.y - gridBounds.top + scroll.value) / hourPx * 60f
        return TimeBlockLayout.snapStart(raw, durationMin)
    }

    focusRequest?.let { request ->
        StartFocusDialog(
            request = request,
            onDismiss = { focusRequest = null },
            onStarted = {
                focusRequest = null
                onFocusStarted()
            },
        )
    }

    pickerFor?.let { item ->
        com.aliveline.app.ui.components.TimePickerDialog(
            initialMinute = ((nowMinute + 14) / 15) * 15 % 1440,
            onConfirm = { minute ->
                viewModel.onSchedule(item.id, minute.coerceAtMost(1440 - 60), 60)
                pickerFor = null
            },
            onDismiss = { pickerFor = null },
        )
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned { rootOffset = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Daily Map", color = AliveColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(state.dateLabel, color = AliveColors.TextSecondary)
            }

            if (state.unscheduled.isNotEmpty()) {
                Text(
                    "Needs a time \u00B7 tap to pick, or hold and drag onto the timeline",
                    color = AliveColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.unscheduled.forEach { item ->
                        TrayChip(
                            item = item,
                            onTap = { pickerFor = item },
                            onDragMove = { drag = TrayDrag(item, it) },
                            onDrop = {
                                val d = drag
                                drag = null
                                if (d != null) {
                                    minuteAt(d.pointer, 60)?.let { minute ->
                                        viewModel.onSchedule(d.item.id, minute, 60)
                                    }
                                }
                            },
                            onCancel = { drag = null },
                        )
                    }
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { gridBounds = it.boundsInRoot() },
            ) {
                Column(Modifier.verticalScroll(scroll)) {
                    TimeGrid(
                        blocks = state.blocks,
                        nowMinute = nowMinute,
                        hourPx = hourPx,
                        onTaskClick = onTaskClick,
                        onMove = viewModel::onMove,
                        onFocus = { block ->
                            focusRequest = StartFocusRequest(
                                taskId = block.id,
                                title = block.title,
                                bot = block.bot,
                                suggestedMinutes = block.durationMin,
                            )
                        },
                    )
                }
            }
        }

        // The chip that follows your finger while dragging from the tray.
        drag?.let { d ->
            val dropMinute = minuteAt(d.pointer, 60)
            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (d.pointer.x - rootOffset.x).roundToInt() - 70.dp.roundToPx(),
                            (d.pointer.y - rootOffset.y).roundToInt() - 56.dp.roundToPx(),
                        )
                    }
                    .width(140.dp),
                shape = RoundedCornerShape(14.dp),
                color = AliveColors.Primary,
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(d.item.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        if (dropMinute != null) "Drop at ${TimeFormat.minuteOfDay(dropMinute)}" else "Drag onto the timeline",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrayChip(
    item: TaskItem,
    onTap: () -> Unit,
    onDragMove: (Offset) -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit,
) {
    val accent = item.bot.palette().base
    var chipRoot by remember { mutableStateOf(Offset.Zero) }
    val move by rememberUpdatedState(onDragMove)
    val drop by rememberUpdatedState(onDrop)
    val cancel by rememberUpdatedState(onCancel)

    Surface(
        modifier = Modifier
            .onGloballyPositioned { chipRoot = it.positionInRoot() }
            .pointerInput(item.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { local -> move(chipRoot + local) },
                    onDrag = { change, _ ->
                        change.consume()
                        move(chipRoot + change.position)
                    },
                    onDragEnd = { drop() },
                    onDragCancel = { cancel() },
                )
            }
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.22f).compositeOver(AliveColors.Surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
    ) {
        Text(
            item.title,
            color = AliveColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun TimeGrid(
    blocks: List<MapBlockUi>,
    nowMinute: Int,
    hourPx: Float,
    onTaskClick: (Long) -> Unit,
    onMove: (MapBlockUi, Int) -> Unit,
    onFocus: (MapBlockUi) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(HourHeight * 24)) {
        val areaWidth = maxWidth - Gutter - 12.dp

        Column(Modifier.fillMaxWidth()) {
            repeat(24) { hour ->
                Row(Modifier.height(HourHeight), verticalAlignment = Alignment.Top) {
                    Text(
                        TimeFormat.minuteOfDay(hour * 60),
                        color = AliveColors.TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(Gutter).padding(end = 8.dp),
                    )
                    Box(Modifier.weight(1f).height(1.dp).background(AliveColors.Surface))
                }
            }
        }

        blocks.forEach { block ->
            key(block.id) {
                val laneWidth = areaWidth / block.lanes
                TimeBlockView(
                    block = block,
                    x = Gutter + laneWidth * block.lane,
                    y = HourHeight * (block.startMin / 60f),
                    width = laneWidth,
                    height = HourHeight * ((block.endMin - block.startMin) / 60f),
                    hourPx = hourPx,
                    onClick = { onTaskClick(block.id) },
                    onFocus = { onFocus(block) },
                    onMoved = { onMove(block, it) },
                )
            }
        }

        // "Now" line.
        Box(
            Modifier
                .offset(y = HourHeight * (nowMinute / 60f))
                .padding(start = Gutter - 6.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(AliveColors.Primary),
        )
    }
}

@Composable
private fun TimeBlockView(
    block: MapBlockUi,
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    hourPx: Float,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    onMoved: (Int) -> Unit,
) {
    val accent = block.bot.palette().base
    var dragDy by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val latestOnMoved by rememberUpdatedState(onMoved)
    val shownHeight = maxOf(height, 28.dp) - 2.dp

    // Once the saved position catches up with where we dropped the block, clear the drag offset.
    LaunchedEffect(block.startMin) { dragDy = 0f }

    Surface(
        modifier = Modifier
            .offset(x = x, y = y)
            .offset { IntOffset(0, dragDy.roundToInt()) }
            .zIndex(if (dragging) 1f else 0f)
            .width(width - 3.dp)
            .height(shownHeight)
            .pointerInput(block.id, block.startMin, block.endMin) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragging = true },
                    onDrag = { change, amount ->
                        change.consume()
                        dragDy += amount.y
                    },
                    onDragEnd = {
                        dragging = false
                        val raw = block.startMin + dragDy / hourPx * 60f
                        val newStart = TimeBlockLayout.snapStart(raw, block.durationMin)
                        if (newStart == block.startMin) {
                            dragDy = 0f
                        } else {
                            dragDy = (newStart - block.startMin) * hourPx / 60f
                            latestOnMoved(newStart)
                        }
                    },
                    onDragCancel = {
                        dragging = false
                        dragDy = 0f
                    },
                )
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = if (block.done) 0.12f else 0.28f).compositeOver(AliveColors.Surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
        shadowElevation = if (dragging) 8.dp else 0.dp,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    block.title,
                    color = AliveColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = if (shownHeight >= 56.dp) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (block.done) TextDecoration.LineThrough else null,
                )
                if (shownHeight >= 46.dp) {
                    Text(
                        "${TimeFormat.minuteOfDay(block.startMin)} - ${TimeFormat.minuteOfDay(block.endMin)}",
                        color = AliveColors.TextSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
            if (block.done) {
                Icon(Icons.Default.Check, contentDescription = "Done", tint = accent, modifier = Modifier.size(20.dp))
            } else {
                Box(
                    modifier = Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onFocus),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start focus", tint = accent)
                }
            }
        }
    }
}