package example

import fs2.io
import fs2.{text, Stream}
import cats.effect._
import java.nio.file.Paths
import scala.io.Source
import java.io.File

import cats.syntax.applicative._
import cats.kernel.Semigroup
import cats.instances.map.catsKernelStdMonoidForMap
import java.io.FilenameFilter
import cats.kernel.Monoid
import java.nio.file.Paths
import java.nio.file.Path

final case class MeasurementEntry(sensorId: String, humidity: Option[Int])

final case class MeasurementStateData(
  currentMeasure: Int,
  min: Int,
  max: Int,
  avg: Float
)

final case class MeasurementState(
  data: Option[MeasurementStateData],
  failedCount: Long,
  measurementsCount: Long,
)

object MeasurementState {
  implicit val semigroup: Semigroup[MeasurementState] = {
    case (
      MeasurementState(Some(dataLeft), failedLeft, totalLeft),
      MeasurementState(Some(dataRight), failedRight, totalRight)
    ) =>
      val validLeftCount = totalLeft - failedLeft
      val validRightCount = totalRight - failedRight
      val currentMeasure = dataRight.currentMeasure
      val min = math.min(
        math.min(dataLeft.min, dataRight.min),
        math.min(dataLeft.currentMeasure, dataRight.currentMeasure)
      )
      val max = math.max(
        math.max(dataLeft.max, dataRight.max),
        math.max(dataLeft.currentMeasure, dataRight.currentMeasure)
      )
      val avg = ((dataLeft.avg * validLeftCount + dataRight.avg * validRightCount)/(validLeftCount + validRightCount))
      MeasurementState(
        Some(MeasurementStateData(currentMeasure, min, max, avg)),
        failedLeft + failedRight,
        totalLeft + totalRight
      )
    case (left, right) => {
      import cats.derived.auto.semigroup._
      import cats.implicits._
      MeasurementState(
        left.data |+| right.data,
        left.failedCount + right.failedCount,
        left.measurementsCount + right.measurementsCount
      )
    }
  }
}

object Main extends IOApp {

  val validHumidity = raw"(\d+)".r

  val toMeasurementEntry: String => MeasurementEntry = _.split(",").toList match {
    case sensorId :: ("nan" | "Nan") :: _ => MeasurementEntry(sensorId, None)
    case sensorId :: measurement :: _ if validHumidity.matches(measurement) =>
      MeasurementEntry(sensorId, Some(measurement.toInt))
    case _ => MeasurementEntry("measure-error", None)
  }

  val toMeasurementState: MeasurementEntry => Map[String, MeasurementState] = {
    case MeasurementEntry(sensorId, Some(humidity)) =>
      Map(
        sensorId -> MeasurementState(
          Some(
            MeasurementStateData(humidity, humidity, humidity, humidity)
          ),
          0,
          1
        )
      )
    case MeasurementEntry(sensorId, _) => Map(
      sensorId -> MeasurementState(None, 1, 1)
    )
  }

  type ResultInterpreter[F[_]] = (Int, Map[String,MeasurementState]) => F[Unit]

  import cats.syntax.foldable._

  def consoleInterpreter[F[_]: Sync]: ResultInterpreter[F] = {
    case (numberOfFiles, result) =>
      val numberOfSensorReports = 10
      val processedMeasurements = result.values.map(_.measurementsCount).reduce(_ + _)
      val failedMeasurements = result.values.map(_.failedCount).reduce(_ + _)
      val highestAvgHumidity = result.toSeq.sortBy {
        case (_, measurement) => measurement.data.fold(Float.MinValue)(_.avg)
      }(Ordering[Float].reverse)
        .take(numberOfSensorReports)
        .map {
          case (sensorId, measurement) =>
            s"$sensorId,${measurement.data.fold("Nan")(_.avg.toString())}"
        }.mkString("\n")

      val resultMsg = s"""
        |Num of processed files: $numberOfFiles
        |Num of processed measurements: $processedMeasurements
        |Num of failed measurements: $failedMeasurements
        |
        |Sensors with highest avg humidity:
        |$highestAvgHumidity
      """.stripMargin

      Sync[F].delay(println(resultMsg))
  }

  def program[F[_]: Concurrent: ContextShift](
    directory: String,
    resultsInterpreter: ResultInterpreter[F]
  ): Stream[F, Unit] = {
    val header = 1
    val chunkSize = 4096

    val onlyCsvFiles: FilenameFilter =
      (_: File, name: String) => name.toLowerCase.endsWith(".csv")

    lazy val csvPaths: List[Path] = Option(
      (new File("./files")).listFiles(onlyCsvFiles)
    ).toList.flatten.map(_.toPath)

    val streams = for {
      blocker   <- Stream.resource(Blocker[F])
      path      <- Stream(csvPaths: _*)
      fileStream = io.file.readAll(path, blocker, chunkSize)
        .through(text.utf8Decode andThen text.lines)
        .drop(header)
        .filter(!_.isEmpty())
        .foldMap[Map[String,MeasurementState]](
          toMeasurementEntry andThen toMeasurementState
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
