package example

object Main extends  App {

}

object FileCopier {
  import cats.effect.IO
  import java.io.File

  def copy(origin: File, destination: File): IO[Long] = ???
}

