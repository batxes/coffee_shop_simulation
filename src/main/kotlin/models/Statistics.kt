package models
data class Statistics(
    var totalCustomers: Int = 0,
    var totalWaitTime: Double = 0.0,
    var queueLengths: MutableList<Pair<Double, Int>> = mutableListOf()
)
