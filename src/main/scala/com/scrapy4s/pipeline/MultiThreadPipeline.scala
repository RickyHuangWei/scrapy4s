package com.scrapy4s.pipeline

import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
import java.util.concurrent.{LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}

import com.scrapy4s.http.Response
import org.slf4j.LoggerFactory


/**
  * Created by sheep3 on 2017/11/28.
  */
abstract class MultiThreadPipeline[T](threadCount: Int) extends Pipeline[T] {
  val logger = LoggerFactory.getLogger(this.getClass)
  lazy private val threadPool = new ThreadPoolExecutor(threadCount, threadCount,
    0L, TimeUnit.MILLISECONDS,
    new LinkedBlockingQueue[Runnable](),
    new CallerRunsPolicy())

  override def pipe(t: T, response: Response): Unit = {
    threadPool.execute(() => {
      logger.debug(s"pipe -> exec ${response.url}")
      execute(t, response)
    })
  }

  def execute(t: T, response: Response): Unit

  override def close(): Unit = {
    threadPool.shutdown()
    while (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
      logger.debug("wait for spider done ...")
    }
    shutdownHook()
    logger.debug("spider done !")
  }

  /**
    * 给子类用于资源收尾
    */
  def shutdownHook() = {
  }
}

object MultiThreadPipeline {
  def apply[T](threadCount: Int = Runtime.getRuntime.availableProcessors() * 2)(p: (T, Response) => Unit): MultiThreadPipeline[T] = {
    new MultiThreadPipeline[T](threadCount) {
      override def execute(t: T, response: Response): Unit = p(t, response)
    }
  }
}
