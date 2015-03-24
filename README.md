### SPMFWrapper

This is a WEKA package that wraps the [SPMF](http://www.philippe-fournier-viger.com/spmf/) data mining library (specifically, the association rule and item set mining algorithms).

```
SPMF is an open-source data mining mining library written in Java, specialized in pattern mining.

It is distributed under the GPL v3 license.

It offers implementations of 88 data mining algorithms for:

* sequential pattern mining,
* association rule mining,
* itemset mining,
* sequential rule mining,
* clustering.
```

### Usage

For example, to use SPMF's `Apriori` algorithm with a min support of 0.4:

```
java weka.Run weka.associations.SPMFWrapper -M Apriori -P 0.4 -t <file>.arff
```

The `-M` parameter specifies the algorithm, and `-P` the parameters to the algorithm. This [link](http://www.philippe-fournier-viger.com/spmf/index.php?link=documentation.php) is useful as it lets you know how many parameters each algorithm takes and what those parameters are.

### Installation

If you have `ant`, `cd` into the root directory and run:

```
ant -Dpackage=spmfWrapper make_package`
```

Then go to `dist` and use the WEKA package manager to install `spmfWrapper.zip`.

### Issues

* Not all association / item-set mining algorithms work. This is because some of the algorithms take in special datasets that can't be represented in ARFF (or so I believe).
* I wasn't able to "elegantly" pass the currently-loaded ARFF into SPMF so it writes the ARFF to the OS's temp directory and gets SPMF to load it there. That means that on a large dataset things might get quite slow as it has to be saved to disk and then read in again by SPMF.
