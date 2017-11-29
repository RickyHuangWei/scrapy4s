package spider

import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
import java.util.concurrent.{Future, LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}

import http.{Request, RequestConfig, Response}
import org.slf4j.LoggerFactory
import pipeline.Pipeline
import scheduler.{HashSetScheduler, Scheduler}


/**
  * 爬虫核心类，用于组装爬虫
  * @tparam T 封装数据的bean
  */
case class Spider[T](
                 paser: Response => T,
                 threadCount: Int = Runtime.getRuntime.availableProcessors() * 2,
                 requestConfig: RequestConfig = RequestConfig.default,
                 startUrl: Seq[Request] = Seq.empty[Request],
                 pipelines: Seq[Pipeline[T]] = Seq.empty[Pipeline[T]],
                 scheduler: Scheduler = new HashSetScheduler()
               ) {
  val logger = LoggerFactory.getLogger(this.getClass)

  lazy private val threadPool = new ThreadPoolExecutor(threadCount, threadCount,
    0L, TimeUnit.MILLISECONDS,
    new LinkedBlockingQueue[Runnable](),
    new CallerRunsPolicy())

  def withTestFunc(test_func: Response => Boolean) = {
    new Spider[T](
      paser = paser,
      threadCount = threadCount,
      requestConfig = requestConfig.withTestFunc(test_func),
      startUrl = startUrl,
      pipelines = pipelines,
      scheduler = scheduler
    )
  }

  def withStartUrl(urls: Seq[Request]) = {
    new Spider[T](
      paser = paser,
      threadCount = threadCount,
      startUrl = startUrl ++ urls,
      pipelines = pipelines,
      scheduler = scheduler
    )
  }

  def withThreadCount(count: Int) = {
    new Spider[T](
      paser = paser,
      threadCount = count,
      startUrl = startUrl,
      pipelines = pipelines,
      scheduler = scheduler
    )
  }

  def withPipeline(pipeline: Pipeline[T]) = {
    new Spider[T](
      paser = paser,
      threadCount = threadCount,
      startUrl = startUrl,
      pipelines = pipelines :+ pipeline,
      scheduler = scheduler
    )
  }

  def withScheduler(s: Scheduler) = {
    new Spider[T](
      paser = paser,
      threadCount = threadCount,
      startUrl = startUrl,
      pipelines = pipelines,
      scheduler = s
    )
  }

  def withPaser(p: Response => T) = {
    new Spider[T](
      paser = p,
      threadCount = threadCount,
      startUrl = startUrl,
      pipelines = pipelines,
      scheduler = scheduler
    )
  }

  def start(): Unit ={
    run()
    waitForShop()
  }

  /**
    * 初始化爬虫设置，并将初始url倒入任务池中
    */
  def run(): Unit ={
    startUrl.foreach(request => {
      execute(request)
    })
  }

  def waitForShop() = {
    threadPool.shutdown()
    while (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
      logger.debug("wait for spider done ...")
    }
    pipelines.foreach(p => {
      p.close()
    })
    logger.info("spider done !")
  }

  /**
    * 提交请求任务到线程池
    * @param request 等待执行的请求
    */
  def execute(request: Request): Unit = {
    threadPool.execute(() => {
      /**
        * 判断是否已经爬取过
        */
      if(scheduler.check(request)) {
        logger.info(s"crawler -> ${request.method}: ${request.url}")
        val response = request.execute(this)
        val model = paser(response)

        /**
          * 执行数据操作
          */
        pipelines.foreach(p => {
          p.pipe(model, response)
        })
      } else {
        logger.debug(s"$request has bean spider !")
      }
    })
  }
}
object Spider{
  def apply[T](p: Response => T): Spider[T] = new Spider[T](p)
}