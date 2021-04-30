package com.joolsf.essentialeffects

import cats.effect.{Blocker, ContextShift, IO, Timer}
import cats.implicits._
import com.joolsf.essentialeffects.debug.DebugHelper

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

/*
  1. Threads abstract over what is concurrently executing atop the available set of
  processors,so we can have many more threads than CPUs. A scala.concurrent.ExecutionContext
  represents a scheduling queue along with a set of threads used for computation.
  2. Asynchronous boundaries help to ensure applications make progress in the
  presence of long-running effects by rescheduling the remainder of the effect. At
  the same time we can specify a computation to resume on a different context
  in order to isolate various workloads from one another.
  3. IOApp provides a default ExecutionContext with a fixed number—the number of
  CPUs on the machine—of threads. This is meant for CPU-bound (non-blocking)
  work.
  4. I/O-bound work, which is usually slower than CPU-bound work because it will
 */
class Chapter5()(implicit cs: ContextShift[IO], t: Timer[IO]) {

  /*
   5.1
    In the below example we run x 2 the number of tasks than we have cpus.
    The underlying thread pool, i.e the ExecutionContext has at most numCpus threads.
    We compose effects effects in parallel but during execution we only schedule an effect to be executed.
    Another async process is responsible for executing the scheduled effects on an available thread.

    In Scala the ExecutionContext encapsulates both a queue of scheduled tasks and a set of threads.
    In Cats effect, every IOApp has a default EC and on the JVM this is constructed as a fixed pool
    based on the number of available CPUs.
   */
  def parallelExample1() = {
    def task(i: Int): IO[Int] = IO(i).debug
    val numCpus =
      Runtime.getRuntime
        .availableProcessors() //We ask for this so we can submit more than this number of tasks
    val tasks = List.range(0, numCpus * 2).parTraverse(task)

    for {
      _ <- IO(s"number of CPUs: $numCpus")
      _ <- tasks.debug
    } yield ()
  }

  /*
   * Blocking example CE 2 instantiating a
   * CE 3 has updated syntax i.e IO.blocking(....)
   *
   * Useful for blocking operations e.g. io which defaults to blocked on JVM.
   * Generally if there is no callback API then the operation probably blocks.
   */

  def blockingExample1() = {
    Blocker[IO].use { blocker =>
      withBlocker(blocker).as(1)
    }
  }

  /*
    [ioapp-compute-0] on default
    [cats-effect-blocker-0] on blocker
    [ioapp-compute-1] where am I?
   */
  def withBlocker(blocker: Blocker): IO[Unit] =
    for {
      _ <- IO("on default").debug
      _ <- blocker.blockOn(IO("on blocker").debug)
      _ <- IO("where am I?").debug
    } yield ()

  // This recursive loop won't still a thread forever since IO.sleep
  // introduces an async boundary.  It would be bad for long-running process to take a thread.
  // With this boundary the next iteration can be scheduled as normal.
  // This is equivalent to using IO.shift
  def tickingClock(): IO[Unit] =
    for {
      _ <- IO(System.currentTimeMillis).debug
      _ <- IO.sleep(1.second)
      _ <- tickingClock()
    } yield ()

  //Shift can take an EC
  def shiftWithEc(): IO[Unit] =
    (ec("1"), ec("2")) match {
      case (ec1, ec2) =>
        for {
          _ <- IO("one").debug
          _ <- IO.shift(ec1)
          _ <- IO("two").debug
          _ <- IO.shift(ec2)
          _ <- IO("three").debug
        } yield ()
    }
  def ec(name: String): ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor { r =>
      val t = new Thread(r, s"pool-$name-thread-1")
      t.setDaemon(true)
      t
    })

}
