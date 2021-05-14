package com.joolsf.essentialeffects

import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.implicits.{
  catsSyntaxFlatMapOps,
  catsSyntaxTuple2Parallel,
  catsSyntaxTuple2Semigroupal,
  toFunctorOps
}
import com.joolsf.essentialeffects.debug.DebugHelper

import scala.concurrent.duration.DurationInt

/*
 Managing resources
 */
class Chapter7()(implicit cs: ContextShift[IO], t: Timer[IO]) {

  // 7.1 cancelling a background task
  // Run a task until some other process is complete and then cancel it.
  // Resource can be used for this

  val withBackgroundTask1: IO[String] =
    backgroundTask.use(_ => stuff())

  private def stuff(): IO[String] =
    IO("Doing stuff...").debug *> IO.sleep(400.millis) *> IO(
      "Doing more stuff..."
    ).debug
  private def loop = ((IO("looping...").debug) *> IO.sleep(100.millis)).foreverM
  private def backgroundTask: Resource[IO, Unit] = {

    Resource
      .make(IO("> forking backgroundTask").debug *> loop.start)(
        IO("< canceling backgroundTask").debug.void *> _.cancel
      )
      .void
  }

  // The above pattern is common so Cats effect provides the following
  // This avoids having to manually manage Fibers
  val withBackgroundTask2: IO[String] =
    loop.background.use(_ => stuff())

  // 7.2 composing managed state

  trait Example1[A, B, C, D] {
    def ra: Resource[IO, A] = ???

    def rb: Resource[IO, B] =
      ra.map(makeB) //Resource is a functor so you can map

    def rc: Resource[IO, C] = ra.flatMap(makeC) // Its a monad

    def rd = (rb, rc).mapN(makeD) //Its also an applicative

    def makeB(a: A): B = ???

    def makeD(a: B, b: C): D = ???

    def makeC(a: A): Resource[IO, C] = ???

  }

  // Composing using tupled
  private val stringRes: Resource[IO, String] =
    Resource.make(
      IO("> acquiring stringResource").debug *> IO("String")
    )(_ => IO("< releasing stringResource").debug.void)
  private val intRes: Resource[IO, Int] =
    Resource.make(
      IO("> acquiring intResource").debug *> IO(99)
    )(_ => IO("< releasing intResource").debug.void)
  // Would be released in opposite order to how they're acquired e.g. intRes would be released first
  private val combined: Resource[IO, (String, Int)] = (stringRes, intRes).tupled

  // Parallel resource composition
  // Resource is a Monad but also has a parallel typeclass instance
  private val parCombined: Resource[IO, (String, Int)] =
    (stringRes, intRes).parTupled

  //Note how resource acquisition and cleanup occurs on different threads
  def parCombinedExample =
    parCombined.use {
      case (s, i) =>
        IO(s"Using string: $s and int: $i")
    }
}
