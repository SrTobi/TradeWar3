package org.github.srtobi.tradewar3.model

import com.badlogic.gdx.graphics.Color
import java.io.Serializable

case class Faction(name: String) extends Serializable:
  def isNeutral: Boolean = name == "Neutral"

object Faction:
  val Neutral = Faction("Neutral")
  val Player1 = Faction("Player 1")
  val Player2 = Faction("Player 2")
  val Player3 = Faction("Player 3")
  val Player4 = Faction("Player 4")
  
  val AllPlayers = Seq(Player1, Player2, Player3, Player4)

class FactionColors(val localFaction: Faction):
  private val otherFactions = Faction.AllPlayers.filter(_ != localFaction)
  private val otherColors = Seq(Color.RED, Color.YELLOW, Color.ORANGE)
  private val colorMap = otherFactions.zip(otherColors).toMap

  def apply(faction: Faction): Color =
    if faction == localFaction then Color.BLUE
    else if faction.isNeutral then Color.GRAY
    else colorMap.getOrElse(faction, Color.WHITE)

case class Company(
    name: String,
    price: Int,
    nextUpdate: Float = 0f
) extends Serializable

case class HexCoordinate(q: Int, r: Int) extends Serializable:
  def s: Int = -q - r
  def +(other: HexCoordinate): HexCoordinate = HexCoordinate(q + other.q, r + other.r)
  def neighbors: Seq[HexCoordinate] = HexCoordinate.directions.map(this + _)

object HexCoordinate:
  val directions: Seq[HexCoordinate] = Seq(
    HexCoordinate(1, 0), HexCoordinate(1, -1), HexCoordinate(0, -1),
    HexCoordinate(-1, 0), HexCoordinate(-1, 1), HexCoordinate(0, 1)
  )

case class Country(
    coords: HexCoordinate,
    units: Map[Faction, Int] = Map.empty,
    nextBattleUpdate: Float = 0f
) extends Serializable:
  def owner: Faction = units.maxByOption(_._2).map(_._1).getOrElse(Faction.Neutral)
  def unitCount: Int = units.values.sum

case class GameState(
    money: Map[Faction, Long],
    companies: Seq[Company],
    holdings: Map[Faction, Map[String, Int]],
    countries: Seq[Country],
    bulkAmount: Map[Faction, Int]
) extends Serializable
