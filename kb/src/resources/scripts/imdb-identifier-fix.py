#!/usr/bin/python

import csv
import sys

with open(sys.argv[1], 'rb') as csvin:
	with open('output.tsv', 'wb') as csvout:
		tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07')
		tsvout = csv.writer(csvout, delimiter='\t', quotechar='', quoting=csv.QUOTE_NONE)
		
		for row in tsvin:
			output = []
			output.append('<'+ row[0] + '>')
			#output.append(row[1])
			#output.append(row[2])
			tsvout.writerow(output)	
