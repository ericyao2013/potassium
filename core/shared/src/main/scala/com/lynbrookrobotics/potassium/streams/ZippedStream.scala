package com.lynbrookrobotics.potassium.streams

class ZippedStream[A, B](aPeriodicity: ExpectedPeriodicity,
                         bPeriodicity: ExpectedPeriodicity,
                         parentA: Stream[A], parentB: Stream[B]) extends Stream[(A, B)] {
  override val expectedPeriodicity: ExpectedPeriodicity = (aPeriodicity, bPeriodicity) match {
    case (Periodic(a), Periodic(b)) =>
      if (a eq b) {
        Periodic(a)
      } else {
        NonPeriodic
      }

    case _ => NonPeriodic
  }

  private[this] var aSlot: Option[A] = None
  private[this] var bSlot: Option[B] = None

  def attemptPublish(): Unit = {
    if (aSlot.isDefined && bSlot.isDefined) {
      publishValue((aSlot.get, bSlot.get))
      aSlot = None
      bSlot = None
    }
  }

  def receiveA(aValue: A): Unit = {
    aSlot = Some(aValue)
    attemptPublish()
  }

  def receiveB(bValue: B): Unit = {
    bSlot = Some(bValue)
    attemptPublish()
  }
}
