package com.joolsf.essentialeffects

import cats.effect.implicits._
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import com.joolsf.essentialeffects.debug.DebugHelper

import scala.concurrent.duration.{DurationInt, FiniteDuration}
/*
  Concurrent control - How do we 'control' a running computations

  Concurrent vs parallel

  Concurrent: Computations are concurrent when their execution lifetimes overlap.  It is about
  looking at the structure of the computation

  Parallel: Computations are parallel when their executions occur at the same instant in time.
  It is more about the operational utilization of resources during the execution.

  Concurrency emphasizes the non-deterministic aspects of computation, we can't tell when anything
  happens.  Only that their lifetimes overlap

  Parallelism requires determinism - no matter how many resources you have, you must produce the
  same answer.

  Summary
  1 Concurrency allows us to control running computations.

  2. A Fiber is our handle to this control. After we start a concurrent computation,
  we can cancel or join it (wait for completion).

  3. Concurrently executing effects can be cancelled. Cancelled effects are expected
  to stop executing via implicit or explicit cancelation boundaries.

  4. We can race two computations to know who finished first. Higher-order effects
  like timeouts can be constructed using races
 */

class Chapter4()(implicit cs: ContextShift[IO], t: Timer[IO]) {

  // 4.2
  // Run below with and without .start.
  // With will print on different threads.
  def forkExample1(): IO[Unit] = {
    val task: IO[String] = IO("task").debug

    for {
      _ <- task.start //start forks the execution from its current context
      _ <- IO("task was started").debug
    } yield ()
  }

  /*
    4.2.2
    def start(implicit cs: ContextShift[IO]): IO[Fiber[IO, A]]
    This is the signature of start.  Fiber is wrapped in IO else it would running right now
   */
  def joinExample1(): IO[Unit] = {
    val task: IO[String] = IO.sleep(2.seconds) *> IO("task").debug

    for {
      fiber <- task.start
      _ <- IO("pre-join").debug
      _ <-
        fiber.join.debug // waits asynchronously - note that execution continues on the thread the fiber was started on
      _ <- IO("post-join").debug
    } yield ()

  }

  //4.3
  def cancelExample1(): IO[Unit] = {
    val task: IO[String] =
      IO("task").debug *> IO.never

    for {
      //TODO .onCancel
      fiber <-
        task
          .onCancel(
            IO("i was cancelled").debug.void
          )
          .start
      _ <- IO("pre-cancel").debug
      _ <- fiber.cancel
      _ <- IO("canceled").debug
    } yield ()

  }

  //TODO
  // 4.4
  // Racing multiple effects
  def raceExample1(): IO[String] = {

    def random = scala.util.Random.nextInt(3000)
    def annotatedSleep(name: String, duration: FiniteDuration) =
      (
        IO(s"$name: starting").debug *>
          IO.sleep(duration) *>
          IO(s"$name: done").debug
      ).onCancel(IO(s"$name: cancelled").debug.void).void

    val task: IO[Unit] = annotatedSleep("task", random.millis)
    val timeout: IO[Unit] = annotatedSleep("timeout", random.millis)

    // This pattern can be expressed more simply with IO.timeout
    for {
      done <- IO.race(task, timeout)
      res <- done match {
        case Left(_)  => IO(" task: won").debug
        case Right(_) => IO("timeout: won").debug
      }
    } yield res

  }

  /*
   IO.racePair type: IO[Either[(A, Fiber[IO, B]), (Fiber[IO, A], B)]]
   Gives you the "winning" value and the Fiber of the losing value
   */
  def myParMapN[A, B, C](ia: IO[A], ib: IO[B])(f: (A, B) => C): IO[C] = {
    IO.racePair(ia, ib).flatMap {
      case Left((a, fb))  => (IO.pure(a), fb.join).mapN(f)
      case Right((fa, b)) => (fa.join, IO(b)).mapN(f)
    }

  }
}
