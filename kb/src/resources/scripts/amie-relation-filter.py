#!/usr/bin/python

import sys
import csv

relations = sys.argv[1].split(',')

with open(sys.argv[2], 'rb') as csvin:
	with open('output.tsv', 'wb') as csvout:
		tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07')
		tsvout = csv.writer(csvout, delimiter='\t', quotechar='', quoting=csv.QUOTE_NONE)

		for row in tsvin:
			if len(row) == 3 and row[1] in relations:
				tsvout.writerow(row)		
