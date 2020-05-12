package org.bbstilson.config

case class Config(
  clusterName: String,
  masterInstanceType: String,
  driverMemory: String,
  workerInstanceType: String,
  numWorkers: Int,
  steps: List[Step],
  bootstrapAction: Option[BootstrapAction],
  logUri: String,
  tags: List[Tag]
)
