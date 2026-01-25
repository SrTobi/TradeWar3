package org.github.srtobi.tradewar3.logic

import org.github.srtobi.tradewar3.model.*
import scala.util.Random

object MapGenerator:
  def generateMap(radius: Int, factions: Seq[Faction]): Seq[Country] =
    val countries = for
      q <- -radius to radius
      r <- Math.max(-radius, -q - radius) to Math.min(radius, -q + radius)
    yield Country(HexCoordinate(q, r))
    
    // Assign starting factions
    val availableCountries = Random.shuffle(countries)
    
    val assignedCountries = availableCountries.zipWithIndex.map { (country, index) =>
      val nextBattle = Random.nextFloat() * 5f + 1f
      if index < factions.size then
        country.copy(units = Map(factions(index) -> 10), nextBattleUpdate = nextBattle)
      else
        country.copy(units = Map(Faction.Neutral -> (Random.nextInt(16) + 5)), nextBattleUpdate = nextBattle)
    }
    
    assignedCountries
