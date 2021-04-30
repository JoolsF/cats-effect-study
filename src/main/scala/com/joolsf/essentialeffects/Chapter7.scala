package com.joolsf.essentialeffects

import cats.effect.{ContextShift, IO, Timer}
import com.joolsf.essentialeffects.debug.DebugHelper

import java.util.concurrent.CompletableFuture
import scala.jdk.FunctionConverters.enrichAsJavaBiFunction


class Chapter7()(implicit cs: ContextShift[IO], t: Timer[IO]) {


}
