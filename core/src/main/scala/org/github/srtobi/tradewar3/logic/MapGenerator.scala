package org.github.srtobi.tradewar3.logic

import org.github.srtobi.tradewar3.model.*
import org.github.srtobi.tradewar3.GameConfig.*
import scala.util.Random

object MapGenerator:
  def generateMap(radius: Int, factions: Seq[Faction]): Seq[Country] =
    val allCoords = for
      q <- -radius to radius
      r <- math.max(-radius, -q - radius) to math.min(radius, -q + radius)
    yield HexCoordinate(q, r)
    
    val borderCoords = allCoords.filter { c =>
      math.max(math.abs(c.q), math.max(math.abs(c.r), math.abs(c.q + c.r))) == radius
    }
    
    def getAngle(c: HexCoordinate): Double =
      val x = math.sqrt(3) * c.q + math.sqrt(3) / 2.0 * c.r
      val y = 3.0 / 2.0 * c.r
      math.atan2(y, x)
      
    val sortedBorder = borderCoords.sortBy(getAngle)
    val shuffledFactions = Random.shuffle(factions)
    
    val offset = if sortedBorder.nonEmpty then Random.nextInt(sortedBorder.size) else 0
    val factionStarts = if factions.nonEmpty && sortedBorder.nonEmpty then
        val step = sortedBorder.size.toDouble / factions.size
        shuffledFactions.zipWithIndex.map { (faction, i) =>
          val idx = (offset + (i * step).toInt) % sortedBorder.size
          sortedBorder(idx) -> faction
        }.toMap
    else Map.empty[HexCoordinate, Faction]

    allCoords.map { coords =>
      val nextBattle = BATTLE_INTERVAL_MIN + Random.nextFloat() * (BATTLE_INTERVAL_MAX - BATTLE_INTERVAL_MIN)
      factionStarts.get(coords) match
        case Some(faction) =>
          Country(coords, units = Map(faction -> INITIAL_UNITS), nextBattleUpdate = nextBattle)
        case None =>
          Country(coords, units = Map(Faction.Neutral -> (Random.nextInt(NEUTRAL_UNITS_MAX - NEUTRAL_UNITS_MIN + 1) + NEUTRAL_UNITS_MIN)), nextBattleUpdate = nextBattle)
    }
