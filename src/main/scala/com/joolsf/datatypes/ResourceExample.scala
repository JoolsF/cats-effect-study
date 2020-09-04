package com.joolsf.datatypes

import cats.Applicative.ops.toAllApplicativeOps
import cats.effect.{IO, Resource}

import scala.reflect.ClassManifestFactory.Nothing

/*
 Resource takes care of the LIFO (Last-In-First-Out) order of acquiring / releasing.
 If something goes wrong the resources are released
 */
object ResourceExample {

  def mkResource(s: String): Resource[IO, String] = {
    val acquire = IO(println(s"Acquiring $s")) *> IO.pure(s)

    def release(s: String) = IO(println(s"Releasing $s"))

    Resource.make(acquire)(release)
  }

  def resources(error: Boolean) = {
    for {
      outer <- mkResource("outer")
      _ <- raiseError(error)
      inner <- mkResource("inner")
    } yield (outer, inner)
  }

  def program(error: Boolean): IO[Unit] =
    resources(error)
      .use { case (a, b) => IO(println(s"Using $a and $b")) }
      .handleErrorWith { error => IO(println(s"Handled error: $error")) }

  def raiseError(error: Boolean) = {
    if (error) Resource.liftF(IO.raiseError(new Throwable("Boom!")))
    else Resource.liftF(IO.unit)
  }

}
