package org.github.srtobi.tradewar3.audio

enum WaveForm:
  case Sine, Square, Triangle, Sawtooth, Noise

object Oscillator:
  private val random = new scala.util.Random()

  def sample(waveForm: WaveForm, phase: Double): Float =
    val value = waveForm match
      case WaveForm.Sine =>
        Math.sin(phase * 2 * Math.PI)
      case WaveForm.Square =>
        if (phase % 1.0) < 0.5 then 1.0 else -1.0
      case WaveForm.Triangle =>
        val t = phase % 1.0
        if t < 0.5 then 4.0 * t - 1.0 else 3.0 - 4.0 * t
      case WaveForm.Sawtooth =>
        2.0 * (phase % 1.0) - 1.0
      case WaveForm.Noise =>
        random.nextDouble() * 2.0 - 1.0
    value.toFloat

  def noteToFreq(note: Int): Double =
    440.0 * Math.pow(2.0, (note - 69) / 12.0)
