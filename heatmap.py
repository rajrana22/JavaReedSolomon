import os
import argparse
import numpy as np
import seaborn as sns; sns.set_theme()
import matplotlib.pylab as plt
import re

max_n = 16
max_k = 3
throughput_filename = "throughput"
heatmap_filename = "heatmap"

def run_benchmark(n, k, filename):
    os.system("./run_benchmark.sh -n " + str(n) + " -k " + str(k) + " -f " + filename)

def create_array():
    array = [[0 for n in range(max_n + 1)] for k in range(max_k + 1)]
    return array

def generate_single_point(n, k, data, filename):
    with open(filename) as f:
        lines = f.readlines()
    for key, line in enumerate(lines):
        if "Summary:" in line:
            desired_line = lines[key + 2]
    throughput = float(re.sub("[^0-9.]", "", desired_line))
    data[k+1][n+1] = throughput

def generate_data(data, filename):
    for n in range(max_n):
        for k in range(max_k):
            print(n)
            print(k)
            run_benchmark(n+1, k+1, filename)
            generate_single_point(n, k, data, filename)
            os.remove(filename)

def generate_heatmap(data):
    array = np.array(data, dtype=float)
    ax = sns.heatmap(array, linewidths=0.5, cmap="RePuOrYlGn")
    ax.invert_yaxis()


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
    max_k = args.k
    throughput_filename = args.b + ".log"
    heatmap_filename = args.o + ".png"

def main():
    parse_args()
    data = create_array()
    generate_data(data, throughput_filename)
    generate_heatmap(data)
    plt.show()

if __name__ == "__main__":
    main()