package org.github.srtobi.tradewar3.logic

import org.github.srtobi.tradewar3.model.Company
import scala.util.Random

object StockMarket:
  val MinPrice = 100
  val MaxPrice = 4000
  val MinUpdateInterval = 0.5f
  val MaxUpdateInterval = 2.5f
  val Sigma = 400.0 // Standard deviation for price changes

  def updateCompany(company: Company, delta: Float): Company =
    val newNextUpdate = company.nextUpdate - delta
    if newNextUpdate <= 0 then
      val change = (Random.nextGaussian() * Sigma).toInt
      // Simple mean reversion to keep it within bounds and satisfy "less likely at extremes"
      val meanReversion = (2000 - company.price) * 0.05
      val newPrice = Math.max(MinPrice, Math.min(MaxPrice, (company.price + change + meanReversion).toInt))
      val nextInterval = MinUpdateInterval + Random.nextFloat() * (MaxUpdateInterval - MinUpdateInterval)
      company.copy(price = newPrice, nextUpdate = nextInterval)
    else
      company.copy(nextUpdate = newNextUpdate)

  def generateRandomPrice(): Int =
    Random.nextInt(MaxPrice - MinPrice) + MinPrice

  def generateRandomUpdateInterval(): Float =
    MinUpdateInterval + Random.nextFloat() * (MaxUpdateInterval - MinUpdateInterval)

  def getBulkUpgradeCost(currentBulk: Int): Long =
    5000L * Math.pow(2, currentBulk - 1).toLong
