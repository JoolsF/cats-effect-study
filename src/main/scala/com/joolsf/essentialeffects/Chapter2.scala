package com.joolsf.essentialeffects

import cats.effect.{IO, Timer}

import scala.concurrent.duration.DurationInt

object Chapter2 {
  /*
  What is the definition of 'effect' here

  1. Does the type of the program tell us
    a. what kind of effects the program will perform
    b. what type of value will it produce?
  2. When externally-visible side effects are required, is the effect description separate from the execution?
  */

  //Essential effects - chapter 2 exercise
  def tickingClock(implicit t: Timer[IO]): IO[Unit] = {
    val tick = IO {
      val sysTime = System.currentTimeMillis()
      println(s"System time: $sysTime")
    }

    for {
      _ <- tick
      _ <- IO.sleep(1.second)
      _ <- tickingClock
    } yield ()

  }

}
