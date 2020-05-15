package org.bbstilson

import org.bbstilson.config._

import pureconfig._
import pureconfig.generic.auto._

object SpawnCluster {

  def main(args: Array[String]): Unit = {
    val config = ConfigSource.default.loadOrThrow[Config]
    new ClusterManager(config).runAndWait()
  }
}
