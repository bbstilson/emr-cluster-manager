# emr-cluster-manager

Half-baked implementation of a cluster manager for EMR.

Build tool is [mill](http://www.lihaoyi.com/mill/).

## Usage

1) Bundle your jar and put it on S3.
2) Create a config file. (e.g., `export_data.conf`)
3) Run the cluster manager with the resource path to your config:

```bash
export JAVA_TOOL_OPTIONS="-Xms512m -Xmx1536m -Xss2m"
mill emr_cluster_manager.run --configPath export_data.conf
```

Grab a coffee or tea. ☕️
