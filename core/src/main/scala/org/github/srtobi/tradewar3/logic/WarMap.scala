package org.github.srtobi.tradewar3.logic

import org.github.srtobi.tradewar3.model.*
import org.github.srtobi.tradewar3.GameConfig.*

object WarMap:
  def canPlaceUnits(gameState: GameState, coords: HexCoordinate, faction: Faction): Boolean =
    gameState.countries.find(_.coords == coords) match
      case Some(country) =>
        val isPresent = country.units.getOrElse(faction, 0) > 0
        val canSupportFromNeighbor = coords.neighbors.exists { nbCoords =>
          gameState.countries.find(_.coords == nbCoords).exists { neighbor =>
            val myUnits = neighbor.units.getOrElse(faction, 0)
            val otherUnits = neighbor.units.filter(_._1 != faction).values.sum
            myUnits > WAR_EXPANSION_THRESHOLD * otherUnits
          }
        }
        
        isPresent || canSupportFromNeighbor
      case None => false

  def placeUnits(gameState: GameState, coords: HexCoordinate, faction: Faction, amount: Int): GameState =
    if !canPlaceUnits(gameState, coords, faction) then return gameState
    
    val country = gameState.countries.find(_.coords == coords).get
    val playerMoney = gameState.money.getOrElse(faction, 0L)
    
    var totalCost = 0L
    var count = 0
    var currentPrice = gameState.unitCost
    
    while count < amount && totalCost + currentPrice <= playerMoney do
      totalCost += currentPrice
      currentPrice += UNIT_COST_INCREASE
      count += 1
    
    if count > 0 then
      val newUnits = country.units + (faction -> (country.units.getOrElse(faction, 0) + count))
      
      val newCountry = country.copy(units = newUnits)
      gameState.copy(
        money = gameState.money + (faction -> (playerMoney - totalCost)),
        countries = gameState.countries.map(c => if c.coords == coords then newCountry else c),
        unitCost = currentPrice
      )
    else
      gameState

  def updateBattles(gameState: GameState, delta: Float): (GameState, Boolean) =
    var structuralChange = false
    val newCountries = gameState.countries.map { country =>
      val newNextUpdate = country.nextBattleUpdate - delta
      if newNextUpdate <= 0 then
        val nextInterval = BATTLE_INTERVAL_MIN + scala.util.Random.nextFloat() * (BATTLE_INTERVAL_MAX - BATTLE_INTERVAL_MIN)
        val processedCountry = processBattle(gameState, country)
        if processedCountry.units != country.units then
          structuralChange = true
        processedCountry.copy(nextBattleUpdate = nextInterval)
      else
        country.copy(nextBattleUpdate = newNextUpdate)
    }
    (gameState.copy(countries = newCountries), structuralChange)

  private def processBattle(gameState: GameState, country: Country): Country =
    val factionsPresent = country.units.filter(_._2 > 0)
    if factionsPresent.size <= 1 then return country

    val totalUnits = factionsPresent.values.sum
    val newUnits = factionsPresent.map { (faction, count) =>
      val otherUnits = totalUnits - count
      
      // Each enemy unit has a chance to kill one of our units.
      // We use a base efficiency and add randomness.
      // Expected losses: otherUnits * efficiency
      val randomness = BATTLE_RANDOMNESS_MIN + scala.util.Random.nextFloat() * (BATTLE_RANDOMNESS_MAX - BATTLE_RANDOMNESS_MIN)
      
      val losses = Math.round(otherUnits * randomness).toInt
      val newCount = Math.max(0, count - losses)
      faction -> newCount
    }.filter(_._2 > 0)

    country.copy(units = newUnits)
