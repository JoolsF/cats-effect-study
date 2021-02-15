package com.joolsf

import cats.effect.{ExitCode, IO, IOApp}
import com.joolsf.essentialeffects.{Chapter3, Chapter4}

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    new Chapter4().cancelExample1().map(_ => ExitCode.Success)
  }

}
