package com.aliveline.app.domain

import kotlin.math.roundToInt

data class TimeBlock(val id: Long, val startMin: Int, val endMin: Int)

data class PlacedBlock(val id: Long, val startMin: Int, val endMin: Int, val lane: Int, val lanes: Int)

/**
 * Pure layout for the Daily Map. Blocks that overlap in time are split into side-by-side lanes;
 * blocks that merely touch (one ends when the next starts) do not overlap.
 */
object TimeBlockLayout {

    fun place(blocks: List<TimeBlock>): List<PlacedBlock> {
        val sorted = blocks.sortedWith(compareBy({ it.startMin }, { it.endMin }, { it.id }))
        val result = mutableListOf<PlacedBlock>()

        var cluster = mutableListOf<Pair<TimeBlock, Int>>() // block + its lane
        var laneEnds = mutableListOf<Int>()
        var clusterEnd = Int.MIN_VALUE

        fun flush() {
            val lanes = laneEnds.size.coerceAtLeast(1)
            cluster.forEach { (block, lane) ->
                result += PlacedBlock(block.id, block.startMin, block.endMin, lane, lanes)
            }
            cluster = mutableListOf()
            laneEnds = mutableListOf()
            clusterEnd = Int.MIN_VALUE
        }

        for (block in sorted) {
            if (cluster.isNotEmpty() && block.startMin >= clusterEnd) flush()

            var lane = laneEnds.indexOfFirst { it <= block.startMin }
            if (lane == -1) {
                laneEnds.add(block.endMin)
                lane = laneEnds.lastIndex
            } else {
                laneEnds[lane] = block.endMin
            }
            cluster.add(block to lane)
            clusterEnd = maxOf(clusterEnd, block.endMin)
        }
        if (cluster.isNotEmpty()) flush()
        return result
    }

    /** Rounds a dragged start time to the nearest 15 minutes and keeps the whole block inside the day. */
    fun snapStart(rawMinutes: Float, durationMin: Int): Int {
        val snapped = (rawMinutes / 15f).roundToInt() * 15
        return snapped.coerceIn(0, (1440 - durationMin).coerceAtLeast(0))
    }
}