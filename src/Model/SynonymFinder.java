package Model;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.lang.Double.parseDouble;

public class SynonymFinder {


    private final String USER_AGENT = "Mozilla/5.0";
    protected double maxValue;
    protected double rate;
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

        final String USER_AGENT = "Mozilla/5.0";

        HttpURLConnectionExample http = new HttpURLConnectionExample();

        String wordToSearch = s;
        //http.searchSynonym(wordToSearch);

        //System.out.println("Sending request...");
        String url = "https://api.datamuse.com/words?ml=" + wordToSearch;
       // String url = "https://api.datamuse.com/words?rel_syn=" + wordToSearch;
        //String url = "https://api.datamuse.com/words?rd=" + wordToSearch;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = con.getResponseCode();
        //System.out.println("\nSending request to: " + url);
        //System.out.println("JSON Response: " + responseCode + "\n");

        // ordering the response
        StringBuilder response;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
        }
        if (response.toString().isEmpty() ||  response.toString().contains("word")) {
            int w = response.indexOf("word");
            int sc = response.indexOf("score");
            int end = response.indexOf(",",sc);
            String term = response.substring(w + 7, sc - 3);
            String score = response.substring(sc + 7, end);
            //System.out.println(s);
            //System.out.println("t:"+term+" s:"+score + "nirmul:" +(parseDouble(score)/120000));
            //System.out.println(response.toString());
            return new Pair<String, Double>(term, parseDouble(score));
        }
        else{
            return null;
        }

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