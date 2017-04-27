#!/usr/bin/python

import csv
import sets
import sys
from random import randint

def output(aList):
	with open('seeds.txt', 'wb') as out:
		for seed in aList:		
			out.write(seed + '\n')
		

def sampleSet(aSet, nSamples):	
	tmp = list(aSet)
	output = tmp[:nSamples]
	
	for i in range(nSamples + 1, len(tmp)):
		randVal = randint(0, i)
		if randVal > nSamples:
			output[randint(0, nSamples - 1)] = tmp[i]
	
	return output

if __name__ == '__main__':
	allSubjects = sets.Set()
	sampleSize = int(sys.argv[1])

	with open(sys.argv[2], 'rU') as csvin:
		tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07')
	
		nrow = 0	
		try:
			for row in tsvin:
				++nrow
				if len(row) >= 3:
					allSubjects.add(row[0])
		except Exception as e:
			print 'Row ' + str(nrow) + ' ' + str(e)

	#Now sample the array
	if sampleSize < 1 or sampleSize >= len(allSubjects):
		output([x for x in allSubjects])
	else:
		output(sampleSet(allSubjects, sampleSize))

