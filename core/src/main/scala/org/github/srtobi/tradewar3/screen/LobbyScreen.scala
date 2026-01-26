package org.github.srtobi.tradewar3.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.{Color, Pixmap, Texture}
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.scenes.scene2d.{Actor, Stage}
import com.badlogic.gdx.scenes.scene2d.ui.{Label, Skin, Table, TextButton}
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import org.github.srtobi.tradewar3.Tradewar3
import org.github.srtobi.tradewar3.net.*
import org.github.srtobi.tradewar3.ui.StarfieldBackground
import org.github.srtobi.tradewar3.model.*
import org.github.srtobi.tradewar3.logic.*
import org.github.srtobi.tradewar3.GameConfig.*

import scala.compiletime.uninitialized

class LobbyScreen(game: Tradewar3, 
                  server: Option[GameServer], 
                  client: GameClient) extends BaseScreen:
  private val stage = new Stage(new ScreenViewport())
  private val starfield = new StarfieldBackground()
  private var skin: Skin = uninitialized
  private var playerTable: Table = uninitialized
  private var lastFactions: Seq[Faction] = Seq.empty
  private val companyNames = Seq(
    "Nebula Corp", "Star Dynamics", "Galactic Mining", "Void Energy", "Orbit Logistics",
    "Comet Tech", "Pulsar Systems", "Titan Alloys", "Quasar Media", "Nova Pharma"
  )

  override def show(): Unit =
    Gdx.input.setInputProcessor(stage)
    skin = createSimpleSkin()
    
    val rootTable = new Table()
    rootTable.setFillParent(true)
    stage.addActor(rootTable)
    
    val titleLabel = new Label("Lobby", skin, "title")
    rootTable.add(titleLabel).padBottom(30).row()
    
    playerTable = new Table()
    rootTable.add(playerTable).expandX().fillX().pad(20).row()
    
    if server.isDefined then
      val startButton = new TextButton("Start Game", skin)
      startButton.addListener(new ChangeListener {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          startGame()
      })
      rootTable.add(startButton).pad(10).width(300).height(70).row()
    else
      rootTable.add(new Label("Waiting for host to start...", skin)).pad(10).row()
      
    val backButton = new TextButton("Back to Menu", skin)
    backButton.addListener(new ChangeListener {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        client.stop()
        server.foreach(_.stop())
        game.setScreen(new MainMenuScreen(game))
    })
    rootTable.add(backButton).pad(10).width(300).height(70)

  private def startGame(): Unit =
    val factions = client.getFactions

    // Initialize Game State
    val selectedNames = scala.util.Random.shuffle(companyNames).take(STOCK_COMPANY_COUNT)
    val companies = selectedNames.map(name => 
      Company(name, StockMarket.generateRandomPrice(), StockMarket.generateRandomUpdateInterval())
    )
    
    val initialGameState = GameState(
      money = factions.map(_ -> INITIAL_MONEY).toMap,
      companies = companies,
      holdings = factions.map(f => f -> selectedNames.map(_ -> 0).toMap).toMap,
      countries = MapGenerator.generateMap(MAP_RADIUS, factions),
      bulkAmount = factions.map(_ -> INITIAL_BULK_AMOUNT).toMap
    )
    
    server.foreach { s =>
      s.broadcast(GameStateUpdate(initialGameState))
      s.broadcast(StartGame)
    }

  override def render(delta: Float): Unit =
    clearScreen()
    starfield.render(delta)
    
    if client.isGameStarted then
      game.setScreen(new GameScreen(game, server, client))
      return

    updatePlayerList()
    
    stage.act(delta)
    stage.draw()

  private def updatePlayerList(): Unit =
    val factions = client.getFactions
    if factions != lastFactions then
      lastFactions = factions
      playerTable.clearChildren()
      factions.foreach { faction =>
        val label = new Label(faction.name, skin)
        playerTable.add(label).pad(5).row()
      }

  override def resize(width: Int, height: Int): Unit =
    stage.getViewport.update(width, height, true)
    starfield.resize(width, height)

  override def dispose(): Unit =
    stage.dispose()
    starfield.dispose()
    if skin != null then skin.dispose()
    server.foreach(_.stop())
    client.stop()

  private def createSimpleSkin(): Skin =
    val skin = new Skin()
    val generator = new FreeTypeFontGenerator(Gdx.files.internal("assets/fonts/Roboto-Regular.ttf"))
    
    val parameter = new FreeTypeFontParameter()
    parameter.size = 24
    val font = generator.generateFont(parameter)
    skin.add("default", font)

    val titleParameter = new FreeTypeFontParameter()
    titleParameter.size = 48
    val titleFont = generator.generateFont(titleParameter)
    skin.add("title", titleFont)
    
    generator.dispose()
    
    val pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888)
    pixmap.setColor(Color.WHITE)
    pixmap.fill()
    val texture = new Texture(pixmap)
    pixmap.dispose()
    skin.add("white", texture)

    val labelStyle = new Label.LabelStyle()
    labelStyle.font = font
    skin.add("default", labelStyle)

    val titleLabelStyle = new Label.LabelStyle()
    titleLabelStyle.font = titleFont
    skin.add("title", titleLabelStyle)
    
    val textButtonStyle = new TextButton.TextButtonStyle()
    textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY)
    textButtonStyle.down = skin.newDrawable("white", Color.LIGHT_GRAY)
    textButtonStyle.over = skin.newDrawable("white", Color.GRAY)
    textButtonStyle.font = skin.getFont("default")
    skin.add("default", textButtonStyle)

    skin
