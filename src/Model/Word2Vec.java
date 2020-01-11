/**
 * this class use word2vec model from medallia/github to find related word to query words
 */
package Model;

import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Searcher.Match;
import com.medallia.word2vec.Word2VecModel;
import javafx.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Word2Vec {

    private Word2VecModel model;
    private double THRESHOLD = 0.975;

    public Word2Vec() {
        //init model
        String modelFilePath = "Resources/word2vec.c.output.model.txt";
        try {
            model = Word2VecModel.fromTextFile(new File(modelFilePath));
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * this method return set of related words to input set of word
     * @param queryWords
     * @return
     */
    public Set<String> getSimilarTerms(Set<String> queryWords) {
        List<Pair<String, Double>> similarTerms = new ArrayList<>();
        Set<String> ans = new HashSet<>();
        for (String term : queryWords) {
            try {
                List<Match> matches = model.forSearch().getMatches(term.toLowerCase(), 5);
                for (Match m : matches) {
                    String matchWord = m.match();
                    double cosineDist = m.distance();
                    if (cosineDist >= THRESHOLD && cosineDist < 1) {
                        similarTerms.add(new Pair<>(matchWord, cosineDist));
                        System.out.println(term+":   "+matchWord + ":" + cosineDist);
                    }
                //similarTerms.remove(0); //remove words itself

                }

                similarTerms.forEach(p -> ans.add(p.getKey()));

            } catch (Searcher.UnknownWordException e) {
                System.out.println("unknown word: " + term);
            }
        }
        return ans;

    }

    //test
    public static void main(String[] args) {
        Set<String> terms = new HashSet<>();
        terms.add("Falkland");
        terms.add("petroleum");
        terms.add("exploration");
        terms.add("British");
        terms.add("Chunnel");
        terms.add("impact");
        terms.add("blood-alcohol");
        terms.add("fatalities");
        terms.add("mutual");
        terms.add("fund");
        terms.add("predictors");
        terms.add("human");
        terms.add("smuggling");
        terms.add("piracy");
        terms.add("encryption");
        terms.add("equipment");
        terms.add("export");
        terms.add("Nobel");
        terms.add("prize");
        terms.add("winners");
        terms.add("cigar");
        terms.add("smoking");




        Word2Vec test = new Word2Vec();
        double startTime = System.currentTimeMillis();
        try {
            Set<String> ssss = test.getSimilarTerms(terms);
            //ssss.forEach(System.out::println);
            System.out.println( (System.currentTimeMillis() - startTime) / 1000 + " seconds");
        } catch (Exception e) {
            System.out.println(e);
        }

    }
}