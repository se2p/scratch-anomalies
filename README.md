# Anomaly Detection in Scratch

Anomaly detection is a quick way to find erroneous scripts, alternative solutions or distinguished work in a set of Scratch programs which implement the same or similar tasks. To the best of our knowledge, this repository contains the first collection of tools for anomaly detection in Scratch: 
- An extension of [LitterBox](https://github.com/se2p/LitterBox/) that is used for the generation of script models - for the actor specific variant check out the branch ```script-model-generator-actor-specific```
- An adapted version of [JADET](https://www.st.cs.uni-saarland.de/models/jadet/) used for pattern and violation mining
- An adapted version of [OUMExtractor](https://www.st.cs.uni-saarland.de/models/jadet/), a dependency for LitterBox and JADET


# How to Mine Patterns and Anomalies
## Script Model Generation
LitterBox creates script models which are the input for JADET for both violation and pattern mining.
Use the [LitterBox jar](https://github.com/se2p/scratch-anomalies/blob/master/litterbox/Litterbox-1.4-SNAPSHOT.jar) to generate script models:
```
java -jar Litterbox-1.4-SNAPSHOT.jar --models --path <path to projects directory> --output <path to models directory>
```
Note that LitterBox deletes all files in the output directory before every run to keep models of different runs separated.

## Violations and Patterns
Linux and Mac: For all JADET-related steps use this [bash script](https://github.com/se2p/scratch-anomalies/blob/master/jadet/jadet).
### Violation Mining

To mine violations on Linux and Mac use 
```
./jadet -output-violations-xml -models-dir <path to models directory> 20 2 10000 0.9 <path to xml file>
```
<!--
On Windows use 
```

``` 
-->
The values 20 and 2 are our suggested default values for the minimum support 
(#scripts supporting a pattern) and minimum size (#temporal properties) of a pattern.
The value 10,000 defines the maximum deviation level (#missing temporal properties) 
and we strongly suggest to keep it at its default value. The last value sets the
threshold for the confidence of violations.  
If you want to have more anomalies reported, lower the minimum support or minimum
confidence values.

### Graphical User Interface
JADET has a GUI we adapted to work with Scratch scripts.  
To view the graphical representations of the violations you need to have [graphviz](https://graphviz.org/) installed.  
GUI Usage on Linux and Mac:
```
 ./jadet -edit-violations-xml <path to xml file>
```
<!--
On Windows (we do not support the graphical representation of the violations on Windows yet):
```

``` 
-->
 The GUI displays all violations found and offers means to classify violations 
 and compare the script a violation hints at with the scripts that support the 
 pattern. For all these scripts, we display scratchblocks output that can 
 conveniently be copy+pasted to [scratchblocks](https://scratchblocks.github.io)
 to have a look at the scripts. 

### Pattern Mining
JADET can be used to create a human-readable list of patterns of code.  
Linux and Mac:
```
 ./jadet -output-patterns -models-dir <path to models directory> 20 1 <path to patterns file>
```
<!--
Windows:  
```

```
-->
The values 20 and 1 are our suggested default values for the minimum support 
(#scripts supporting a pattern) and minimum size (#temporal properties) of a pattern.

To generate dotfile output for these patterns on Linux and Mac use
```
./jadet -visualize-all <path to directory for dotfiles> <path to patterns file> 
```
<!--
On Windows use 
```

```
-->
To render these patterns use
```
for i in {1..<num of patterns generated>}; do dot -Tpng -Gdpi=300 pattern$i.dot > ../<directory for pngs>/pattern$i.png; done
```
