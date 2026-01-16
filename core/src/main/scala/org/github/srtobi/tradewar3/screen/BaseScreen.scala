package org.github.srtobi.tradewar3.screen

import com.badlogic.gdx.Screen
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

abstract class BaseScreen extends Screen:
  override def show(): Unit = {}
  override def resize(width: Int, height: Int): Unit = {}
  override def pause(): Unit = {}
  override def resume(): Unit = {}
  override def hide(): Unit = {}
  override def dispose(): Unit = {}

  def clearScreen(): Unit =
    Gdx.gl.glClearColor(0, 0, 0, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
