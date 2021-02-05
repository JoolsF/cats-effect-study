package com.joolsf.essentialeffects
import cats.effect.{ContextShift, IO}
import cats.implicits._
import cats.{Parallel, effect}

import scala.concurrent._
import scala.concurrent.duration._
object Chapter3 {

  implicit val ec = ExecutionContext.global

  // Only 1 result will be visible here since Future eagerly schedules the action and caches the result.
  // Unsafe side effect not separately executed from functions like the Future constructor.  It's not an
  // 'Effect' by book's definition therefore.
  //
  // Future isn't implemented with parallelism so much as it comes as a side effect of eagerly scheduling
  // the computation.  E.g. in the mapN example this happens before mapN is evaluated.
  def futureParallelExample: Unit = {

    // If we change these 2 vals to defs we run get two pairs of Hello and World
    def hello = Future(println(s"[${Thread.currentThread.getName}] Hello"))
    def world = Future(println(s"[${Thread.currentThread.getName}] World"))

    val hw1: Future[Unit] =
      for {
        _ <- hello
        _ <- world
      } yield ()

    Await.ready(hw1, 5.seconds)

    val hw2: Future[Unit] =
      (hello, world).mapN((_, _) => ())

    Await.ready(hw2, 5.seconds)
  }

  // This is sequential since IO does not have parallelism built in
  // Will output the same threads
  def ioParallelExample: IO[Unit] = {
    val hello = IO(println(s"[${Thread.currentThread.getName}] Hello"))
    val world = IO(println(s"[${Thread.currentThread.getName}] World"))

    val hw1: IO[Unit] =
      for {
        _ <- hello
        _ <- world
      } yield ()

    val hw2: IO[Unit] =
      (hello, world).mapN((_, _) => ())

    hw1 *> hw2
  }

  //The parallel typeclass

  def theParallelTypeClassExample1[A](implicit
      cs: ContextShift[IO]
  ): IO[String] = {

    def ia: IO[Int] = IO(2)
    def ib: IO[Int] = IO(4)

    def f(a: Int, b: Int): String = s"Result is ${a * b}"

    val ipa: IO.Par[Int] = IO.Par(ia)
    val ipb: IO.Par[Int] = IO.Par(ib)

    val ipc: effect.IO.Par[String] = (ipa, ipb).mapN(f)

    IO.Par.unwrap(ipc)

  }

  /*
    trait Parallel[S[_]] {    <- S is the sequential type to transformed
      type P[_]                 <- The typeclass instance defined the P type for a parallel e.g. for Parallel[IO] it would be IO.Par

      def monad: Monad[S]       <- S must have a Monad
      def applicative: Applicative[P]     <- P must have an applicative i.e operations using P must not have any data ordering dependencies
      def sequential: P ~> S    <- Convert from P to S
      def parallel: S ~> P      <- Convert from S to P
   }

   ~> is a type alias for cats.arrow.FuntionK which does an F[A] =>G[A] transformation
   */

  // Refactoring example 1 to use the above type class
  def theParallelTypeClassExample2[A](implicit
      cs: ContextShift[IO]
  ): IO[String] = {

    def ia: IO[Int] = IO(2)
    def ib: IO[Int] = IO(4)

    def f(a: Int, b: Int): String = s"Result is ${a * b}"

    val ipa = Parallel[IO, IO.Par].parallel(ia)
    val ipb = Parallel[IO, IO.Par].parallel(ib)

    val ipc: IO.Par[String] = (ipa, ipb).mapN(f)

    Parallel[IO, IO.Par].sequential(ipc)

  }

  // Refactoring example 2 - once we have a Parallel typeclass, par-prefixed versions of functions become available
  def theParallelTypeClassExample2[A](implicit
      cs: ContextShift[IO]
  ): IO[String] = {

    def ia: IO[Int] = IO(2)
    def ib: IO[Int] = IO(4)

    def f(a: Int, b: Int): String = s"Result is ${a * b}"

    val ic: IO[String] = (ia, ib).parMapN(f)
    ic

  }



}
