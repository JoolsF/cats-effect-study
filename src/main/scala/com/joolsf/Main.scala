package com.joolsf

import cats.effect.{ExitCode, IO, IOApp}
import com.joolsf.essentialeffects.Chapter7

import scala.concurrent.duration.DurationInt

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    new Chapter7().testContextExample1(2 seconds, 3 seconds)
    IO.sleep(4.seconds) *> IO(
      ExitCode.Success
    )

  }
}
