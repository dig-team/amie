import csv
import sys
import re
import urllib

with open(sys.argv[1], 'rU') as csvin:
	tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07');
	for row in tsvin:
		if len(row) >= 3 :		
			try :
				subject = urllib.unquote(row[0]).decode('utf8')
				relation = urllib.unquote(row[1]).decode('utf8')
				nobject = urllib.unquote(row[2]).decode('utf8')
				print subject + "\t" + relation + "\t" + nobject
			except :
				continue
