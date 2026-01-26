package org.github.srtobi.tradewar3.logic

import org.github.srtobi.tradewar3.model.Company
import org.github.srtobi.tradewar3.GameConfig.*
import scala.util.Random

object StockMarket:
  def updateCompany(company: Company, delta: Float): Company =
    val newNextUpdate = company.nextUpdate - delta
    if newNextUpdate <= 0 then
      val change = (Random.nextGaussian() * STOCK_PRICE_SIGMA).toInt
      // Simple mean reversion to keep it within bounds and satisfy "less likely at extremes"
      val meanReversion = (STOCK_MEAN_REVERSION_TARGET - company.price) * STOCK_MEAN_REVERSION_FACTOR
      val newPrice = Math.max(STOCK_MIN_PRICE, Math.min(STOCK_MAX_PRICE, (company.price + change + meanReversion).toInt))
      val nextInterval = STOCK_MIN_UPDATE_INTERVAL + Random.nextFloat() * (STOCK_MAX_UPDATE_INTERVAL - STOCK_MIN_UPDATE_INTERVAL)
      company.copy(price = newPrice, nextUpdate = nextInterval)
    else
      company.copy(nextUpdate = newNextUpdate)

  def generateRandomPrice(): Int =
    Random.nextInt(STOCK_MAX_PRICE - STOCK_MIN_PRICE) + STOCK_MIN_PRICE

  def generateRandomUpdateInterval(): Float =
    STOCK_MIN_UPDATE_INTERVAL + Random.nextFloat() * (STOCK_MAX_UPDATE_INTERVAL - STOCK_MIN_UPDATE_INTERVAL)

  def getBulkUpgradeCost(currentBulk: Int): Long =
    STOCK_BULK_UPGRADE_BASE_COST * Math.pow(3, currentBulk - 1).toLong
