#!/usr/bin/python

import sys
import csv
import re

#The first argument contains the mappings. Build a dictionary id -> YAGO uri
mappings = {}
with open(sys.argv[1], 'rb') as csvin:
	tsvin = csv.reader(csvin, delimiter='\t')
	for row in tsvin:		
		mappings[row[1]] = row[0]

#The second file is the identifiers file. Build a second dictionary tmpid -> id
mappings2 = {}
with open(sys.argv[2], 'rb') as csvin:
	tsvin = csv.reader(csvin, delimiter='\t')
	for row in tsvin:
		idbmMatchObj = re.match( r'nm([0-9]+)', row[2], re.M|re.I)
		tmpidMatchObj =	re.match( r'(tt|p)([0-9]+)', row[1], re.M|re.I)	
		if idbmMatchObj == None or tmpidMatchObj == None:
			continue
		
		mappings2[tmpidMatchObj.group(2)] = idbmMatchObj.group(1)

#Now proceed with the join
with open(sys.argv[3], 'rb') as csvin:
	with open('join.tsv', 'wb') as csvout:
		tsvin = csv.reader(csvin, delimiter='\t')

		tsvout = csv.writer(csvout, delimiter='\t')
		for row in tsvin:
			output = []
			if row[0] not in mappings2 or mappings2[row[0]] not in mappings:
				continue
				
			output.append('<' + mappings[mappings2[row[0]]] + '>')
			output.append(row[1])
			output.append(row[2])
			
			tsvout.writerow(output)
