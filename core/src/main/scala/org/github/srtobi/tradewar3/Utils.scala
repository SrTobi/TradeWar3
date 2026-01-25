package org.github.srtobi.tradewar3

import com.badlogic.gdx.graphics.Color

extension (c: Color) {
  def distance(other: Color): Double =
    math.sqrt(distanceSquared(other))
    
  def distanceSquared(other: Color): Double =
    math.pow(c.r - other.r, 2) + math.pow(c.g - other.g, 2) + math.pow(c.b - other.b, 2)
}