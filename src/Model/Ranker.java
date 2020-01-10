package Model;

import javafx.util.Pair;

import java.util.*;

import static java.util.stream.Collectors.toMap;

public class Ranker {

    private double avgDoclength;

    private double numOfDocumnetInCorpus;
    private HashMap<String, Double> rankedDocsBM25 = new HashMap<>();
    private HashMap<String, Double> rankedDocsTitle = new HashMap<>();
    public HashMap<String, Double> rankedDocs = new HashMap<>();

    private final double k1 = 1.2;
    private final double b = 0.75;
    private final double BM25_WEIGHT = 0.9;
    private final double TITLE_WEIGHT = 0.1;


    Ranker() {
        this.numOfDocumnetInCorpus = Model.docs.size();
        this.avgDoclength = calculate_average_length();

    }

    private double calculate_average_length() {
        double avg = 0;
        for (DocInfo value : Model.docs.values()) {
            avg += avg + value.getUniqeWords();
        }
        return avg / numOfDocumnetInCorpus;
    }

    public List<Pair<String, Double>> caclculateRateTDoc(HashMap<String, List<Pair<Integer, Integer>>> relevantdoc, Set<String> queryTerms) {

        rankedDocs.clear();
        rankedDocsBM25.clear();
        rankedDocsTitle.clear();

        for (String docID : relevantdoc.keySet()) {

            double bm25Rank = scoreBM25ForDoc(docID, relevantdoc.get(docID));
            rankedDocsBM25.put(docID,bm25Rank);

            double titleRank = scoreTitle(docID, queryTerms);
            rankedDocsTitle.put(docID,titleRank);

        }
        double maxRankBM25 = Collections.max(rankedDocsBM25.values());

        //Normelize bm25 values according to best score (between 0-1)
        for (String docID:rankedDocsBM25.keySet()){
            Double normRank = rankedDocsBM25.get(docID)/maxRankBM25;
            rankedDocsBM25.put(docID, normRank);
        }
        //rankedDocsBM25.values().forEach(v -> v=v/maxRankBM25);

        //iterate over docs, calculate Total Rank and add it to rankedDocs
        for (String docID:rankedDocsBM25.keySet()){
            Double totalRank = TITLE_WEIGHT* rankedDocsTitle.get(docID)  + BM25_WEIGHT * rankedDocsBM25.get(docID);
            rankedDocs.put(docID, totalRank);
        }




        return sortRankedQurey(rankedDocs);

    }



    private List<Pair<String, Double>> sortRankedQurey(HashMap<String, Double> rankedDocs) {
        List<Pair<String, Double>> ans = new ArrayList<Pair<String, Double>>();
        Map<String, Double> sorted = rankedDocs
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));

        int i = 1;
        for (Map.Entry<String, Double> entry : sorted.entrySet()) {
            if (i <= 50) {
                ans.add(new Pair<>(entry.getKey(), entry.getValue()));
                i++;
            } else
                break;
        }

        return ans;
    }

    private double scoreBM25ForDoc(String docName, List<Pair<Integer, Integer>> tf_df) {

        double score = 0;
        for (Pair p : tf_df) {
            double tf = new Double((Integer) p.getKey());
            double tfN = tf / Model.docs.get(docName).getMax_tf();
            double idf = Math.log((numOfDocumnetInCorpus - (int) p.getValue() + 0.5) / ((int) p.getValue() + 0.5));
            double mone = (idf * tfN * (k1 + 1)); // IDF*TF*K+1
            double mechane = tfN + (k1 * (1 - b + (b * ((Model.docs.get(docName).getUniqeWords()) / avgDoclength)))); // TF+k1* (1-b+b*(lenD/avgDoclength))
            score += mone / mechane;
        }
        return score;
    }

    private double scoreTitle(String docID, Set<String> queryTerms) {
        String docTitle = Model.docs.get(docID).getTitle().toLowerCase();
        double termsInTitleCounter = 0;
        for (String term : queryTerms) {
            if (docTitle.contains(term.toLowerCase()))
                termsInTitleCounter++;
        }

        double queryTermsInTitlePercents = termsInTitleCounter / queryTerms.size();

        return queryTermsInTitlePercents;

    }

    private double scoreDate(String docID, Set<String> queryTerms) {
        String docTitle = Model.docs.get(docID).getTitle().toLowerCase();
        double termsInTitleCounter = 0;
        for (String term : queryTerms) {
            if (docTitle.contains(term.toLowerCase()))
                termsInTitleCounter++;
        }

        double queryTermsInTitlePercents = termsInTitleCounter / queryTerms.size();

        return queryTermsInTitlePercents;

    }
}
