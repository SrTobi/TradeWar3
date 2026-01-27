package org.github.srtobi.tradewar3

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import scala.compiletime.uninitialized

object MusicManager:
  private var menuMusic: Music = uninitialized
  private var gameMusic: Music = uninitialized
  private var currentMusic: Music = uninitialized
  private var musicVolume: Float = GameConfig.MUSIC_DEFAULT_VOLUME

  private def ensureLoaded(): Unit =
    if menuMusic == null then
      try
        menuMusic = Gdx.audio.newMusic(Gdx.files.internal("assets/music/menu.mp3"))
        menuMusic.setLooping(true)
        menuMusic.setVolume(musicVolume)
      catch
        case e: Exception =>
          Gdx.app.log("MusicManager", s"Could not load menu music: ${e.getMessage}")

    if gameMusic == null then
      try
        gameMusic = Gdx.audio.newMusic(Gdx.files.internal("assets/music/game.mp3"))
        gameMusic.setLooping(true)
        gameMusic.setVolume(musicVolume)
      catch
        case e: Exception =>
          Gdx.app.log("MusicManager", s"Could not load game music: ${e.getMessage}")

  def playMenuMusic(): Unit =
    ensureLoaded()
    if currentMusic == menuMusic && menuMusic != null && menuMusic.isPlaying then return
    if currentMusic != null && currentMusic.isPlaying then currentMusic.stop()
    if menuMusic != null then
      menuMusic.play()
      currentMusic = menuMusic

  def playGameMusic(): Unit =
    ensureLoaded()
    if currentMusic == gameMusic && gameMusic != null && gameMusic.isPlaying then return
    if currentMusic != null && currentMusic.isPlaying then currentMusic.stop()
    if gameMusic != null then
      gameMusic.play()
      currentMusic = gameMusic

  def stop(): Unit =
    if currentMusic != null then
      currentMusic.stop()

  def pause(): Unit =
    if currentMusic != null then
      currentMusic.pause()

  def resume(): Unit =
    if currentMusic != null then
      currentMusic.play()

  def setVolume(volume: Float): Unit =
    musicVolume = Math.max(0f, Math.min(1f, volume))
    if menuMusic != null then menuMusic.setVolume(musicVolume)
    if gameMusic != null then gameMusic.setVolume(musicVolume)

  def getVolume: Float = musicVolume

  def isPlaying: Boolean =
    currentMusic != null && currentMusic.isPlaying

  def dispose(): Unit =
    if menuMusic != null then
      menuMusic.dispose()
      menuMusic = null
    if gameMusic != null then
      gameMusic.dispose()
      gameMusic = null
    currentMusic = null
