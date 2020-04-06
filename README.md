# SearchEngine_IR
## INTRODUCTION

This is our Search Engine Project. It includes the following process:

Reading files from a given corpus, segmenting them into documents.

Parsing the corpus in batches of 50000 documents, one by one. 

The parsing could be executed with or without stemming.

Indexing the terms of each batch: 

creating posting files and writing information about the terms into them. 

In addition, we create a file containing information about all the parsed documents and a united dictionary for the entire corpus.

## INFO

project name : IR_SearchEngine java version 1.8.0

## OPERATIONS

<img src="/Resources/se.PNG" width="400" height="550" alt="graph flow example">

## Part A-
Run the project jar file
Select a corpus path in the first text area by pressing browse
Select a posting files path
Click start to run processing the corpus

Reset button : clicking this button will delete all content in the selected posting files path

Load dictionary : will load the term dictionary to memory

Show dictionary : shows all the unique terms in the corpus with this total tf 


## Part B-

Pre Query:
Press browse button to select an index path
If the stemming option was selected on the corpus in part A,than select the stemming option again.
Press 'load Dictionary' button to load the dictionary to the memory

Query:

Press browse button to select an text file path to enter some queries together, you can enable semantic option and press run Query. 


or

Type query in the Query text area, you can enable semantic option and press run Query

Now, wait until the searching process is finish, then press 'show result' to see the results.

If you would like to choose document to see its entities,stand on the specific document row and press button 'Show Entities'.



Post Query:

If you would like to save the results, press button 'save result'. 

