package com.joolsf.essentialeffects

import cats.effect.{ContextShift, IO, Timer}
import com.joolsf.essentialeffects.debug.DebugHelper

import java.util.concurrent.CompletableFuture
import scala.jdk.FunctionConverters.enrichAsJavaBiFunction

/*
  1. IO.async allows us to build effects that (1) can start asynchronous processes; (2)
  can emit one result on completion or can end in error.

  2. Asynchronous effects fundamentally rely upon continuation passing, where
  the actual asynchronous computation is given code to run when the
  computation completes.

  3. scala.concurrent.Future is a common source of asynchronous computation.
  IO.fromFuture transforms a Future into a referentially-transparent effect
 */
class Chapter6()(implicit cs: ContextShift[IO], t: Timer[IO]) {

  /*
    Exercise 4: Java CompletableFuture

    Use IO.async to adapt a java.util.concurrent.CompletableFuture into an IO value.
    Implement handler.  Note java signature (A, Throwable).  Expection here is either A
    OR Throwable will be null.
   */

  val effect: IO[String] =
    fromCF(IO(cf())).debug

  def fromCF[A](cfa: IO[CompletableFuture[A]]): IO[A] =
    cfa.flatMap { fa: CompletableFuture[A] =>
      IO.async { cb =>
        val handler: (A, Throwable) => Unit = {
          case (a, null) => cb(Right(a))
          case (null, t) => cb(Left(t))
          case (a, t)    => sys.error("Should have one null")
        }
        fa.handle(handler.asJavaBiFunction)
        ()
      }
    }
  def cf(): CompletableFuture[String] =
    CompletableFuture.supplyAsync(() => "woo!")

  /*
    Exercise 5: Never!
   */
  def runNever() =
    never.guarantee(IO("I guess never is now").debug.void)

  val never: IO[Nothing] =
    IO.async { (cb: Either[Throwable, Nothing] => Unit) =>
      ()
    }
}
