#!/usr/bin/python

import re
import sys

with open(sys.argv[1], 'rb') as f:
	lines = f.readlines()
	for line in lines:		
		mto = re.search(r".+\t.+\t(.+)@eng", line)
		if mto != None:
			literal = mto.group(1)		
			print line.strip().replace(literal + '@eng', '"' + literal + '"@eng')
		else:
			print line.strip()
