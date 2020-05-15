package org.bbstilson

import org.bbstilson.config._

import pureconfig._
import pureconfig.generic.auto._

object SpawnCluster {

  def main(args: Array[String]): Unit = {
    val config = ConfigSource.default.loadOrThrow[Config]
    run(config)
  }

  def run(config: Config): Unit = {
    new ClusterManager(config).runAndWait()
  }
}
