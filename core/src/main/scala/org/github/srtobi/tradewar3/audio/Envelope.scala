package org.github.srtobi.tradewar3.audio

class Envelope(
  val attack: Double,
  val decay: Double,
  val sustain: Double,
  val release: Double
):
  def amplitude(time: Double, noteOnTime: Double, noteOffTime: Option[Double]): Double =
    val timeSinceOn = time - noteOnTime

    noteOffTime match
      case Some(offTime) =>
        val timeSinceOff = time - offTime
        val ampAtOff = amplitudeAtTime(offTime - noteOnTime)
        val releaseProgress = timeSinceOff / release
        if releaseProgress >= 1.0 then 0.0
        else ampAtOff * (1.0 - releaseProgress)

      case None =>
        amplitudeAtTime(timeSinceOn)

  private def amplitudeAtTime(t: Double): Double =
    if t < attack then
      t / attack
    else if t < attack + decay then
      val decayProgress = (t - attack) / decay
      1.0 - (1.0 - sustain) * decayProgress
    else
      sustain

  def isFinished(time: Double, noteOffTime: Option[Double]): Boolean =
    noteOffTime match
      case Some(offTime) => (time - offTime) >= release
      case None => false

object Envelope:
  val Pad = Envelope(1.2, 0.8, 0.7, 2.5)
  val Pluck = Envelope(0.01, 0.4, 0.3, 0.8)
  val Bass = Envelope(0.05, 0.3, 0.6, 0.6)
  val Arp = Envelope(0.02, 0.2, 0.4, 0.4)
  val Kick = Envelope(0.005, 0.2, 0.0, 0.2)
  val Hihat = Envelope(0.002, 0.08, 0.0, 0.1)
  val Snare = Envelope(0.005, 0.1, 0.1, 0.15)
  val Clap = Envelope(0.01, 0.08, 0.05, 0.2)
  val Tom = Envelope(0.005, 0.15, 0.0, 0.25)
