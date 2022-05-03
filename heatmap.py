import os
import argparse
import numpy as np
import seaborn as sns; sns.set_theme()

max_n = 16
max_k = 3
throughput_filename = "throughput"
heatmap_filename = "heatmap"

def run_single_benchmark(n, k, filename):
    os.system("./run_benchmark.sh -n " + str(n) + " -k " + str(k) + " -f " + filename + ".log")


def run_full_benchmark(filename):
    for i in range(max_n):
        for j in range(max_k):
            run_single_benchmark(i+1, j+1, filename)

def generate_data(filename):
    with open(filename + ".log") as f:
        lines = f.readlines()
    data = []
    for i in range(max_n):
        for j in range(max_k):
            data[i][j] = 1
    for line in lines:
        ...
    np.array(data, dtype=float)


def generate_heatmap(data):
    ax = sns.heatmap(data)


def parse_args():
    global max_n
    global max_k
    global throughput_filename
    global heatmap_filename
    parser = argparse.ArgumentParser()
    parser.add_argument("-n", help="Number of data shards", default=max_n, type=int)
    parser.add_argument("-k", help="Number of parity shards", default=max_k, type=int)
    parser.add_argument("-b", help="Output filename for throughput benchmark", default=throughput_filename, type=str)
    parser.add_argument("-o", help="Output filename for heatmap", default=heatmap_filename, type=str)
    args = parser.parse_args()
    max_n = args.n
    max_n = args.k
    throughput_filename = args.b
    heatmap_filename = args.o

def main():
    parse_args()
    run_single_benchmark(max_n, max_k, throughput_filename)

if __name__ == "__main__":
    main()