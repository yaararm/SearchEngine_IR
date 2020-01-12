/**
 * this class get the query words and return relevant rate docs from the corpus
 */
package Model;


import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static Model.Model.isStem;

public class Searcher {
    private TextParse qParser;
    private Ranker ranker;
    private SynonymFinder synonymFinder;
    private Word2Vec w2v;
    private String posting_Path;

    //              DocId               tf       df
    private HashMap<String, List<Pair<Integer, Integer>>> relevantQueryDocs = new HashMap<>();
    private HashMap<String, List<Pair<Integer, Integer>>> relevantQueryDescriptionDocs = new HashMap<>();

    private Set<String> currentQueryTerms = new HashSet<>();


    public Searcher(String posting_Path)  {
        this.qParser = new TextParse();
        this.posting_Path = posting_Path;
        ranker = new Ranker();
        synonymFinder = new SynonymFinder();
        w2v = new Word2Vec();
    }

    /**
     *
     * @param query
     * @param queryDescription
     * @return the relevant docs with their score
     */
    public List<Pair<String, Double>> getRankedDocsFromQuery(String query,String queryDescription) {
        //process query and query description
        parseQuery(query);
        parseDescription(queryDescription);

        //calculate docs score for query and query description
        ranker.calculateQuery(relevantQueryDocs, currentQueryTerms);
        ranker.calculateQueryDescription(relevantQueryDescriptionDocs);

        return ranker.getRankedDocs();
    }

    /**
     * this method update score of relevant docs that found with description words
     * @param queryDescription
     */
    private void parseDescription(String queryDescription) {
        relevantQueryDescriptionDocs = new HashMap<>();

        //parse description
        qParser.parseText(queryDescription, isStem);

        //get terms and entities from parsed query
        Set<String> queryTerms = getCurrentQueryTerms();
        Set<String> queryEntities = getQueryEntities();

        readTermsInPostingToDict(queryTerms,queryEntities,false);

    }

    /**
     * this method update the score of each relevant doc according to user choice- word2vec model or synonym finder
     * @param query
     */
    private void parseQuery(String query) {
        //init relevant docs hash map for this query
        relevantQueryDocs = new HashMap<>();
        currentQueryTerms = new HashSet<>();

        //parse query
        qParser.parseText(query, isStem);

        //get terms and entities from parsed query
        Set<String> queryTerms = getCurrentQueryTerms();
        Set<String> queryEntities = getQueryEntities();

        Set<String> allTermsToCalculate = new HashSet<>(queryTerms);


        //add related term if semantic handle is on
        if(Model.isSemanticTreatment){
            // always parse w/o stem for w2v model
            if(isStem) qParser.parseText(query, false);
            Set<String> queryTermWOStem = qParser.getTerms().keySet();

            Set<String> similarWords = w2v.getSimilarTerms(queryTermWOStem);

            StringBuilder similarWordsAsString = new StringBuilder();
            similarWords.forEach(s -> similarWordsAsString.append(s).append(" "));

            //parse new similar terms
            qParser.parseText(new String(similarWordsAsString), isStem);
            Set<String> queryTermWithStem = qParser.getTerms().keySet();

            allTermsToCalculate.addAll(queryTermWithStem);
        }

        //add synonymous term if API synonymous is on
        if(Model.is_API_synonym){
            Set<String> synonymousTerms = getSynonymousTerms(queryTerms);
            allTermsToCalculate.addAll(synonymousTerms);
        }

        readTermsInPostingToDict(allTermsToCalculate,queryEntities,true);


        // relevantQueryDocs DS is now updated!
    }

    /**
     * this method read relevant posting file and extract their data
     * @param terms
     * @param entities
     * @param isQuery
     */
    private void readTermsInPostingToDict(Set<String> terms,Set<String> entities ,boolean isQuery) {


        //region handle regular terms:
        for (String term : terms) {
            if(isQuery) this.currentQueryTerms.add(term);
            term = findTermCaseInDic(term);
            if (term != null) {
                //get data for this term
                HashMap<String, Pair<Integer, Integer>> termData = readTermDataFromPosting(term);
                //insert data into relevant doc DS
                insertDataToRelevantDocsDS(termData,isQuery);
            }
        }
        //endregion

        //region handle Entities
        for (String entity : entities) {
            List<String> termsFromEntities = findEntityCaseInDic(entity);
            for (String term : termsFromEntities) {
                if(isQuery) this.currentQueryTerms.add(term);
                if (term != null) {
                    HashMap<String, Pair<Integer, Integer>> termData = readTermDataFromPosting(term);
                    insertDataToRelevantDocsDS(termData,isQuery);
                }
            }


        }
        //endregion
    }

