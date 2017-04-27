#!/usr/bin/python

import sys
import os

if len(sys.argv) < 2 :
	print 'The program needs at least one input file'

kbPath = sys.argv[1]
tmpFile = 'keysNonKeysFile'
sakeyCmdLine = 'java -jar sakey.jar ' + sys.argv[1] + ' 1 > ' + tmpFile

print 'Running ' + sakeyCmdLine

if os.system(sakeyCmdLine) != 0 :
	print 'There was a problem at running SAKey'
	sys.exit(1)

with open(tmpFile) as kf :
	lines = kf.readlines()
	nonKeysLine = lines[0]
	startPos = nonKeysLine.find(": [[")
	if startPos == -1 :
		print 'There is a problem with the SAKey temporal file. Sequence =[['
		sys.exit(1)
	nonKeysSubstr = nonKeysLine[startPos + 4:len(nonKeysLine) - 3]	
	nonKeys = nonKeysSubstr.split("], [") 
	with open('nonKeysFile', 'w') as nonKeysFile :
		for nonKey in nonKeys :
                    if nonKey.find(',')!=-1:
			nonKeysFile.write(nonKey + "\n")


minSupport = 100
if len(sys.argv) > 2 :
	minSupport = int(sys.argv[2])	

heapSpace = '4G'
if len(sys.argv) > 3 :
	heapSpace = sys.argv[3]
	

amieCmdLine = 'java -XX:-UseGCOverheadLimit -Xmx' 
amieCmdLine += heapSpace + ' -jar amie.jar -bias conditionalKeys -dpr ' 
amieCmdLine += ' -htr "equals" -minpca 1 -mins ' + str(minSupport) 
amieCmdLine += ' -pm support -maxad 100 -nkf nonKeysFile ' + kbPath
amieCmdLine += ' > amieOutputFile '
			
print 'Running ' + amieCmdLine	
if os.system(amieCmdLine) != 0 :
	print 'There was a problem running AMIE'
	sys.exit(1)

with open('rules', 'w') as rulesFile :
	with open('amieOutputFile') as amieFile :
		amieLines = amieFile.readlines()	
		for amieLine in amieLines :
			if amieLine.startswith('?') :
				rulesFile.write(amieLine)

rulesToKeysCmd = 'java -cp amie.jar amie.keys.rulesToKeys ' + kbPath 
rulesToKeysCmd += ''' rules > conditionalKeys'''
print 'Running ' + rulesToKeysCmd
if os.system(rulesToKeysCmd) != 0 :
	print 'There was a problem converting the rules to keys'
	sys.exit(1)
