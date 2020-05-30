package com.github.kczulko.sst

import fs2.io
import fs2.{text, Stream}
import cats.effect._
import cats.syntax.applicative._
import cats.instances.map._
import cats.kernel.Monoid
import cats.derived.auto.semigroup._

import java.io.File
import java.io.FilenameFilter
import java.nio.file.Path

import com.github.kczulko.sst.ResultsInterpreter._

object Main extends IOApp {

  val validHumidity = raw"(\d+)".r

  val toMeasurementEntry: String => MeasurementEntry = _.split(StringConstants.comma).toList match {
    case sensorId :: StringConstants.NaN :: _ => MeasurementEntry(sensorId, None)
    case sensorId :: measurement :: _ if validHumidity.matches(measurement) =>
      MeasurementEntry(sensorId, Some(measurement.toInt))
    case _ => MeasurementEntry("measure-error", None)
  }

  val toSensorMeasurements: MeasurementEntry => Map[String, SensorMeasurements] = {
    case MeasurementEntry(sensorId, Some(humidity)) =>
      Map(
        sensorId -> SensorMeasurements(
          Some(MeasurementState(humidity, humidity, humidity, humidity)), 0, 1
        )
      )
    case MeasurementEntry(sensorId, _) => Map(
      sensorId -> SensorMeasurements(None, 1, 1)
    )
  }

  def program[F[_]: Concurrent: ContextShift](
    directory: String,
    resultsInterpreter: ResultInterpreter[F]
  ): Stream[F, Unit] = {
    val header = 1
    val chunkSize = 4096

    val onlyCsvFiles: FilenameFilter =
      (_: File, name: String) => name.toLowerCase.endsWith(StringConstants.csvExtension)

    val csvPaths: List[Path] = Option(
      (new File(directory)).listFiles(onlyCsvFiles)
    ).toList.flatten.map(_.toPath)

    val streams = for {
      blocker   <- Stream.resource(Blocker[F])
      path      <- Stream(csvPaths: _*)
      fileStream = io.file.readAll(path, blocker, chunkSize)
        .through(text.utf8Decode andThen text.lines)
        .drop(header)
        .filter(!_.isEmpty())
        .foldMap[Map[String,SensorMeasurements]](
          toMeasurementEntry andThen toSensorMeasurements
        )
    } yield fileStream

    streams
      .parJoinUnbounded
      .foldMonoid
      .evalMap(resultsInterpreter(csvPaths.size, _))
  }

  override def run(args: List[String]): IO[ExitCode] =
    args.headOption.fold(ExitCode.Error.pure[IO]) {
      program[IO](_, consoleInterpreter).compile.drain.as(ExitCode.Success)
    }
}
