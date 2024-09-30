#!/usr/bin/python

import csv
import sys
from sets import Set

output = 'seeds.txt'
sampleSize = 10000

if len(sys.argv) < 2:
	sys.exit(1)

if len(sys.argv) > 2:
	output = sys.argv[2]

if len(sys.argv) > 3:
	sampleSize = int(sys.argv[3])

seedsSet = Set()
with open(sys.argv[1],'rb') as tsvin: 
	with open(output, 'wb') as csvout:
		tsvin = csv.reader(tsvin, delimiter='\t')

		for row in tsvin:
			print str(row)
			if len(row) < 3:
				continue
		    
			seedsSet.add(row[0])

		#Now apply reserviour sampling to take a sample
		finalSeeds = list(seedsSet)
		finalSeeds = finalSeeds[:sampleSize]
		for i in range(sampleSize + 1, len(finalSeeds)):
			csvout.write(finalSeeds[i] + "\n");
	 
