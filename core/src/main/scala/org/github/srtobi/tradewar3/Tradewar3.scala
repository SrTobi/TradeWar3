package org.github.srtobi.tradewar3

import com.badlogic.gdx.Game
import org.github.srtobi.tradewar3.screen.MainMenuScreen

class Tradewar3 extends Game:
  override def create(): Unit =
    setScreen(new MainMenuScreen(this))
