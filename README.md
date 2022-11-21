# Proggers

Proggers is progress-bars-as-a-service!

Quickly create hosted progress bars for tracking long-running (or any other!)
processes using a dead-simple HTTP API.

See and use it at [https://proggers.cloud/](https://proggers.cloud/)

## Building

```
# simple build
./gradlew build

# build a distributable java package
./gradlew assembleDist
```

## Running

It's not configurable right now, so you may want to twiddle some values in 
`Main.java` for data store and port.

After executing an `assembleDist`, unzip/untar the output in 
`build/distributions`, change to the `proggers` directory that was extracted,
and execute `./bin/proggers` (make this executable if you used the `.zip`
package).

## Usage

Refer to the [Proggers Website Info Page](https://proggers.cloud/info.html)
for usage information.
