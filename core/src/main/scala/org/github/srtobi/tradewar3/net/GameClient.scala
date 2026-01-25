package org.github.srtobi.tradewar3.net

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net
import com.badlogic.gdx.net.SocketHints
import org.github.srtobi.tradewar3.model.*
import java.io.*
import scala.util.control.NonFatal

class GameClient(host: String, port: Int) {
  private val socket = Gdx.net.newClientSocket(Net.Protocol.TCP, host, port, new SocketHints())
  private val out = new ObjectOutputStream(socket.getOutputStream)
  private val in = new ObjectInputStream(socket.getInputStream)
  private var running = true
  
  private var latestState: Option[GameState] = None
  private var assignedFaction: Option[Faction] = None
  private var lobbyPlayers: Seq[(String, Faction)] = Seq.empty
  private var gameStarted: Boolean = false

  private val thread = new Thread(() => {
    try {
      while (running) {
        val obj = in.readObject()
        obj match {
          case JoinResponse(faction) =>
            assignedFaction = Some(faction)
          case LobbyUpdate(players) =>
            lobbyPlayers = players
          case StartGame =>
            gameStarted = true
          case GameStateUpdate(state) =>
            latestState = Some(state)
          case _ =>
        }
      }
    } catch {
      case _: EOFException => Gdx.app.log("Client", "Disconnected from server")
      case NonFatal(e) if running => Gdx.app.error("Client", "Error in client thread", e)
      case _: Throwable =>
    }
  }, "GameClient-Receiver")
  thread.setDaemon(true)
  thread.start()

  def send(message: NetworkMessage): Unit = {
    try {
      out.synchronized {
        out.writeObject(message)
        out.flush()
        out.reset()
      }
    } catch {
      case NonFatal(e) => Gdx.app.error("Client", "Error sending message", e)
    }
  }

  def pollState(): Option[GameState] = {
    val s = latestState
    latestState = None
    s
  }
  
  def getAssignedFaction: Option[Faction] = assignedFaction
  
  def getLobbyPlayers: Seq[(String, Faction)] = lobbyPlayers
  
  def isGameStarted: Boolean = gameStarted

  def stop(): Unit = {
    running = false
    try { socket.dispose() } catch { case _: Throwable => }
  }
}
