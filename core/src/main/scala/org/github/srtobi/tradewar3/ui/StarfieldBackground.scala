package org.github.srtobi.tradewar3.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.{Color, GL20}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import scala.util.Random

class StarfieldBackground(starCount: Int = 300) extends Disposable:
  private val shapeRenderer = new ShapeRenderer()
  private val viewport = new ScreenViewport()

  // Star layers for parallax effect
  private class Star(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    var twinklePhase: Float,
    val twinkleSpeed: Float
  )

  // Nebula clouds
  private class Nebula(
    var x: Float,
    var y: Float,
    val radius: Float,
    val color: Color,
    val speed: Float
  )

  // Create colorful stars with different properties
  private val stars = Array.fill(starCount) {
    val layer = Random.nextInt(3) // 0 = far, 1 = mid, 2 = close
    val (speed, sizeRange) = layer match
      case 0 => (Random.nextFloat() * 10 + 5, Random.nextFloat() * 0.8f + 0.3f)   // Far: slow, small
      case 1 => (Random.nextFloat() * 25 + 15, Random.nextFloat() * 1.2f + 0.6f)  // Mid
      case _ => (Random.nextFloat() * 50 + 30, Random.nextFloat() * 2.0f + 1.0f)  // Close: fast, large

    // Star colors: mostly white/blue, some yellow/orange
    val colorRoll = Random.nextFloat()
    val starColor = colorRoll match
      case r if r < 0.5f => new Color(0.9f + Random.nextFloat() * 0.1f, 0.9f + Random.nextFloat() * 0.1f, 1f, 1f) // White-blue
      case r if r < 0.75f => new Color(0.7f + Random.nextFloat() * 0.3f, 0.8f + Random.nextFloat() * 0.2f, 1f, 1f) // Blue
      case r if r < 0.9f => new Color(1f, 1f, 0.7f + Random.nextFloat() * 0.3f, 1f) // Yellow
      case _ => new Color(1f, 0.7f + Random.nextFloat() * 0.2f, 0.5f + Random.nextFloat() * 0.2f, 1f) // Orange

    new Star(0, 0, speed, sizeRange, starColor, Random.nextFloat() * math.Pi.toFloat * 2, Random.nextFloat() * 3 + 1)
  }

  // Create nebula clouds
  private val nebulas = Array.fill(8) {
    val nebulaColors = Array(
      new Color(0.2f, 0.1f, 0.4f, 0.15f),  // Purple
      new Color(0.1f, 0.2f, 0.4f, 0.12f),  // Deep blue
      new Color(0.3f, 0.1f, 0.2f, 0.10f),  // Magenta
      new Color(0.1f, 0.3f, 0.3f, 0.08f),  // Teal
      new Color(0.4f, 0.2f, 0.1f, 0.10f),  // Orange-brown
    )
    new Nebula(
      0, 0,
      Random.nextFloat() * 150 + 100,
      nebulaColors(Random.nextInt(nebulaColors.length)),
      Random.nextFloat() * 8 + 3
    )
  }

  // Shooting stars
  private class ShootingStar(
    var x: Float,
    var y: Float,
    val angle: Float,
    val speed: Float,
    var life: Float,
    val maxLife: Float
  )

  private var shootingStars: List[ShootingStar] = Nil
  private var shootingStarTimer: Float = 0

  def render(delta: Float): Unit =
    viewport.apply()

    Gdx.gl.glEnable(GL20.GL_BLEND)
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

    shapeRenderer.setProjectionMatrix(viewport.getCamera.combined)

    val worldWidth = viewport.getWorldWidth
    val worldHeight = viewport.getWorldHeight
    val halfW = worldWidth / 2f
    val halfH = worldHeight / 2f

    if worldWidth > 0 && worldHeight > 0 then
      // Draw nebulas (background layer)
      shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
      nebulas.foreach { nebula =>
        nebula.x -= nebula.speed * delta

        if nebula.x < -halfW - nebula.radius then
          nebula.x = halfW + nebula.radius
          nebula.y = Random.nextFloat() * worldHeight - halfH

        // Draw nebula as multiple overlapping circles for soft effect
        for i <- 0 until 5 do
          val scale = 1f - i * 0.15f
          val alpha = nebula.color.a * (1f - i * 0.2f)
          shapeRenderer.setColor(nebula.color.r, nebula.color.g, nebula.color.b, alpha)
          shapeRenderer.circle(nebula.x, nebula.y, nebula.radius * scale, 32)
      }
      shapeRenderer.end()

      // Draw stars with twinkling
      shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
      stars.foreach { star =>
        star.x -= star.speed * delta
        star.twinklePhase += star.twinkleSpeed * delta

        if star.x < -halfW then
          star.x = halfW
          star.y = Random.nextFloat() * worldHeight - halfH

        if star.y > halfH then star.y = halfH
        if star.y < -halfH then star.y = -halfH

        // Twinkle effect
        val twinkle = (math.sin(star.twinklePhase).toFloat + 1f) / 2f * 0.4f + 0.6f
        val alpha = twinkle

        shapeRenderer.setColor(star.color.r * twinkle, star.color.g * twinkle, star.color.b * twinkle, alpha)
        shapeRenderer.circle(star.x, star.y, star.size)

        // Add glow for brighter stars
        if star.size > 1.2f then
          shapeRenderer.setColor(star.color.r, star.color.g, star.color.b, alpha * 0.2f)
          shapeRenderer.circle(star.x, star.y, star.size * 2.5f)
      }
      shapeRenderer.end()

      // Shooting stars
      shootingStarTimer -= delta
      if shootingStarTimer <= 0 then
        shootingStarTimer = Random.nextFloat() * 4 + 2 // Every 2-6 seconds
        val angle = Random.nextFloat() * 0.5f + 0.2f // Mostly horizontal
        shootingStars = new ShootingStar(
          halfW + 50,
          Random.nextFloat() * worldHeight - halfH,
          angle,
          Random.nextFloat() * 400 + 300,
          0,
          Random.nextFloat() * 0.8f + 0.4f
        ) :: shootingStars

      // Update and draw shooting stars
      shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
      shootingStars = shootingStars.filter { ss =>
        ss.life += delta
        ss.x -= math.cos(ss.angle).toFloat * ss.speed * delta
        ss.y -= math.sin(ss.angle).toFloat * ss.speed * delta

        if ss.life < ss.maxLife then
          val progress = ss.life / ss.maxLife
          val alpha = if progress < 0.3f then progress / 0.3f else (1f - progress) / 0.7f
          val tailLength = 30 + progress * 50

          // Draw tail
          for i <- 0 until 10 do
            val t = i / 10f
            val tx = ss.x + math.cos(ss.angle).toFloat * tailLength * t
            val ty = ss.y + math.sin(ss.angle).toFloat * tailLength * t
            val ta = alpha * (1f - t) * 0.8f
            shapeRenderer.setColor(1f, 1f, 1f, ta)
            shapeRenderer.circle(tx, ty, 2f * (1f - t * 0.5f))

          // Draw head
          shapeRenderer.setColor(1f, 1f, 1f, alpha)
          shapeRenderer.circle(ss.x, ss.y, 3f)
          true
        else false
      }
      shapeRenderer.end()

  def resize(width: Int, height: Int): Unit =
    viewport.update(width, height, false)
    viewport.getCamera.position.set(0, 0, 0)
    viewport.getCamera.update()

    val worldWidth = viewport.getWorldWidth
    val worldHeight = viewport.getWorldHeight
    val halfW = worldWidth / 2f
    val halfH = worldHeight / 2f

    stars.foreach { star =>
      star.x = Random.nextFloat() * worldWidth - halfW
      star.y = Random.nextFloat() * worldHeight - halfH
    }

    nebulas.foreach { nebula =>
      nebula.x = Random.nextFloat() * worldWidth - halfW
      nebula.y = Random.nextFloat() * worldHeight - halfH
    }

  override def dispose(): Unit =
    shapeRenderer.dispose()
