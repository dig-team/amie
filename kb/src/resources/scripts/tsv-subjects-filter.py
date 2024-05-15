#!/usr/bin/python

import csv
import sys

with open(sys.argv[1], 'rU') as csvin:
	tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07');
	for row in tsvin:
		print row[0]

