package org.github.srtobi.tradewar3.net

import com.badlogic.gdx.{Gdx, Net}
import com.badlogic.gdx.net.{ServerSocketHints, Socket}
import org.github.srtobi.tradewar3.model.*

import java.io.*
import java.util.concurrent.CopyOnWriteArrayList
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

class GameServer(port: Int) {
  private val serverSocket = {
    val hints = new ServerSocketHints()
    hints.acceptTimeout = 0
    Gdx.net.newServerSocket(Net.Protocol.TCP, "::", port, hints)
  }
  private val clients = new CopyOnWriteArrayList[ClientHandler]()
  private var running = true

  private val thread = new Thread(() => {
    while (running) {
      try {
        val socket = serverSocket.accept(null)
        val handler = new ClientHandler(socket)
        clients.add(handler)
        val t = new Thread(handler)
        t.setDaemon(true)
        t.start()
      } catch {
        case NonFatal(e) if running => Gdx.app.error("Server", "Error accepting connection", e)
        case _: Throwable => // Stopped
      }
    }
  }, "GameServer-Acceptor")
  thread.setDaemon(true)
  thread.start()

  def broadcast(message: NetworkMessage): Unit = {
    clients.forEach(_.send(message))
  }

  def getPlayers: Seq[Faction] = {
    clients.asScala.flatMap(_.faction).toSeq
  }

  def stop(): Unit = {
    running = false
    serverSocket.dispose()
    clients.forEach(_.stop())
  }

  def getActions: Seq[PlayerAction] = {
    clients.asScala.flatMap(_.pollActions()).toSeq
  }

  private class ClientHandler(socket: Socket) extends Runnable {
    private val out = new ObjectOutputStream(socket.getOutputStream)
    private val in = new ObjectInputStream(socket.getInputStream)
    private val actionQueue = new java.util.concurrent.ConcurrentLinkedQueue[PlayerAction]()
    private var handlerRunning = true
    var faction: Option[Faction] = None

    private val sendQueue = new java.util.concurrent.LinkedBlockingQueue[NetworkMessage]()
    private val senderThread = new Thread(() => {
      while (handlerRunning) {
        try {
          val message = sendQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS)
          if (message != null) {
            out.synchronized {
              out.writeObject(message)
              out.flush()
              out.reset()
            }
          }
        } catch {
          case _: InterruptedException => // Normal during shutdown
          case NonFatal(e) if handlerRunning =>
            Gdx.app.error("Server", "Error sending message", e)
            stop()
          case _: Throwable =>
        }
      }
    }, "ClientHandler-Sender")
    senderThread.setDaemon(true)
    senderThread.start()

    def send(message: NetworkMessage): Unit = {
      if (handlerRunning) {
        sendQueue.offer(message)
      }
    }

    def pollActions(): Seq[PlayerAction] = {
      val actions = List.newBuilder[PlayerAction]
      var a = actionQueue.poll()
      while (a != null) {
        actions += a
        a = actionQueue.poll()
      }
      actions.result()
    }

    override def run(): Unit = {
      try {
        while (handlerRunning) {
          val obj = in.readObject()
          obj match {
            case JoinRequest(name)  =>
              if (getPlayers.forall(p => p.name != name) && name != "Neutral") {
                val f = Faction(name)
                faction = Some(f)
                send(JoinResponse(f))
                broadcast(LobbyUpdate(getPlayers))
                Gdx.app.log("Server", s"Player $name joined as $f")
              }
            case action: PlayerAction =>
              actionQueue.add(action)
            case _ => Gdx.app.log("Server", s"Received unknown message: $obj")
          }
        }
      } catch {
        case _: EOFException => Gdx.app.log("Server", "Client disconnected")
        case NonFatal(e) if handlerRunning => Gdx.app.error("Server", "Error in ClientHandler", e)
        case _: Throwable =>
      } finally {
        stop()
      }
    }

    def stop(): Unit = {
      handlerRunning = false
      clients.remove(this)
      senderThread.interrupt()
      try { socket.dispose() } catch { case _: Throwable => }
    }
  }
}
