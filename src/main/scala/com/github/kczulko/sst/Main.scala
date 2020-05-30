package com.github.kczulko.sst

import cats.syntax.applicative._
import cats.effect.{IOApp, IO, ExitCode}

import com.github.kczulko.sst.ResultInterpreter.consoleInterpreter

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    args.headOption.fold(ExitCode.Error.pure[IO]) {
      App[IO](_, consoleInterpreter[IO])
        .program
        .compile
        .drain
        .as(ExitCode.Success)
    }
}
