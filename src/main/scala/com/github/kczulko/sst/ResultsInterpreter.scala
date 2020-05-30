package com.github.kczulko.sst

import cats.Foldable
import cats.effect.Sync
import cats.kernel.Monoid
import cats.instances.list._
import cats.instances.long._

object ResultsInterpreter {

  type ResultInterpreter[F[_]] = (Int, Map[String, SensorMeasurements]) => F[Unit]

  def consoleInterpreter[F[_]: Sync]: ResultInterpreter[F] = {
    case (numberOfFiles, result) =>

      def reduceMeasurementsBy[B: Monoid](f: SensorMeasurements => B) =
        Foldable[List].foldMap[SensorMeasurements, B](result.values.toList)(f)

      val numberOfSensorReports = 10
      val processedMeasurements = reduceMeasurementsBy(_.totalMeasurements)
      val failedMeasurements = reduceMeasurementsBy(_.failedCount)
      val highestAvgHumidity = result.toSeq.sortBy {
        case (_, measurement) => measurement.data.fold(Float.MinValue)(_.avg)
      }(Ordering[Float].reverse)
        .take(numberOfSensorReports)
        .map {
          case (sensorId, measurement) =>
            lazy val nans = List.fill(3)(StringConstants.NaN)
              .mkString(StringConstants.comma)
            val stats = measurement
              .data
              .map(data => s"${data.min},${data.avg},${data.max}")
              .getOrElse(nans)

            s"$sensorId,$stats"
        }.mkString(StringConstants.newline)

      val outputMsg = s"""
        |Num of processed files: $numberOfFiles
        |Num of processed measurements: $processedMeasurements
        |Num of failed measurements: $failedMeasurements
        |
        |Sensors with highest avg humidity:
        |
        |sensor-id,min,avg,max
        |$highestAvgHumidity
      """.stripMargin

      Sync[F].delay(println(outputMsg))
  }

}
