package com.joolsf

import cats.Applicative.ops.toAllApplicativeOps
import cats.effect.{ExitCode, IO, IOApp}
import com.joolsf.concurrency.{
  RefExample1,
  RefExample1Unsafe,
  RefExample2,
  RefExample2Unsafe
}
import com.joolsf.datatypes.{ResourceExample, SemaphoreExample1}

import scala.util.control.NonFatal

object Main extends IOApp {

  def getStrLn(): IO[String] = IO(scala.io.StdIn.readLine())
  def printStrLn(str: String): IO[Unit] = IO(println(str))

  def run(args: List[String]): IO[ExitCode] = {

    def runSelection(i: String): IO[Unit] =
      i.filterNot(_ == ' ') match {
        case "1" =>
          ResourceExample.program(true) *> getInput
        case "2" =>
          ResourceExample.program(false) *> getInput
        case "3"    => RefExample1.program *> getInput
        case "4"    => RefExample1Unsafe.program *> getInput
        case "5"    => RefExample2.program *> getInput
        case "6"    => RefExample2Unsafe.program *> getInput
        case "7"    => SemaphoreExample1.program *> getInput
        case "exit" => IO(println("bye bye")) *> IO.unit
        case args =>
          printStrLn(s"Warning args: $args not recognised") *> getInput

      }

    def getInput = {
      printStrLn("\n*** Please enter new program name  ***\n") *>
        getStrLn().flatMap(runSelection)
    }

    runSelection(args mkString ",")

  }.map(_ => ExitCode.Success)
    .handleErrorWith {
      case NonFatal(error) =>
        printStrLn(
          s"Non-fatal error occurred: ${error.getMessage} -  handling and returning success exit code"
        ) *>
          IO(ExitCode.Success)
    }

}
