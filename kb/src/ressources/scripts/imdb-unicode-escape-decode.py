#!/usr/bin/python

import csv
import sys
import codecs


with codecs.open(sys.argv[1], 'rb', 'utf8') as csvin:
	with open('output.tsv', 'wb') as csvout:
		#csvout.write(codecs.BOM_UTF8)
		lines = csvin.readlines()
		for line in lines:
			csvout.write(line.encode('utf8'))



'''with codecs.open(sys.argv[1], 'rb', 'utf8') as csvin:
	with open('output.tsv', 'wb') as csvout:
		csvout.write(codecs.BOM_UTF8)
		tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07')
		tsvout = csv.writer(csvout, delimiter='\t', quotechar='', quoting=csv.QUOTE_NONE)
		
		for row in tsvin:
			if len(row) >= 3:
				output = []			
				output.append(row[0].decode('raw-unicode-escape').encode('utf-8'))
				output.append(row[1])
				output.append(row[2].decode('raw-unicode-escape').encode('utf-8'))
				tsvout.writerow(output)'''
