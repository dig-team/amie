from c_clause import RankingHandler, Loader
from clause.eval.evaluation import Ranking
from clause.data.triples import TripleSet
from clause import Options
import pandas as pd

# ***Example for Query Answering***

# define a knowledge graph
# alternatively, specify file path or use arrays + indices
#data = [
#    ("anna", "livesIn", "london"),
#    ("anna", "learns", "english"),
#    ("bernd", "speaks", "french")
#]

train = "/home/lgalarra/Documents/git/mm-kge/data/wn18rr/train.tsv"
filter_set = "/home/lgalarra/Documents/git/mm-kge/data/wn18rr/valid.tsv"
target = "/home/lgalarra/Documents/git/mm-kge/data/wn18rr/test.tsv"
rules = 'inference/rules-100'

#data = pd.read_csv(train, delimiter='\t', header=None, dtype={0: str, 1: str, 2: str})
#rules = pd.read_csv('inference/rules.tsv', delimiter='\t', header=None, dtype={0: int, 1: int, 2: float, 3: str})

options = Options()
options.set("ranking_handler.aggregation_function", "maxplus")
options.set("ranking_handler.topk", 100)
options.set("loader.load_u_d_rules", False)
options.set("loader.load_u_xxc_rules", False)
options.set("loader.load_u_xxd_rules", False)

loader = Loader(options=options.get("loader"))
loader.load_data(data=train, filter=filter_set, target=target)
loader.load_rules(rules='inference/rules-amie-constants')

ranker = RankingHandler(options=options.get("ranking_handler"))
ranker.calculate_ranking(loader=loader)
headRanking = ranker.get_ranking(direction="head", as_string=True)
tailRanking = ranker.get_ranking(direction="tail", as_string=True)

testset = TripleSet(target, encod="utf-8")
ranking = Ranking(k=100)

# process the handler ranking which is defined on queries and not
# on triples, e.g. assign to every triple of 'testset' the corresponding query rankings
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


# finally, write the ranking to a file, there are two ways to to this, both reults into the same ranking
ranker.write_ranking(path='inference/ranking', loader=loader)