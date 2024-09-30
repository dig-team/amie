#!/usr/bin/python

import sys
from sets import Set
import csv
import codecs

nHops = int(sys.argv[1])

if len(sys.argv) >= 5:
	direction = sys.argv[4]
else:
	direction = 'directed'

with open(sys.argv[2], 'rU') as fin:
	lines = fin.readlines()
	subjects = Set()
	for line in lines:
		subjects.add(line.rstrip('\n'))

	for i in range(nHops):
		with open(sys.argv[3], 'rU') as finin:
			#Scan the file and add the objects in the set
				tsvin = csv.reader(finin, delimiter='\t', quotechar='\x07')
				rowit = iter(tsvin)
				while True:
					try:
						row = next(rowit)
						added = False						
						if len(row) >= 3 and row[0] in subjects:
							subjects.add(row[2])
							added = True
						
						if not added and direction == 'undirected' and row[2] in subjects:
							subjects.add(row[0])
						
					except StopIteration as e:
						break
					except Exception as e:
						continue
				'''for row in tsvin:
					if len(row) >= 3:
						subjects.add(row[2]) '''

	#Now collect all triples for the subjects in the set
	#print subjects
	with open(sys.argv[3], 'rU') as finin:
		with open('sample.tsv', 'wb') as fout:
			tsvout = csv.writer(fout, delimiter='\t', quotechar='', quoting=csv.QUOTE_NONE)
			tsvin = csv.reader(finin, delimiter='\t', quotechar='\x07')			
			rowit = iter(tsvin)
			while True:
				try:
					row = next(rowit)
					if row[0] in subjects:
						tsvout.writerow(row)
				except StopIteration as e:
					break
				except Exception as e:
					continue
