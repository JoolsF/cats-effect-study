package com.joolsf.concurrency

import cats.effect.concurrent.Ref
import cats.effect.{IO, Sync}
import cats.implicits._

import scala.concurrent.ExecutionContext
//https://typelevel.org/cats-effect/concurrency/ref.html
object RefExample1 {

  // Needed for triggering evaluation in parallel
  implicit val ctx = IO.contextShift(ExecutionContext.global)

  class Worker[F[_]](number: Int, ref: Ref[F, Int])(implicit F: Sync[F]) {

    private def putStrLn(value: String): F[Unit] = F.delay(println(value))

    def start: F[Unit] =
      for {
        c1 <- ref.get
        _ <- putStrLn(show"Before #$number >> $c1")
        c2 <- ref.modify(x => (x + 1, x))
        _ <- putStrLn(show"After  #$number >> $c2")
      } yield ()

  }

  val program: IO[Unit] =
    for {
      ref <- Ref.of[IO, Int](0)
      w1 = new Worker[IO](1, ref)
      w2 = new Worker[IO](2, ref)
      w3 = new Worker[IO](3, ref)
      _ <- List(
        w1.start,
        w2.start,
        w3.start
      ).parSequence.void
      res <- ref.get
      _ <- IO(println(s"Result: $res"))
    } yield ()

}

//To illustrate how the above would work without using Ref
object RefExample1Unsafe {
  var ref: Int = 0

  // Needed for triggering evaluation in parallel
  implicit val ctx = IO.contextShift(ExecutionContext.global)

  class Worker[F[_]](number: Int)(implicit F: Sync[F]) {

    private def putStrLn(value: String): F[Unit] = F.delay(println(value))

    def start: F[Unit] = {
      for {
        _ <- putStrLn(show"Before #$number >> $ref")
        _ <- F.delay {
          val refBefore = ref
          ref = ref + 1
          refBefore
        }
        _ <- putStrLn(show"After  #$number >> $ref")
      } yield ()
    }

  }

//  val program: IO[Unit] = {
//
//    for {
//      _ <- IO(ref = 0)
//      _ <- IO.unit
//      w1 = new Worker[IO](1)
//      w2 = new Worker[IO](2)
//      w3 = new Worker[IO](3)
//      _ <- List(
//        w1.start,
//        w2.start,
//        w3.start
//      ).parSequence.void
//      _ <- IO(println(s"Result: $ref"))
//    } yield ()
//  }

}
object RefExample2 {

  // Needed for triggering evaluation in parallel
  implicit val ctx = IO.contextShift(ExecutionContext.global)

  class Worker[F[_]](ref: Ref[F, Int])(implicit F: Sync[F]) {
    def start: F[Int] = ref.modify(x => (x + 1, x))

  }

  def workers(ref: Ref[IO, Int]) =
    Range(1, 10000).map(_ => new Worker[IO](ref)).map(_.start).toList

  val program = {

    for {
      ref <- Ref.of[IO, Int](0)
      _ <- workers(ref).parSequence.void
      res <- ref.get
      _ <- IO(println(s"Result: $res"))
    } yield ()
  }

}

object RefExample2Unsafe {
  var ref: Int = 0

  // Needed for triggering evaluation in parallel
  implicit val ctx = IO.contextShift(ExecutionContext.global)

  class Worker[F[_]]()(implicit F: Sync[F]) {

    def start: F[Unit] = F.delay { ref = ref + 1 }

  }

  val workers =
    Range(1, 10000).map(_ => new Worker[IO]).map(_.start).toList

//  val program: IO[Unit] = {
//
//    for {
//      _ <- IO(ref = 0)
//      _ <- IO.unit
//      _ <- workers.parSequence.void
//      _ <- IO(println(s"Result: $ref"))
//    } yield ()
//  }

}
