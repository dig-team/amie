# AMIE 

AMIE is a system to mine Horn rules on RDF knowledge bases. An RDF knowledge base is a collection of facts of the form <subject, relation, object> such as <Elvis, bornIn, Tupelo>. AMIE can find rules accurate rules such as locatedInCity(x, y) ^ locatedInCountry(y, z) => locatedInCountry(x, z) from large knowledge bases. AMIE stands for Association Rule Mining under Incomplete Evidence. 

Its latest version is AMIE 3. Previous version of AMIE can be downloaded [here](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/amie/)

## Dependencies

* Java >= 7
* Apache Commons >= 1.2
* [Javatools](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/javatools/) and "telecom-utils". Both packages can be found as maven projects here: https://github.com/lajus/amie-utils.

## Deployment

AMIE is managed with [Maven](https://maven.apache.org/), therefore you do not to deploy you need:

1. Clone this repository: $ git clone https://github.com/lajus/amie/
2. Import and compile the project
 ** IDEs such as Eclipse offer the option to create a project an existing Maven project 
3. Maven will generate an executable jar named amie3.jar. AMIE accepts RDF files in TSV format [like this one](http://resources.mpi-inf.mpg.de/yago-naga/amie/data/yago2_sample/yago2core.10kseedsSample.compressed.notypes.tsv). To run it, just write in your comand line: 

java -jar amie3.jar [TSV file]

In case of memory issues, try to increase the virtual machine's memory resources using the arguments -XX:-UseGCOverheadLimit -Xmx [MAX_HEAP_SPACE], e.g:

java -XX:-UseGCOverheadLimit -Xmx2G -jar amie3.jar [TSV file]

MAX_HEAP_SPACE depends on your input size and the system's available memory. The package also contains the utilities to generate and evaluate predictions from the rules mined by AMIE. Without additional arguments AMIE+ thresholds using PCA confidence 0.1 and head coverage 0.01. You can change these default settings. Run java -jar amie+.jar (without an input file) to see a detailed description of the available options.

## Known bug

AMIE main class (amie.mining.AMIE) will, by default, print the rules during the mining phase. However, on some recent JVM, the Thread handling the printing may run indefinitely. I suggest you for now to use the "-oute" option, that prints all the rules found only at the end of the mining process, but also disable the faulty Thread. 

## Publications 

### AMIE 3

Latest version of AMIE with a set of runtime enhancements: 

Lajus, J., Galárraga, L., & Suchanek, F. M (2020). Fast and Exact Rule Mining with AMIE 3. Under revision at the Extended Semantic Web Conference.

'''Data''': 

### Determining Obligatory Attributes in Knowledge Bases

Source files for:

Lajus, J., & Suchanek, F. M. (2018, April). Are All People Married?: Determining Obligatory Attributes in Knowledge Bases. 
In Proceedings of the 2018 World Wide Web Conference on World Wide Web (pp. 1115-1124). International World Wide Web Conferences Steering Committee.

article: https://suchanek.name/work/publications/www-2018.pdf

Codes in: kb/ rosa/ rules/ 
