package com.github.kczulko.sst

import cats.tests.CatsSuite
import cats.kernel.Eq
import cats.kernel.laws.discipline.SemigroupTests

import org.scalacheck.ScalacheckShapeless._

class SensorMeasurementsSemigroupLaws extends CatsSuite {

  implicit val eq: Eq[SensorMeasurements] = Eq.allEqual

  checkAll("Semigroup[SensorMeasurements]", SemigroupTests[SensorMeasurements].semigroup)
}
