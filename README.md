# Typing

Source files for:

Lajus, J., & Suchanek, F. M. (2018, April). Are All People Married?: Determining Obligatory Attributes in Knowledge Bases. 
In Proceedings of the 2018 World Wide Web Conference on World Wide Web (pp. 1115-1124). International World Wide Web Conferences Steering Committee.

article: https://suchanek.name/work/publications/www-2018.pdf

Codes in: kb/ rosa/ rules/ 

## Dependencies

Depends on javatools (https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/javatools/) and "telecom-utils". Both packages can be found as maven projects here: https://github.com/lajus/amie-utils.

## Known bug

AMIE main class (amie.mining.AMIE) will, by default, print the rules during the mining phase. However, on some recent JVM, the Thread handling the printing may run indefinitely. I suggest you for now to use the "-oute" option, that prints all the rules found only at the end of the mining process, but also disable the faulty Thread. 
