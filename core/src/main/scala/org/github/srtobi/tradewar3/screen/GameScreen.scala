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
import org.github.srtobi.tradewar3.logic.{MapGenerator, StockMarket, WarMap}
import org.github.srtobi.tradewar3.model.*
import org.github.srtobi.tradewar3.ui.{StockMarketUI, WarMapUI}
import org.github.srtobi.tradewar3.net.*
import org.github.srtobi.tradewar3.GameConfig.*

import scala.compiletime.uninitialized

class GameScreen(game: Tradewar3, 
                 private val server: Option[GameServer], 
                 private val client: GameClient) extends BaseScreen:
  private val stage = new Stage(new ScreenViewport())
  private var skin: Skin = uninitialized
  private var gameState: GameState = uninitialized
  
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

  // Creates a view of the game state with local money/holdings/bulk for UI
  private def localViewState: GameState =
    if gameState == null then return null
    gameState.copy(
      money = Map(localFaction -> localMoney),
      holdings = Map(localFaction -> localHoldings),
      bulkAmount = Map(localFaction -> localBulkAmount)
    )

  // Creates a stripped state for network broadcast (no money/holdings/bulk)
  private def networkState: GameState =
    gameState.copy(
      money = Map.empty,
      holdings = Map.empty,
      bulkAmount = Map.empty
    )

  private val companyNames = Seq(
    "Nebula Corp", "Star Dynamics", "Galactic Mining", "Void Energy", "Orbit Logistics",
    "Comet Tech", "Pulsar Systems", "Titan Alloys", "Quasar Media", "Nova Pharma"
  )

  override def show(): Unit =
    Gdx.input.setInputProcessor(stage)
    skin = createGameSkin()

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
    }, factionColors)
    if gameState != null then warMapUI.update(gameState)

    rootTable.add(stockMarketUI).expandY().fill().width(Value.percentWidth(UI_STOCK_PANEL_WIDTH_PERCENT, rootTable))
    rootTable.add(warMapUI).expandY().fill().expandX()
    
    playerListTable = new Table()
    playerListTable.setFillParent(true)
    playerListTable.top().right()
    stage.addActor(playerListTable)

    if gameState != null then updateUI()

  private def sendAction(action: PlaceUnitsAction): Unit =
    client.send(action)

  private def applyAction(action: PlayerAction): Unit =
    action match
      case PlaceUnitsAction(faction, coords) =>
        placeUnits(faction, coords)
      case _ => // Buy/Sell/Bulk actions are client-only, ignore from server

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
      container.setBackground(skin.newDrawable("white", factionColor))
      container.add(label).pad(5, 15, 5, 15)
      
      playerListTable.add(container).pad(5).right()
      playerListTable.row()
    }

  override def render(delta: Float): Unit =
    clearScreen()

    client.pollState().foreach { newState =>
      val isFirstState = gameState == null
      gameState = newState
      if isFirstState then stockMarketUI.init(localViewState, localFaction)
      updateUI()
    }

    server.foreach { s =>
      // Process actions
      s.getActions.foreach(applyAction)
      
      if gameState != null then
        // Update logic
        val updatedCompanies = gameState.companies.map(c => StockMarket.updateCompany(c, delta))
        if updatedCompanies != gameState.companies then
            gameState = gameState.copy(companies = updatedCompanies)

        val (updatedGameState, _) = WarMap.updateBattles(gameState, delta)
        if updatedGameState != gameState then
          gameState = updatedGameState

          // Broadcast state (without money/holdings/bulk - those are client-only)
          s.broadcast(GameStateUpdate(networkState))
    }
    
    if gameState != null then
        if Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && lastAction != null then
            lastAction()

        stage.act(delta)
        stage.draw()

  override def resize(width: Int, height: Int): Unit =
    stage.getViewport.update(width, height, true)

  override def dispose(): Unit =
    stage.dispose()
    if skin != null then skin.dispose()
    server.foreach(_.stop())
    client.stop()

  private def checkWinCondition(): Unit =
    val activeFactions = gameState.countries.map(_.owner).filter(!_.isNeutral).distinct
    if activeFactions.size <= 1 then
        server.foreach(_.stop())
        client.stop()
        game.setScreen(new MainMenuScreen(game))

  private def createGameSkin(): Skin =
    val skin = new Skin()
    val generator = new FreeTypeFontGenerator(Gdx.files.internal("assets/fonts/Roboto-Regular.ttf"))

    val parameter = new FreeTypeFontParameter()
    parameter.size = 32
    val font = generator.generateFont(parameter)
    skin.add("default", font)
    
    val bigParameter = new FreeTypeFontParameter()
    bigParameter.size = 42
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
    
    val textButtonStyle = new TextButton.TextButtonStyle()
    textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY)
    textButtonStyle.down = skin.newDrawable("white", Color.LIGHT_GRAY)
    textButtonStyle.over = skin.newDrawable("white", Color.GRAY)
    textButtonStyle.disabled = skin.newDrawable("white", Color.BLACK)
    textButtonStyle.font = skin.getFont("default")
    skin.add("default", textButtonStyle)
    skin
