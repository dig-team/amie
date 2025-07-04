# AMIE 
AMIE is a system to mine Horn rules on knowledge bases. A knowledge base is a collection of facts, such as e.g. 
> wasBornIn(Elvis, Tupelo)  
> isLocatedIn(Tupelo, USA)

AMIE can find rules in such knowledge bases, such as for example
> wasBornIn(x, y) & isLocatedIn(y, z) => hasNationality(x, z)

These rules are accompanied by various confidence scores. “AMIE” stands for “Association Rule Mining under Incomplete Evidence”. This repository contains the latest version of AMIE, called AMIE 3.5. The versions of AMIE prior to 3.x can be found [here](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/amie/). The code of version 3.0 (used for our [2020 ESWC publication](https://luisgalarraga.de/docs/amie3.pdf)) can be found [here](https://github.com/dig-team/amie/tree/v3.0).

## Input files

AMIE takes as input a file that contains a knowledge base. The knowledge base can be in format TTL, N3, or CSV. AMIE supports two CSV variants:
 1. `subject DELIM predicate DELIM object [whitespace/tabulation .] NEWLINE`
 2. `factid DELIM subject DELIM predicate DELIM object [whitespace/tabulation .] NEWLINE`

The default delimiter `DELIM` is the tabulation (as in .tsv files) but can be changed using the `-d` option. Any trailing whitespaces followed by a point are ignored.

## Running AMIE

Make sure that you have the latest version of [Java](https://java.com/en/download/) installed. Download an AMIE executable jar file [AMIE-JAR], and type:

```java -jar [AMIE-JAR] [Input file]```

In case of memory issues, try to increase the virtual machine's memory resources using the arguments `-XX:-UseGCOverheadLimit -Xmx [MAX_HEAP_SPACE]`, e.g:

```java -XX:-UseGCOverheadLimit -Xmx2G -jar [AMIE-JAR] [Input file]```

`MAX_HEAP_SPACE` depends on your input size and the system's available memory. The package also contains the utilities to generate and evaluate predictions from the rules mined by AMIE. Without additional arguments AMIE thresholds with PCA confidence 0.1 and head coverage 0.01. You can change these default settings. Run `java -jar [AMIE-JAR] -h` (without an input file) to see a detailed description of the available options.

### PyClause Integration
To output rules that can be used by the PyClause library, you need to run AMIE with these additional parameters:

```-bias amie.mining.assistant.pyclause.AnyBurlMiningAssistant -ofmt anyburl```

Additionally this version of AMIE also offers the possibility of outputting the rules directly into a file via the parameter via the argument: `-ofile [OUTPUT file]`. Also, users can establish different limits on rule length for rules with constants and for rules without constants (the default setting). For example, the argument `-maxad 4` mines rules up to 4 atoms (head atom included, the default value being 3). Similarly the combination of arguments `-const -maxad 4 -maxadc 3` enables constants in rule atoms, sets a limit of 4 atoms in rules without constants, and a limit of 3 atoms for rules for constants. This can be useful since the inclusion of constants in atoms (`-const`) increases the search space, thus the runtime, in a significant way.


### Use with remote knowledge base server

Since loading and storing knowledge graphs can take a significant amount of memory space and time, the latest version of AMIE makes it possible to run the mining routine against a remote knowledge base, splitting the architecture into two parts communicating over network.

Below is a basic setup example to use AMIE with a remote knowledge base.

#### Server-side

```java -jar [AMIE-JAR] -server [Input file] -port <Server Port (default: 9092)>```

This will load the data into the memory of the server. 

#### Client-side

```java -jar [AMIE-JAR] -client -serverAddress <Server Address (default: localhost:9092)>```

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

AMIE is managed with [Maven](https://maven.apache.org/), therefore to deploy you need:

1. Clone this repository: `$ git clone https://github.com/lajus/amie/`
2. Import and compile the project
 * It is usually done by executing the following command in the amie directory: `$ mvn install`
 * IDEs such as Eclipse offer the option to create a project from an existing Maven project. The IDE will call Maven to compile the code.
3. Maven will generate an executable jar named amie[LATEST-VERSION].jar in a new "bin/" directory. 

## Publications 

> Patrick Betz, Luis Galárraga, Simon Ott, Christian Meilicke, Fabian M. Suchanek: 
> ["PyClause-Simple and Efficient Rule Handling for Knowledge Graphs"](https://luisgalarraga.de/docs/IJCAI_2024_demo_paper.pdf)
> Demo paper at the International Conference on Artificial Intelligence (IJCAI), 2024 ["Software"](https://github.com/symbolic-kg/PyClause)

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
