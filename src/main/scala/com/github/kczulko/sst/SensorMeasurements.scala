package com.github.kczulko.sst

import cats.kernel.Semigroup
import cats.derived.auto.semigroup._

final case class MeasurementEntry(sensorId: String, humidity: Option[Int])

final case class MeasurementState(
  currentMeasure: Int,
  min: Int,
  max: Int,
  avg: Float
)

final case class SensorMeasurements(
  data: Option[MeasurementState],
  failedCount: Long,
  totalMeasurements: Long
)

object SensorMeasurements {

  implicit val semigroup: Semigroup[SensorMeasurements] = {
    case (
      SensorMeasurements(Some(dataLeft), failedLeft, totalLeft),
      SensorMeasurements(Some(dataRight), failedRight, totalRight)
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
      SensorMeasurements(
        Some(MeasurementState(currentMeasure, min, max, avg)),
        failedLeft + failedRight,
        totalLeft + totalRight
      )
    case (left, right) => {
      import cats.implicits._
      SensorMeasurements(
        left.data |+| right.data,
        left.failedCount + right.failedCount,
        left.totalMeasurements + right.totalMeasurements
      )
    }
  }

}
