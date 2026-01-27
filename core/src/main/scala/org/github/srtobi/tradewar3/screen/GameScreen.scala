package org.github.srtobi.tradewar3.screen

import com.badlogic.gdx.{Gdx, Input}
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.graphics.{Color, Pixmap, Texture}
import com.badlogic.gdx.scenes.scene2d.ui.{Label, Skin, Table, TextButton, Value}
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.{Actor, Stage}
import com.badlogic.gdx.utils.viewport.ScreenViewport
import org.github.srtobi.tradewar3.*
import org.github.srtobi.tradewar3.Tradewar3
import org.github.srtobi.tradewar3.MusicManager
import org.github.srtobi.tradewar3.logic.{MapGenerator, StockMarket, WarMap}
import org.github.srtobi.tradewar3.model.*
import org.github.srtobi.tradewar3.ui.{StarfieldBackground, StockMarketUI, WarMapUI}
import org.github.srtobi.tradewar3.net.*
import org.github.srtobi.tradewar3.GameConfig.*

import scala.compiletime.uninitialized

class GameScreen(game: Tradewar3,
                 private val server: Option[GameServer],
                 private val client: GameClient) extends BaseScreen:
  private val stage = new Stage(new ScreenViewport())
  private var skin: Skin = uninitialized
  private var gameState: GameState = uninitialized

  // Animated starfield background
  private val starfield = new StarfieldBackground(400)

  // UI Components
  private var stockMarketUI: StockMarketUI = uninitialized
  private var warMapUI: WarMapUI = uninitialized
  private var playerListTable: Table = uninitialized
  private var lastAction: () => Unit = uninitialized

  private var localFaction: Faction = client.getAssignedFaction.get
  val factionColors: FactionColors = client.getFactionColors

  // Client-only state (not synced over network)
  private var localMoney: Long = INITIAL_MONEY
  private var localHoldings: Map[String, Int] = Map.empty
  private var localBulkAmount: Int = INITIAL_BULK_AMOUNT

  // Music state tracking
  private var musicUpdateTimer: Float = 0f
  private val musicUpdateInterval: Float = 2.0f

  // Network broadcast throttling
  private var broadcastTimer: Float = 0f
  private val broadcastInterval: Float = 0.05f  // Max 20 broadcasts per second
  private var needsBroadcast: Boolean = true

  // Creates a view of the game state with local money/holdings/bulk for UI
  private def localViewState: GameState =
    if gameState == null then return null
    gameState.copy(
      money = Map(localFaction -> localMoney),
      holdings = Map(localFaction -> localHoldings),
      bulkAmount = Map(localFaction -> localBulkAmount)
    )

  // Creates a stripped state for network broadcast (no money/holdings/bulk/battle timers)
  private def networkState: GameState =
    gameState.copy(
      money = Map.empty,
      holdings = Map.empty,
      bulkAmount = Map.empty,
      countries = gameState.countries.map(c => c.copy(nextBattleUpdate = 0f))
    )

  private val companyNames = Seq(
    "Nebula Corp", "Star Dynamics", "Galactic Mining", "Void Energy", "Orbit Logistics",
    "Comet Tech", "Pulsar Systems", "Titan Alloys", "Quasar Media", "Nova Pharma"
  )

  override def show(): Unit =
    Gdx.input.setInputProcessor(stage)
    skin = createGameSkin()
    MusicManager.playGameMusic()

    setupUI()

  private def setupUI(): Unit =
    val rootTable = new Table()
    rootTable.setFillParent(true)
    stage.addActor(rootTable)

    // Left Panel: Stock Market (all stock actions are client-only)
    stockMarketUI = new StockMarketUI(skin,
      companyName => {
        buyStock(companyName)
        lastAction = () => buyStock(companyName)
      },
      companyName => {
        sellStock(companyName)
        lastAction = () => sellStock(companyName)
      },
      () => increaseBulk(),
      factionColors
    )
    if gameState != null then stockMarketUI.init(localViewState, localFaction)

    // Right Panel: War Map
    warMapUI = new WarMapUI(skin, coords => {
      if tryPlaceUnit(coords) then
        lastAction = () => tryPlaceUnit(coords)
    }, factionColors, localFaction)
    if gameState != null then warMapUI.update(gameState)

    rootTable.add(stockMarketUI).expandY().fill().width(Value.percentWidth(UI_STOCK_PANEL_WIDTH_PERCENT, rootTable))
    rootTable.add(warMapUI).expandY().fill().expandX()

    playerListTable = new Table()
    playerListTable.setFillParent(true)
    playerListTable.top().right()
    stage.addActor(playerListTable)

    if gameState != null then updateUI()

  private def tryPlaceUnit(coords: HexCoordinate): Boolean =
    if gameState == null then return false
    if !WarMap.canPlaceUnits(gameState, coords, localFaction) then return false
    if localMoney < gameState.unitCost then return false

    localMoney -= gameState.unitCost
    client.send(PlaceUnitsAction(localFaction, coords))
    updateUI()
    true

  private def applyAction(action: PlayerAction): Unit =
    action match
      case PlaceUnitsAction(faction, coords) =>
        placeUnits(faction, coords)

  private def buyStock(companyName: String): Unit =
    val company = gameState.companies.find(_.name == companyName).get
    val maxAffordable = (localMoney / company.price).toInt
    val amountToBuy = Math.min(localBulkAmount, maxAffordable)

    if amountToBuy > 0 then
      val cost = amountToBuy.toLong * company.price
      val currentHoldings = localHoldings.getOrElse(companyName, 0)
      localMoney -= cost
      localHoldings = localHoldings + (companyName -> (currentHoldings + amountToBuy))
      updateUI()

  private def sellStock(companyName: String): Unit =
    val company = gameState.companies.find(_.name == companyName).get
    val currentHoldings = localHoldings.getOrElse(companyName, 0)
    val amountToSell = Math.min(localBulkAmount, currentHoldings)

    if amountToSell > 0 then
      val gain = amountToSell.toLong * company.price
      localMoney += gain
      localHoldings = localHoldings + (companyName -> (currentHoldings - amountToSell))
      updateUI()

  private def placeUnits(faction: Faction, coords: HexCoordinate): Unit =
    gameState = WarMap.placeUnits(gameState, coords, faction, 1)

  private def increaseBulk(): Unit =
    val cost = StockMarket.getBulkUpgradeCost(localBulkAmount)
    if localMoney >= cost then
      localMoney -= cost
      localBulkAmount += 1
      updateUI()

  private def updateUI(): Unit =
    if stockMarketUI != null && gameState != null then stockMarketUI.update(localViewState, localFaction)
    if warMapUI != null then warMapUI.update(gameState)
    updatePlayerList()

  private def updatePlayerList(): Unit =
    if playerListTable == null || gameState == null then return
    playerListTable.clearChildren()
    val players = client.getFactions
    players.foreach { faction =>
      val factionColor = factionColors(faction)
      val fontColor =
        if factionColor.distanceSquared(Color.BLACK) < factionColor.distanceSquared(Color.WHITE) then
          Color.WHITE
        else
          Color.BLACK

      val style = new Label.LabelStyle(skin.get(classOf[Label.LabelStyle]))
      style.fontColor = fontColor
      val label = new Label(faction.name, style)

      val container = new Table()
      // Slightly transparent background for better visual integration
      val bgColor = new Color(factionColor.r * 0.9f, factionColor.g * 0.9f, factionColor.b * 0.9f, 0.9f)
      container.setBackground(skin.newDrawable("white", bgColor))
      container.add(label).pad(8, 20, 8, 20)

      playerListTable.add(container).pad(3).right()
      playerListTable.row()
    }

  override def render(delta: Float): Unit =
    clearScreen()

    // Render animated starfield background
    starfield.render(delta)

    // Only update gameState from network if we're NOT the host
    // The host has authoritative state and shouldn't overwrite it
    // (network state has battle timers stripped to 0)
    if server.isEmpty then
      client.pollState().foreach { newState =>
        val isFirstState = gameState == null
        gameState = newState
        if isFirstState then stockMarketUI.init(localViewState, localFaction)
        updateUI()
      }
    else
      // Host still needs to initialize UI on first state
      client.pollState().foreach { newState =>
        if gameState == null then
          gameState = newState
          stockMarketUI.init(localViewState, localFaction)
        updateUI()
      }

    server.foreach { s =>
      if gameState != null then
        // Update battles BEFORE processing actions, so newly placed units survive at least one frame
        val (updatedGameState, structuralChange) = WarMap.updateBattles(gameState, delta)
        gameState = updatedGameState
        if structuralChange then needsBroadcast = true

        // Update stock prices
        val updatedCompanies = gameState.companies.map(c => StockMarket.updateCompany(c, delta))
        if updatedCompanies != gameState.companies then
            gameState = gameState.copy(companies = updatedCompanies)
            needsBroadcast = true

      // Process actions after battles
      val actions = s.getActions
      if actions.nonEmpty then
        actions.foreach(applyAction)
        needsBroadcast = true

      // Throttle broadcasts to avoid overwhelming the network
      broadcastTimer += delta
      if gameState != null && needsBroadcast && broadcastTimer >= broadcastInterval then
        broadcastTimer = 0f
        needsBroadcast = false
        Gdx.app.log("GameScreen", "About to broadcast...")
        val start = System.currentTimeMillis()
        s.broadcast(GameStateUpdate(networkState))
        val elapsed = System.currentTimeMillis() - start
        if elapsed > 10 then
          Gdx.app.log("GameScreen", s"Broadcast took ${elapsed}ms")
    }

    if gameState != null then
        updateMusic(delta)

        if Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && lastAction != null then
            lastAction()

        stage.act(delta)
        stage.draw()

  override def resize(width: Int, height: Int): Unit =
    stage.getViewport.update(width, height, true)
    starfield.resize(width, height)

  override def dispose(): Unit =
    stage.dispose()
    if skin != null then skin.dispose()
    starfield.dispose()
    server.foreach(_.stop())
    client.stop()

  private def checkWinCondition(): Unit =
    val activeFactions = gameState.countries.map(_.owner).filter(!_.isNeutral).distinct
    if activeFactions.size <= 1 then
        server.foreach(_.stop())
        client.stop()
        game.setScreen(new MainMenuScreen(game))

  private def updateMusic(delta: Float): Unit =
    musicUpdateTimer += delta
    if musicUpdateTimer < musicUpdateInterval then return
    musicUpdateTimer = 0f

    if gameState == null then return

    val countries = gameState.countries
    val totalTerritories = countries.size
    val playerTerritories = countries.count(_.owner == localFaction)
    val playerUnits = countries.filter(_.owner == localFaction).map(_.unitCount).sum

    // Count territories with imminent battles (timer < 2 seconds)
    val activeBattles = countries.count { c =>
      c.nextBattleUpdate < 2.0f && c.unitCount > 0 && !c.owner.isNeutral
    }

    // Count enemy territories adjacent to player
    val playerCoords = countries.filter(_.owner == localFaction).map(_.coords).toSet
    val threatenedBorders = countries.count { c =>
      c.owner != localFaction && !c.owner.isNeutral &&
        c.coords.neighbors.exists(playerCoords.contains)
    }

    val playerShare = playerTerritories.toFloat / totalTerritories

    // Determine music based on game state
    if playerTerritories == 0 then
      // Player eliminated
      MusicManager.playDangerMusic()
    else if playerShare > 0.6f then
      // Dominating
      MusicManager.playVictoryMusic()
    else if playerShare < 0.15f || playerUnits < 5 then
      // In danger
      MusicManager.playDangerMusic()
    else if activeBattles > 3 || threatenedBorders > 2 then
      // Intense combat
      MusicManager.playBattleMusic()
    else
      // Normal gameplay
      MusicManager.playGameMusic()

  private def createGameSkin(): Skin =
    val skin = new Skin()
    val generator = new FreeTypeFontGenerator(Gdx.files.internal("assets/fonts/Roboto-Regular.ttf"))

    // Default font with subtle shadow for depth
    val parameter = new FreeTypeFontParameter()
    parameter.size = 32
    parameter.shadowOffsetX = 1
    parameter.shadowOffsetY = 1
    parameter.shadowColor = new Color(0, 0, 0, 0.5f)
    val font = generator.generateFont(parameter)
    skin.add("default", font)

    // Big font with stronger shadow
    val bigParameter = new FreeTypeFontParameter()
    bigParameter.size = 42
    bigParameter.shadowOffsetX = 2
    bigParameter.shadowOffsetY = 2
    bigParameter.shadowColor = new Color(0, 0, 0, 0.6f)
    val bigFont = generator.generateFont(bigParameter)
    skin.add("big", bigFont)

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

    val bigLabelStyle = new Label.LabelStyle()
    bigLabelStyle.font = bigFont
    skin.add("big", bigLabelStyle)

    // Enhanced button style with cooler colors
    val textButtonStyle = new TextButton.TextButtonStyle()
    textButtonStyle.up = skin.newDrawable("white", new Color(0.2f, 0.25f, 0.35f, 1f))
    textButtonStyle.down = skin.newDrawable("white", new Color(0.35f, 0.4f, 0.5f, 1f))
    textButtonStyle.over = skin.newDrawable("white", new Color(0.3f, 0.35f, 0.45f, 1f))
    textButtonStyle.disabled = skin.newDrawable("white", new Color(0.12f, 0.12f, 0.18f, 0.7f))
    textButtonStyle.font = skin.getFont("default")
    textButtonStyle.fontColor = Color.WHITE
    textButtonStyle.disabledFontColor = new Color(0.5f, 0.5f, 0.5f, 1f)
    skin.add("default", textButtonStyle)
    skin
