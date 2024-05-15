#!/usr/bin/python

import sys
import csv

#Read the files
mappings = {}
with open(sys.argv[1], 'rb') as csvin1:
	with open(sys.argv[2], 'rb') as csvin2:
		tsvin1 = csv.reader(csvin1, delimiter='\t')
		tsvin2 = csv.reader(csvin2, delimiter='\t')
		for row in tsvin2:
			mappings[row[1]] = row[2]
		
		#Now replace every occurrence of an id with the label
		with open('join.tsv', 'wb') as csvout:
			tsvout = csv.writer(csvout, delimiter='\t')
			for row in tsvin1:
				output = []
				if row[2] in mappings:
					output.append(row[0])
					output.append(row[1])
					output.append(mappings[row[2]])
					tsvout.writerow(output)
