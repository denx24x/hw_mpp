package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.Comparator
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

// Returns `Integer.MAX_VALUE` if a path has not been found.
/**
 * @author Белозоров Денис
 */
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = PriorityMultiQueue(4 * workers, NODE_DISTANCE_COMPARATOR)
    q.insert(start)
    val activeNodes = AtomicInteger(1)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (activeNodes.get() > 0) {

                val cur: Node? = q.delete()
                if (cur == null) {
                    continue
                    if (activeNodes.get() == 0) break else continue
                }
                for (e in cur.outgoingEdges) {
                    //if (e.to.distance > cur.distance + e.weight) {
                        val dist = cur.distance + e.weight

                        val relaxed =  updateDistIfLower(e.to, dist)
                        if(relaxed){
                            activeNodes.incrementAndGet()
                            q.insert(e.to)
                        }
                    //}
                }
                activeNodes.decrementAndGet()
            }
            onFinish.arrive()
        }
    }

    onFinish.arriveAndAwaitAdvance()
}

fun updateDistIfLower(node : Node, dist: Int) : Boolean {
    while (true){
        val curDist = node.distance
        if(curDist > dist){
            if(node.casDistance(curDist, dist)){
                return true
            }
        }else{
            return false
        }
    }
}

class PriorityMultiQueue<E : Any>(val size: Int, val comparator: Comparator<E>) {
    private val queues : Array<PriorityQueue<E>>
    private val locks : Array<AtomicBoolean>
    private val random = Random(42)

    init {
        queues = Array(size, { PriorityQueue(comparator) })
        locks = Array(size, { AtomicBoolean(false) })
    }

    private fun tryLock(index: Int) : Boolean {
        return locks[index].compareAndSet(false, true)
    }

    private fun unlock(index: Int) {
        locks[index].set(false)
    }
    fun insert(task : E){
        while(true){
            val ind = random.nextInt(size)
            if(!tryLock(ind)){
                continue
            }
            queues[ind].add(task)
            unlock(ind)
            return
        }
    }

    private fun distinctRandom() : Pair<Int, Int> {
        while(true){
            val first = random.nextInt(size)
            val second = random.nextInt(size)
            if(first == second){
                continue
            }
            return Pair(first, second)
        }
    }

    private fun getIndex(ind : Pair<Int, Int>) : Int? {
        val first = queues[ind.first].peek()
        val second = queues[ind.second].peek()
        if(first == null && second == null) return null

        if(first == null) return ind.second
        if(second == null) return ind.first

        return if(comparator.compare(first, second) < 0){
            ind.first
        }else{
            ind.second
        }
    }

    fun delete() : E? {
        while(true){
            var ind = distinctRandom()
            if(ind.first > ind.second){
                ind = Pair(ind.second, ind.first)
            }

            val q = getIndex(ind)

            if(q == null){
                return null
            }

            if(!tryLock(q)){
                continue
            }

            val task = queues[q].poll()
            unlock(q)
            return task
        }
    }
}