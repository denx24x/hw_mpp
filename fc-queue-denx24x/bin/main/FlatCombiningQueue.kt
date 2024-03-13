import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * @Author Белозоров Денис
 */
class FlatCombiningQueue<E> : Queue<E> {
    private val queue = ArrayDeque<E>() // sequential queue
    private val combinerLock = AtomicBoolean(false) // unlocked initially
    private val tasksForCombiner = AtomicReferenceArray<Any?>(TASKS_FOR_COMBINER_SIZE)

    private fun helpOthers(){
        for(i in 0 until tasksForCombiner.length()){
            val task = tasksForCombiner.get(i) ?: continue
            if(task is Result<*>){
                continue
            }
            if(task == Dequeue){
                tasksForCombiner.set(i, Result(queue.removeFirstOrNull()))
            }else{
                queue.add(task as E)
                tasksForCombiner.set(i, Result(null))
            }
        }
    }

    override fun enqueue(element: E) {
        var registeredTask = false
        var registeredIndex = -1
        while(true) {
            if(tryLock()) {

                if(registeredTask){
                    val bf = tasksForCombiner.getAndSet(registeredIndex, null)
                    if(bf !is Result<*>){
                        queue.add(element)
                    }
                }else {
                    queue.add(element)
                }

                helpOthers()
                unlock()
                return
            }else if (!registeredTask) {
                val index = randomCellIndex()
                if(tasksForCombiner.compareAndSet(index, null, element)) {
                    registeredTask = true
                    registeredIndex = index
                }
            }else {
                val current = tasksForCombiner.get(registeredIndex)
                if(current is Result<*>){
                    tasksForCombiner.set(registeredIndex, null)
                    return
                }
            }
        }
    }

    override fun dequeue(): E? {
        var registeredTask = false
        var registeredIndex = -1
        while(true) {
            if(tryLock()) {
                val result = if(registeredTask){
                    val bf = tasksForCombiner.getAndSet(registeredIndex, null)
                    if(bf is Result<*>){
                        (bf as Result<E>).value
                    }else{
                        queue.removeFirstOrNull()
                    }
                }else {
                    queue.removeFirstOrNull()
                }

                helpOthers()
                unlock()
                return result
            }else if (!registeredTask) {
                val index = randomCellIndex()
                if(tasksForCombiner.compareAndSet(index, null, Dequeue)) {
                    registeredTask = true
                    registeredIndex = index
                }
            }
            if(registeredTask) {
                val current = tasksForCombiner.get(registeredIndex)
                if(current is Result<*>){
                    tasksForCombiner.set(registeredIndex, null)
                    return (current as Result<E>).value
                }
            }
        }
    }

    private fun tryLock() : Boolean {
        return combinerLock.compareAndSet(false, true)
    }

    private fun unlock(){
        combinerLock.set(false)
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(tasksForCombiner.length())
}

private const val TASKS_FOR_COMBINER_SIZE = 3 // Do not change this constant!

// TODO: Put this token in `tasksForCombiner` for dequeue().
// TODO: enqueue()-s should put the inserting element.
private object Dequeue

// TODO: Put the result wrapped with `Result` when the operation in `tasksForCombiner` is processed.
private class Result<V>(
    val value: V
)