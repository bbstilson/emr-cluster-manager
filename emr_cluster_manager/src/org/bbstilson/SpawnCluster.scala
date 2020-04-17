package org.bbstilson

import scopt.OptionParser
import collection.JavaConverters._

object SpawnCluster {

  case class Args(
    clusterName: String = "",
    prefix: String = "",
    mainClasses: List[String] = Nil,
    jarPaths: List[String] = Nil,
    numWorkers: Int = 1,
    masterInstanceType: String = "m5.2xlarge",
    workerInstanceType: String = "r5.2xlarge",
    bootstrap: Option[String] = None
  )

  def main(args: Array[String]): Unit = {
    val argParser = new OptionParser[Args](getClass.getSimpleName) {
      opt[String]("clusterName")
        .required()
        .text("Name of the cluster in EMR.")
        .action((name, c) => c.copy(clusterName = name))

      opt[String]("prefix")
        .required()
        .valueName("some-bucket/path/to/files")
        .action((path, c) => c.copy(prefix = path))

      opt[Seq[String]]("mainClasses")
        .required()
        .valueName("some.path.to.SomeClass,some.path.to.AnotherClass")
        .action((classes, c) => c.copy(mainClasses = classes.toList))

      opt[Seq[String]]("jarPaths")
        .required()
        .valueName("step1.jar,step2.jar,...")
        .action((jarPaths, c) => c.copy(jarPaths = jarPaths.toList))
        .text("jars to execute from s3. File names are prefixed with `prefix`.")

      opt[String]("bootstrap")
        .text("path to bootstrap script. Will be prefixed with `prefix`")
        .action((path, c) => c.copy(bootstrap = Some(path)))

      opt[String]("masterInstanceType")
        .text("Master node EC2 type.")
        .action((t, c) => c.copy(masterInstanceType = t))

      opt[String]("workerInstanceType")
        .text("Worker node EC2 type.")
        .action((t, c) => c.copy(workerInstanceType = t))

      opt[Int]("numWorkers")
        .text("How many workers to run.")
        .action((num, c) => c.copy(numWorkers = num))
    }

    argParser
      .parse(args, Args())
      .foreach(run)
  }

  def run(args: Args): Unit = {
    require(
      args.jarPaths.size == args.mainClasses.size,
      "There must be as many mainclasses as there are jars."
    )
    val cm = new ClusterManager(
      args.clusterName,
      args.masterInstanceType,
      args.workerInstanceType,
      args.numWorkers,
      args.prefix,
      args.bootstrap,
      args.mainClasses.zip(args.jarPaths)
    )

    cm.runAndWait()
  }
}
