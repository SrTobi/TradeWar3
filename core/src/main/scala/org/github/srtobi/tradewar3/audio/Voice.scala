package org.github.srtobi.tradewar3.audio

class Voice(
  val waveForm: WaveForm,
  val envelope: Envelope,
  val frequency: Double,
  val noteOnTime: Double,
  val volume: Float = 1.0f,
  val detune: Double = 0.0,
  val duration: Double = 0.5
):
  private var phase: Double = 0.0
  private val noteOffTime: Double = noteOnTime + duration

  def isFinished(time: Double): Boolean =
    time >= noteOffTime + envelope.release

  def sample(time: Double, sampleRate: Double): Float =
    val amp = envelope.amplitude(time, noteOnTime, Some(noteOffTime))
    val freq = frequency * (1.0 + detune)
    phase += freq / sampleRate
    (Oscillator.sample(waveForm, phase) * amp * volume).toFloat
