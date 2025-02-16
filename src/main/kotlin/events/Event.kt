package events
sealed class Event(val time: Double) : Comparable<Event> {
    override fun compareTo(other: Event) = time.compareTo(other.time)
}
