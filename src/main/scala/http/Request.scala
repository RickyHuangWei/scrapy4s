package http

import spider.Spider

case class Request(
                    url: String,
                    method: String = Method.GET,
                    param: Map[String, String] = Map.empty,
                    data: Map[String, String] = Map.empty
                  ) {
  override def equals(obj: scala.Any) = {
    obj match {
      case _obj: Request =>
        if (
          _obj.method.equals(this.method) ||
          _obj.url.equals(this.url)
        ){
          true
        } else {
          false
        }
      case _ =>
        false
    }
  }

  /**
    * 执行请求
    * @return 返回Response对象
    */
  def execute(spider: Spider[_]): Response = execute(spider.requestConfig)

  def execute(config: RequestConfig = RequestConfig.default): Response = {
    var error_count = 0
    while (error_count <= config.tryCount) {
      try {
        val _res = Response(this, UAHttp(this.url).method(this.method).asString)
        if (config.test_func(_res)) {
          return _res
        } else {
          throw new Exception("test function return false")
        }
      } catch {
        case e:Exception =>
          error_count += 1
          if (error_count > config.tryCount) {
            throw new Exception(s"try count is max -> ${config.tryCount}", e)
          }
      }
    }
    throw new Exception("unknown exception")
  }
}
