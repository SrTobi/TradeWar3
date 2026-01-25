package org.github.srtobi.tradewar3.net

import org.github.srtobi.tradewar3.model.*
import java.io.Serializable

sealed trait NetworkMessage extends Serializable

case class JoinRequest(playerName: String) extends NetworkMessage
case class JoinResponse(faction: Faction) extends NetworkMessage
case class LobbyUpdate(players: Seq[Faction]) extends NetworkMessage
case object StartGame extends NetworkMessage
case class GameStateUpdate(state: GameState) extends NetworkMessage

sealed trait PlayerAction extends NetworkMessage:
  def faction: Faction

case class BuyAction(faction: Faction, companyName: String) extends PlayerAction
case class SellAction(faction: Faction, companyName: String) extends PlayerAction
case class PlaceUnitsAction(faction: Faction, coords: HexCoordinate) extends PlayerAction
case class UpgradeBulkAction(faction: Faction) extends PlayerAction
