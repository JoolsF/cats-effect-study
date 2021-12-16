package com.joolsf.essentialeffects

import cats.effect.laws.util.TestContext
import cats.effect.{ContextShift, IO, Timer}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, TimeoutException}
import scala.util.{Failure, Success}
class Chapter8 {

  //TODO
  /*
   * Testing effects with TestContext allows us to control the effect scheduling clock manually
   */
  def testContextExample1(
      timeoutTime: FiniteDuration,
      tickTime: FiniteDuration
  ) = {

    val ctx: TestContext = TestContext()

    implicit val cs: ContextShift[IO] = ctx.ioContextShift
    implicit val timer: Timer[IO] = ctx.timer

    val timeoutError = new TimeoutException
    val timeout: IO[Int] =
      IO.sleep(timeoutTime) *> IO.raiseError[Int](timeoutError)
    val f: Future[Int] = timeout.unsafeToFuture() //

    ctx.tick(tickTime)

    f.onComplete {
      case Success(value)     => println(value)
      case Failure(exception) => println(exception)
    }
  }
}
