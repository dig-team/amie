## Evaluation script for link prediction. It takes as input a JSON configuration
## file with a list of job descriptions. Each description contains:
## 1. A rule file - as output by AMIE or miniAMIE
## 2. A description of the parameters sent to the miner to obtain the rules
import multiprocessing
import os
import subprocess
import sys
import json

from joblib import Parallel, delayed

OUTPUT_DIR = './results/inference/'
AMIE_ARGS = ['java', '-cp', 'bin/amie3.5.1.jar', 'amie.linkprediction.Evaluator']


def filename_from_config(json_config):
    miner = json_config['miner']
    min_support = 'min_support=' + str(json_config["mining_config"]['min_support'])
    min_std_conf = ''
    min_pca_conf = ''
    dataset = os.path.basename(os.path.normpath(json_config['dataset']))
    if 'min_std_confidence' in json_config["mining_config"]:
        min_std_conf += '_min_std_conf=' + str(json_config["mining_config"]['min_std_confidence'])
    if 'min_pca_confidence' in json_config["mining_config"]:
        min_pca_conf += '_min_pca_conf=' + str(json_config["mining_config"]['min_pca_confidence'])
    return f'linkprediction_{miner}_{dataset}{min_support}{min_std_conf}{min_pca_conf}.out'


def run_job(json_config: dict, n_configs: int) :
    global AMIE_ARGS
    global OUTPUT_DIR
    instantiated_amie_args = list(AMIE_ARGS)
    json_config.setdefault('n_jobs', multiprocessing.cpu_count())
    free_cpus = json_config['n_jobs'] - min(json_config['n_jobs'], n_configs) + 1
    n_threads = max(int(free_cpus), 1)
    instantiated_amie_args.extend([json_config['dataset'], json_config['rules_file'], str(n_threads)])
    fout_path = filename_from_config(json_config)
    print('Running job', file=sys.stderr)
    print(instantiated_amie_args, file=sys.stderr)
    completed_process = subprocess.Popen(instantiated_amie_args,
                                         stdout=subprocess.PIPE,
                                         stderr=subprocess.PIPE,
                                         text=True)
    try:
        sout, serr = completed_process.communicate()
        scores = json.loads(sout)
        print(scores)
        with open(f'{OUTPUT_DIR}{fout_path}', 'w') as fout:
            print(f'Writing output to file {OUTPUT_DIR}{fout_path}', file=sys.stderr)
            fout.write(sout)
    except Exception as e:
        print(e, file=sys.stderr)
        print(serr, file=sys.stderr)

if __name__ == '__main__':
    try:
        os.makedirs(OUTPUT_DIR, exist_ok=True)
    except Exception as e:
        print(f'An error just occurred: \n {e}')
    with open(sys.argv[1], 'r') as config_file:
        json_config = json.load(config_file)
        n_configs = len(json_config)
        n_jobs = multiprocessing.cpu_count() if len(sys.argv) < 3 else max(1, min(int(sys.argv[2]),
                                                                                 multiprocessing.cpu_count()))
        rule_batches = Parallel(n_jobs=n_jobs, prefer="processes")(
            delayed(run_job)(job_config, n_configs) for job_config in json_config
        )


