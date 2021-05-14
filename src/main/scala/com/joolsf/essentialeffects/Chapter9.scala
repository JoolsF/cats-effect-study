package com.joolsf.essentialeffects
import cats.effect.concurrent.Ref
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits.{catsSyntaxApplicative, catsSyntaxParallelTraverse, catsSyntaxTuple2Parallel}
import com.joolsf.essentialeffects.debug.DebugHelper

import scala.concurrent.duration.DurationInt
/*
 Concurrent coordination
 */
class Chapter9(implicit cs: ContextShift[IO], t: Timer[IO]) {

  //Atomic updates with Ref

  var ticks: Long = 0L

  val tickingClock: IO[Unit] =
    for {
      _ <- IO.sleep(1.second)
      _ <- IO(System.currentTimeMillis()).debug
      _ = ticks = ticks + 1
      _ <- tickingClock
    } yield ()

  val printTicks: IO[Unit] =
    for {
      _ <- IO.sleep(5.seconds)
      _ <- IO(s"TICKS: $ticks").debug.void
      _ <- printTicks
    } yield ()

  def tickingClockExampleWithoutRef: IO[(Unit, Unit)] =
    (tickingClock, printTicks).parTupled

  //With ref
  val ticksRef: IO[Ref[IO, Long]] =
    Ref[IO].of(0L)
  def tickingClock(r: Ref[IO, Long]): IO[Unit] =
    for {
      _ <- IO.sleep(1.second)
      _ <- IO(System.currentTimeMillis()).debug
      _ <- r.update(_ + 1)
      _ <- tickingClock
    } yield ()

  def printTicks(r: Ref[IO, Long]): IO[Unit] =
    for {
      _ <- IO.sleep(5.seconds)
      ticks <- r.get
      _ <- IO(s"TICKS: $ticks").debug.void
      _ <- printTicks
    } yield ()

  // p.119
  def tickingClockExampleRef: IO[(Unit, Unit)] = {
    ticksRef.flatMap(r => (tickingClock(r), printTicks(r)).parTupled)
  }

  // Ref update impure
  // Do not do impure side effects in ref modify
  // This is because modify / update methods will retry if another concurrent update succeeds
  def refUpdateImpure: IO[Unit] =
    for {
      ref <- Ref[IO].of(0)
      _ <- List(1, 2, 3).parTraverse(task(_, ref))
    } yield ()

  def task(id: Int, ref: Ref[IO, Int]): IO[Unit] = {
    ref
      .modify { previous =>
        id -> println(s"$previous->$id")
      } //change println to IO(....).debug with flatten as we'll have a nested IO
      .replicateA(3)
      .void
  }

  // Write-once synchronization with Deferred

}
