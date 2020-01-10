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
    private HashMap<String, List<Pair<Integer, Integer>>> relevantDocs = new HashMap<>();
    private Set<String> currentQueryTerms = new HashSet<>();


    public Searcher(String posting_Path)  {
        this.qParser = new TextParse();
        this.posting_Path = posting_Path;
        ranker = new Ranker();
        synonymFinder = new SynonymFinder();
        w2v = new Word2Vec();
    }

    public List<Pair<String, Double>> getRankedDocsFromQuery(String query) {
        updateRelevantDocsToRankFromQuery(query);
        List<Pair<String, Double>> rankedDocs = ranker.caclculateRateTDoc(relevantDocs, currentQueryTerms);
        return rankedDocs;
    }

    private void updateRelevantDocsToRankFromQuery(String query) {
        //init relevant docs hash map for this query
        relevantDocs = new HashMap<>();
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

        //read relevant posting file and extract their data
        //region handle regular terms:
        for (String term : allTermsToCalculate) {
            this.currentQueryTerms.add(term);
            term = findTermCaseInDic(term);
            if (term != null) {
                //get data for this term
                HashMap<String, Pair<Integer, Integer>> termData = readTermDataFromPosting(term);
                //insert data into relevant doc DS
                insertDataToRelevantDocsDS(termData);
            }
        }
        //endregion

        //region handle Entities
        for (String entity : queryEntities) {
            List<String> terms = findEntityCaseInDic(entity);
            for (String term : terms) {
                this.currentQueryTerms.add(term);
                if (term != null) {
                    HashMap<String, Pair<Integer, Integer>> termData = readTermDataFromPosting(term);
                    insertDataToRelevantDocsDS(termData);
                }
            }


        }
        //endregion



        // relevantDocs DS is now updated!
    }

    private Set<String> getSynonymousTerms(Set<String> queryTerms) {
        return synonymFinder.getSetOfQuery(queryTerms);
    }

    private void insertDataToRelevantDocsDS(HashMap<String, Pair<Integer, Integer>> termData) {
        for (Map.Entry<String, Pair<Integer, Integer>> entry : termData.entrySet()) {
            String docID = entry.getKey();
            Pair<Integer, Integer> tfdf = entry.getValue();

            //add data from this term to all relevant docs DS
            if (relevantDocs.containsKey(docID)) { //docId exist
                relevantDocs.get(docID).add(tfdf);
            } else { //add new relevant doc
                List<Pair<Integer, Integer>> newList = new ArrayList<>();
                newList.add(tfdf);
                relevantDocs.put(docID, newList);
            }

        }
    }

    private String findTermCaseInDic(String term) {
        if (Model.terms.containsKey(term.toLowerCase()))
            return term.toLowerCase();
        if (Model.terms.containsKey(term.toUpperCase()))
            return term.toUpperCase();

        return null;
    }

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

    private Set<String> getCurrentQueryTerms() {
        return new HashSet<>(qParser.getTerms().keySet());
    }

    private Set<String> getQueryEntities() {
        return new HashSet<>(qParser.getEntities().keySet());
    }

    // return list of <DocID,Pair<tf,df>> for one term
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
            System.out.println(line);
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
