#!/usr/bin/python

import csv
import sys

mappings = {}

with open(sys.argv[1], 'rU') as csvin:
	tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07')
	for row in tsvin:
		mappings[row[1]] = row[2]


with open(sys.argv[2], 'rU') as csvin2:
	with open('output.tsv', 'wb') as csvout:
		tsvin2 = csv.reader(csvin2, delimiter='\t', quotechar='\x07')
		tsvout = csv.writer(csvout, delimiter='\t', quotechar='', quoting=csv.QUOTE_NONE)
		for row in tsvin2:
			newRow = []
			newRow.append(row[0])

			if row[1] in mappings:
				#print >> sys.stderr, "Match" + row[1]
				newRow.append(mappings[row[1]])
			else:
				continue
				#newRow.append(row[1])
			
			newRow.append(row[2])
			tsvout.writerow(newRow)				
