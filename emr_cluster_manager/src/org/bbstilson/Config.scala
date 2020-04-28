package org.bbstilson

case class TagConfig(key: String, value: String)

case class Config(
  clusterName: String,
  masterInstanceType: String,
  driverMemory: String,
  workerInstanceType: String,
  numWorkers: Int,
  jarPaths: List[String],
  mainClasses: List[String],
  mainClassArgs: List[List[String]],
  bootstrap: Option[String],
  logUri: String,
  tags: List[TagConfig]
) {

  override def toString(): String = {
    s"""
       |Config(
       |  clusterName: $clusterName,
       |  masterInstanceType: $masterInstanceType,
       |  driverMemory: $driverMemory,
       |  workerInstanceType: $workerInstanceType,
       |  numWorkers: $numWorkers,
       |  jarPaths: $jarPaths,
       |  mainClasses: $mainClasses,
       |  mainClassArgs: $mainClassArgs,
       |  bootstrap: $bootstrap,
       |  logUri: $logUri,
       |  tags: $tags
       |)
    """.stripMargin
  }
}
