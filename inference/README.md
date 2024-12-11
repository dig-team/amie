# AMIE's inference module

The 'inference' project contains the classes required to conduct a transductive link prediction evaluation using the
rules run by AMIE. 

## Running a link prediction evaluation

The evaluation is wrapped by the Python script amie_link_prediction_evaluate.py, which we can run as follows:

```
$ python amie_linkprediction_evaluate.py <RULES_FILE> <SOURCE_DATASET> <OUTPUT_FILE> [N_JOBS]
```
**Note**: This script assumes that (i) execution is done from the amie's project root directory and (ii) the binary executable 
AMIE jar bin/amie3.5.1.jar exists

The argument ``<RULES_FILE>`` is the path to a TSV file expected to contain rules as mined by AMIE or miniAMIE on the given dataset. 
An example of such a file is: 
``
Rule	Head Coverage	Standard Confidence	Pca Confidence	Support	Body Size	Pca Body Size	Functional Variable
?b  _verb_group  ?a   => ?a  _verb_group  ?b	0.931459	0.931459	0.980574	1060	1138	1081	-2
?b  _also_see  ?a   => ?a  _also_see  ?b	0.637413	0.637413	0.883671	828	1299	937	-2
?b  _hypernym  ?a   => ?a  _also_see  ?b	0.029253	0.001092	0.130137	38	34796	292	-2
``
It is crucial that those rules were mined from the dataset pointed by the ``<SOURCE_DATASET>`` attribute. The directory pointed 
there should contain at least two TSV files: train.tsv and test.tsv. The former is where the rules should 
have been mined. The latter is the one used for the evaluation. These file correspond to two subsets of a 
knowledge graph stored as a set of triples <subject, predicate, object>.

The optional attribute ``[N_JOBS]`` determines the level of parallelism used to execute the evaluation. Set it discretionally knowing
that by default it will use as many threads as supported by your system.

The results of the evaluation will be stored in the file pointed by the argument ``<OUTPUT_FILE>``.

## Using the results

As a result of a successful execution, the output file will contain a detailed description of the rules inference 
performance on the test subset of the source dataset. It is a JSON file that contains a single object with 
three main sections:
```
{
"headResults" : {"All" : {...}, "predicate1" : { "metric1" : value11, "metric2" : value12, ... }, "predicate2" : {...} }
"tailResults" : {...}
"bothResults" : {...}
}
```
Head results contains the evaluation performance of queries of the form (?, p, o). This originates from the ML notation
that calls the subject the "head" of a triple. Likewise tail results refer to the performance for queries (s, p, ?). "Both"
refers to the total performance for all queries. For each category of query the results are dissagregated by the value 
of the predicate 'p' in the query. The "All" category includes the aggregated results for all predicates.

The metrics computed by the evaluator are: Hits@1, Hits@3, Hits@5, Hits@10, and MRR (Mean Reciprocal Rank). They are offered
both in 'filtered' and 'non-filtered' versions.

Note: The filtered version is preferred nowadays because it excludes (filters) from the ranking those solutions 
that are known in the training set. That way we do not penalize our link prediction method for ranking already known 
solutions before an unknown (and correct) solution.

While all metrics are interesting, we will probably report aggregated ones on both sides, i.e., bothResults -> All. That said,
we could have sections of the paper where we include disaggregated values in order to provide some nuances for our results.