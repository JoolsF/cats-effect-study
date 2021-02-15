package com.joolsf.errors

import cats.effect.{ContextShift, IO}


import scala.concurrent.Future
import scala.util.control.NonFatal

class ErrorHandlingExample1()(implicit cs: ContextShift[IO]) {

  import Helpers._



  // Examples a
  val succeed: IO[Int] = fromFuture(future(1, false))
  val fail: IO[Int] = fromFuture(future(2, true))

  val program1: IO[Int] = fail
    .handleErrorWith {
      case e: ServiceError => IO(0)
      case NonFatal(msg)   => IO(-1)
    }

  val program2: IO[Either[String, Int]] = fail.attempt
    .map {
      case Right(i)               => Right(i)
      case Left(se: ServiceError) => Right(-1)
      case e                      => Left(s"error $e")
    }

  // Examples b - Composition and fail fast behaviour

  val program3: IO[Int] = {
    for {
      res1 <- fromFuture(future(1, false))
      _ <- putStrLn("res 1")
      res2 <- fromFuture(future(2, false))
      _ <- putStrLn("res 2")
      res3 <- fromFuture(future(3, true))
      _ <- putStrLn("res 3")
      res4 <- fromFuture(future(4, false))
      _ <- putStrLn("res 4")
    } yield res1 + res2 + res3 + res4
  }.handleErrorWith { case e: ServiceError => IO(-1) }

}

object Helpers {

  // Errors
  trait ServiceError extends Throwable {
    val statusCode: Int
    val msg: String
  }

  case class ErrorCaseOne(msg: String) extends ServiceError {
    val statusCode = 500
  }



  // Helpers
  def putStrLn(str: String): IO[Unit] = IO(println(str))

  def future(i: Int, fail: Boolean): Future[Int] =
    if (fail)
      Future.failed(ErrorCaseOne("failure"))
    else
      Future.successful(i)

  def fromFuture[A](f: => Future[A])(implicit cs: ContextShift[IO]): IO[A] =
    IO.fromFuture(IO(f))
}
