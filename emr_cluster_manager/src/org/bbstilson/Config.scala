package org.bbstilson

case class TagConfig(key: String, value: String)

case class Config(
  clusterName: String,
  masterInstanceType: String,
  driverMemory: String,
  workDir: String,
  workerInstanceType: String,
  numWorkers: Int,
  jarPaths: List[String],
  mainClasses: List[String],
  mainClassArgs: List[List[String]],
  bootstrap: Option[String],
  logUri: String,
  tags: List[TagConfig]
)
