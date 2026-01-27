package org.github.srtobi.tradewar3.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.{Color, GL20}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.graphics.g2d.{Batch, GlyphLayout}
import org.github.srtobi.tradewar3.model.{Faction, FactionColors, GameState, HexCoordinate}
import org.github.srtobi.tradewar3.distanceSquared
import org.github.srtobi.tradewar3.GameConfig.*

import scala.collection.mutable.ArrayBuffer
import scala.compiletime.uninitialized
import scala.util.Random

class WarMapUI(skin: Skin, onHexClick: HexCoordinate => Unit, factionColors: FactionColors, localFaction: Faction) extends Actor:
  private val shapeRenderer = new ShapeRenderer()
  private var gameState: GameState = uninitialized
  private var prevGameState: GameState = uninitialized
  private val layout = new GlyphLayout()

  // Animation state
  private var pulseTime: Float = 0f
  private var hoveredHex: Option[HexCoordinate] = None

  // Particle system for battles and effects
  private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color,
    val size: Float
  )
  private val particles = ArrayBuffer[Particle]()

  // Explosion effect tracker
  private class Explosion(
    val x: Float,
    val y: Float,
    var time: Float,
    val maxTime: Float,
    val color: Color
  )
  private val explosions = ArrayBuffer[Explosion]()

  locally {
    import com.badlogic.gdx.scenes.scene2d.InputListener
    import com.badlogic.gdx.scenes.scene2d.InputEvent
    addListener(new InputListener {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean =
        val centerX = getWidth / 2
        val centerY = getHeight / 2
        val coords = pixelToHex(x - centerX, y - centerY)
        onHexClick(coords)

        // Add click ripple effect
        val (px, py) = hexToPixel(coords)
        addRippleEffect(getX + centerX + px, getY + centerY + py, Color.WHITE)
        true

      override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean =
        val centerX = getWidth / 2
        val centerY = getHeight / 2
        hoveredHex = Some(pixelToHex(x - centerX, y - centerY))
        true

      override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor): Unit =
        if pointer == -1 then hoveredHex = None
    })
  }

  private def addRippleEffect(x: Float, y: Float, baseColor: Color): Unit =
    explosions += new Explosion(x, y, 0, 0.4f, baseColor)

  private def addBattleParticles(x: Float, y: Float, color: Color): Unit =
    for _ <- 0 until 15 do
      val angle = Random.nextFloat() * math.Pi.toFloat * 2
      val speed = Random.nextFloat() * 100 + 50
      particles += new Particle(
        x, y,
        math.cos(angle).toFloat * speed,
        math.sin(angle).toFloat * speed,
        0, Random.nextFloat() * 0.5f + 0.3f,
        color,
        Random.nextFloat() * 3 + 1
      )

  def update(state: GameState): Unit =
    // Detect battles (unit count decreases)
    if prevGameState != null && gameState != null then
      val centerX = getX + getWidth / 2
      val centerY = getY + getHeight / 2
      state.countries.foreach { country =>
        val prevCountry = prevGameState.countries.find(_.coords == country.coords)
        prevCountry.foreach { prev =>
          // Check if any faction lost units (battle happened)
          country.units.foreach { (faction, count) =>
            val prevCount = prev.units.getOrElse(faction, 0)
            if prevCount > count && count > 0 then
              val (px, py) = hexToPixel(country.coords)
              addBattleParticles(centerX + px, centerY + py, factionColors(faction))
          }
        }
      }
    prevGameState = gameState
    this.gameState = state

  override def draw(batch: Batch, parentAlpha: Float): Unit =
    if gameState == null then return

    val delta = Gdx.graphics.getDeltaTime
    pulseTime += delta

    batch.end()

    Gdx.gl.glEnable(GL20.GL_BLEND)
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

    shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix)
    shapeRenderer.setTransformMatrix(batch.getTransformMatrix)

    val centerX = getX + getWidth / 2
    val centerY = getY + getHeight / 2

    // Draw connection bridges between territories of the same faction
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    val drawnConnections = scala.collection.mutable.Set[(HexCoordinate, HexCoordinate)]()
    gameState.countries.foreach { country =>
      if !country.owner.isNeutral then
        val (px1, py1) = hexToPixel(country.coords)
        country.coords.neighbors.foreach { neighborCoords =>
          // Avoid drawing the same connection twice
          val connectionKey = if country.coords.hashCode < neighborCoords.hashCode
            then (country.coords, neighborCoords)
            else (neighborCoords, country.coords)

          if !drawnConnections.contains(connectionKey) then
            gameState.countries.find(_.coords == neighborCoords).foreach { neighbor =>
              if neighbor.owner == country.owner then
                drawnConnections += connectionKey
                val (px2, py2) = hexToPixel(neighborCoords)
                val color = factionColors(country.owner)

                // Draw glowing bridge
                val bridgePulse = (math.sin(pulseTime * 1.5f).toFloat + 1f) / 2f * 0.3f + 0.4f
                shapeRenderer.setColor(color.r, color.g, color.b, bridgePulse * 0.6f)
                shapeRenderer.rectLine(
                  centerX + px1, centerY + py1,
                  centerX + px2, centerY + py2,
                  8f
                )
                // Brighter center line
                shapeRenderer.setColor(color.r * 1.2f min 1f, color.g * 1.2f min 1f, color.b * 1.2f min 1f, bridgePulse)
                shapeRenderer.rectLine(
                  centerX + px1, centerY + py1,
                  centerX + px2, centerY + py2,
                  3f
                )
            }
        }
    }
    shapeRenderer.end()

    // Draw hex glow effects (under hexagons)
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    gameState.countries.foreach { country =>
      val (px, py) = hexToPixel(country.coords)
      val color = factionColors(country.owner)

      // Glow for non-neutral territories
      if !country.owner.isNeutral then
        val glowPulse = (math.sin(pulseTime * 2).toFloat + 1f) / 2f * 0.15f + 0.1f
        drawHexagonGlow(centerX + px, centerY + py, UI_HEX_SIZE + 8, color, glowPulse)
    }
    shapeRenderer.end()

    // Draw filled hexagons with gradient effect
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    gameState.countries.foreach { country =>
      val (px, py) = hexToPixel(country.coords)
      val baseColor = factionColors(country.owner)

      // Check if this hex is hovered
      val isHovered = hoveredHex.exists(_ == country.coords)
      val hoverBoost = if isHovered then 0.2f else 0f

      // Draw gradient hexagon
      drawGradientHexagon(centerX + px, centerY + py, UI_HEX_SIZE, baseColor, hoverBoost)
    }
    shapeRenderer.end()

    // Draw hex borders with style
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    gameState.countries.foreach { country =>
      val (px, py) = hexToPixel(country.coords)
      val baseColor = factionColors(country.owner)
      val isHovered = hoveredHex.exists(_ == country.coords)

      // Thicker, colored borders
      val borderColor = if isHovered then Color.WHITE else darken(baseColor, 0.3f)
      val borderWidth = if isHovered then 3f else 2f
      drawHexagonBorder(centerX + px, centerY + py, UI_HEX_SIZE, borderColor, borderWidth)
    }
    shapeRenderer.end()

    // Draw inner hex highlights
    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
    Gdx.gl.glLineWidth(1f)
    gameState.countries.foreach { country =>
      val (px, py) = hexToPixel(country.coords)
      val baseColor = factionColors(country.owner)
      shapeRenderer.setColor(lighten(baseColor, 0.3f).r, lighten(baseColor, 0.3f).g, lighten(baseColor, 0.3f).b, 0.3f)
      drawHexagonOutline(centerX + px, centerY + py, UI_HEX_SIZE * 0.7f)
    }
    shapeRenderer.end()

    // Update and draw particles
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    particles.filterInPlace { p =>
      p.life += delta
      if p.life < p.maxLife then
        p.x += p.vx * delta
        p.y += p.vy * delta
        p.vy -= 50 * delta // gravity
        p.vx *= 0.98f // drag

        val alpha = 1f - (p.life / p.maxLife)
        shapeRenderer.setColor(p.color.r, p.color.g, p.color.b, alpha)
        shapeRenderer.circle(p.x, p.y, p.size * (1f - p.life / p.maxLife * 0.5f))
        true
      else false
    }
    shapeRenderer.end()

    // Update and draw explosions/ripples
    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
    Gdx.gl.glLineWidth(2f)
    explosions.filterInPlace { e =>
      e.time += delta
      if e.time < e.maxTime then
        val progress = e.time / e.maxTime
        val radius = UI_HEX_SIZE * (0.5f + progress * 1.5f)
        val alpha = (1f - progress) * 0.8f
        shapeRenderer.setColor(e.color.r, e.color.g, e.color.b, alpha)
        shapeRenderer.circle(e.x, e.y, radius, 32)
        true
      else false
    }
    shapeRenderer.end()
    Gdx.gl.glLineWidth(1f)

    val font = skin.getFont("default")
    val textPositions = gameState.countries.flatMap { country =>
      val (px, py) = hexToPixel(country.coords)

      // Filter out factions with 0 units
      val factionUnits = country.units.filter(_._2 > 0).toSeq.sortBy(_._1.name)

      val totalHeight = factionUnits.size * font.getLineHeight
      var currentY = centerY + py + (totalHeight / 2f)

      factionUnits.map { (faction, count) =>
        val factionColor = factionColors(faction)
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

    // Draw rounded rectangles behind unit counts
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    textPositions.foreach { case (_, _, factionColor, tx, ty) =>
      // Draw with slight shadow
      shapeRenderer.setColor(0, 0, 0, 0.3f)
      drawRoundedRect(tx - 12, ty - 26, 44, 28, 6)
      // Main background
      shapeRenderer.setColor(factionColor)
      drawRoundedRect(tx - 14, ty - 28, 44, 28, 6)
    }
    shapeRenderer.end()

    // Draw unit counts
    batch.begin()
    textPositions.foreach {
      case (text, fontColor, _, tx, ty) =>
        font.setColor(fontColor)
        font.draw(batch, text, tx, ty)
    }

    // Draw territorial bonus indicators for local player
    gameState.countries.foreach { country =>
      val localUnits = country.units.getOrElse(localFaction, 0)
      val isContested = country.units.filter(_._2 > 0).size > 1

      // Show bonus on hexes where local player has units
      if localUnits > 0 then
        val controlledNeighbors = country.coords.neighbors.count { neighborCoords =>
          gameState.countries.find(_.coords == neighborCoords).exists(_.owner == localFaction)
        }

        if controlledNeighbors > 0 then
          val bonusPercent = (controlledNeighbors * TERRITORIAL_ADVANTAGE_PER_NEIGHBOR * 100).toInt
          val bonusText = s"+$bonusPercent%"
          val (px, py) = hexToPixel(country.coords)

          layout.setText(font, bonusText)
          val bx = centerX + px - layout.width / 2
          val by = centerY + py - UI_HEX_SIZE * 0.55f

          // Draw with green color for bonus
          if isContested then
            font.setColor(0.3f, 1f, 0.5f, 1f) // Bright green for contested
          else
            font.setColor(0.5f, 0.9f, 0.6f, 0.7f) // Softer green for non-contested
          font.draw(batch, bonusText, bx, by)
    }

    // Reset font colors
    font.setColor(Color.WHITE)

  private def drawRoundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float): Unit =
    // Main rectangle
    shapeRenderer.rect(x + radius, y, width - 2 * radius, height)
    shapeRenderer.rect(x, y + radius, width, height - 2 * radius)
    // Corners
    shapeRenderer.circle(x + radius, y + radius, radius, 8)
    shapeRenderer.circle(x + width - radius, y + radius, radius, 8)
    shapeRenderer.circle(x + radius, y + height - radius, radius, 8)
    shapeRenderer.circle(x + width - radius, y + height - radius, radius, 8)

  private def hexToPixel(coords: HexCoordinate): (Float, Float) =
    // Pointy topped
    val x = UI_HEX_SIZE * (math.sqrt(3).toFloat * coords.q + math.sqrt(3).toFloat / 2f * coords.r)
    val y = UI_HEX_SIZE * (3f / 2f * coords.r)
    (x, y)

  private def pixelToHex(x: Float, y: Float): HexCoordinate =
    val q = (math.sqrt(3).toFloat / 3f * x - 1f / 3f * y) / UI_HEX_SIZE
    val r = (2f / 3f * y) / UI_HEX_SIZE
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

  private def lighten(color: Color, amount: Float): Color =
    new Color(
      math.min(1f, color.r + amount),
      math.min(1f, color.g + amount),
      math.min(1f, color.b + amount),
      color.a
    )

  private def darken(color: Color, amount: Float): Color =
    new Color(
      math.max(0f, color.r - amount),
      math.max(0f, color.g - amount),
      math.max(0f, color.b - amount),
      color.a
    )

  private def drawGradientHexagon(x: Float, y: Float, size: Float, baseColor: Color, hoverBoost: Float): Unit =
    val centerColor = lighten(baseColor, 0.15f + hoverBoost)
    val edgeColor = darken(baseColor, 0.1f)

    // Draw gradient from center to edges using triangles with vertex colors
    // API: triangle(x1, y1, x2, y2, x3, y3, col1, col2, col3)
    for i <- 0 until 6 do
      val angleRad1 = math.toRadians(60 * i + 30).toFloat
      val angleRad2 = math.toRadians(60 * (i + 1) + 30).toFloat

      val x1 = x + size * math.cos(angleRad1).toFloat
      val y1 = y + size * math.sin(angleRad1).toFloat
      val x2 = x + size * math.cos(angleRad2).toFloat
      val y2 = y + size * math.sin(angleRad2).toFloat

      // Coordinates first, then colors
      shapeRenderer.triangle(
        x, y,
        x1, y1,
        x2, y2,
        centerColor, edgeColor, edgeColor
      )

  private def drawHexagonGlow(x: Float, y: Float, size: Float, color: Color, alpha: Float): Unit =
    // Draw multiple rings for soft glow effect
    for i <- 0 until 3 do
      val ringSize = size + i * 4
      val ringAlpha = alpha * (1f - i * 0.3f)
      shapeRenderer.setColor(color.r, color.g, color.b, ringAlpha)

      for j <- 0 until 6 do
        val angleRad1 = math.toRadians(60 * j + 30).toFloat
        val angleRad2 = math.toRadians(60 * (j + 1) + 30).toFloat
        shapeRenderer.triangle(
          x, y,
          x + ringSize * math.cos(angleRad1).toFloat, y + ringSize * math.sin(angleRad1).toFloat,
          x + ringSize * math.cos(angleRad2).toFloat, y + ringSize * math.sin(angleRad2).toFloat
        )

  private def drawHexagonBorder(x: Float, y: Float, size: Float, color: Color, width: Float): Unit =
    shapeRenderer.setColor(color)
    for i <- 0 until 6 do
      val angleRad1 = math.toRadians(60 * i + 30).toFloat
      val angleRad2 = math.toRadians(60 * (i + 1) + 30).toFloat

      val x1 = x + size * math.cos(angleRad1).toFloat
      val y1 = y + size * math.sin(angleRad1).toFloat
      val x2 = x + size * math.cos(angleRad2).toFloat
      val y2 = y + size * math.sin(angleRad2).toFloat

      // Draw thick line as rectangle
      shapeRenderer.rectLine(x1, y1, x2, y2, width)

  private def drawHexagonOutline(x: Float, y: Float, size: Float): Unit =
    for i <- 0 until 6 do
      val angleRad1 = math.toRadians(60 * i + 30).toFloat
      val angleRad2 = math.toRadians(60 * (i + 1) + 30).toFloat
      shapeRenderer.line(
        x + size * math.cos(angleRad1).toFloat, y + size * math.sin(angleRad1).toFloat,
        x + size * math.cos(angleRad2).toFloat, y + size * math.sin(angleRad2).toFloat
      )
