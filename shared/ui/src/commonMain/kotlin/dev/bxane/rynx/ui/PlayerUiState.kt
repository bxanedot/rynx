package dev.bxane.rynx.ui

data class TrackItem(val id: String, val title: String, val subtitle: String = "Local audio")
data class PlayerUiState(
    val tracks: List<TrackItem> = emptyList(),
    val queue: List<TrackItem> = emptyList(),
    val currentId: String? = null,
    val playing: Boolean = false,
    val positionMillis: Long = 0,
    val durationMillis: Long = 0,
    val busy: Boolean = false,
    val error: String? = null,
    val repeatLabel: String = "Off",
) { val current: TrackItem? get() = tracks.firstOrNull { it.id == currentId } }

interface PlayerActions {
    fun importFiles()
    fun play(id: String)
    fun toggle()
    fun previous()
    fun next()
    fun seek(fraction: Float)
    fun cycleRepeat()
    fun removeFromQueue(id: String)
    fun dismissError()
}
