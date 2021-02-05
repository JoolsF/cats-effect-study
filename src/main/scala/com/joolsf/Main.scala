package com.joolsf

import cats.effect.{ExitCode, IO, IOApp}
import com.joolsf.essentialeffects.Chapter2


object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    //    Runner.run(List())
    Chapter2.tickingClock.map(_ => ExitCode.Success)
  }

}


