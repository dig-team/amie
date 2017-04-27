#!/usr/bin/python

import csv
import sys

with open(sys.argv[1], 'rU') as csvin:
	with open('swap.tsv', 'wb') as csvout:
		tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07');
		tsvout = csv.writer(csvout, delimiter='\t', quotechar='', quoting=csv.QUOTE_NONE)
		for row in tsvin:
			newRow = []
			newRow.append(row[1])
			newRow.append(row[0])
			newRow.append(row[2])
			tsvout.writerow(newRow)
	

