## Evaluation script for link prediction. It takes as input a JSON configuration
## file with a list of job descriptions. Each description contains:
## 1. A rule file - as output by AMIE or miniAMIE
## 2. A description of the parameters sent to the miner to obtain the rules
import multiprocessing
import subprocess
import sys
import json

from joblib import Parallel, delayed

AMIE_ARGS =  ['java', '-cp', 'amie.3.5.1.jar', 'amie.linkprediction.Evaluator']

def run_job(json_config: dict) :
    global AMIE_ARGS
    instantiated_amie_args = list(AMIE_ARGS)
    json_config.setdefault('n_jobs', multiprocessing.cpu_count())
    instantiated_amie_args.extend([json_config['dataset'], json_config['rules_file'], json_config['n_jobs']])
    try:
        completed_process = subprocess.run(args=instantiated_amie_args,
                                           stdout=subprocess.PIPE,
                                           stderr=subprocess.STDOUT,
                                           #timeout=get_setting('rule_timeout', 'amie'),
                                           text=True)
        scores = json.loads(completed_process.stdout.split('\n'))
        ##  Do something with the scores, e.g., format them
    except Exception as e:
        print(e, file=sys.stderr)
        return None

if __name__ == '__main__':
    with open(sys.argv[1], 'r') as config_file:
        json_config = json.load(config_file)
        n_jobs = multiprocessing.cpu_count() if len(sys.arg) < 3 else max(1, min(int(sys.argv[2]),
                                                                                 multiprocessing.cpu_count()))
        rule_batches = Parallel(n_jobs=n_jobs, prefer="processes")(
            delayed(run_job)(job_config) for job_config in json_config
        )


