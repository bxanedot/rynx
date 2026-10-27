package dev.bxane.rynx.playback

/** Entry identity is separate from media identity so a song may occur more than once. */
data class QueueEntry(val id: String, val mediaId: String) {
    init { require(id.isNotBlank()); require(mediaId.isNotBlank()) }
}

enum class RepeatMode { Off, All, One }
enum class AdvanceReason { Ended, UserSkip }

/** Pure queue decisions. Null position means no active item, including an exhausted queue. */
class PlaybackQueue(
    entries: List<QueueEntry> = emptyList(),
    val position: Int? = if (entries.isEmpty()) null else 0,
    val repeat: RepeatMode = RepeatMode.Off,
) {
    private val snapshot = entries.toList()
    val entries: List<QueueEntry> get() = snapshot.toList()
    val current: QueueEntry? get() = position?.let(snapshot::get)

    init {
        require(position == null || position in snapshot.indices) { "Position outside queue" }
        require(snapshot.map { it.id }.toSet().size == snapshot.size) { "Entry IDs must be unique" }
    }

    fun advance(reason: AdvanceReason): PlaybackQueue {
        val index = position ?: return this
        if (reason == AdvanceReason.Ended && repeat == RepeatMode.One) return this
        val next = when {
            index + 1 < snapshot.size -> index + 1
            repeat == RepeatMode.All -> 0
            else -> null
        }
        return PlaybackQueue(snapshot, next, repeat)
    }

    fun previous(): PlaybackQueue {
        if (snapshot.isEmpty()) return this
        val previous = when {
            position == null -> snapshot.lastIndex
            position > 0 -> position - 1
            repeat == RepeatMode.All -> snapshot.lastIndex
            else -> 0
        }
        return PlaybackQueue(snapshot, previous, repeat)
    }

    fun select(id: String): PlaybackQueue {
        val index = snapshot.indexOfFirst { it.id == id }
        return if (index < 0) this else PlaybackQueue(snapshot, index, repeat)
    }

    fun withRepeat(mode: RepeatMode) = PlaybackQueue(snapshot, position, mode)

    fun remove(id: String): PlaybackQueue {
        val removed = snapshot.indexOfFirst { it.id == id }
        if (removed < 0) return this
        val remaining = snapshot.filterNot { it.id == id }
        val next = when {
            position == null || remaining.isEmpty() -> null
            removed < position -> position - 1
            removed == position -> position.coerceAtMost(remaining.lastIndex)
            else -> position
        }
        return PlaybackQueue(remaining, next, repeat)
    }

    fun move(id: String, destination: Int): PlaybackQueue {
        require(destination in snapshot.indices) { "Destination outside queue" }
        val source = snapshot.indexOfFirst { it.id == id }
        if (source < 0) return this
        val moved = snapshot.toMutableList().apply { add(destination, removeAt(source)) }
        val next = current?.let { active -> moved.indexOfFirst { it.id == active.id } }
        return PlaybackQueue(moved, next, repeat)
    }
}
