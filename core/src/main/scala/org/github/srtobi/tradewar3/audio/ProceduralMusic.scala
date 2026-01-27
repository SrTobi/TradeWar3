package org.github.srtobi.tradewar3.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.AudioDevice
import scala.collection.mutable.ArrayBuffer
import scala.compiletime.uninitialized
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

enum MusicMode:
  case Menu, Game

class ProceduralMusic:
  private val sampleRate = 44100
  private val bufferSize = 2048
  private val running = new AtomicBoolean(false)
  private val currentMode = new AtomicReference[MusicMode](MusicMode.Menu)
  private var audioThread: Thread = uninitialized
  private var volume: Float = 0.7f

  private val voices = ArrayBuffer[Voice]()
  private var time: Double = 0.0
  private val random = new scala.util.Random()

  // Musical scales (MIDI note numbers)
  private val menuScale = Array(48, 50, 52, 55, 57, 60, 62, 64, 67, 69, 72, 74, 76, 79, 81)
  private val gameScale = Array(45, 48, 50, 52, 55, 57, 60, 62, 64, 67, 69, 72)

  // Timing
  private var lastBassTime: Double = 0
  private var lastArpTime: Double = 0
  private var lastPadTime: Double = 0
  private var lastKickTime: Double = 0
  private var lastHihatTime: Double = 0
  private var arpIndex: Int = 0
  private var chordIndex: Int = 0
  private var measureCount: Int = 0

  // Chord progressions (relative to root)
  private val menuChords = Array(
    Array(0, 4, 7, 11),   // maj7
    Array(5, 9, 12, 16),  // IVmaj7
    Array(7, 11, 14, 17), // Vmaj7
    Array(2, 5, 9, 12)    // ii7
  )

  private val gameChords = Array(
    Array(0, 3, 7),       // i minor
    Array(5, 8, 12),      // iv
    Array(7, 10, 14),     // v
    Array(3, 7, 10)       // III
  )

  def start(): Unit =
    if running.get() then return

    running.set(true)
    audioThread = new Thread(() => audioLoop(), "ProceduralMusic")
    audioThread.setDaemon(true)
    audioThread.start()

  def stop(): Unit =
    running.set(false)
    if audioThread != null then
      audioThread.interrupt()
      audioThread = null

  def setMode(mode: MusicMode): Unit =
    if currentMode.get() != mode then
      currentMode.set(mode)
      synchronized {
        voices.clear()
      }
      // Reset timing for new mode
      lastBassTime = time
      lastArpTime = time
      lastPadTime = time
      lastKickTime = time
      lastHihatTime = time

  def setVolume(vol: Float): Unit =
    volume = Math.max(0f, Math.min(1f, vol))

  def getVolume: Float = volume

  def isRunning: Boolean = running.get()

  private def audioLoop(): Unit =
    var device: AudioDevice = null
    try
      device = Gdx.audio.newAudioDevice(sampleRate, true)
      val buffer = new Array[Float](bufferSize)

      while running.get() do
        generateSamples(buffer)
        device.writeSamples(buffer, 0, buffer.length)
    catch
      case _: InterruptedException => ()
      case e: Exception =>
        Gdx.app.log("ProceduralMusic", s"Audio error: ${e.getMessage}")
    finally
      if device != null then device.dispose()

  private def generateSamples(buffer: Array[Float]): Unit =
    val mode = currentMode.get()
    val dt = 1.0 / sampleRate

    for i <- buffer.indices do
      updateMusic(mode)

      var sample = 0f
      synchronized {
        val activeVoices = voices.filterNot(_.isFinished(time))
        voices.clear()
        voices ++= activeVoices

        for voice <- voices do
          sample += voice.sample(time, sampleRate)
      }

      // Soft clip and apply volume
      sample = Math.tanh(sample * 0.5).toFloat * volume
      buffer(i) = sample
      time += dt

  private def updateMusic(mode: MusicMode): Unit =
    mode match
      case MusicMode.Menu => updateMenuMusic()
      case MusicMode.Game => updateGameMusic()

  private def updateMenuMusic(): Unit =
    val bpm = 72.0
    val beatDuration = 60.0 / bpm

    // Ambient pad chords every 4 beats
    if time - lastPadTime >= beatDuration * 4 then
      lastPadTime = time
      playPadChord(menuChords(chordIndex % menuChords.length), menuScale(0))
      chordIndex += 1

    // Slow arpeggio
    if time - lastArpTime >= beatDuration * 0.75 then
      lastArpTime = time
      val chord = menuChords(chordIndex % menuChords.length)
      val noteOffset = chord(arpIndex % chord.length)
      val octaveShift = if arpIndex >= chord.length then 12 else 0
      playArpNote(menuScale(0) + noteOffset + octaveShift, 0.15f)
      arpIndex = (arpIndex + 1) % (chord.length * 2)

    // Occasional bass
    if time - lastBassTime >= beatDuration * 8 then
      lastBassTime = time
      val root = menuScale(0) - 12
      playBass(root, 0.25f)

  private def updateGameMusic(): Unit =
    val bpm = 128.0
    val beatDuration = 60.0 / bpm

    // Kick drum on beats
    if time - lastKickTime >= beatDuration then
      lastKickTime = time
      playKick(0.35f)
      measureCount += 1

      if measureCount % 4 == 0 then
        chordIndex = (chordIndex + 1) % gameChords.length

    // Hihat off-beats
    if time - lastHihatTime >= beatDuration * 0.5 then
      lastHihatTime = time
      if ((time / beatDuration).toInt % 2) == 1 then
        playHihat(0.12f)

    // Bass line
    if time - lastBassTime >= beatDuration * 2 then
      lastBassTime = time
      val chord = gameChords(chordIndex % gameChords.length)
      val root = gameScale(0) + chord(0) - 24
      playBass(root, 0.4f)

    // Fast arpeggio
    if time - lastArpTime >= beatDuration * 0.25 then
      lastArpTime = time
      val chord = gameChords(chordIndex % gameChords.length)
      val noteIdx = arpIndex % chord.length
      val octave = (arpIndex / chord.length) % 2
      val note = gameScale(0) + chord(noteIdx) + (octave * 12)
      playArpNote(note, 0.18f)
      arpIndex = (arpIndex + 1) % (chord.length * 2)

    // Pad changes
    if time - lastPadTime >= beatDuration * 8 then
      lastPadTime = time
      playPadChord(gameChords(chordIndex % gameChords.length), gameScale(0), 0.2f)

  private def playPadChord(chord: Array[Int], root: Int, vol: Float = 0.12f): Unit =
    synchronized {
      for offset <- chord do
        val freq = Oscillator.noteToFreq(root + offset)
        voices += new Voice(WaveForm.Sine, Envelope.Pad, freq, time, vol, 0.001, 5.0)
        voices += new Voice(WaveForm.Triangle, Envelope.Pad, freq, time, vol * 0.5f, -0.002, 5.0)
    }

  private def playArpNote(note: Int, vol: Float): Unit =
    synchronized {
      val freq = Oscillator.noteToFreq(note)
      voices += new Voice(WaveForm.Sawtooth, Envelope.Arp, freq, time, vol, 0.003, 0.6)
      voices += new Voice(WaveForm.Square, Envelope.Arp, freq * 2, time, vol * 0.3f, 0.0, 0.6)
    }

  private def playBass(note: Int, vol: Float): Unit =
    synchronized {
      val freq = Oscillator.noteToFreq(note)
      voices += new Voice(WaveForm.Sine, Envelope.Bass, freq, time, vol, 0.0, 1.5)
      voices += new Voice(WaveForm.Square, Envelope.Bass, freq, time, vol * 0.3f, 0.0, 1.5)
    }

  private def playKick(vol: Float): Unit =
    synchronized {
      voices += new Voice(WaveForm.Sine, Envelope.Kick, 60, time, vol, 0.0, 0.3)
      voices += new Voice(WaveForm.Sine, Envelope.Kick, 50, time, vol * 0.8f, 0.0, 0.3)
    }

  private def playHihat(vol: Float): Unit =
    synchronized {
      voices += new Voice(WaveForm.Noise, Envelope.Hihat, 8000, time, vol, 0.0, 0.1)
    }

  def dispose(): Unit =
    stop()
