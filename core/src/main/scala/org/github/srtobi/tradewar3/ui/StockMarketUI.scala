package org.github.srtobi.tradewar3.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.{Label, Skin, Table, TextButton}
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import org.github.srtobi.tradewar3.model.{Faction, GameState}
import org.github.srtobi.tradewar3.logic.StockMarket

import scala.compiletime.uninitialized

class StockMarketUI(skin: Skin,
                    onBuy: String => Unit,
                    onSell: String => Unit,
                    onUpgradeBulk: () => Unit) extends Table(skin):
  
  private var balanceLabel: Label = uninitialized
  private var companyRows: Seq[CompanyRow] = uninitialized
  private var bulkLabel: Label = uninitialized
  private var increaseBulkButton: TextButton = uninitialized

  private case class CompanyRow(
    nameLabel: Label,
    priceLabel: Label,
    holdingsLabel: Label,
    buyButton: TextButton,
    sellButton: TextButton
  )

  setBackground(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 1f)))

  def init(state: GameState): Unit =
    clearChildren()
    balanceLabel = new Label(s"Balance: ${state.money} €", skin, "big")
    add(balanceLabel).pad(10).colspan(5).left()
    row()

    companyRows = state.companies.map { company =>
      val nameLabel = new Label(company.name, skin)
      val priceLabel = new Label(s"${company.price} €", skin)
      val holdings = state.holdings(Faction.Player).getOrElse(company.name, 0)
      val holdingsLabel = new Label(s"Owned: $holdings", skin)
      val buyButton = new TextButton("Buy", skin)
      val sellButton = new TextButton("Sell", skin)

      add(nameLabel).pad(5).left().expandX()
      add(priceLabel).pad(5).width(90)
      add(holdingsLabel).pad(5).width(120)
      add(buyButton).pad(5).width(90).height(60)
      add(sellButton).pad(5).width(90).height(60)
      row()

      buyButton.addListener(new ChangeListener {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          onBuy(company.name)
      })

      sellButton.addListener(new ChangeListener {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          onSell(company.name)
      })

      CompanyRow(nameLabel, priceLabel, holdingsLabel, buyButton, sellButton)
    }

    bulkLabel = new Label(s"Bulk Amount: ${state.bulkAmount}", skin)
    increaseBulkButton = new TextButton(s"Upgrade Bulk (${StockMarket.getBulkUpgradeCost(state.bulkAmount)} €)", skin)
    increaseBulkButton.addListener(new ChangeListener {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        onUpgradeBulk()
    })

    add(bulkLabel).pad(5).colspan(3).left()
    add(increaseBulkButton).pad(5).colspan(2).right().width(300).height(60)

  def update(state: GameState): Unit =
    if balanceLabel == null then return
    
    balanceLabel.setText(s"Balance: ${state.money} €")
    bulkLabel.setText(s"Bulk Amount: ${state.bulkAmount}")

    val upgradeCost = StockMarket.getBulkUpgradeCost(state.bulkAmount)
    increaseBulkButton.setText(s"Upgrade Bulk ($upgradeCost €)")
    increaseBulkButton.setDisabled(state.money < upgradeCost)

    state.companies.zip(companyRows).foreach { (company, row) =>
      row.priceLabel.setText(s"${company.price} €")
      val holdings = state.holdings(Faction.Player).getOrElse(company.name, 0)
      row.holdingsLabel.setText(s"Owned: $holdings")

      row.buyButton.setDisabled(state.money < company.price.toLong)
      row.sellButton.setDisabled(holdings == 0)
    }
