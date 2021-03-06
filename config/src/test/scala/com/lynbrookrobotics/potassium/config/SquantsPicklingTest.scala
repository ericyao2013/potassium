package com.lynbrookrobotics.potassium.config

import org.scalatest.FunSuite
import argonaut.Argonaut._
import argonaut._
import ArgonautShapeless._
import SquantsPickling._
import com.lynbrookrobotics.potassium.units.GenericIntegral
import com.lynbrookrobotics.potassium.units.GenericDerivative
import squants.motion._
import com.lynbrookrobotics.potassium.units.GenericValue._

import scala.language.experimental.macros
import squants.space._
import squants.time.{Milliseconds, Minutes}

class SquantsPicklingTest extends FunSuite {
  test("Basic squants values can be encoded and decoded") {
    val a = FeetPerSecond(3.14)
    assert(a.jencode.toString() == s"""[${3.14},"FeetPerSecond"]""")
    assert(s"""[3.14,"FeetPerSecond"]""".decodeOption[Velocity].get == a)

    val b = DegreesPerSecondSquared(123.4)
    assert(b.jencode.toString() == s"""[${123.4},"DegreesPerSecondSquared"]""")
    assert(s"""[123.4,"DegreesPerSecondSquared"]""".decodeOption[AngularAcceleration].get == b)
  }

  test("Generic integrals can be encoded and decoded") {
    val a = Feet(3.14) * Minutes(2)
    assert(a.jencode.toString() == s"""[${376.8},"Feet * s"]""")
    assert(s"""[376.8,"Feet * s"]""".decodeOption[GenericIntegral[Length]].get == a)

    val b = Degrees(31.4) * Milliseconds(2)
    assert(b.jencode.toString() == s"""[${0.0628},"Degrees * s"]""")
    assert(s"""[0.0628,"Degrees * s"]""".decodeOption[GenericIntegral[Angle]].get == b)
  }

  test("Generic derivatives can be encoded and decoded") {
    val a = FeetPerSecondCubed(12.3) / Minutes(5)
    assert(a.jencode.toString() == s"""[${0.041},"FeetPerSecondCubed / s"]""")
    assert(s"""[0.041,"FeetPerSecondCubed / s"]""".decodeOption[GenericDerivative[Jerk]].get == a)

    val b = DegreesPerSecondSquared(31.141) / Milliseconds(2)
    assert(b.jencode.toString() == s"""[${15570.5},"DegreesPerSecondSquared / s"]""")
    assert(s"""[15570.5,"DegreesPerSecondSquared / s"]""".decodeOption[GenericDerivative[AngularAcceleration]].get == b)
  }
}
