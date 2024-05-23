#!/usr/bin/python
# -*- coding: iso-8859-1 -*-

import sys
import re
import csv

#mode can be: date, uri, literal
mode = sys.argv[1]
months = {'january': '01', 'february' : '02', 'march': '03', 'april': '04', 'may': '05', 'june': '06', 'july': '07', 'august': '08', 'september': '09', 'october': '10', 'november': '11', 'december': '12'}

with open('idmb.tsv', 'wb') as csvout:
	for filename in sys.argv[2:]:
		tsvout = csv.writer(csvout, delimiter='\t', quotechar='', quoting=csv.QUOTE_NONE)
 
		with open(filename, 'rb') as csvin:
			tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07')
			for row in tsvin:
				#match the relation				
				predMatchObj = re.match( r'[a-zA-Z]+', row[0], re.M|re.I)
				pred = "<" + predMatchObj.group().replace(' ', '_') + ">"
				
				subMatchObj = re.match( r'([a-zA-Z]+)([0-9]+)', row[1], re.M|re.I)
				if subMatchObj != None:				
					sub = subMatchObj.group(2)
				else:
					sub = '<' + row[1] + '>'

				if mode == 'uri':				
					obj = "<" + row[2].replace(' ', '_') + ">"
				elif mode == 'date':
					dateMatchObj = re.match( r'[0-9]{4}', row[2], re.M|re.I)
					if dateMatchObj != None:
						obj = '"' + dateMatchObj.group() + '-##-##"^^xsd:date'
					else:
						dateMatchObj = re.match( r'([0-3]?[0-9])\s(january|february|march|april|may|june|july|august|september|october|november|december)\s([0-9]{4})', row[2], re.M|re.I)
						if dateMatchObj == None:
							continue
						day = dateMatchObj.group(1)						
						if len(dateMatchObj.group(1)) == 1:
							day = '0' + day
						obj = '"' + dateMatchObj.group(3) + '-' + months[dateMatchObj.group(2).lower()] + '-' + day  + '"^^xsd:date'
				elif mode == 'literal':
					if row[2][0] == '"' or row[2][0] == '\'':
						obj = row[2]
					else:
						obj = '\"' + row[2] + '\"@en'
				else:
					obj = row[2]
					
				
				outrow = []
				outrow.append(sub)
				outrow.append(pred)
				outrow.append(obj)
	
				tsvout.writerow(outrow)
