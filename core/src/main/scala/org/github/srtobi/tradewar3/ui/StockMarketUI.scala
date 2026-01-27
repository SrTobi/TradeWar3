package org.github.srtobi.tradewar3.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.{Label, Skin, Table, TextButton}
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import org.github.srtobi.tradewar3.model.{Faction, FactionColors, GameState}
import org.github.srtobi.tradewar3.logic.StockMarket

import scala.compiletime.uninitialized
import scala.collection.mutable.ArrayBuffer

class StockMarketUI(skin: Skin,
                    onBuy: String => Unit,
                    onSell: String => Unit,
                    onUpgradeBulk: () => Unit,
                    factionColors: FactionColors) extends Table(skin):

  private var balanceLabel: Label = uninitialized
  private var unitCostLabel: Label = uninitialized
  private var companyRows: Seq[CompanyRow] = uninitialized
  private var bulkLabel: Label = uninitialized
  private var increaseBulkButton: TextButton = uninitialized

  // Price change indicators
  private var priceChanges: Map[String, Float] = Map.empty // companyName -> change amount
  private var lastPrices: Map[String, Int] = Map.empty

  private case class CompanyRow(
    nameLabel: Label,
    priceLabel: Label,
    holdingsLabel: Label,
    buyButton: TextButton,
    sellButton: TextButton,
    changeIndicator: Label
  )

  // Semi-transparent dark background with slight blue tint
  setBackground(skin.newDrawable("white", new Color(0.08f, 0.1f, 0.15f, 0.92f)))

  def init(state: GameState, faction: Faction): Unit =
    clearChildren()
    val money = state.money.getOrElse(faction, 0L)

    // Title section
    val titleLabel = new Label("GALACTIC EXCHANGE", skin, "big")
    titleLabel.setColor(new Color(0.7f, 0.8f, 1f, 1f))
    add(titleLabel).pad(15).colspan(6).center()
    row()

    // Balance display with glow effect
    balanceLabel = new Label(s"$money", skin, "big")
    balanceLabel.setColor(new Color(0.4f, 1f, 0.6f, 1f)) // Green money color
    val balanceContainer = new Table()
    val euroLabel = new Label(" Credits", skin)
    euroLabel.setColor(new Color(0.6f, 0.8f, 0.7f, 1f))
    balanceContainer.add(balanceLabel)
    balanceContainer.add(euroLabel)
    add(balanceContainer).pad(10).colspan(4).left()

    unitCostLabel = new Label(s"Unit: ${state.unitCost}", skin)
    unitCostLabel.setColor(new Color(1f, 0.8f, 0.5f, 1f)) // Orange unit cost
    add(unitCostLabel).pad(10).colspan(2).right()
    row()

    // Separator
    val separator = new Table()
    separator.setBackground(skin.newDrawable("white", new Color(0.3f, 0.4f, 0.5f, 0.5f)))
    add(separator).height(2).colspan(6).fillX().pad(5, 10, 10, 10)
    row()

    // Column headers
    val headerStyle = new Label.LabelStyle(skin.get(classOf[Label.LabelStyle]))
    headerStyle.fontColor = new Color(0.6f, 0.7f, 0.8f, 1f)

    val nameHeader = new Label("COMPANY", skin)
    nameHeader.setStyle(headerStyle)
    val priceHeader = new Label("PRICE", skin)
    priceHeader.setStyle(headerStyle)
    val changeHeader = new Label("", skin)
    val ownedHeader = new Label("OWNED", skin)
    ownedHeader.setStyle(headerStyle)

    add(nameHeader).pad(5).left().expandX()
    add(priceHeader).pad(5).width(90)
    add(changeHeader).pad(5).width(40)
    add(ownedHeader).pad(5).width(100)
    add().pad(5).width(90) // Buy
    add().pad(5).width(90) // Sell
    row()

    companyRows = state.companies.map { company =>
      val nameLabel = new Label(company.name, skin)
      nameLabel.setColor(new Color(0.9f, 0.95f, 1f, 1f))

      val priceLabel = new Label(s"${company.price}", skin)
      priceLabel.setColor(new Color(1f, 1f, 0.7f, 1f)) // Yellow price

      val changeIndicator = new Label("", skin)

      val holdings = state.holdings.get(faction).flatMap(_.get(company.name)).getOrElse(0)
      val holdingsLabel = new Label(s"$holdings", skin)
      holdingsLabel.setColor(if holdings > 0 then new Color(0.5f, 1f, 0.7f, 1f) else new Color(0.5f, 0.5f, 0.6f, 1f))

      val buyButton = new TextButton("BUY", skin)
      val sellButton = new TextButton("SELL", skin)

      add(nameLabel).pad(5).left().expandX()
      add(priceLabel).pad(5).width(90).right()
      add(changeIndicator).pad(5).width(40).center()
      add(holdingsLabel).pad(5).width(100).center()
      add(buyButton).pad(5).width(90).height(50)
      add(sellButton).pad(5).width(90).height(50)
      row()

      buyButton.addListener(new ChangeListener {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          onBuy(company.name)
      })

      sellButton.addListener(new ChangeListener {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          onSell(company.name)
      })

      CompanyRow(nameLabel, priceLabel, holdingsLabel, buyButton, sellButton, changeIndicator)
    }

    // Another separator
    val separator2 = new Table()
    separator2.setBackground(skin.newDrawable("white", new Color(0.3f, 0.4f, 0.5f, 0.5f)))
    add(separator2).height(2).colspan(6).fillX().pad(10, 10, 5, 10)
    row()

    // Bulk upgrade section
    val bulkAmount = state.bulkAmount.getOrElse(faction, 1)
    bulkLabel = new Label(s"Trade Amount: $bulkAmount", skin)
    bulkLabel.setColor(new Color(0.8f, 0.85f, 1f, 1f))

    increaseBulkButton = new TextButton(s"UPGRADE (${StockMarket.getBulkUpgradeCost(bulkAmount)})", skin)
    increaseBulkButton.addListener(new ChangeListener {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        onUpgradeBulk()
    })

    add(bulkLabel).pad(10).colspan(4).left()
    add(increaseBulkButton).pad(10).colspan(2).right().width(200).height(55)

    // Initialize price tracking
    lastPrices = state.companies.map(c => c.name -> c.price).toMap

  def update(state: GameState, faction: Faction): Unit =
    if balanceLabel == null then return

    val money = state.money.getOrElse(faction, 0L)
    val bulkAmount = state.bulkAmount.getOrElse(faction, 1)
    balanceLabel.setText(s"$money")
    unitCostLabel.setText(s"Unit: ${state.unitCost}")
    bulkLabel.setText(s"Trade Amount: $bulkAmount")

    val upgradeCost = StockMarket.getBulkUpgradeCost(bulkAmount)
    increaseBulkButton.setText(s"UPGRADE ($upgradeCost)")
    increaseBulkButton.setDisabled(money < upgradeCost)

    state.companies.zip(companyRows).foreach { (company, row) =>
      val oldPrice = lastPrices.getOrElse(company.name, company.price)
      val priceDiff = company.price - oldPrice

      row.priceLabel.setText(s"${company.price}")

      // Update change indicator
      if priceDiff > 0 then
        row.changeIndicator.setText("+")
        row.changeIndicator.setColor(new Color(0.3f, 1f, 0.5f, 1f)) // Bright green
      else if priceDiff < 0 then
        row.changeIndicator.setText("-")
        row.changeIndicator.setColor(new Color(1f, 0.4f, 0.4f, 1f)) // Red
      else
        row.changeIndicator.setText("")

      val holdings = state.holdings.get(faction).flatMap(_.get(company.name)).getOrElse(0)
      row.holdingsLabel.setText(s"$holdings")
      row.holdingsLabel.setColor(if holdings > 0 then new Color(0.5f, 1f, 0.7f, 1f) else new Color(0.5f, 0.5f, 0.6f, 1f))

      row.buyButton.setDisabled(money < company.price.toLong)
      row.sellButton.setDisabled(holdings == 0)

      // Color price based on relative value
      val priceRatio = company.price / 2000f // Assuming 2000 is mid-range
      if priceRatio > 1.2f then
        row.priceLabel.setColor(new Color(1f, 0.5f, 0.5f, 1f)) // Red for expensive
      else if priceRatio < 0.8f then
        row.priceLabel.setColor(new Color(0.5f, 1f, 0.7f, 1f)) // Green for cheap
      else
        row.priceLabel.setColor(new Color(1f, 1f, 0.7f, 1f)) // Yellow for normal
    }

    // Update tracked prices
    lastPrices = state.companies.map(c => c.name -> c.price).toMap