    /**
     *
     * @param queryTerms
     * @return set of synonym terms
     */
    private Set<String> getSynonymousTerms(Set<String> queryTerms) {
        return synonymFinder.getSetOfQuery(queryTerms);
    }

    /**
     * this method fill the data (tf-df) for each documnet in query to the relvant
     * @param termData
     * @param isQuery
     */
    private void insertDataToRelevantDocsDS(HashMap<String, Pair<Integer, Integer>> termData, boolean isQuery) {
        for (Map.Entry<String, Pair<Integer, Integer>> entry : termData.entrySet()) {
            String docID = entry.getKey();
            Pair<Integer, Integer> tfdf = entry.getValue();

            HashMap<String, List<Pair<Integer, Integer>>> relevantDict = (isQuery) ? relevantQueryDocs:relevantQueryDescriptionDocs;
            //add data from this term to all relevant docs DS

            if (relevantDict.containsKey(docID)) { //docId exist
                relevantDict.get(docID).add(tfdf);
            } else { //add new relevant doc
                List<Pair<Integer, Integer>> newList = new ArrayList<>();
                newList.add(tfdf);
                relevantDict.put(docID, newList);
            }

        }
    }

    /**
     *
     * @param term
     * @return the term in his original form in dictionary
     */
    private String findTermCaseInDic(String term) {
        if (Model.terms.containsKey(term.toLowerCase()))
            return term.toLowerCase();
        if (Model.terms.containsKey(term.toUpperCase()))
            return term.toUpperCase();

        return null;
    }

    /**
     *
     * @param entity
     * @return list of entity for specific doc
     */
    private List<String> findEntityCaseInDic(String entity) {
        List<String> ans = new ArrayList<>();

        if (Model.terms.containsKey(entity)) //entity found in dictionary
            ans.add(entity);
        else { //entity not found -> split it to terms
            String[] terms = entity.split("-");
            for (String term : terms) {
                ans.add(findTermCaseInDic(term));
            }
        }
        return ans;
    }

    /**
     *
     * @return current Query Terms
     */
    private Set<String> getCurrentQueryTerms() {
        return new HashSet<>(qParser.getTerms().keySet());
    }

    /**
     *
     * @return QueryEntities
     */
    private Set<String> getQueryEntities() {
        return new HashSet<>(qParser.getEntities().keySet());
    }

    /**
     *
     * @param term
     * @return return list of <DocID,Pair<tf,df>> for one term
     */

    private HashMap<String, Pair<Integer, Integer>> readTermDataFromPosting(String term) {

        HashMap<String, Pair<Integer, Integer>> dataFromPosting = new HashMap<>();

        //find relevant posting file name
        String postingFileName = Model.terms.get(term).getKey();

        //String postingFileName ="A1";

        String folderPath = (Model.isStem) ? posting_Path + "\\WithStem\\Posting\\" : posting_Path + "\\WithOutStem\\Posting\\";
        String filePath = folderPath + postingFileName + ".txt";

        File postingFile = new File(filePath);

        try (BufferedReader br = new BufferedReader(new FileReader(postingFile))) {
            String line;
            while (!(line = br.readLine()).matches(term + ":\\d+")) {

            }
            //System.out.println(line);
            int df = Integer.valueOf(line.split(":")[1]);
            //System.out.println(df);
            for (int i = 0; i < df; i++) {
                line = br.readLine();
                String[] data = line.split(":");
                String docID = data[0];
                int tf = Integer.valueOf(data[1]);

                dataFromPosting.put(docID, new Pair<>(tf, df));
            }

        } catch (IOException io) {
            io.printStackTrace();
        }

        return dataFromPosting;
    }


}
