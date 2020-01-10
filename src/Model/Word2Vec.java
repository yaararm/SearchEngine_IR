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

    public Word2Vec() {
        //init model
        String modelFilePath = "Resources/word2vec.c.output.model.txt";
        try {
           model = Word2VecModel.fromTextFile(new File(modelFilePath));
            //model = Word2VecModel.fromBinFile(new File("C:\\Users\\YAARA\\Desktop\\checkFiles\\IR\\300.bin"));
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public Set<String> getSimilarTerms(String term) throws Searcher.UnknownWordException {
        List<Pair<String, Double>> similarTerms = new ArrayList<>();
        try {
            List<Match> matches = model.forSearch().getMatches(term, 4);
            for (Match m : matches) {
                String matchWord = m.match();
                double cosineDist = m.distance();
                similarTerms.add(new Pair<>(matchWord, cosineDist));

            }
                similarTerms.remove(0); //remove words itself

                Set<String> ans = new HashSet<>();
                similarTerms.forEach(p -> ans.add(p.getKey()));
                return ans;

        } catch (Searcher.UnknownWordException e) {
            return new HashSet<>();
        }


    }

    //test
    public static void main(String[] args) {
        Word2Vec testSemantics = new Word2Vec();
        double startTime = System.currentTimeMillis();
        try {
            Set<String> ssss = testSemantics.getSimilarTerms("explore");
            ssss.forEach(System.out::println);
            System.out.println("year: " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
        } catch (Exception e) {
            System.out.println(e);
        }

    }
}