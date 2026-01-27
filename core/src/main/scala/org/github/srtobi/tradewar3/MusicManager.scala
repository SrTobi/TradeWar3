package org.github.srtobi.tradewar3

import org.github.srtobi.tradewar3.audio.{MusicMode, ProceduralMusic}
import scala.compiletime.uninitialized

object MusicManager:
  private var music: ProceduralMusic = uninitialized
  private var currentMode: MusicMode = uninitialized

  private def ensureInitialized(): Unit =
    if music == null then
      music = new ProceduralMusic()
      music.setVolume(GameConfig.MUSIC_DEFAULT_VOLUME)

  def playMenuMusic(): Unit =
    ensureInitialized()
    if currentMode != MusicMode.Menu then
      currentMode = MusicMode.Menu
      music.setMode(MusicMode.Menu)
      if !isPlaying then music.start()

  def playGameMusic(): Unit =
    ensureInitialized()
    if currentMode != MusicMode.Game then
      currentMode = MusicMode.Game
      music.setMode(MusicMode.Game)
      if !isPlaying then music.start()

  def stop(): Unit =
    if music != null then
      music.stop()
      currentMode = null

  def pause(): Unit =
    if music != null then
      music.stop()

  def resume(): Unit =
    if music != null && currentMode != null then
      music.start()
      music.setMode(currentMode)

  def setVolume(volume: Float): Unit =
    ensureInitialized()
    music.setVolume(volume)

  def getVolume: Float =
    if music != null then music.getVolume else GameConfig.MUSIC_DEFAULT_VOLUME

  def isPlaying: Boolean =
    music != null && music.isRunning

  def dispose(): Unit =
    if music != null then
      music.dispose()
      music = null
      currentMode = null
