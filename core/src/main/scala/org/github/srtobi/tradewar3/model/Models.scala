package org.github.srtobi.tradewar3.model

import com.badlogic.gdx.graphics.Color

enum Faction(val color: Color, isEnemy: Boolean):
  case Player extends Faction(Color.BLUE, false)
  case Enemy1 extends Faction(Color.RED, true)
  case Enemy2 extends Faction(Color.YELLOW, true)
  case Enemy3 extends Faction(Color.ORANGE, true)
  case Neutral extends Faction(Color.GRAY, false)

  def isPlayer: Boolean = this == Player
  def isNeutral: Boolean = this == Neutral

case class Company(
    name: String,
    price: Int,
    nextUpdate: Float = 0f
)

case class HexCoordinate(q: Int, r: Int):
  def s: Int = -q - r
  def +(other: HexCoordinate): HexCoordinate = HexCoordinate(q + other.q, r + other.r)
  def neighbors: Seq[HexCoordinate] = HexCoordinate.directions.map(this + _)

object HexCoordinate:
  val directions = Seq(
    HexCoordinate(1, 0), HexCoordinate(1, -1), HexCoordinate(0, -1),
    HexCoordinate(-1, 0), HexCoordinate(-1, 1), HexCoordinate(0, 1)
  )

case class Country(
    coords: HexCoordinate,
    units: Map[Faction, Int] = Map.empty
):
  def owner: Faction = units.maxByOption(_._2).map(_._1).getOrElse(Faction.Neutral)
  def unitCount: Int = units.values.sum

case class GameState(
    money: Long,
    companies: Seq[Company],
    holdings: Map[Faction, Map[String, Int]],
    countries: Seq[Country],
    bulkAmount: Int = 1
)
