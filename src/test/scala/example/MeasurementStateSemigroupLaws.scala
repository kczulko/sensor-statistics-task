package example

import cats.tests.CatsSuite
import cats.kernel.Eq
import cats.kernel.laws.discipline.SemigroupTests

import org.scalacheck.ScalacheckShapeless._

class MeasurementStateSemigroupLaws extends CatsSuite {
  implicit val eq: Eq[MeasurementState] = Eq.allEqual

  checkAll("Semigroup[MeasurementState]", SemigroupTests[MeasurementState].semigroup)
}
