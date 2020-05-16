<img src="./public/images/SageRankLogo320x320.png" width="250" height="250"></img>

# SageRank

SageRank is a tool for keeping track of the papers you read, and perhaps more importantly, for
helping you find papers that can help you understand those you're interested in. Upon adding papers
that you've read or that are on your to-read list to SageRank, it builds a graph of these papers and
those in their bibliographies. SageRank can then recommend relevant articles to you using an
algorithm similar to Google's PageRank.

## Installation

SageRank can be installed either by downloading a precompiled binary or by building it from source.
Users without Scala installed may want to download just the binary, however users that have Scala
installed can save some storage space (and benefit from other advantages of source-based packages).

### Installing the Binary

Navigate to <https://github.com/harwiltz/SageRank/releases> to download the latest release of
SageRank, distributed as a `.zip` file. Specifically, download the file called
`sagerank-server-<version>-SNAPSHOT.zip`. Place the zip file in the directory of your choice, like
`~/sagerank` for example. Then, issue the following commands,

```bash
cd ~/sagerank # or whichever directory you saved the zip file
unzip sagerank-server-1.0-SNAPSHOT.zip # or whichever version you downloaded
sagerank-server-1.0-SNAPSHOT/bin/sagerank-server
```

This will run the SageRank server on port `9000`, which you can access by navigating to
`localhost:9000` from the browser of your choice.

### Building from Source

To build SageRank from source, you must have Maven and SBT installed. First clone this repo:

```bash
git clone --recurse-submodules https://github.com/harwiltz/SageRank
cd SageRank
```

Next, build and install the `sagerank-lib` library packaged in this repo:

```bash
cd sagerank-lib
mvn install
cd ..
```

Finally, build and run the SageRank server as follows:

```bash
# To run a development server
sbt run

# To run a production server
sbt compile
sbt playGenerateSecret
sbt playUpdateSecret
sbt dist
unzip target/universal/sagerank-server-1.0-SNAPSHOT.zip
sagerank-server-1.0-SNAPSHOT/bin/sagerank-server
```

Both options above run the server on port `9000`. Note that at this stage, SageRank should not be
run in a real production environment.
