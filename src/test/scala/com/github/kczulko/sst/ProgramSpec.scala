package com.github.kczulko.sst

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.Resources
import cats.effect.ContextShift
import cats.effect.ConcurrentEffect
import cats.effect.IO

import scala.concurrent.ExecutionContext.Implicits.global

class ProgramSpec extends AnyFlatSpec
    with Matchers
    with ProgramSpecExpectedResults {

  implicit val cs = IO.contextShift(global)

  def testInterpreter(
    expectedNumberOfProcessedFiles: Int,
    expectedResults: Map[String,SensorMeasurements]
  ): ResultInterpreter.ResultInterpreter[IO] = {
    case (numOfFiles, results) =>

      // TODO: code duplication!
      val programResults = results.mapValues { value =>
        val data = value
          .data
          .map(measurement => (measurement.avg, measurement.min, measurement.max))
        (data, value.failedCount, value.totalMeasurements)
      }.toMap

      val modifiedExpectedResults = expectedResults.mapValues { value =>
        val data = value
          .data
          .map(measurement => (measurement.avg, measurement.min, measurement.max))
        (data, value.failedCount, value.totalMeasurements)
      }.toMap

      numOfFiles shouldEqual expectedNumberOfProcessedFiles
      programResults shouldEqual modifiedExpectedResults
      IO.unit    
  }

  def test(params: TestParameters) =
    "program[F]" should s"properly analyze ${params.suiteName}" in {
      import params._

      App[IO](
        s"src/test/resources/${params.suiteName}",
        testInterpreter(expectedNumberOfFiles, expectedResults)
      ).program
       .compile
       .drain
       .unsafeRunSync()
    }

  it should behave like test(singleFileSingleSensor)
  it should behave like test(singleFileSingleSensorOnlyNaNs)
  it should behave like test(singleFileInvalidEntries)
  it should behave like test(multipleFilesSingleSensor)
  it should behave like test(multipleFilesMultipleSensorsNaNs)
  it should behave like test(multipleFilesInvalidEntries)
  it should behave like test(nonExistingDirectory)
  it should behave like test(emptyDirectory)
}
