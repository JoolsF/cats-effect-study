package com.joolsf.essentialeffects
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import cats.{Parallel, effect}
import com.joolsf.essentialeffects.debug.DebugHelper

import scala.concurrent._
import scala.concurrent.duration._

/*
  Chapter 3 - Parallel execution

  1. IO does not support parallel operations itself, because it is a Monad.
  2. The Parallel typeclass specifies the translation between a pair of effect types:
  one that is a Monad and the other that is “only” an Applicative.
  3. Parallel[IO] connects the IO effect to its parallel counterpart, IO.Par.
  4. Parallel IO composition requires the ability to shift computations to other
  threads within the current ExecutionContext. This is how parallelism is
  “implemented”.
  5. parMapN, parTraverse, parSequence are the parallel versions of (the sequential)
  mapN, traverse, and sequence. Errors are managed in a fail-fast manner.
 */
object Chapter3 {

  implicit val ec = ExecutionContext.global

  //TODO
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

  //TODO
  // This is sequential since IO does not have parallelism built in
  // Will output the same threads
  def ioParallelExample(implicit cs: ContextShift[IO]): IO[Unit] = {
    val hello: IO[Unit] = IO(println(s"[${Thread.currentThread.getName}] Hello"))
    val world: IO[Unit] = IO(println(s"[${Thread.currentThread.getName}] World"))

    val hw1: IO[Unit] =
      for {
        _ <- hello
        _ <- world
      } yield ()

    val hw2: IO[Unit] =
      (hello, world).parMapN((_, _) => ())

    hw2
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
  def theParallelTypeClassExample3[A](implicit
      cs: ContextShift[IO]
  ): IO[String] = {

    def ia: IO[Int] = IO(2)
    def ib: IO[Int] = IO(4)

    def f(a: Int, b: Int): String = s"Result is ${a * b}"

    val ic: IO[String] = (ia, ib).parMapN(f)
    ic

  }

  // Sequential effect
  def debugExampleSeq(): IO[String] = {

    val hello = IO("hello").debug
    val world = IO("world").debug

    (hello, world, hello, world)
      .mapN((h, w, h2, w2) => s"$h $w $h2 $w2")
      .debug
  }

  // Parallel effects
  def debugExamplePar()(implicit contextShift: ContextShift[IO]): IO[String] = {

    val hello = IO("hello").debug
    val world = IO("world").debug

    (hello, world, hello, world)
      .parMapN((h, w, h2, w2) => s"$h $w $h2 $w2")
      .debug
  }

  //The first failure to happen in parMapN is used as the failure of the composed effect
  def parMapNErrors()(implicit cs: ContextShift[IO], t: Timer[IO]) = {

    val ok = IO("hi").debug
    val ko1 = IO
      .sleep(1.second)
      .as(
        "ko1"
      ) *> //Run with and without this sleep and observe the effect on e3
      IO.raiseError[String](new RuntimeException("oh!")).debug
    val ko2 = IO.raiseError[String](new RuntimeException("ko2!")).debug

//    val e1 = (ok, ko1).parMapN((_, _) => ()) // parTupled below works better here. No need to pass in a function for example
    val e1 = (ok, ko1).parTupled.void
    val e2 = (ko1, ok).parTupled.void
    val e3 = (ko1, ko2).parTupled.void

    e1.attempt.debug *>
      IO("---").debug *>
      e2.attempt.debug *>
      IO("---").debug *>
      e3.attempt.debug *> IO.pure(())

  }

  /*
    F[A] => (A => G[B]) => G[F[B]]

    e.g apply a function A => IO[B] to each element in List[A]
    List[A] => (A => IO[B]) => IO[List[B]]

    In example below, its necessary to wait until all elements have been traversed but the returned List[B] can be
    built incrementally
   */
  def parTraverse()(implicit p: ContextShift[IO]): IO[List[Int]] = {

    val numTasks = 100
    val tasks = List.range(0, numTasks)

    def task(id: Int): IO[Int] = IO(id).debug

    tasks.parTraverse(task).debug

  }

  //Could also be thought of as a variation of (par)mapN where results are collected but every input effect has same
  //same output type
  def parTraverseAlternateView()(implicit p: ContextShift[IO]) = {

    def f(i: Int): IO[Int] = IO(i)

    (f(1), f(2)).parMapN((a, b) => List(a, b))
    (f(1), f(2), f(3)).parMapN((a, b, c) => List(a, b, c))

  }

  /*
    Sequence
    F[G[A]] => G[F[A]]
    e.g.
    List[IO[A]] => IO[List[A]]
   */
  def parSequence()(implicit cs: ContextShift[IO]): IO[List[Int]] = {
    val numTasks = 100
    def tasks: List[IO[Int]] = List.tabulate(numTasks)(task)

    def task(id: Int): IO[Int] = IO(id).debug

    tasks.parSequence.debug
  }

}

