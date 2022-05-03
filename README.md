# JavaReedSolomon
Backblaze Reed-Solomon Erasure Coding Implementation in Java

## Background:
- Uses a Reed-Solomon encoding matrix to construct parity bits.
- Uses the inverse matrix to reconstruct the original data.

## Simple Functionality:
- Construct a ReedSolomon “codec”
- Construct a byte matrix to simulate intact and failed shards
- Use codec.encodeParity() function to reconstruct the failed shards.

## Usage:
1. Break a file into shards.
2. Encode parity.
3. Take a subset of the shards and reconstruct the original file.

## Setup:
1. Clone the repo from https://github.com/Backblaze/JavaReedSolomon
2. Install the following dependencies:
  - Latest version of JDK (check updates).
  - Latest version of JRE (check updates).
  - Latest version of Gradle.
3. Open the build.gradle file and change “testCompile” to “testImplementation”
Why? Answer: https://github.com/Backblaze/JavaReedSolomon/issues/18
4. Run gradle build
5. Run ./gradlew run

## Benchmarking Notes:
- ReedSolomon loops over the data shards, parity shards, and bytes within the shards to perform reads and computations, and uses one of two multiplication methods.
- Three Loops:
  - “Byte”: Index of byte within shard (default = 200,000 bytes per shard)
  - “Input”: Which input shard is being read (default = 17 data shards)
  - “Output”: Which output shard is being computed (default = 3 parity shards)
- Multiplication Method for matrices:
  - “Table”: Multiplication table for the Galois (finite) Field
  - “Exp”: Logarithm/Exponents tables
- Performance of the inner loop depends on the specific processor you’re running on.
- There are 12 different permutations of the loop, some may be faster than others on a particular machine.
- Each permutation nests the three loops in a different order and uses one of the two multiplication methods.
- Number of parity shards, number of data shards and buffer sizes in the benchmark are adjustable.
