#!/usr/bin/python

import csv
import sys
import re

mappings = {}
months = {'january': '01', 'february' : '02', 'march': '03', 'april': '04', 'may': '05', 'june': '06', 'july': '07', 'august': '08', 'september': '09', 'october': '10', 'november': '11', 'december': '12'}

mode = sys.argv[3]

with open(sys.argv[1], 'rU') as csvin:
	tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07');
	for row in tsvin:
		mappings[row[2]] = row[0]


with open(sys.argv[2], 'rU') as csvin2:
	with open('output.tsv', 'wb') as csvout:
		tsvin2 = csv.reader(csvin2, delimiter='\t', quotechar='\x07')
		tsvout = csv.writer(csvout, delimiter='\t', quotechar='', quoting=csv.QUOTE_NONE)
		for row in tsvin2:
			newRow = []			
			if row[0] in mappings:
				#print >> sys.stderr, "Match" + row[1]
				newRow.append(mappings[row[0]])
			else:
				newRow.append('<' + row[0] + '>')

			newRow.append('<' + row[1] + '>')
			
			row[2] = row[2].rstrip('.')			
			if mode == 'uri':
				if row[2] in mappings:
					newRow.append('<' + mappings[row[2]] + '>')
				else:
					newRow.append('<' + row[2] + '>')
			elif mode == 'literal':
				newRow.append('"' + row[2] + '"@eng')
			elif mode == 'date':
				dateMatchObj = re.match( r'([0-3]?[0-9])\s(january|february|march|april|may|june|july|august|september|october|november|december)\s([0-9]{4})', row[2], re.M|re.I)
				if dateMatchObj == None:
					continue
				day = dateMatchObj.group(1)						
				if len(dateMatchObj.group(1)) == 1:
					day = '0' + day
				obj = '"' + dateMatchObj.group(3) + '-' + months[dateMatchObj.group(2).lower()] + '-' + day  + '"^^xsd:date'
				newRow.append(obj)
			else:
				if row[2] in mappings:
					newRow.append(mappings[row[2]])
				else:
					furiMatchObj = re.match(r'ns:m\.[-_a-zA-Z0-9]', row[2], re.M|re.I)
					if furiMatchObj != None:					
						newRow.append('<' + furiMatchObj.group() + '>')
					else:
						newRow.append(row[2])
			
			tsvout.writerow(newRow)
