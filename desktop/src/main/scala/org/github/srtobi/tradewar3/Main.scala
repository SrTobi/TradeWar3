package org.github.srtobi.tradewar3

import com.badlogic.gdx.backends.lwjgl._

@main
def run = {
    val cfg = new LwjglApplicationConfiguration
    cfg.title = "TradeWarGalaxy"
    cfg.height = 480
    cfg.width = 800
    cfg.forceExit = false
    new LwjglApplication(new Tradewar3, cfg)
}
