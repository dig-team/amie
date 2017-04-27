#!/usr/bin/python

import csv
import sys

joinCol1 = int(sys.argv[1])
joinCol2 = int(sys.argv[2])
tail1 = sys.argv[3].split(',')
tail2 = sys.argv[4].split(',')

if sys.argv[3] == '':
	tail1 = []


if sys.argv[4] == '':
	tail2 = []

with open(sys.argv[5], 'rb') as csvin1:
	with open(sys.argv[6], 'rb') as csvin2:
		with open('join.tsv', 'wb') as csvout:
			#Build a hash table with the elements of the second file
			hashTable = {}
			tsvin2 = csv.reader(csvin2, delimiter='\t', quotechar='\x07')
			for row in tsvin2:
				if row[joinCol2] not in hashTable:
					hashTable[row[joinCol2]] = [[row[int(i)] for i in tail2]]
				else:
					hashTable[row[joinCol2]].append([row[int(i)] for i in tail2])

			
			#Now proceed with the join
			tsvin1 = csv.reader(csvin1, delimiter='\t', quotechar='\x07')
			tsvout = csv.writer(csvout, delimiter='\t', quotechar='', quoting=csv.QUOTE_NONE)
			for row in tsvin1:
				if row[joinCol1] in hashTable:
					#Add all rows
					for subrow in hashTable[row[joinCol1]]:
						rowtail1 = [row[int(i)] for i in tail1]
						rowtail1.extend(subrow) 
						tsvout.writerow(rowtail1)
