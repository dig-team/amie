# AMIE 

AMIE is a system to mine Horn rules on RDF knowledge bases. An RDF knowledge base is a collection of facts of the form <subject, relation, object> such as <Elvis, bornIn, Tupelo>. AMIE can find rules accurate rules such as locatedInCity(x, y) ^ locatedInCountry(y, z) => locatedInCountry(x, z) in large knowledge bases. AMIE stands for Association Rule Mining under Incomplete Evidence. 

Its latest version is AMIE 3. Previous version of AMIE can be found [here](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/amie/). 

## Dependencies

* Apache Maven >= 3.6.0
* Java >= 8
* Apache Commons >= 1.3.1
* [Javatools](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/javatools/) and "telecom-utils". Both packages can be found as Maven projects here: https://github.com/lajus/amie-utils. As those artifact are not yet uploaded into a central repository, please follow the installation procedure described in the previous link before trying to compile this project.

## Deployment

AMIE is managed with [Maven](https://maven.apache.org/), therefore to deploy you need:

0. (Provisional) Install Javatools and telecom-utils dependencies as explained in https://github.com/lajus/amie-utils.
1. Clone this repository: $ git clone https://github.com/lajus/amie/
2. Import and compile the project
 * It is usually done by executing the following command in the amie directory: $ mvn install
 * IDEs such as Eclipse offer the option to create a project from an existing Maven project. The IDE will call Maven to compile the code.
3. Maven will generate an executable jar named amie3.jar in a new "bin/" directory. This executable accepts RDF files in TSV format [like this one](http://resources.mpi-inf.mpg.de/yago-naga/amie/data/yago2_sample/yago2core.10kseedsSample.compressed.notypes.tsv) as input, but also other format described below. To run it, just write in your comand line: 

java -jar amie3.jar [TSV file]

In case of memory issues, try to increase the virtual machine's memory resources using the arguments -XX:-UseGCOverheadLimit -Xmx [MAX_HEAP_SPACE], e.g:

java -XX:-UseGCOverheadLimit -Xmx2G -jar amie3.jar [TSV file]

MAX_HEAP_SPACE depends on your input size and the system's available memory. The package also contains the utilities to generate and evaluate predictions from the rules mined by AMIE. Without additional arguments AMIE thresholds using PCA confidence 0.1 and head coverage 0.01. You can change these default settings. Run java -jar amie3.jar -h (without an input file) to see a detailed description of the available options.

## Input files

AMIE is now compatible with input file with the following formats:
 1. subject DELIM predicate DELIM object [whitespace/tabulation .] NEWLINE
 2. factid DELIM subject DELIM predicate DELIM object [whitespace/tabulation .] NEWLINE

The default delimiter (DELIM) is the tabulation (.tsv files) but can be changed using the "-d" option. Any trailing whitespaces followed by a point are ignored. This allows parsing most NT files using the option: -d" ". 

However make sure the factid, subject, predicate nor the object contains the delimiter used (particularly in litteral facts files). Otherwise the parsing may fail or facts may be wrongfully recognized as the second format.

## Publications 

### AMIE 3

Latest version of AMIE with a set of runtime enhancements: 

Lajus, J., Galárraga, L., & Suchanek, F. M (2020). Fast and Exact Rule Mining with AMIE 3. Under revision at the Extended Semantic Web Conference.

**Executables used**: Can be found in the milestones directory or in the milestones github onglet. Option names and default options are subject to change compared these milestones. To reproduce experiments, use by default:

java -jar amie-milestone-intKB.jar -bias lazy -full -noHeuristics -ostd [TSV file]

**Data**: 

### Determining Obligatory Attributes in Knowledge Bases

Source files for:

Lajus, J., & Suchanek, F. M. (2018, April). Are All People Married?: Determining Obligatory Attributes in Knowledge Bases. 
In Proceedings of the 2018 World Wide Web Conference on World Wide Web (pp. 1115-1124). International World Wide Web Conferences Steering Committee.

article: https://suchanek.name/work/publications/www-2018.pdf

Codes in: kb/ rosa/ rules/ 
