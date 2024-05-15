#!/usr/bin/python

import csv
import sys
import urllib

with open(sys.argv[1], 'rb') as csvin:
	with open(sys.argv[2], 'wb') as csvout:
		tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07')
		tsvout = csv.writer(csvout, delimiter='\t', quotechar='', quoting=csv.QUOTE_NONE)
		
		for row in tsvin:
			#finalRow = [urllib.unquote(record) for record in row[1:4]]
			finalRow = 	[urllib.unquote(record) for record in row]
			finalRow[2] = finalRow[2][:len(finalRow[2])-1]		
			tsvout.writerow(finalRow)
