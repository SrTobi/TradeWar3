package org.github.srtobi.tradewar3

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import scala.compiletime.uninitialized

enum MusicTrack:
  case Menu, Game, Battle, Victory, Danger

object MusicManager:
  private var tracks: Map[MusicTrack, Music] = Map.empty
  private var currentTrack: MusicTrack = uninitialized
  private var currentMusic: Music = uninitialized
  private var musicVolume: Float = GameConfig.MUSIC_DEFAULT_VOLUME
  private var targetVolume: Float = GameConfig.MUSIC_DEFAULT_VOLUME
  private var fadeSpeed: Float = 0.5f

  private val trackFiles = Map(
    MusicTrack.Menu -> "assets/music/menu.mp3",
    MusicTrack.Game -> "assets/music/game.mp3",
    MusicTrack.Battle -> "assets/music/battle.mp3",
    MusicTrack.Victory -> "assets/music/victory.mp3",
    MusicTrack.Danger -> "assets/music/danger.mp3"
  )

  private def loadTrack(track: MusicTrack): Option[Music] =
    if tracks.contains(track) then return tracks.get(track)

    trackFiles.get(track).flatMap { path =>
      try
        val music = Gdx.audio.newMusic(Gdx.files.internal(path))
        music.setLooping(true)
        music.setVolume(musicVolume)
        tracks = tracks + (track -> music)
        Some(music)
      catch
        case e: Exception =>
          Gdx.app.log("MusicManager", s"Could not load $path: ${e.getMessage}")
          None
    }

  private def playTrack(track: MusicTrack): Unit =
    if currentTrack == track && currentMusic != null && currentMusic.isPlaying then return

    loadTrack(track).foreach { music =>
      if currentMusic != null && currentMusic.isPlaying then
        currentMusic.stop()
      music.setVolume(musicVolume)
      music.play()
      currentMusic = music
      currentTrack = track
    }

  def playMenuMusic(): Unit = playTrack(MusicTrack.Menu)
  def playGameMusic(): Unit = playTrack(MusicTrack.Game)
  def playBattleMusic(): Unit = playTrack(MusicTrack.Battle)
  def playVictoryMusic(): Unit = playTrack(MusicTrack.Victory)
  def playDangerMusic(): Unit = playTrack(MusicTrack.Danger)

  def getCurrentTrack: MusicTrack = currentTrack

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
    targetVolume = musicVolume
    tracks.values.foreach(_.setVolume(musicVolume))

  def getVolume: Float = musicVolume

  def isPlaying: Boolean =
    currentMusic != null && currentMusic.isPlaying

  def dispose(): Unit =
    tracks.values.foreach(_.dispose())
    tracks = Map.empty
    currentMusic = null
