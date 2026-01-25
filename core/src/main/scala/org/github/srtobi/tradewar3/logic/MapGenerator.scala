package org.github.srtobi.tradewar3.logic

import org.github.srtobi.tradewar3.model.*
import scala.util.Random

object MapGenerator:
  def generateMap(radius: Int): Seq[Country] =
    val countries = for
      q <- -radius to radius
      r <- Math.max(-radius, -q - radius) to Math.min(radius, -q + radius)
    yield Country(HexCoordinate(q, r))
    
    // Assign starting factions
    val factions = Seq(Faction.Player, Faction.Enemy1, Faction.Enemy2, Faction.Enemy3)
    val availableCountries = Random.shuffle(countries)
    
    val assignedCountries = availableCountries.zipWithIndex.map { (country, index) =>
      if index < factions.size then
        country.copy(units = Map(factions(index) -> 10))
      else
        country.copy(units = Map(Faction.Neutral -> (Random.nextInt(16) + 5)))
    }
    
    assignedCountries
