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
  private var lastSnareTime: Double = 0
  private var lastHihatTime: Double = 0
  private var lastFillTime: Double = 0
  private var arpIndex: Int = 0
  private var chordIndex: Int = 0
  private var measureCount: Int = 0
  private var beatInMeasure: Int = 0

  // Variation state
  private var arpPattern: Int = 0
  private var drumPattern: Int = 0
  private var intensityLevel: Float = 0.5f

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
    Array(3, 7, 10),      // III
    Array(0, 3, 7, 10),   // i7
    Array(-2, 2, 5)       // VII
  )

  // Arpeggio patterns (indices into chord)
  private val arpPatterns = Array(
    Array(0, 1, 2, 1),           // up-down
    Array(0, 2, 1, 2),           // skip
    Array(0, 1, 2, 0, 1, 2),     // straight up x2
    Array(2, 1, 0, 1),           // down-up
    Array(0, 0, 1, 2),           // stutter
    Array(0, 2, 0, 1, 0, 2)      // rhythmic
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
      lastSnareTime = time
      lastHihatTime = time
      lastFillTime = time
      beatInMeasure = 0
      measureCount = 0

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
      // Occasionally change arp pattern
      if random.nextFloat() < 0.3f then
        arpPattern = random.nextInt(arpPatterns.length)

    // Slow arpeggio with variation
    if time - lastArpTime >= beatDuration * (0.6 + random.nextFloat() * 0.3) then
      lastArpTime = time
      val chord = menuChords(chordIndex % menuChords.length)
      val pattern = arpPatterns(arpPattern % arpPatterns.length)
      val noteOffset = chord(pattern(arpIndex % pattern.length) % chord.length)
      val octaveShift = if random.nextFloat() < 0.3f then 12 else 0
      val volVariation = 0.12f + random.nextFloat() * 0.08f
      playArpNote(menuScale(0) + noteOffset + octaveShift, volVariation)
      arpIndex = (arpIndex + 1) % pattern.length

    // Occasional bass with slight timing variation
    if time - lastBassTime >= beatDuration * (7 + random.nextFloat() * 2) then
      lastBassTime = time
      val root = menuScale(0) - 12
      playBass(root, 0.2f + random.nextFloat() * 0.1f)

    // Rare soft kick for pulse
    if time - lastKickTime >= beatDuration * 16 then
      lastKickTime = time
      if random.nextFloat() < 0.4f then
        playKick(0.1f)

  private def updateGameMusic(): Unit =
    val bpm = 128.0
    val beatDuration = 60.0 / bpm

    // Update intensity over time
    intensityLevel = 0.5f + 0.3f * Math.sin(time * 0.02).toFloat

    // Kick drum pattern
    if time - lastKickTime >= beatDuration then
      lastKickTime = time
      beatInMeasure = (beatInMeasure + 1) % 4

      val kickVol = 0.3f + random.nextFloat() * 0.1f
      // Different kick patterns based on drumPattern
      drumPattern % 4 match
        case 0 => playKick(kickVol) // four on floor
        case 1 => if beatInMeasure != 2 then playKick(kickVol) // skip beat 3
        case 2 => if beatInMeasure == 0 || beatInMeasure == 2 then playKick(kickVol) // half time
        case 3 => playKick(kickVol); if beatInMeasure == 1 then playKick(kickVol * 0.6f) // double hit

      if beatInMeasure == 0 then
        measureCount += 1
        if measureCount % 4 == 0 then
          chordIndex = (chordIndex + 1) % gameChords.length
        if measureCount % 8 == 0 then
          drumPattern = random.nextInt(4)
          arpPattern = random.nextInt(arpPatterns.length)

    // Snare on beats 2 and 4
    if time - lastSnareTime >= beatDuration then
      lastSnareTime = time
      if beatInMeasure == 1 || beatInMeasure == 3 then
        val snareVol = 0.2f + random.nextFloat() * 0.08f
        playSnare(snareVol)
        // Occasional ghost notes
        if random.nextFloat() < 0.2f then
          playSnare(snareVol * 0.3f)

    // Hihat patterns
    if time - lastHihatTime >= beatDuration * 0.25 then
      lastHihatTime = time
      val hihatStep = ((time / (beatDuration * 0.25)).toInt % 4)
      val playHH = hihatStep match
        case 0 => true
        case 1 => random.nextFloat() < 0.7f
        case 2 => true
        case 3 => random.nextFloat() < 0.5f
      if playHH then
        val hhVol = if hihatStep == 0 || hihatStep == 2 then 0.12f else 0.07f
        val isOpen = hihatStep == 2 && random.nextFloat() < 0.3f
        playHihat(hhVol + random.nextFloat() * 0.03f, isOpen)

    // Occasional drum fill
    if time - lastFillTime >= beatDuration * 16 then
      if beatInMeasure == 3 && random.nextFloat() < 0.5f then
        lastFillTime = time
        playDrumFill()

    // Bass line with variation
    if time - lastBassTime >= beatDuration * (1.5 + random.nextFloat() * 1.0) then
      lastBassTime = time
      val chord = gameChords(chordIndex % gameChords.length)
      val bassNote = if random.nextFloat() < 0.7f then chord(0) else chord(random.nextInt(chord.length))
      val root = gameScale(0) + bassNote - 24
      val bassVol = 0.35f + random.nextFloat() * 0.1f
      playBass(root, bassVol)
      // Occasional octave jump
      if random.nextFloat() < 0.2f then
        playBass(root + 12, bassVol * 0.5f)

    // Arpeggio with more variation
    val arpSpeed = beatDuration * (0.2 + intensityLevel * 0.15)
    if time - lastArpTime >= arpSpeed then
      lastArpTime = time
      val chord = gameChords(chordIndex % gameChords.length)
      val pattern = arpPatterns(arpPattern % arpPatterns.length)
      val noteIdx = pattern(arpIndex % pattern.length) % chord.length
      val octave = (arpIndex / pattern.length) % 3
      val note = gameScale(0) + chord(noteIdx) + (octave * 12) - 12

      // Random waveform selection
      val waveChoice = random.nextInt(10)
      val arpVol = 0.14f + random.nextFloat() * 0.08f + intensityLevel * 0.05f
      playArpNote(note, arpVol, waveChoice)
      arpIndex = (arpIndex + 1) % (pattern.length * 3)

    // Pad changes
    if time - lastPadTime >= beatDuration * 8 then
      lastPadTime = time
      val padVol = 0.15f + intensityLevel * 0.1f
      playPadChord(gameChords(chordIndex % gameChords.length), gameScale(0), padVol)

  private def playPadChord(chord: Array[Int], root: Int, vol: Float = 0.12f): Unit =
    synchronized {
      for offset <- chord do
        val freq = Oscillator.noteToFreq(root + offset)
        val detuneAmt = (random.nextFloat() - 0.5f) * 0.006f
        voices += new Voice(WaveForm.Sine, Envelope.Pad, freq, time, vol, detuneAmt, 5.0)
        voices += new Voice(WaveForm.Triangle, Envelope.Pad, freq, time, vol * 0.4f, -detuneAmt, 5.0)
        // Add subtle saw for shimmer
        if random.nextFloat() < 0.3f then
          voices += new Voice(WaveForm.Sawtooth, Envelope.Pad, freq * 2, time, vol * 0.1f, detuneAmt * 2, 5.0)
    }

  private def playArpNote(note: Int, vol: Float, waveChoice: Int = 0): Unit =
    synchronized {
      val freq = Oscillator.noteToFreq(note)
      val detune = (random.nextFloat() - 0.5f) * 0.008f

      // Vary the waveform
      val mainWave = waveChoice % 3 match
        case 0 => WaveForm.Sawtooth
        case 1 => WaveForm.Square
        case 2 => WaveForm.Triangle

      voices += new Voice(mainWave, Envelope.Arp, freq, time, vol, detune, 0.6)

      // Harmonic layer
      if waveChoice < 5 then
        voices += new Voice(WaveForm.Sine, Envelope.Arp, freq * 2, time, vol * 0.25f, -detune, 0.5)
      if waveChoice < 3 then
        voices += new Voice(WaveForm.Square, Envelope.Arp, freq * 0.5, time, vol * 0.15f, 0.0, 0.7)
    }

  private def playBass(note: Int, vol: Float): Unit =
    synchronized {
      val freq = Oscillator.noteToFreq(note)
      val detune = random.nextFloat() * 0.003f
      voices += new Voice(WaveForm.Sine, Envelope.Bass, freq, time, vol, 0.0, 1.5)
      voices += new Voice(WaveForm.Square, Envelope.Bass, freq, time, vol * 0.25f, detune, 1.5)
      voices += new Voice(WaveForm.Sawtooth, Envelope.Bass, freq, time, vol * 0.15f, -detune, 1.2)
    }

  private def playKick(vol: Float): Unit =
    synchronized {
      val pitchVar = random.nextFloat() * 5
      voices += new Voice(WaveForm.Sine, Envelope.Kick, 55 + pitchVar, time, vol, 0.0, 0.35)
      voices += new Voice(WaveForm.Sine, Envelope.Kick, 45 + pitchVar, time, vol * 0.7f, 0.0, 0.3)
      // Add click
      voices += new Voice(WaveForm.Noise, Envelope.Kick, 100, time, vol * 0.15f, 0.0, 0.02)
    }

  private def playSnare(vol: Float): Unit =
    synchronized {
      voices += new Voice(WaveForm.Noise, Envelope.Snare, 200, time, vol, 0.0, 0.2)
      voices += new Voice(WaveForm.Sine, Envelope.Snare, 180, time, vol * 0.6f, 0.0, 0.15)
      voices += new Voice(WaveForm.Triangle, Envelope.Snare, 120, time, vol * 0.3f, 0.0, 0.1)
    }

  private def playHihat(vol: Float, open: Boolean = false): Unit =
    synchronized {
      val duration = if open then 0.25 else 0.08
      voices += new Voice(WaveForm.Noise, Envelope.Hihat, 8000 + random.nextFloat() * 2000, time, vol, 0.0, duration)
      if open then
        voices += new Voice(WaveForm.Noise, Envelope.Hihat, 6000, time, vol * 0.5f, 0.0, 0.3)
    }

  private def playDrumFill(): Unit =
    synchronized {
      // Quick tom pattern
      val fillType = random.nextInt(3)
      fillType match
        case 0 => // descending toms
          voices += new Voice(WaveForm.Sine, Envelope.Tom, 200, time, 0.25f, 0.0, 0.2)
          voices += new Voice(WaveForm.Sine, Envelope.Tom, 150, time + 0.1, 0.25f, 0.0, 0.2)
          voices += new Voice(WaveForm.Sine, Envelope.Tom, 100, time + 0.2, 0.3f, 0.0, 0.25)
        case 1 => // snare roll
          for i <- 0 until 4 do
            voices += new Voice(WaveForm.Noise, Envelope.Snare, 200, time + i * 0.06, 0.15f + i * 0.03f, 0.0, 0.1)
        case 2 => // kick snare combo
          voices += new Voice(WaveForm.Sine, Envelope.Kick, 55, time, 0.3f, 0.0, 0.2)
          voices += new Voice(WaveForm.Noise, Envelope.Snare, 200, time + 0.12, 0.25f, 0.0, 0.15)
          voices += new Voice(WaveForm.Sine, Envelope.Kick, 55, time + 0.25, 0.35f, 0.0, 0.2)
    }

  def dispose(): Unit =
    stop()
