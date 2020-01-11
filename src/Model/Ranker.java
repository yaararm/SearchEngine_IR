package Model;

import javafx.util.Pair;

import java.util.*;

import static java.util.stream.Collectors.toMap;

public class Ranker {

    private double avgDoclength;

    private double numOfDocumnetInCorpus;
    private HashMap<String, Double> rankedDocsByQueryBM25 = new HashMap<>();
    private HashMap<String, Double> rankedDocsTitle = new HashMap<>();
    private HashMap<String, Double> rankedDocsByDescriptionBM25 = new HashMap<>();

    private HashMap<String, Double> rankedDocs = new HashMap<>();

    private final double k1 = 1.8;
    private final double b = 0.95;
    private final double BM25_QUERY_WEIGHT = 0.5;
    private final double BM25_DESCRIPTION_WEIGHT = 0.2;
    private final double TITLE_WEIGHT = 0.3;


    Ranker() {
        this.numOfDocumnetInCorpus = Model.docs.size();
        this.avgDoclength = calculate_average_length();

    }

    public List<Pair<String, Double>> getRankedDocs() {

        //iterate over docs from query, calculate Total Rank and add it to rankedDocs
        for (String docID : rankedDocsByQueryBM25.keySet()) {
            double titleScore = rankedDocsTitle.get(docID);
            double queryBM25Score = rankedDocsByQueryBM25.get(docID);
            double descriptionBM25Score = (rankedDocsByDescriptionBM25.containsKey(docID)) ? rankedDocsByDescriptionBM25.get(docID) : 0;

            Double totalRank = TITLE_WEIGHT*titleScore  + BM25_QUERY_WEIGHT*queryBM25Score + BM25_DESCRIPTION_WEIGHT*descriptionBM25Score;
            rankedDocs.put(docID, totalRank);
        }

        //iterate over docs from description, calculate Total Rank and add it to rankedDocs
        for (String docID : rankedDocsByDescriptionBM25.keySet())  {
            //if not already calculated earlier
            if(!rankedDocs.containsKey(docID)){
                double descriptionBM25Score = rankedDocsByDescriptionBM25.get(docID);
                rankedDocs.put(docID, BM25_DESCRIPTION_WEIGHT*descriptionBM25Score);
            }
        }


        return sortRankedQurey(rankedDocs);

    }

    public void calculateQueryDescription(HashMap<String, List<Pair<Integer, Integer>>> relevantDocs) {
        rankedDocs.clear();
        rankedDocsByDescriptionBM25.clear();

        for (String docID : relevantDocs.keySet()) {

            double bm25Rank = scoreBM25ForDoc(docID, relevantDocs.get(docID));
            rankedDocsByDescriptionBM25.put(docID, bm25Rank);

        }
        //Normalize bm25 values according to best score (between 0-1)
        NormalizeBM25Score(rankedDocsByDescriptionBM25);

    }

    public void calculateQuery(HashMap<String, List<Pair<Integer, Integer>>> relevantDocs, Set<String> queryTerms) {

        rankedDocs.clear();
        rankedDocsByQueryBM25.clear();
        rankedDocsTitle.clear();

        for (String docID : relevantDocs.keySet()) {

            double bm25Rank = scoreBM25ForDoc(docID, relevantDocs.get(docID));
            rankedDocsByQueryBM25.put(docID, bm25Rank);

            double titleRank = scoreTitle(docID, queryTerms);
            rankedDocsTitle.put(docID, titleRank);

        }
        //Normalize bm25 values according to best score (between 0-1)
        NormalizeBM25Score(rankedDocsByQueryBM25);


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

    private double calculate_average_length() {
        double avg = 0;
        for (DocInfo value : Model.docs.values()) {
            avg += avg + value.getUniqeWords();
        }
        return avg / numOfDocumnetInCorpus;
    }

    private void NormalizeBM25Score(HashMap<String, Double> rankedDocsBM25) {
        double maxRankBM25 = Collections.max(rankedDocsBM25.values());

        for (String docID : rankedDocsBM25.keySet()) {
            Double normRank = rankedDocsBM25.get(docID) / maxRankBM25;
            rankedDocsBM25.put(docID, normRank);
        }
    }
}
