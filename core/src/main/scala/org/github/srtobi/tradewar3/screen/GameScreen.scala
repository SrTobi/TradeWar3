package org.github.srtobi.tradewar3.screen

import com.badlogic.gdx.{Gdx, Input}
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.graphics.{Color, Pixmap, Texture}
import com.badlogic.gdx.scenes.scene2d.ui.{Label, Skin, Table, TextButton, Value}
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.{Actor, Stage}
import com.badlogic.gdx.utils.viewport.ScreenViewport
import org.github.srtobi.tradewar3.Tradewar3
import org.github.srtobi.tradewar3.logic.{MapGenerator, StockMarket, WarMap}
import org.github.srtobi.tradewar3.model.*
import org.github.srtobi.tradewar3.ui.{StockMarketUI, WarMapUI}

import scala.compiletime.uninitialized

class GameScreen(game: Tradewar3) extends BaseScreen:
  private val stage = new Stage(new ScreenViewport())
  private var skin: Skin = uninitialized
  private var gameState: GameState = uninitialized
  
  // UI Components
  private var stockMarketUI: StockMarketUI = uninitialized
  private var warMapUI: WarMapUI = uninitialized
  private var lastAction: () => Unit = uninitialized

  private val companyNames = Seq(
    "Nebula Corp", "Star Dynamics", "Galactic Mining", "Void Energy", "Orbit Logistics",
    "Comet Tech", "Pulsar Systems", "Titan Alloys", "Quasar Media", "Nova Pharma"
  )

  override def show(): Unit =
    Gdx.input.setInputProcessor(stage)
    skin = createGameSkin()
    
    // Initialize Game State
    val selectedNames = scala.util.Random.shuffle(companyNames).take(5)
    val companies = selectedNames.map(name => 
      Company(name, StockMarket.generateRandomPrice(), StockMarket.generateRandomUpdateInterval())
    )
    
    gameState = GameState(
      money = 10000,
      companies = companies,
      holdings = Map(Faction.Player -> selectedNames.map(_ -> 0).toMap),
      countries = MapGenerator.generateMap(3),
      bulkAmount = 1
    )

    setupUI()

  private def setupUI(): Unit =
    val rootTable = new Table()
    rootTable.setFillParent(true)
    stage.addActor(rootTable)

    // Left Panel: Stock Market
    stockMarketUI = new StockMarketUI(skin,
      companyName => {
        buyStock(companyName)
        lastAction = () => buyStock(companyName)
      },
      companyName => {
        sellStock(companyName)
        lastAction = () => sellStock(companyName)
      },
      () => increaseBulk()
    )
    stockMarketUI.init(gameState)

    // Right Panel: War Map
    warMapUI = new WarMapUI(skin, coords => {
      placeUnits(coords)
      lastAction = () => placeUnits(coords)
    })
    warMapUI.update(gameState)

    rootTable.add(stockMarketUI).expandY().fill().width(Value.percentWidth(0.33f, rootTable))
    rootTable.add(warMapUI).expandY().fill().expandX()
    
    updateUI()

  private def buyStock(companyName: String): Unit =
    val company = gameState.companies.find(_.name == companyName).get
    val maxAffordable = (gameState.money / company.price).toInt
    val amountToBuy = Math.min(gameState.bulkAmount, maxAffordable)
    
    if amountToBuy > 0 then
      val cost = amountToBuy.toLong * company.price
      val currentHoldings = gameState.holdings(Faction.Player).getOrElse(companyName, 0)
      val newHoldings = gameState.holdings + (Faction.Player -> (gameState.holdings(Faction.Player) + (companyName -> (currentHoldings + amountToBuy))))
      gameState = gameState.copy(
        money = gameState.money - cost,
        holdings = newHoldings
      )
      updateUI()

  private def sellStock(companyName: String): Unit =
    val company = gameState.companies.find(_.name == companyName).get
    val currentHoldings = gameState.holdings(Faction.Player).getOrElse(companyName, 0)
    val amountToSell = Math.min(gameState.bulkAmount, currentHoldings)
    
    if amountToSell > 0 then
      val gain = amountToSell.toLong * company.price
      val newHoldings = gameState.holdings + (Faction.Player -> (gameState.holdings(Faction.Player) + (companyName -> (currentHoldings - amountToSell))))
      gameState = gameState.copy(
        money = gameState.money + gain,
        holdings = newHoldings
      )
      updateUI()

  private def placeUnits(coords: HexCoordinate): Unit =
    val oldMoney = gameState.money
    gameState = WarMap.placeUnits(gameState, coords, gameState.bulkAmount)
    if gameState.money != oldMoney then
        updateUI()

  private def increaseBulk(): Unit =
    val cost = StockMarket.getBulkUpgradeCost(gameState.bulkAmount)
    if gameState.money >= cost then
        gameState = gameState.copy(
            money = gameState.money - cost,
            bulkAmount = gameState.bulkAmount + 1
        )
        updateUI()

  private def updateUI(): Unit =
    stockMarketUI.update(gameState)
    if warMapUI != null then warMapUI.update(gameState)

  override def render(delta: Float): Unit =
    clearScreen()
    
    // Update logic
    val updatedCompanies = gameState.companies.map(c => StockMarket.updateCompany(c, delta))
    if updatedCompanies != gameState.companies then
        gameState = gameState.copy(companies = updatedCompanies)
        updateUI()

    if Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && lastAction != null then
        lastAction()

    stage.act(delta)
    stage.draw()

  override def resize(width: Int, height: Int): Unit =
    stage.getViewport.update(width, height, true)

  override def dispose(): Unit =
    stage.dispose()
    if skin != null then skin.dispose()

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
