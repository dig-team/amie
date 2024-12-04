#!/usr/bin/env python3
import subprocess
import sys

if len(sys.argv) < 4:
    print("Usage : " + sys.argv[0].split("/")[-1] + " <amie jar path> <kg path>")
    exit(1)

amie_path = sys.argv[1]
KG_PATH_ARG = [sys.argv[2]]
RUN_OUTPUT_FILE = sys.argv[3]

# RUN CONFIGS
JAR_EXEC = ["java", "-jar", amie_path]
MINI_AMIE_ARG = ["-mini"]

PM_STR = "-pm"
HC_STR = "hc"
SUPPORT_STR = "support"
MAXAD_STR = "-maxad"
CONST_STR = "-const"
MINHC_STR = "-minhc"
MINS_STR = "-mins"
MINHC_0_01_VALUE_STR = "0.01"
MINS_1_VALUE_STR = "1"
MINS_100_VALUE_STR = "100"
MAXAD_3_VALUE_STR = "3"
MAXAD_4_VALUE_STR = "4"
GLOBAL_SEARCH_RESULT_PATH_STR = "-globalSearchInfoPath"





def amie_args():
    return [
        [PM_STR, HC_STR, MINHC_STR, MINHC_0_01_VALUE_STR, MAXAD_STR, MAXAD_3_VALUE_STR],
        [PM_STR, SUPPORT_STR, MINS_STR, MINS_100_VALUE_STR, MAXAD_STR, MAXAD_3_VALUE_STR],
        [PM_STR, HC_STR, MINHC_STR, MINHC_0_01_VALUE_STR, MAXAD_STR, MAXAD_3_VALUE_STR],
        [PM_STR, SUPPORT_STR, MINS_STR, MINS_100_VALUE_STR, MAXAD_STR, MAXAD_3_VALUE_STR, CONST_STR],
        [PM_STR, SUPPORT_STR, MINS_STR, MINS_1_VALUE_STR, MAXAD_STR, MAXAD_3_VALUE_STR, CONST_STR],
    ]
def amie_confs() :
    return [JAR_EXEC + args + KG_PATH_ARG for args in amie_args()]

def mini_amie_confs():
    return [JAR_EXEC + MINI_AMIE_ARG + args + KG_PATH_ARG for args in amie_args()]


print("Running benchmark on " + KG_PATH_ARG[0])
print("Configurations :")
for conf in amie_confs() + mini_amie_confs():
    argstr = ""
    for arg in conf:
        argstr += " " + arg
    print(argstr)

[subprocess.run(conf) for conf in mini_amie_confs()]
