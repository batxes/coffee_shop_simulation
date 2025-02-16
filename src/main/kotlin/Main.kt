import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.util.PriorityQueue
import kotlin.math.ln
import kotlin.random.Random

// Events
sealed class Event(val time: Double) : Comparable<Event> {
    override fun compareTo(other: Event) = time.compareTo(other.time)
}

class CustomerArrivalEvent(time: Double) : Event(time)
class OrderCompletionEvent(time: Double, val customer: Customer) : Event(time)

// Data classes
data class Customer(val id: Int, val arrivalTime: Double)
data class Statistics(
    var totalCustomers: Int = 0,
    var totalWaitTime: Double = 0.0,
    var queueLengths: MutableList<Pair<Double, Int>> = mutableListOf()
)

// Main simulation class
class CoffeeShopSimulation(
    private val simulationDuration: Double,
    private val averageServiceTime: Double = 3.0,
    private val averageTimeBetweenArrivals: Double = 4.0
) {
    private val eventQueue = PriorityQueue<Event>()
    private val customerQueue = mutableListOf<Customer>()
    private var currentTime = 0.0
    private var nextCustomerId = 1
    private val random = Random(42)
    private val stats = Statistics()
    private var baristas = 2 // Number of baristas

    private fun scheduleNextArrival() {
        val nextArrivalTime = currentTime + random.exponential(averageTimeBetweenArrivals)
        eventQueue.add(CustomerArrivalEvent(nextArrivalTime))
    }

    private fun handleCustomerArrival(event: CustomerArrivalEvent) {
        val customer = Customer(nextCustomerId++, event.time)
        customerQueue.add(customer)
        stats.totalCustomers++
        stats.queueLengths.add(currentTime to customerQueue.size)

        // Schedule next arrival
        scheduleNextArrival()

        // If barista is available, start serving
        if (customerQueue.size <= baristas) {
            val serviceTime = random.exponential(averageServiceTime)
            eventQueue.add(OrderCompletionEvent(currentTime + serviceTime, customer))
        }
    }

    private fun handleOrderCompletion(event: OrderCompletionEvent) {
        customerQueue.remove(event.customer)
        stats.totalWaitTime += (event.time - event.customer.arrivalTime)
        stats.queueLengths.add(currentTime to customerQueue.size)

        // If there are more customers waiting and barista available, serve next
        if (customerQueue.size >= baristas) {
            val nextCustomer = customerQueue[baristas - 1]
            val serviceTime = random.exponential(averageServiceTime)
            eventQueue.add(OrderCompletionEvent(currentTime + serviceTime, nextCustomer))
        }
    }

    fun runSimulation(): Statistics {
        // Schedule first arrival
        scheduleNextArrival()

        // Run simulation
        while (eventQueue.isNotEmpty() && currentTime < simulationDuration) {
            val event = eventQueue.poll()
            currentTime = event.time

            when (event) {
                is CustomerArrivalEvent -> handleCustomerArrival(event)
                is OrderCompletionEvent -> handleOrderCompletion(event)
            }
        }

        return stats
    }
}

// Extension function for random exponential distribution
fun Random.exponential(mean: Double): Double = -mean * ln(nextDouble())

// Visualization
class CoffeeShopVisualization : Application() {
    companion object {
        lateinit var statistics: Statistics
    }

    override fun start(stage: Stage) {
        val xAxis = NumberAxis("Time (minutes)", 0.0, 60.0, 5.0)
        val yAxis = NumberAxis("Queue Length", 0.0, 10.0, 1.0)
        
        val lineChart = LineChart(xAxis, yAxis)
        lineChart.title = "Coffee Shop Queue Length Over Time"

        val series = XYChart.Series<Number, Number>()
        series.name = "Queue Length"

        statistics.queueLengths.forEach { (time, length) ->
            series.data.add(XYChart.Data(time, length))
        }

        lineChart.data.add(series)

        val root = VBox(lineChart)
        val scene = Scene(root, 800.0, 600.0)
        
        stage.title = "Coffee Shop Simulation"
        stage.scene = scene
        stage.show()
    }
}

// Main function to run everything
fun main() {
    // Run simulation
    val simulation = CoffeeShopSimulation(
        simulationDuration = 60.0,  // 60 minutes
        averageServiceTime = 3.0,   // 3 minutes per customer
        averageTimeBetweenArrivals = 4.0  // New customer every 4 minutes on average
    )
    
    val stats = simulation.runSimulation()
    
    // Print results
    println("Simulation Results:")
    println("Total customers served: ${stats.totalCustomers}")
    println("Average wait time: ${stats.totalWaitTime / stats.totalCustomers} minutes")
    
    // Launch visualization
    CoffeeShopVisualization.statistics = stats
    Application.launch(CoffeeShopVisualization::class.java)
}
