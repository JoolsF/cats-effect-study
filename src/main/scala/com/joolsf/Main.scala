package com.joolsf

import cats.effect.{ExitCode, IO, IOApp}
import com.joolsf.essentialeffects.{Chapter2, Chapter3}

import scala.concurrent.duration.DurationInt


object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    //    Runner.run(List())
    Chapter3.futureParallelExample

    IO.sleep(5 seconds).map(_ => ExitCode.Success)
  }

}


