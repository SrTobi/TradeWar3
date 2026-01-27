package org.github.srtobi.tradewar3.logic

import org.github.srtobi.tradewar3.model.*
import org.github.srtobi.tradewar3.GameConfig.*

object WarMap:
  /** Calculate total non-neutral units on the map */
  def totalNonNeutralUnits(gameState: GameState): Int =
    gameState.countries.flatMap(_.units.toSeq)
      .filter(!_._1.isNeutral)
      .map(_._2)
      .sum

  /** Calculate unit cost based on total non-neutral units */
  def calculateUnitCost(gameState: GameState): Int =
    UNIT_COST + (totalNonNeutralUnits(gameState) * UNIT_COST_INCREASE)

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
    val newUnits = country.units + (faction -> (country.units.getOrElse(faction, 0) + amount))
    // Reset battle timer when placing units to give them a grace period
    val newCountry = country.copy(units = newUnits, nextBattleUpdate = BATTLE_INTERVAL_MAX)

    val newState = gameState.copy(
      countries = gameState.countries.map(c => if c.coords == coords then newCountry else c)
    )
    // Recalculate unit cost based on new total
    newState.copy(unitCost = calculateUnitCost(newState))

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
    val newState = gameState.copy(countries = newCountries)
    // Recalculate unit cost based on surviving units (goes down when units die)
    (newState.copy(unitCost = calculateUnitCost(newState)), structuralChange)

  private def processBattle(gameState: GameState, country: Country): Country =
    val factionsPresent = country.units.filter(_._2 > 0)
    if factionsPresent.size <= 1 then return country

    val totalUnits = factionsPresent.values.sum
    val newUnits = factionsPresent.map { (faction, count) =>
      val otherUnits = totalUnits - count

      // Count neighboring hexes controlled by this faction for territorial advantage
      val controlledNeighbors = country.coords.neighbors.count { neighborCoords =>
        gameState.countries.find(_.coords == neighborCoords).exists(_.owner == faction)
      }

      // Each controlled neighbor reduces losses by 2% (up to 12% for all 6 neighbors)
      val territorialBonus = controlledNeighbors * TERRITORIAL_ADVANTAGE_PER_NEIGHBOR

      // Each enemy unit has a chance to kill one of our units.
      // We use a base efficiency and add randomness.
      // Expected losses: otherUnits * efficiency * (1 - territorial bonus)
      val randomness = BATTLE_RANDOMNESS_MIN + scala.util.Random.nextFloat() * (BATTLE_RANDOMNESS_MAX - BATTLE_RANDOMNESS_MIN)
      val effectiveRandomness = randomness * (1f - territorialBonus)

      val losses = Math.round(otherUnits * effectiveRandomness).toInt
      val newCount = Math.max(0, count - losses)
      faction -> newCount
    }.filter(_._2 > 0)

    country.copy(units = newUnits)
