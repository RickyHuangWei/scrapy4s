package util

import java.io.File

/**
  * Created by sheep3 on 2017/11/29.
  */
object FileUtil {

  def path(foldList: Seq[String]) ={
    foldList.mkString(File.separator)
  }

  def pathWithHome(foldList: Seq[String]) ={
    val _foldList: Seq[String] = Seq[String](System.getProperty("user.home")) ++ foldList
    _foldList.mkString(File.separator)
  }

}
