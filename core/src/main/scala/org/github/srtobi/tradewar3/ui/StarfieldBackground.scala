package org.github.srtobi.tradewar3.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import scala.util.Random

class StarfieldBackground(count: Int = 200) extends Disposable:
  private val shapeRenderer = new ShapeRenderer()
  private val viewport = new ScreenViewport()
  
  private class Star(var x: Float, var y: Float, val speed: Float, val size: Float)
  
  private val stars = Array.fill(count) {
    new Star(0, 0, Random.nextFloat() * 40 + 20, Random.nextFloat() * 1.5f + 0.5f)
  }

  def render(delta: Float): Unit =
    viewport.apply()
    shapeRenderer.setProjectionMatrix(viewport.getCamera.combined)
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    shapeRenderer.setColor(Color.WHITE)
    
    val worldWidth = viewport.getWorldWidth
    val worldHeight = viewport.getWorldHeight
    val halfW = worldWidth / 2f
    val halfH = worldHeight / 2f
    
    if worldWidth > 0 && worldHeight > 0 then
      stars.foreach { star =>
        star.x -= star.speed * delta
        
        if star.x < -halfW then
          star.x = halfW
          star.y = Random.nextFloat() * worldHeight - halfH
        
        // In case they are outside after a resize (and before next resize call or if resize wasn't enough)
        if star.y > halfH then star.y = halfH
        if star.y < -halfH then star.y = -halfH

        shapeRenderer.circle(star.x, star.y, star.size)
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

  override def dispose(): Unit =
    shapeRenderer.dispose()
