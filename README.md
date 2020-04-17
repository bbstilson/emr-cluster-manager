# emr-cluster-manager

Half-baked implementation of a cluster manager for EMR.

Build tool is [mill](http://www.lihaoyi.com/mill/).

## Usage

1) bundle your jar and put it on s3.
2) Run the main class:

```bash
mill emr_cluster_manager.run \
  --clusterName "S2 Search Service - Rico Training Data" \
  --numWorkers 1 \
  --prefix "ai2-s2-brandons/s2_search_service/training_data" \
  --mainClasses "org.allenai.rico_data.ExportRicoData" \
  --jarPaths jars/out.jar
```

## Todo

So much jank.

1) Need to be able to pass mainclass args.
2) Need a better solution to hardcoding s3 paths.
3) Probably typesafe config with pureconfig.
