package Model;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.lang.Double.parseDouble;

public class SynonymFinder {


    private double maxValue;
    private double rate;
    SynonymFinder() {
        this.maxValue=120000;
        this.rate = 0.75;
    }

    public Set<String> getSetOfQuery(Set<String> queryWords) {
        ArrayList<Pair<String,Double>> scoredWords = new ArrayList<>();
        Set<String> synonyms  =  new HashSet<String>() ;

        try{
            for (String term:queryWords){
                Pair<String,Double> p = getIdentical(term);
                if (p != null){
                    scoredWords.add(p);
                }
            }
        }catch(Exception e){

        }
        if (scoredWords.size()>0){
            synonyms = checkScore(scoredWords);
        }
        return synonyms;
    }

    private  Pair<String, Double> getIdentical(String s) throws Exception {

        URL yahoo = new URL("https://api.datamuse.com/words?ml="+s);
        URLConnection yc = yahoo.openConnection();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        yc.getInputStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null)
           // System.out.println(inputLine);


        if (inputLine.toString().isEmpty() ||  inputLine.toString().contains("word")) {
            int w = inputLine.indexOf("word");
            int sc = inputLine.indexOf("score");
            int end = inputLine.indexOf(",", sc);
            String term = inputLine.substring(w + 7, sc - 3);
            String score = inputLine.substring(sc + 7, end);
           // System.out.println(s);
            //System.out.println("t:" + term + " s:" + score + "nirmul:" + (parseDouble(score) / 120000));
            //System.out.println(inputLine.toString());
            return new Pair<String, Double>(term, parseDouble(score));
        }
        in.close();
        return null;
    }

    private Set<String> checkScore ( ArrayList<Pair<String,Double>> scoredWords){
        Set<String> synonyms  =  new HashSet<String>() ;
        for (Pair <String,Double> p: scoredWords){
            if (p.getValue()/maxValue > rate){
                if (p.getKey().contains(" ")){
                    String [] arr = p.getKey().split(" ");
                        synonyms.addAll(Arrays.asList(arr));
                }
                else{
                    synonyms.add(p.getKey());
                }
            }
        }
        return synonyms;
    }
}