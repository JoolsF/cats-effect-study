package com.joolsf

import cats.effect.{ExitCode, IO, IOApp}
import com.joolsf.essentialeffects.Chapter5

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    new Chapter5().parallelExample1().map(_ => ExitCode.Success)
  }
}
