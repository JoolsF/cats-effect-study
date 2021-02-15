package com.joolsf.essentialeffects

import cats.effect.{ContextShift, IO, Timer}
import cats.effect._
import cats.effect.implicits._
import com.joolsf.essentialeffects.debug.DebugHelper

import scala.concurrent.duration.DurationInt
/*
  Concurrent control

  Concurrent vs parallel

  Concurrent: Computations are concurrent when their execution lifetimes overlap.  It is about
  looking at the structure of the computation

  Parallel: Computations are parallel when their executions occur at the same instant in time.
  It is more about the operational utilization of resources during the execution.

  Concurrency emphasizes the non-deterministic aspects of computation, we can't tell when anything
  happens.  Only that their lifetimes overlap

  Parallelism requires determinism - no matter how many resources you have, you must produce the
  same answer.

 */
class Chapter4()(implicit cs: ContextShift[IO], t: Timer[IO]) {

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
    def start(implicit cs: ContextShift[IO]): IO[Fiber[IO, A]]

    This is the signature of start.  Fiber is wrapped in IO else it would running right now
   */
  def joinExample1(): IO[Unit] = {
    val task: IO[String] = IO.sleep(2.seconds) *> IO("task").debug

    for {
      fiber <- task.start
      _ <- IO("pre-join").debug
      _ <- fiber.join.debug // waits asynchronously
      _ <- IO("post-join").debug
    } yield ()

  }

  def cancelExample1() = {
    val task: IO[String] =
      IO("task").debug *>
        IO.never

    for {
    //TODO .onCancel
      fiber <- task.onCancel(IO("i was cancelled").debug.void).start
      _ <- IO("pre-cancel").debug
      _ <- fiber.cancel
      _ <- IO("canceled").debug
    } yield ()

  }

  /*
    Starts both ia and ib so that they run concurrently ("fork" them)
    Wait for each result
    Cancel the 'other' effect if ia or ib fails
    Combine the results with f

    The thing that will 'wait' and 'cancel' will be a Fiber.
   */
  def myParMapN[A, B, C](ia: IO[A], ib: IO[B])(f: (A, B) => C): IO[C] =
    for {
      fiberA <- ia.start
      fiberB <- ib.start
      a <- fiberA.join //wait for fiber a
      b <- fiberB.join //wait for fiber b
    } yield f(a, b)

}
