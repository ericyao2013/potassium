package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.PIDFConfig
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal, SignalLike}

import squants.motion.{AngularVelocity, DegreesPerSecond, MetersPerSecond, MetersPerSecondSquared}
import squants.space.{Degrees, Meters}
import squants.time.{Milliseconds, Seconds}
import squants.{Angle, Each, Length, Percent, Velocity}

import org.scalacheck.Prop.forAll
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers._

class UnicycleDriveControlTest extends FunSuite {
  implicit val hardware: UnicycleHardware = new UnicycleHardware {
    override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
    override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

    override def forwardPosition: Signal[Length] = ???
    override def turnPosition: Signal[Angle] = ???
  }

  private class TestDrivetrain extends UnicycleDrive {
    override type DriveSignal = UnicycleSignal
    override type DriveVelocity = UnicycleSignal

    override type DrivetrainHardware = UnicycleHardware
    override type DrivetrainProperties = UnicycleProperties

    override protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal = uni

    override protected val controlMode: UnicycleControlMode = NoOperation

    override protected def driveClosedLoop(signal: SignalLike[DriveSignal]): PeriodicSignal[DriveSignal] = signal.toPeriodic

    override type Drivetrain = Nothing
  }

  test("Open forward loop produces same forward speed as input and zero turn speed") {
    val drive = new TestDrivetrain

    check(forAll { x: Double =>
      val out = drive.UnicycleControllers.
        openForwardClosedDrive(Signal.constant(Each(x))).
        currentValue(Milliseconds(5))
      out.forward.toEach == x && out.turn.toEach == 0
    })
  }

  test("Open turn loop produces same turn speed as input and zero forward speed") {
    val drive = new TestDrivetrain

    check(forAll { x: Double =>
      val out = drive.UnicycleControllers.
        openTurnClosedDrive(Signal.constant(Each(x))).
        currentValue(Milliseconds(5))
      out.turn.toEach == x && out.forward.toEach == 0
    })
  }

