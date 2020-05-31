package com.github.kczulko.sst

final case class TestParameters(
  suiteName: String,
  expectedNumberOfFiles: Int,
  expectedResults: Map[String, SensorMeasurements]
)

trait ProgramSpecExpectedResults {

  def singleFileSingleSensor = TestParameters(
    "singleFileSingleSensor",
    1,
    Map("1" -> SensorMeasurements(Some(MeasurementState(0, 22, 61, 44.333332f)), 0, 6))
  )

  def singleFileSingleSensorOnlyNaNs = TestParameters(
    "singleFileSingleSensorOnlyNaNs",
    1,
    Map("3" -> SensorMeasurements(None, 2, 2))
  )

  def singleFileInvalidEntries = TestParameters(
    "singleFileInvalidEntries",
    1,
    Map(
      "3" -> SensorMeasurements(Some(MeasurementState(0, 10, 10, 10)), 0, 1),
      StringConstants.measureError -> SensorMeasurements(None, 2, 2)
    )
  )

  def multipleFilesSingleSensor = TestParameters(
    "multipleFilesSingleSensor",
    2,
    Map(
      "1" -> SensorMeasurements(Some(MeasurementState(0, 10, 80, 45)), 0, 8),
    )
  )

  def multipleFilesMultipleSensorsNaNs = TestParameters(
    "multipleFilesMultipleSensorsNaNs",
    3,
    Map(
      "1" -> SensorMeasurements(Some(MeasurementState(0, 10, 40, 21.25f)), 0, 4),
      "2" -> SensorMeasurements(Some(MeasurementState(0, 20, 50, 36.666668f)), 0, 3),
      "3" -> SensorMeasurements(Some(MeasurementState(0, 70, 70, 70)), 1, 2),
      "4" -> SensorMeasurements(None, 1, 1),
    )
  )

  def multipleFilesInvalidEntries = TestParameters(
    "multipleFilesInvalidEntries",
    2,
    Map(
      "1" -> SensorMeasurements(Some(MeasurementState(0, 50, 60, 55)), 0, 2),
      "2" -> SensorMeasurements(Some(MeasurementState(0, 50, 80, 66.666664f)), 0, 3),
      StringConstants.measureError -> SensorMeasurements(None, 4, 4),
    )
  )

  def nonExistingDirectory = TestParameters(
    "jfkdl;sa,nsdfndskla;",
    0,
    Map.empty
  )

  def emptyDirectory = TestParameters(
    "emptyDirectory",
    0,
    Map.empty
  )
}
