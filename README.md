# SearchEngine_IR
INTRODUCTION

This is our SearchEngine Project. It includes the following process:

Reading files from a given corpus, segmenting them into documents Parsing the corpus in batches of 50000 documents, one by one. 
The parsing could be executed with or without stemming.
Indexing the terms of each batch: 
creating posting files and writing information about the terms into them. 
In addition, we create a file containing information about all the parsed documents and a united dictionary for the entire corpus.

INFO

project name : EngineA java version 1.8.0

OPERATIONS

Part A-
Run the project jar file
Select a corpus path in the first text area by pressing browse
Select a posting files path
Click start to run processing the corpus

Part B-
Press browse button to select an index path
If the stemming option was selected on the corpus in part A,than select the stemming option again.
Press 'load Dictionary' button to load the dictionary to the memory
Type query in the Query text area, you can enable semantic option and press search or press browse button to select an text file path to enter some queries together, you can enable semantic optionan and press search button. -Now, wait until the searching process is finish, it will show you the results.
If you would like to choose document to see its entities. close the results window
and choose a query from choice menu, choose a document (according its id) from choice menu and press 'Get Entities' button.
If you would like to save the results, press browse button near the text field of 'Save the results at:' to select a path to save it, and press 'save' button.

POST PROCESSING

Reset button : clicking this button will delete all content in the selected posting files path

Load dictionary : will load the term dictionary to memory

Show dictionary : shows all the unique terms in the corpus with this total tf
