package org.github.srtobi.tradewar3.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.graphics.g2d.{Batch, GlyphLayout}
import org.github.srtobi.tradewar3.model.{GameState, HexCoordinate}
import org.github.srtobi.tradewar3.distanceSquared

import scala.compiletime.uninitialized

class WarMapUI(skin: Skin, onHexClick: HexCoordinate => Unit) extends Actor:
  private val shapeRenderer = new ShapeRenderer()
  private var gameState: GameState = uninitialized
  private val hexSize = 80f
  private val layout = new GlyphLayout()

  locally {
    import com.badlogic.gdx.scenes.scene2d.InputListener
    import com.badlogic.gdx.scenes.scene2d.InputEvent
    addListener(new InputListener {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean =
        val centerX = getWidth / 2
        val centerY = getHeight / 2
        val coords = pixelToHex(x - centerX, y - centerY)
        onHexClick(coords)
        true
    })
  }

  def update(state: GameState): Unit =
    this.gameState = state

  override def draw(batch: Batch, parentAlpha: Float): Unit =
    if gameState == null then return

    batch.end()

    shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix)
    shapeRenderer.setTransformMatrix(batch.getTransformMatrix)

    val centerX = getX + getWidth / 2
    val centerY = getY + getHeight / 2

    // Draw filled hexagons
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    gameState.countries.foreach { country =>
      val (px, py) = hexToPixel(country.coords)
      drawHexagon(centerX + px, centerY + py, hexSize, country.owner.color)
    }
    shapeRenderer.end()

    // Draw hex borders
    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
    shapeRenderer.setColor(Color.BLACK)
    gameState.countries.foreach { country =>
      val (px, py) = hexToPixel(country.coords)
      drawHexagonOutline(centerX + px, centerY + py, hexSize)
    }
    shapeRenderer.end()

    val font = skin.getFont("default")
    val textPositions = gameState.countries.flatMap { country =>
      val (px, py) = hexToPixel(country.coords)

      // Filter out factions with 0 units
      val factionUnits = country.units.filter(_._2 > 0).toSeq.sortBy(_._1.ordinal)

      val totalHeight = factionUnits.size * font.getLineHeight
      var currentY = centerY + py + (totalHeight / 2f)

      factionUnits.map { (faction, count) =>
        val factionColor = faction.color
        val fontColor =
          if (factionColor.distanceSquared(Color.BLACK) < factionColor.distanceSquared(Color.WHITE)) {
            Color.WHITE
          } else {
            Color.BLACK
          }
        val text = count.toString
        layout.setText(font, text)
        val tx = centerX + px - layout.width / 2
        val ty = currentY

        currentY -= font.getLineHeight
        (text, fontColor, factionColor, tx, ty)
      }
    }

    // Draw hexes around unit counts
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    textPositions.foreach { case (_, _, factionColor, tx, ty) =>
      shapeRenderer.setColor(factionColor)
      shapeRenderer.rect(tx - 15, ty - 28, 50, 32)
    }

    shapeRenderer.end()

    // Draw unit counts
    batch.begin()
    textPositions.foreach {
      case (text, fontColor, _, tx, ty) =>
        font.setColor(fontColor)
        font.draw(batch, text, tx, ty)
    }



    // reset font colors
    font.setColor(Color.WHITE)

  private def hexToPixel(coords: HexCoordinate): (Float, Float) =
    // Pointy topped
    val x = hexSize * (math.sqrt(3).toFloat * coords.q + math.sqrt(3).toFloat / 2f * coords.r)
    val y = hexSize * (3f / 2f * coords.r)
    (x, y)

  private def pixelToHex(x: Float, y: Float): HexCoordinate =
    val q = (math.sqrt(3).toFloat / 3f * x - 1f / 3f * y) / hexSize
    val r = (2f / 3f * y) / hexSize
    roundHex(q, r)

  private def roundHex(q: Float, r: Float): HexCoordinate =
    var rq = math.round(q)
    var rr = math.round(r)
    val rs = math.round(-q - r)

    val qDiff = math.abs(rq - q)
    val rDiff = math.abs(rr - r)
    val sDiff = math.abs(rs - (-q - r))

    if qDiff > rDiff && qDiff > sDiff then
      rq = -rr - rs
    else if rDiff > sDiff then
      rr = -rq - rs

    HexCoordinate(rq, rr)

  private def drawHexagon(x: Float, y: Float, size: Float, color: Color): Unit =
    shapeRenderer.setColor(color)
    for i <- 0 until 6 do
      val angleRad1 = math.toRadians(60 * i + 30).toFloat
      val angleRad2 = math.toRadians(60 * (i + 1) + 30).toFloat
      shapeRenderer.triangle(
        x, y,
        x + size * math.cos(angleRad1).toFloat, y + size * math.sin(angleRad1).toFloat,
        x + size * math.cos(angleRad2).toFloat, y + size * math.sin(angleRad2).toFloat
      )

  private def drawHexagonOutline(x: Float, y: Float, size: Float): Unit =
    for i <- 0 until 6 do
      val angleRad1 = math.toRadians(60 * i + 30).toFloat
      val angleRad2 = math.toRadians(60 * (i + 1) + 30).toFloat
      shapeRenderer.line(
        x + size * math.cos(angleRad1).toFloat, y + size * math.sin(angleRad1).toFloat,
        x + size * math.cos(angleRad2).toFloat, y + size * math.sin(angleRad2).toFloat
      )
