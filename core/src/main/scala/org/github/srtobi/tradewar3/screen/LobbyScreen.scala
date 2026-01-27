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
import org.github.srtobi.tradewar3.MusicManager
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
  private val starfield = new StarfieldBackground(400)
  private var skin: Skin = uninitialized
  private var playerTable: Table = uninitialized
  private var lastFactions: Seq[Faction] = Seq.empty
  private var pulseTime: Float = 0
  private val companyNames = Seq(
    "Nebula Corp", "Star Dynamics", "Galactic Mining", "Void Energy", "Orbit Logistics",
    "Comet Tech", "Pulsar Systems", "Titan Alloys", "Quasar Media", "Nova Pharma"
  )

  override def show(): Unit =
    Gdx.input.setInputProcessor(stage)
    skin = createSimpleSkin()
    MusicManager.playMenuMusic()

    val rootTable = new Table()
    rootTable.setFillParent(true)
    stage.addActor(rootTable)

    val titleLabel = new Label("BATTLE STATIONS", skin, "title")
    titleLabel.setColor(new Color(0.6f, 0.8f, 1f, 1f))
    rootTable.add(titleLabel).padBottom(15).row()

    val subtitleLabel = new Label("Waiting for commanders...", skin)
    subtitleLabel.setColor(new Color(0.7f, 0.75f, 0.85f, 1f))
    rootTable.add(subtitleLabel).padBottom(30).row()

    // Player list container with styled background
    val playerContainer = new Table()
    playerContainer.setBackground(skin.newDrawable("white", new Color(0.1f, 0.12f, 0.18f, 0.85f)))

    playerTable = new Table()
    playerContainer.add(playerTable).pad(20).expandX().fillX()

    rootTable.add(playerContainer).width(400).minHeight(150).pad(20).row()

    if server.isDefined then
      val startButton = new TextButton("LAUNCH BATTLE", skin)
      startButton.addListener(new ChangeListener {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          startGame()
      })
      rootTable.add(startButton).pad(15).width(320).height(70).row()
    else
      val waitingLabel = new Label("Awaiting host command...", skin)
      waitingLabel.setColor(new Color(1f, 0.85f, 0.5f, 1f))
      rootTable.add(waitingLabel).pad(15).row()

    val backButton = new TextButton("RETREAT", skin)
    backButton.addListener(new ChangeListener {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        client.stop()
        server.foreach(_.stop())
        game.setScreen(new MainMenuScreen(game))
    })
    rootTable.add(backButton).pad(10).width(200).height(55)

  private def startGame(): Unit =
    val factions = client.getFactions

    // Initialize Game State
    val selectedNames = scala.util.Random.shuffle(companyNames).take(STOCK_COMPANY_COUNT)
    val companies = selectedNames.map(name =>
      Company(name, StockMarket.generateRandomPrice(), StockMarket.generateRandomUpdateInterval())
    )

    val countries = MapGenerator.generateMap(MAP_RADIUS, factions)
    val initialGameState = GameState(
      money = Map.empty,  // Client-only, not synced
      companies = companies,
      holdings = Map.empty,  // Client-only, not synced
      countries = countries,
      bulkAmount = Map.empty,  // Client-only, not synced
      unitCost = UNIT_COST  // Will be recalculated
    )
    // Calculate initial unit cost based on starting units
    val gameStateWithCost = initialGameState.copy(unitCost = WarMap.calculateUnitCost(initialGameState))

    server.foreach { s =>
      s.broadcast(GameStateUpdate(gameStateWithCost))
      s.broadcast(StartGame)
    }

  override def render(delta: Float): Unit =
    clearScreen()
    starfield.render(delta)
    pulseTime += delta

    if client.isGameStarted then
      game.setScreen(new GameScreen(game, server, client))
      return

    updatePlayerList()

    stage.act(delta)
    stage.draw()

  private def updatePlayerList(): Unit =
    val factions = client.getFactions
    val factionColors = client.getFactionColors

    if factions != lastFactions then
      lastFactions = factions
      playerTable.clearChildren()

      val headerLabel = new Label("COMMANDERS ONLINE", skin)
      headerLabel.setColor(new Color(0.5f, 0.6f, 0.7f, 1f))
      playerTable.add(headerLabel).pad(10).colspan(2).row()

      factions.foreach { faction =>
        val factionColor = factionColors(faction)

        // Color indicator
        val colorBox = new Table()
        colorBox.setBackground(skin.newDrawable("white", factionColor))
        playerTable.add(colorBox).size(20, 20).pad(8)

        // Player name
        val label = new Label(faction.name, skin)
        label.setColor(new Color(0.9f, 0.95f, 1f, 1f))
        playerTable.add(label).pad(8).left().expandX().row()
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

    // Default font with shadow
    val parameter = new FreeTypeFontParameter()
    parameter.size = 24
    parameter.shadowOffsetX = 1
    parameter.shadowOffsetY = 1
    parameter.shadowColor = new Color(0, 0, 0, 0.5f)
    val font = generator.generateFont(parameter)
    skin.add("default", font)

    // Title font
    val titleParameter = new FreeTypeFontParameter()
    titleParameter.size = 52
    titleParameter.shadowOffsetX = 2
    titleParameter.shadowOffsetY = 2
    titleParameter.shadowColor = new Color(0.2f, 0.3f, 0.5f, 0.8f)
    titleParameter.borderWidth = 1
    titleParameter.borderColor = new Color(0.4f, 0.6f, 0.9f, 0.4f)
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

    // Enhanced button style
    val textButtonStyle = new TextButton.TextButtonStyle()
    textButtonStyle.up = skin.newDrawable("white", new Color(0.2f, 0.25f, 0.4f, 0.9f))
    textButtonStyle.down = skin.newDrawable("white", new Color(0.35f, 0.4f, 0.55f, 1f))
    textButtonStyle.over = skin.newDrawable("white", new Color(0.3f, 0.35f, 0.5f, 0.95f))
    textButtonStyle.font = skin.getFont("default")
    textButtonStyle.fontColor = new Color(0.9f, 0.95f, 1f, 1f)
    skin.add("default", textButtonStyle)

    skin
