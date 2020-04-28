package org.bbstilson

import pureconfig._
import pureconfig.error._
import pureconfig.generic.auto._
import scopt.OptionParser

object SpawnCluster {

  case class Args(configPath: String = "")

  val ARG_SIZE_FAILURE = ConfigReaderFailures(
    CannotParse("Number of main classes, jar paths, and main class args must be the same.", None),
    Nil
  )

  def main(args: Array[String]): Unit = {
    val argParser = new OptionParser[Args](getClass.getSimpleName) {
      opt[String]("configPath")
        .required()
        .text("Path to the EMR configuration.")
        .action((cp, c) => c.copy(configPath = cp))
    }

    argParser
      .parse(args, Args())
      .map(_.configPath)
      .map(loadConfig)
      .map(_.flatMap { c =>
        if (c.jarPaths.size != c.mainClasses.size || c.mainClasses.size != c.mainClassArgs.size) {
          Left(ARG_SIZE_FAILURE)
        } else {
          Right(c)
        }
      })
      .foreach {
        case Left(errors)  => errors.toList.foreach(println)
        case Right(config) => run(config)
      }
  }

  private def loadConfig(configPath: String): Either[ConfigReaderFailures, Config] = {
    ConfigSource.resources(configPath).load[Config]
  }

  def run(config: Config): Unit = {
    new ClusterManager(config).runAndWait()
  }
}
