#!/usr/bin/env python3

## Evaluation script for link prediction. It takes as input a JSON configuration
## file with a list of job descriptions. Each description contains:
## 1. A rule file - as output by AMIE or miniAMIE
## 2. A description of the parameters sent to the miner to obtain the rules
import multiprocessing
import subprocess
import sys

AMIE_ARGS = ['java', '-cp', 'bin/amie3.5.1.jar', 'amie.linkprediction.Evaluator']
SCRIPT_ARGS = 'amie_linkprediction_evaluate.py <RULES_FILE> <SOURCE_DATASET> <OUTPUT_FILE> [N_JOBS]'

def run_job(json_config: dict) :
    global AMIE_ARGS
    instantiated_amie_args = list(AMIE_ARGS)
    n_threads = json_config['n_jobs']
    instantiated_amie_args.extend([json_config['dataset'], json_config['rules_file'], str(n_threads)])
    fout_path = json_config['output_file']
    print('Running job', file=sys.stderr)
    print(instantiated_amie_args, file=sys.stderr)
    completed_process = subprocess.Popen(instantiated_amie_args,
                                         stdout=subprocess.PIPE,
                                         stderr=subprocess.PIPE,
                                         text=True)
    try:
        sout, serr = completed_process.communicate()
        with open(f'{fout_path}', 'w') as fout:
            print(f'Writing output to file {fout_path}', file=sys.stderr)
            fout.write(sout)
    except Exception as e:
        print(e, file=sys.stderr)
        print(serr, file=sys.stderr)

if __name__ == '__main__':
    n_jobs = multiprocessing.cpu_count()
    if len(sys.argv) < 4:
        print("Insufficient arguments", file=sys.stderr)
        print(SCRIPT_ARGS, file=sys.stderr)
        sys.exit(1)

    if len(sys.argv) > 4:
        n_jobs = min(int(sys.argv[4]), n_jobs)

    with open(sys.argv[1], 'r') as config_file:
        job_config = {'n_jobs': n_jobs, 'rules_file': sys.argv[1], 'dataset': sys.argv[2], 'output_file': sys.argv[3]}
        run_job(job_config)


