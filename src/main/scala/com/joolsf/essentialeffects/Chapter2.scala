package com.joolsf.essentialeffects

import cats.effect.{IO, Timer}
import cats.implicits._

import scala.concurrent.duration.DurationInt

object Chapter2 {

  //Essential effects - chapter 2 exercise
  def tickingClock(implicit t: Timer[IO]): IO[Unit] = {
    val tick = IO {
      val sysTime = System.currentTimeMillis()
      println(s"System time: $sysTime")
    }

    for {
      _ <- tick
      _<- IO.sleep(1.second)
      _<- tickingClock
    } yield ()

  }

}
