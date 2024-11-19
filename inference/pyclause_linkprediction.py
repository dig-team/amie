from c_clause import RankingHandler, Loader
from clause.eval.evaluation import Ranking
from clause.data.triples import TripleSet
from clause import Options

import sys
import os

def get_evaluation_options():
   options = Options()
   options.set("ranking_handler.aggregation_function", "maxplus")
   options.set("ranking_handler.topk", 100)
   options.set("loader.load_u_d_rules", False)
   options.set("loader.load_u_xxc_rules", False)
   options.set("loader.load_u_xxd_rules", False)
   return options

def get_data_loader(evaluation_options):
   loader = Loader(options=evaluation_options.get("loader"))
   return loader

if __name__ == '__main__':
   if len(sys.argv) < 3:
      print('The program does not have enough arguments.')
      print('pyclause_linkprediction.py DATA_PATH RULES_FILE [EVALUATION_OUTPUT_FILE]', file=sys.stderr)
      sys.exit(1)

   ## First argument is the data path
   data_path = sys.argv[1].rstrip('/')
   ## Second argument is the file with the rules
   rules = sys.argv[2]

   train = f"{data_path}/train.tsv"
   filter_set = f"{data_path}/valid.tsv"
   target = f"{data_path}/test.tsv"

   options = get_evaluation_options()
   loader = get_data_loader(options)
   ## Load the data
   loader.load_data(data=train, filter=filter_set, target=target)
   loader.load_rules(rules=rules)

   ranker = RankingHandler(options=options.get("ranking_handler"))
   ranker.calculate_ranking(loader=loader)
   headRanking = ranker.get_ranking(direction="head", as_string=True)
   tailRanking = ranker.get_ranking(direction="tail", as_string=True)

   testset = TripleSet(target, encod="utf-8")
   ranking = Ranking(k=100)
   ranking.convert_handler_ranking(headRanking, tailRanking, testset)
   ranking.compute_scores(testset.triples)

   print("*** EVALUATION RESULTS ****")
   print("Num triples: " + str(len(testset.triples)))
   print("MRR     " + '{0:.6f}'.format(ranking.hits.get_mrr()))
   print("hits@1  " + '{0:.6f}'.format(ranking.hits.get_hits_at_k(1)))
   print("hits@3  " + '{0:.6f}'.format(ranking.hits.get_hits_at_k(3)))
   print("hits@10 " + '{0:.6f}'.format(ranking.hits.get_hits_at_k(10)))
   print()

   # now some code to some nice overview on the different relations and directions
   # the loop interates over all relations in the test set
   print("relation".ljust(25) + "\t" + "MRR-h" + "\t" + "MRR-t" + "\t" + "Num triples")
   for rel in testset.rels:
      rel_token = testset.index.id2to[rel]
      # store all triples that use the current relation rel in rtriples
      rtriples = list(filter(lambda x: x.rel == rel, testset.triples))

      # compute scores in head direction ...
      ranking.compute_scores(rtriples, True, False)
      (mrr_head, h1_head) = (ranking.hits.get_mrr(), ranking.hits.get_hits_at_k(1))
      # ... and in tail direction
      ranking.compute_scores(rtriples, False, True)
      (mrr_tail, h1_tail) = (ranking.hits.get_mrr(), ranking.hits.get_hits_at_k(1))
      # print the resulting scores
      print(rel_token.ljust(25) +  "\t" + '{0:.3f}'.format(mrr_head) + "\t" + '{0:.3f}'.format(mrr_tail) + "\t" + str(len(rtriples)))

   rules_basename = os.path.basename(rules)
   ranking_name = f'ranking_according_to_{rules_basename}'
   rules_location = os.path.dirname(rules)

   ## write the evaluation results in a JSON file
   # finally, write the ranking to a file, there are two ways to to this, both reults into the same ranking
   ranker.write_ranking(path=f'{rules_location}/{ranking_name}', loader=loader)