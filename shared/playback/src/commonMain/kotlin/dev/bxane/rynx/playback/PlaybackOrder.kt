package dev.bxane.rynx.playback

/** Keep every timeline entry when a provider has not supplied a complete permutation. */
fun normalizedPlaybackOrder(size: Int, order: List<Int>): List<Int> =
    if (order.size == size && order.toSet().size == size && order.all { it in 0 until size }) order
    else (0 until size).toList()

/** Moves an upcoming entry without changing the currently playing entry or the played prefix. */
fun moveUpcomingQueueEntry(order: List<Int>, currentIndex: Int, sourceIndex: Int, destinationIndex: Int): List<Int>? {
    if (order.distinct().size != order.size) return null
    val current = order.indexOf(currentIndex)
    val source = order.indexOf(sourceIndex)
    val destination = order.indexOf(destinationIndex)
    if (current < 0 || source <= current || destination <= current) return null
    return order.toMutableList().apply { add(destination, removeAt(source)) }
}
