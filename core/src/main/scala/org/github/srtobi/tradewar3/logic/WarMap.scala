package org.github.srtobi.tradewar3.logic

import org.github.srtobi.tradewar3.model.*

object WarMap:
  val UnitCost = 100

  def canPlaceUnits(gameState: GameState, coords: HexCoordinate): Boolean =
    gameState.countries.find(_.coords == coords) match
      case Some(country) =>
        val isOwned = country.owner == Faction.Player
        val isAdjacent = coords.neighbors.exists(nb => 
          gameState.countries.find(_.coords == nb).exists(_.owner == Faction.Player)
        )
        isOwned || isAdjacent
      case None => false

  def placeUnits(gameState: GameState, coords: HexCoordinate, amount: Int): GameState =
    if !canPlaceUnits(gameState, coords) then return gameState
    
    val country = gameState.countries.find(_.coords == coords).get
    val maxAffordable = (gameState.money / UnitCost).toInt
    val amountToPlace = Math.min(amount, maxAffordable)
    
    if amountToPlace > 0 then
      val cost = amountToPlace.toLong * UnitCost
      val newUnits = country.units + (Faction.Player -> (country.units.getOrElse(Faction.Player, 0) + amountToPlace))
      val newCountry = country.copy(units = newUnits)
      gameState.copy(
        money = gameState.money - cost,
        countries = gameState.countries.map(c => if c.coords == coords then newCountry else c)
      )
    else
      gameState
