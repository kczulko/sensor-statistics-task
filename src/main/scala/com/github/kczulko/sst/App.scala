package com.github.kczulko.sst

import fs2.io
import fs2.{text, Stream}

import cats.effect.{Blocker, ContextShift, Concurrent}
import cats.instances.map._

import java.io.{File, FilenameFilter}
import java.nio.file.Path

import com.github.kczulko.sst.ResultInterpreter._

final case class App[F[_]: ContextShift: Concurrent](
  directory: String,
  resultInterpreter: ResultInterpreter[F]
) {

  val validHumidity = raw"(\d+)".r

  val toMeasurementEntry: String => MeasurementEntry = _.split(StringConstants.comma).toList match {
    case sensorId :: StringConstants.NaN :: _ => MeasurementEntry(sensorId, None)
    case sensorId :: validHumidity(value) :: _ =>
      MeasurementEntry(sensorId, Some(value.toInt))
    case _ => MeasurementEntry(StringConstants.measureError, None)
  }

  val toSensorMeasurements: MeasurementEntry => Map[String, SensorMeasurements] = {
    case MeasurementEntry(sensorId, humidityOpt) =>
      val error = 0
      val valid = 1
      Map(
        sensorId -> SensorMeasurements(
          data = humidityOpt.map(h => MeasurementState(h, h, h, h)),
          failedCount = humidityOpt.fold(error)(_ => valid),
          totalMeasurements = 1
        )
      )
  }

  def program: Stream[F,Unit] = {
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
      .evalMap(resultInterpreter(csvPaths.size, _))
  }
}
