#!/usr/bin/python

import csv
import sys

with open(sys.argv[1], 'rU') as csvin:
	tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07');
	for row in tsvin:
		print row[1] + "\t" + row[2] + "\t" + row[3]
