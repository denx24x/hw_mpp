import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * @author Белозоров Денис
 */
open class TreiberStackWithElimination<E> : Stack<E> {
    private val stack = TreiberStack<E>()

    private val eliminationArray = AtomicReferenceArray<Any?>(ELIMINATION_ARRAY_SIZE)

    override fun push(element: E) {
        if (tryPushElimination(element)) return
        stack.push(element)
    }
    
    private fun takeCell(element: Any?, index: Int) {
        while(true){
            val current = eliminationArray[index];
            if(eliminationArray.compareAndSet(index, current, element)){
                return
            }
        }
    }

    protected open fun tryPushElimination(element: E): Boolean {
        val index = randomCellIndex();

        if(eliminationArray.compareAndSet(index, CELL_STATE_EMPTY, element)){
            for(i in 0..ELIMINATION_WAIT_CYCLES){
                if(eliminationArray.compareAndSet(index, CELL_STATE_RETRIEVED, CELL_STATE_EMPTY)){
                    return true
                }
            }
            return eliminationArray.getAndSet(index, CELL_STATE_EMPTY) == CELL_STATE_RETRIEVED
        }
        return false
    }

    override fun pop(): E? = tryPopElimination() ?: stack.pop()

    private fun tryPopElimination(): E? {
        val index = randomCellIndex()
        val result = eliminationArray[index];
        
        if(result in arrayOf(CELL_STATE_EMPTY, CELL_STATE_RETRIEVED) || !eliminationArray.compareAndSet(index, result, CELL_STATE_RETRIEVED)){
            return null
        }
        return result as E
        // takeCell(CELL_STATE_RETRIEVED, index)
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(eliminationArray.length())

    companion object {
        private const val ELIMINATION_ARRAY_SIZE = 2 // Do not change!
        private const val ELIMINATION_WAIT_CYCLES = 1 // Do not change!

        // Initially, all cells are in EMPTY state.
        private val CELL_STATE_EMPTY = null

        // `tryPopElimination()` moves the cell state
        // to `RETRIEVED` if the cell contains element.
        private val CELL_STATE_RETRIEVED = Any()
    }
}

