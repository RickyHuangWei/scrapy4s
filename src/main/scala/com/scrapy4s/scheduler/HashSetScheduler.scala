package com.scrapy4s.scheduler

import java.lang
import java.util.concurrent.ConcurrentHashMap

import com.scrapy4s.http.Request
import org.slf4j.LoggerFactory

class HashSetScheduler extends Scheduler {
  val logger = LoggerFactory.getLogger(classOf[HashSetScheduler])
  private val hashSet: ConcurrentHashMap.KeySetView[Request, lang.Boolean] =ConcurrentHashMap.newKeySet[Request]()

  override def check(request: Request): Boolean = {
    logger.debug(s"check request => $request")
    hashSet.add(request)
  }
}
object HashSetScheduler {
  def apply(): HashSetScheduler = new HashSetScheduler()
}