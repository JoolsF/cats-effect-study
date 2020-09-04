package com.joolsf.datatypes

object SemaphoreExample1 {
  import cats.effect.{Concurrent, IO, Timer}
  import cats.effect.concurrent.Semaphore
  import cats.implicits._

  import scala.concurrent.ExecutionContext
  import scala.concurrent.duration._

  // Needed for getting a Concurrent[IO] instance
  implicit val ctx = IO.contextShift(ExecutionContext.global)
  // Needed for `sleep`
  implicit val timer = IO.timer(ExecutionContext.global)

  class PreciousResource[F[_]](name: String, s: Semaphore[F])(implicit
      F: Concurrent[F],
      timer: Timer[F]
  ) {
    def use: F[Unit] =
      for {
        x <- s.available
        _ <- F.delay(println(s"$name >> Availability: $x"))
        _ <- s.acquire
        y <- s.available
        _ <- F.delay(println(s"$name >> Started | Availability: $y"))
        _ <- timer.sleep(2.seconds)
        _ <- s.release
        z <- s.available
        _ <- F.delay(println(s"$name >> Done | Availability: $z"))
      } yield ()
  }

  val program: IO[Unit] =
    for {
      s <- Semaphore[IO](1)
      r1 = new PreciousResource[IO]("R1", s)
      r2 = new PreciousResource[IO]("R2", s)
      r3 = new PreciousResource[IO]("R3", s)
      _ <- List(r1.use, r2.use, r3.use).parSequence.void
    } yield ()

}