  test("Closed loop with only feed-forward is essentially open loop") {
    implicit val props = new UnicycleProperties {
      override val maxForwardVelocity: Velocity = MetersPerSecond(10)
      override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)

      override val forwardControlGains = PIDFConfig(
        Percent(0) / MetersPerSecond(1),
        Percent(0) / MetersPerSecondSquared(1),
        Percent(0) / Meters(1),
        Percent(100) / maxForwardVelocity
      )

      override val turnControlGains = PIDFConfig(
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1)),
        Percent(0) / (DegreesPerSecond(1).toGeneric * Seconds(1)),
        Percent(100) / maxTurnVelocity
      )

      override def forwardPositionControlGains = ???
      override def turnPositionControlGains = ???
    }

    val drive = new TestDrivetrain

    check(forAll { (fwd: Double, turn: Double) =>
      val in = Signal.constant(UnicycleVelocity(MetersPerSecond(fwd), DegreesPerSecond(turn)))
      val out = drive.UnicycleControllers.
        velocityControl(in).
        currentValue(Milliseconds(5))

      (math.abs(out.forward.toEach - (fwd / 10)) / out.forward.toEach <= 0.0000001) &&
        (math.abs(out.turn.toEach - (turn / 10)) / out.turn.toEach <= 0.0000001)
    })
  }

  test("Forward position control when relative distance is zero returns zero speed") {
    val drive = new TestDrivetrain

    val props = new UnicycleProperties {
      override def maxForwardVelocity: Velocity = ???
      override def maxTurnVelocity: AngularVelocity = ???

      override def forwardControlGains = ???
      override def turnControlGains = ???

      override def forwardPositionControlGains = PIDFConfig(
        Percent(100) / Meters(1),
        Percent(0) / MetersPerSecond(1),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / Meters(1)
      )

      override def turnPositionControlGains = ???
    }

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override def forwardPosition: Signal[Length] = Signal(Meters(5))
      override def turnPosition: Signal[Angle] = ???
    }

    val out = drive.UnicycleControllers.
      forwardPositionControl(Meters(5))(hardware, props)._1.currentValue(Milliseconds(5))

    assert(out.forward.toPercent == 0)
  }

  test("Forward position control returns correct proportional control (forward)") {
    val drive = new TestDrivetrain

    val props = new UnicycleProperties {
      override def maxForwardVelocity: Velocity = ???
      override def maxTurnVelocity: AngularVelocity = ???

      override def forwardControlGains = ???
      override def turnControlGains = ???

      override def forwardPositionControlGains = PIDFConfig(
        Percent(100) / Meters(10),
        Percent(0) / MetersPerSecond(1),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / Meters(1)
      )

      override def turnPositionControlGains = ???
    }

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override def forwardPosition: Signal[Length] = Signal(Meters(0))
      override def turnPosition: Signal[Angle] = ???
    }

    val out = drive.UnicycleControllers.
      forwardPositionControl(Meters(5))(hardware, props)._1.currentValue(Milliseconds(5))

    assert(out.forward.toPercent == 50)
  }

  test("Forward position control returns correct proportional control (reverse)") {
    val drive = new TestDrivetrain

    val props = new UnicycleProperties {
      override def maxForwardVelocity: Velocity = ???
      override def maxTurnVelocity: AngularVelocity = ???

      override def forwardControlGains = ???
      override def turnControlGains = ???

      override def forwardPositionControlGains = PIDFConfig(
        Percent(100) / Meters(10),
        Percent(0) / MetersPerSecond(1),
        Percent(0) / (Meters(1).toGeneric * Seconds(1)),
        Percent(0) / Meters(1)
      )

      override def turnPositionControlGains = ???
    }

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override def forwardPosition: Signal[Length] = Signal(Meters(0))
      override def turnPosition: Signal[Angle] = ???
    }

    val out = drive.UnicycleControllers.
      forwardPositionControl(Meters(-5))(hardware, props)._1.currentValue(Milliseconds(5))

    assert(out.forward.toPercent == -50)
  }

  test("Turn position control when relative angle is zero returns zero speed") {
    val drive = new TestDrivetrain

    val props = new UnicycleProperties {
      override def maxForwardVelocity: Velocity = ???
      override def maxTurnVelocity: AngularVelocity = ???

      override def forwardControlGains = ???
      override def turnControlGains = ???

      override def forwardPositionControlGains = ???

      override def turnPositionControlGains = PIDFConfig(
        Percent(100) / Degrees(1),
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / Degrees(1)
      )
    }

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override def forwardPosition: Signal[Length] = ???
      override def turnPosition: Signal[Angle] = Signal(Degrees(5))
    }

    val out = drive.UnicycleControllers.
      turnPositionControl(Degrees(5))(hardware, props)._1.currentValue(Milliseconds(5))

    assert(out.turn.toPercent == 0)
  }

  test("Turn position control returns correct proportional control (clockwise)") {
    val drive = new TestDrivetrain

    val props = new UnicycleProperties {
      override def maxForwardVelocity: Velocity = ???
      override def maxTurnVelocity: AngularVelocity = ???

      override def forwardControlGains = ???
      override def turnControlGains = ???

      override def forwardPositionControlGains = ???

      override def turnPositionControlGains = PIDFConfig(
        Percent(100) / Degrees(10),
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / Degrees(1)
      )
    }

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override def forwardPosition: Signal[Length] = ???
      override def turnPosition: Signal[Angle] = Signal(Degrees(0))
    }

    val out = drive.UnicycleControllers.
      turnPositionControl(Degrees(5))(hardware, props)._1.currentValue(Milliseconds(5))

    assert(out.turn.toPercent == 50)
  }

  test("Turn position control returns correct proportional control (counterclockwise)") {
    val drive = new TestDrivetrain

    val props = new UnicycleProperties {
      override def maxForwardVelocity: Velocity = ???
      override def maxTurnVelocity: AngularVelocity = ???

      override def forwardControlGains = ???
      override def turnControlGains = ???

      override def forwardPositionControlGains = ???

      override def turnPositionControlGains = PIDFConfig(
        Percent(100) / Degrees(10),
        Percent(0) / DegreesPerSecond(1),
        Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
        Percent(0) / Degrees(1)
      )
    }

    val hardware: UnicycleHardware = new UnicycleHardware {
      override val forwardVelocity: Signal[Velocity] = Signal(MetersPerSecond(0))
      override val turnVelocity: Signal[AngularVelocity] = Signal(DegreesPerSecond(0))

      override def forwardPosition: Signal[Length] = ???
      override def turnPosition: Signal[Angle] = Signal(Degrees(0))
    }

    val out = drive.UnicycleControllers.
      turnPositionControl(Degrees(-5))(hardware, props)._1.currentValue(Milliseconds(5))

    assert(out.turn.toPercent == -50)
  }
}