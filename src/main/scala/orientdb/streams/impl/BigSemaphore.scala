package orientdb.streams.impl

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong

// TODO: not optimal (at all) implementation but it seems that clearest and best way to get rid of race condition
//       it's good for now
// TODO: tests
class BigSemaphore {
  val semaphore = new Semaphore(0)
  val counter = new AtomicLong(0L)

  def release(amount: Long) = {
    if (amount > remainingPermits) {
      val diff = amount - remainingPermits
      semaphore.release(remainingPermits)
      if (counter.addAndGet(diff) < 0) { // overflow
        counter.set(Long.MaxValue)
      }
    } else semaphore.release(amount.toInt) // safe
  }

  def acquire() = {
    if (semaphore.availablePermits <= 0) {
      val toRelease = Math.min(Int.MaxValue, counter.get).toInt // safe
      counter.addAndGet(-toRelease)
      semaphore.release(toRelease)
    }

    semaphore.acquire()
  }

  def drainPermits() = {
    semaphore.drainPermits()
  }

  private def remainingPermits: Int = Int.MaxValue - semaphore.availablePermits
}
