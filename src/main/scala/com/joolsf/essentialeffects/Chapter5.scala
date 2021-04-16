package com.joolsf.essentialeffects

import cats.effect.implicits._
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import com.joolsf.essentialeffects.debug.DebugHelper

import scala.concurrent.duration.{DurationInt, FiniteDuration}


class Chapter5()(implicit cs: ContextShift[IO], t: Timer[IO]) {

  //5.1
  def parallelExample1() = {
    def task(i: Int): IO[Int] = IO(i).debug
    val numCpus = Runtime.getRuntime().availableProcessors()
    val tasks = List.range(0, numCpus * 2).parTraverse(task)


    for {
    _ <- IO(s"number of CPUs: $numCpus")
    _ <- tasks.debug
    } yield ()
  }

}
