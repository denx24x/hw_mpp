import java.util.concurrent.atomic.*

/**
 * @author Белозоров Денис
 *
 * TODO: Copy the code from `FAABasedQueueSimplified`
 * TODO: and implement the infinite array on a linked list
 * TODO: of fixed-size `Segment`s.
 */
class FAABasedQueue<E> : Queue<E> {

    private val initial: Segment
    private val head: AtomicReference<Segment>
    private val tail: AtomicReference<Segment>

    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0) 
    init {
        initial = Segment(-1)
        head = AtomicReference(initial)
        tail = AtomicReference(initial)
    }

    private fun findSegment(start : Segment, id : Long) : Segment{
        var current = start
        while(current.id != id){
            val curNext = current.next.get()
            if(curNext == null){
                val newNext = Segment(current.id + 1)

                if(current.next.compareAndSet(null, newNext)){
                    current = newNext
                }
                continue
                
            }
            current = curNext
        }
        return current
    }

    private fun moveHeadForward(segment : Segment){
        while(true){
            val curHead = head.get()
            if(head.compareAndSet(curHead, segment)){
                return
            }
        }
    }

    private fun moveTailForward(segment : Segment){
        while(true){
            val curTail = tail.get()
            if(tail.compareAndSet(curTail, segment)){
                return
            }
        }
    }

    override fun enqueue(element: E) {
        while(true){
            val curTail = tail.get()
            val i = enqIdx.getAndIncrement()
            val s = findSegment(curTail, i / SEGMENT_SIZE)
            moveTailForward(s)
            if(s.cells.compareAndSet((i % SEGMENT_SIZE).toInt(), null, element)){
                return
            }
        }
    }

    override fun dequeue(): E? {
        while(true){
            if (deqIdx.get() >= enqIdx.get()) return null
            val curHead = head.get()
            val i = deqIdx.getAndIncrement()
            val s = findSegment(curHead, i / SEGMENT_SIZE)
            moveHeadForward(s)
            if(s.cells.compareAndSet((i % SEGMENT_SIZE).toInt(), null, POISONED)){
                continue
            }
            val result = s.cells.get((i % SEGMENT_SIZE).toInt())
            return result as E
        }
    }
}

private class Segment(val id: Long) {
    val next = AtomicReference<Segment?>(null)
    val cells = AtomicReferenceArray<Any?>(SEGMENT_SIZE)
}

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2
private val POISONED = Any()
