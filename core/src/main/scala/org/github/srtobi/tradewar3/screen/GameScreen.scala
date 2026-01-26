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

    // Left Panel: Stock Market
    stockMarketUI = new StockMarketUI(skin,
      companyName => {
        val action = BuyAction(localFaction, companyName)
        sendAction(action)
        lastAction = () => sendAction(action)
      },
      companyName => {
        val action = SellAction(localFaction, companyName)
        sendAction(action)
        lastAction = () => sendAction(action)
      },
      () => {
        val action = UpgradeBulkAction(localFaction)
        sendAction(action)
      },
      factionColors
    )
    if gameState != null then stockMarketUI.init(gameState, localFaction)

    // Right Panel: War Map
    warMapUI = new WarMapUI(skin, coords => {
      val action = PlaceUnitsAction(localFaction, coords)
      sendAction(action)
      lastAction = () => sendAction(action)
    }, factionColors)
    if gameState != null then warMapUI.update(gameState)

    rootTable.add(stockMarketUI).expandY().fill().width(Value.percentWidth(UI_STOCK_PANEL_WIDTH_PERCENT, rootTable))
    rootTable.add(warMapUI).expandY().fill().expandX()
    
    playerListTable = new Table()
    playerListTable.setFillParent(true)
    playerListTable.top().right()
    stage.addActor(playerListTable)

    if gameState != null then updateUI()

  private def sendAction(action: PlayerAction): Unit =
    client.send(action)

  private def applyAction(action: PlayerAction): Unit =
    action match
      case BuyAction(faction, companyName) =>
        buyStock(faction, companyName)
      case SellAction(faction, companyName) =>
        sellStock(faction, companyName)
      case PlaceUnitsAction(faction, coords) =>
        placeUnits(faction, coords)
      case UpgradeBulkAction(faction) =>
        increaseBulk(faction)

  private def buyStock(faction: Faction, companyName: String): Unit =
    val company = gameState.companies.find(_.name == companyName).get
    val money = gameState.money.getOrElse(faction, 0L)
    val maxAffordable = (money / company.price).toInt
    val bulkAmount = gameState.bulkAmount.getOrElse(faction, 1)
    val amountToBuy = Math.min(bulkAmount, maxAffordable)
    
    if amountToBuy > 0 then
      val cost = amountToBuy.toLong * company.price
      val factionHoldings = gameState.holdings.getOrElse(faction, Map.empty)
      val currentHoldings = factionHoldings.getOrElse(companyName, 0)
      val newHoldings = gameState.holdings + (faction -> (factionHoldings + (companyName -> (currentHoldings + amountToBuy))))
      gameState = gameState.copy(
        money = gameState.money + (faction -> (money - cost)),
        holdings = newHoldings
      )

  private def sellStock(faction: Faction, companyName: String): Unit =
    val company = gameState.companies.find(_.name == companyName).get
    val factionHoldings = gameState.holdings.getOrElse(faction, Map.empty)
    val currentHoldings = factionHoldings.getOrElse(companyName, 0)
    val bulkAmount = gameState.bulkAmount.getOrElse(faction, 1)
    val amountToSell = Math.min(bulkAmount, currentHoldings)
    
    if amountToSell > 0 then
      val gain = amountToSell.toLong * company.price
      val money = gameState.money.getOrElse(faction, 0L)
      val newHoldings = gameState.holdings + (faction -> (factionHoldings + (companyName -> (currentHoldings - amountToSell))))
      gameState = gameState.copy(
        money = gameState.money + (faction -> (money + gain)),
        holdings = newHoldings
      )

  private def placeUnits(faction: Faction, coords: HexCoordinate): Unit =
    val bulkAmount = gameState.bulkAmount.getOrElse(faction, 1)
    gameState = WarMap.placeUnits(gameState, coords, faction, bulkAmount)

  private def increaseBulk(faction: Faction): Unit =
    val bulkAmount = gameState.bulkAmount.getOrElse(faction, 1)
    val cost = StockMarket.getBulkUpgradeCost(bulkAmount)
    val money = gameState.money.getOrElse(faction, 0L)
    if money >= cost then
        gameState = gameState.copy(
            money = gameState.money + (faction -> (money - cost)),
            bulkAmount = gameState.bulkAmount + (faction -> (bulkAmount + 1))
        )

  private def updateUI(): Unit =
    if stockMarketUI != null then stockMarketUI.update(gameState, localFaction)
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
      if isFirstState then stockMarketUI.init(gameState, localFaction)
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

          // Broadcast state
          s.broadcast(GameStateUpdate(gameState))
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
