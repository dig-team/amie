#!/usr/bin/python3
# -*- coding: utf-8 -*-

import csv
import sys
import urllib

# Print iterations progress
def printProgressBar (iteration, total, prefix = '', suffix = '', decimals = 1, length = 100, fill = '█'):
    """
    Call in a loop to create terminal progress bar
    @params:
        iteration   - Required  : current iteration (Int)
        total       - Required  : total iterations (Int)
        prefix      - Optional  : prefix string (Str)
        suffix      - Optional  : suffix string (Str)
        decimals    - Optional  : positive number of decimals in percent complete (Int)
        length      - Optional  : character length of bar (Int)
        fill        - Optional  : bar fill character (Str)
    """
    percent = ("{0:." + str(decimals) + "f}").format(100 * (iteration / float(total)))
    filledLength = int(length * iteration // total)
    bar = fill * filledLength + '-' * (length - filledLength)
    print('\r%s |%s| %s%% %s' % (prefix, bar, percent, suffix), end = '\r')
    # Print New Line on Complete
    if iteration == total: 
        print()

with open(sys.argv[1], 'r') as csvin:
  with open(sys.argv[2], 'w') as csvout:
    tsvin = csv.reader(csvin, delimiter='\t', quotechar='\x07')
    tsvout = csv.writer(csvout, delimiter='\t', quotechar='', quoting=csv.QUOTE_NONE)

    print("Checking grouping is ensured...")
    alreadyseen = {}
    current = False
    for row in tsvin:
      if (row[1] != current):
        current = row[1]
        assert(current not in alreadyseen)
        alreadyseen[current] = True

    nb_entity = len(alreadyseen)
    print("  OK - Found %d entities."%nb_entity)
    del(alreadyseen)
    current = False
    n = -1
    step = 1
    result = {}
    intermediate = []

    csvin.seek(0)
    for row in tsvin:
      if (row[1] != current):
        current = row[1]
        n += 1
        for i in intermediate:
          for j in intermediate:
            if i is j: continue
            if i not in result:
              result[i] = {}
            if j not in result[i]:
              result[i][j] = 1
            else:
              result[i][j] += 1
        intermediate = []
        printProgressBar(n, nb_entity, 'Computing:', 'Complete', length=50)
      intermediate.append(row[3])

    n += 1
    for i in intermediate:
      for j in intermediate:
        if i is j: continue
        if i not in result:
          result[i] = {}
        if j not in result[i]:
          result[i][j] = 1
        else:
          result[i][j] += 1
    printProgressBar(n, nb_entity, 'Computing:', 'Complete', length=50)
          
    for i in result:
      for j in result[i]:
        if result[i][j] >= 50:
          tsvout.writerow([i, j, result[i][j]])
