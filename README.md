# AMIE 
AMIE is a system to mine Horn rules on knowledge bases. A knowledge base is a collection of facts, such as e.g. 
> wasBornIn(Elvis, Tupelo)  
> isLocatedIn(Tupelo, USA)

AMIE can find rules in such knowledge bases, such as for example
> wasBornIn(x, y) & isLocatedIn(y, z) => hasNationality(x, z)

These rules are accompanied by various confidence scores. “AMIE” stands for “Association Rule Mining under Incomplete Evidence”. This repository contains the latest version of AMIE, called AMIE 3.5. The versions of AMIE prior to 3.x can be found [here](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/amie/). The code of version 3.0 can be found [here](https://github.com/dig-team/amie/tree/v3.0).

## Input files

AMIE takes as input a file that contains a knowledge base. The knowledge base can be in format TTL, N3, or CSV. AMIE supports two CSV variants:
 1. `subject DELIM predicate DELIM object [whitespace/tabulation .] NEWLINE`
 2. `factid DELIM subject DELIM predicate DELIM object [whitespace/tabulation .] NEWLINE`

The default delimiter `DELIM` is the tabulation (.tsv files) but can be changed using the `-d` option. Any trailing whitespaces followed by a point are ignored. This allows parsing most NT files using the option: `-d" "`. 

However make sure the factid, subject, predicate nor the object contains the delimiter used (particularly in literal facts files). Otherwise the parsing may fail or facts may be wrongfully recognized as the second format.

## Running AMIE

Make sure that you have the latest version of [Java](https://java.com/en/download/) installed. Download the jar file, and type:

```java -jar amie3.5.jar [TSV file]```

In case of memory issues, try to increase the virtual machine's memory resources using the arguments `-XX:-UseGCOverheadLimit -Xmx [MAX_HEAP_SPACE]`, e.g:

```java -XX:-UseGCOverheadLimit -Xmx2G -jar amie3_5.jar [TSV file]```

`MAX_HEAP_SPACE` depends on your input size and the system's available memory. The package also contains the utilities to generate and evaluate predictions from the rules mined by AMIE. Without additional arguments AMIE thresholds with PCA confidence 0.1 and head coverage 0.01. You can change these default settings. Run `java -jar amie3_5.jar -h` (without an input file) to see a detailed description of the available options.

### Use with remote knowledge base server

Since loading and storing knowledge graphs can take a significant amount of memory space and time, AMIE 3.5 makes it possible to run the mining routine against a remote knowledge base, splitting the architecture into two parts communicating over network.

Below is a basic setup example to use AMIE with a remote knowledge base.

#### Server-side

```java -jar amie3.5.jar -server [TSVFile] -port <Server Port (default: 9092)>```

This will load the data into the memory of the server. 

#### Client-side

```java -jar amie3.5.jar -client -serverAddress <Server Address (default: localhost:9092)>```

In this case the client will mine the rules on the server deployed at the provided answer.

__NOTE__:
- Client and Server communicate using the WebSocket protocol. 

#### Optional: Enabling cache

AMIE may run the same query more than once. It is therefore possible to enable query caching for either server or client side with the ```-cache``` option. This option is available only for remote mining. The cache option can be set either on the client or on the server side. The cache is automatically saved upon shutdown. If a corresponding cache is found, cache save is loaded, unless `-invalidateCache` is passed as argument.

The cache can improve performance significantly by reducing the amount of queries sent over network or executed by the KB.
Performances will vary depending on the knowledge graph and the user parameters. 

The performance of the cache and the remote setting is sensitive to the data, as this defines the size of AMIE's search space as well as the amount of queries and query answers that will be sent over the network. 

__NOTE__:
- Cache uses Least Recently Used (LRU) policy. As of yet, only LRU cache policy has been implemented. 
- Custom cache policies can be implemented in `amie/data/remote/cachepolicies` package.
- Cache is saved locally in the cache directory using the knowledge graph file name and run options.

## Deploying AMIE

If you want to modify the code of AMIE, you need

* Apache Maven >= 3.6.0
* Java >= 8
* Apache Commons >= 1.3.1
* [Javatools](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/amie.data.javatools/). This package can be found as Maven projects here: https://github.com/lajus/amie-utils. As this artifact is not yet uploaded into a central repository, please follow the installation procedure described in the previous link before trying to compile this project.

AMIE is managed with [Maven](https://maven.apache.org/), therefore to deploy you need:

0. (Provisional) Install Javatools dependency as explained in https://github.com/lajus/amie-utils (you can omit telecom-util).
1. Clone this repository: `$ git clone https://github.com/lajus/amie/`
2. Import and compile the project
 * It is usually done by executing the following command in the amie directory: `$ mvn install`
 * IDEs such as Eclipse offer the option to create a project from an existing Maven project. The IDE will call Maven to compile the code.
3. Maven will generate an executable jar named amie3.5.jar in a new "bin/" directory. This executable accepts RDF files in TSV format [like this one](http://resources.mpi-inf.mpg.de/yago-naga/amie/data/yago2_sample/yago2core.10kseedsSample.compressed.notypes.tsv) as input, but also other format described below. To run it, just write in your comand line: 

### Reproducing our experiments (AMIE 3)

Our [2020 ESWC publication](https://luisgalarraga.de/docs/amie3.pdf) introduced a handful of algorithmic optimizations that gave birth to [AMIE3](https://github.com/dig-team/amie/tree/v3.0). Besides an extensive code refactoring, the lastest version of AMIE includes novel features, optimizations, and several bug fixes. You might therefore not obtain the exact same runtime results as AMIE3. 

If you want nevertheless reproduce the experiments published in 2020, you can find the executables used for the experiments in the milestone directory or in the "releases" github tab. Option names and default options are subject to change compared these milestones. To reproduce experiments, use by default:

```java -jar amie-milestone-intKB.jar -bias lazy -full -noHeuristics -ostd [TSV file]```

Experimental implementation of the GPro and GRank measures can be found in the "gpro" branch. After recompiling the sources ot this branch, use:

```java -jar amie3.jar -bias amie.mining.assistant.experimental.[GPro|GRank] [TSVFile]```

## Publications 

> Jonathan Lajus, Luis Galárraga, Fabian M. Suchanek:  
> [“Fast and Exact Rule Mining with AMIE 3”  ](https://suchanek.name/work/publications/eswc-2020-amie-3.pdf)  
> Full paper at the Extended Semantic Web Conference (ESWC), 2020  

> Luis Galárraga, Christina Teflioudi, Katja Hose, Fabian M. Suchanek:  
> [“Fast Rule Mining in Ontological Knowledge Bases with AMIE+”](https://suchanek.name/work/publications/vldbj2015.pdf)  
> Journal article in the VLDB Journal  (VLDBJ), 2015

> Luis Galárraga, Christina Teflioudi, Katja Hose, Fabian M. Suchanek:  
> [“AMIE: Association Rule Mining under Incomplete Evidence in Ontological Knowledge Bases”](https://suchanek.name/work/publications/www2013.pdf)  
> Full paper at the International World Wide Web Conference (WWW), 2013  

## Licensing

<a rel="license" href="http://creativecommons.org/licenses/by/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by/4.0/80x15.png" /></a>. AMIE is distributed under the terms of the <a rel="license" href="http://creativecommons.org/licenses/by/4.0/">Creative Commons Attribution 4.0 International License</a> by the [YAGO-NAGA team](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/amie/) and the [DIG team](https://dig.telecom-paris.fr/blog/).

AMIE uses Javatools, a library released under the [Creative Commons Attribution license v3.0](https://creativecommons.org/licenses/by/3.0/) by the [YAGO-NAGA team](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/amie.data.javatools/).
