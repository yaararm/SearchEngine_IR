package Model;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

//import com.fasterxml.jackson.databind.ObjectMapper;


public class HttpURLConnectionExample {

   // private final String USER_AGENT = "Mozilla/5.0";

    public static void main(String[] args) throws Exception {
        //getIdentical("car");
        /*HttpURLConnectionExample http = new HttpURLConnectionExample();

        Scanner sc = new Scanner(System.in);

        System.out.println("Write a word...");
        String wordToSearch = sc.next();

        http.searchSynonym(wordToSearch);
        */
        test();


    }

    public static void test() throws Exception {

        SynonymFinder af = new SynonymFinder();
        Set<String> hash_Set = new HashSet<String>();
       //hash_Set.add("winner");
       //hash_Set.add("cat");
       //hash_Set.add("car");
       //hash_Set.add("dog");
       //hash_Set.add("vehicle");
       //hash_Set.add("boat");
       //hash_Set.add("ship");
       //hash_Set.add("encryption");
       //hash_Set.add("implement");
       //hash_Set.add("ocean");
        String s = "Falkland petroleum exploration " +
                "British Chunnel impact " +
                "blood-alcohol fatalities " +
                "mutual fund predictors" +
                "human smuggling " +
                "piracy " +
                "encryption equipment export " +
                "Nobel prize getSemanticTreatment " +
                "cigar smoking " +
                "obesity medical treatment " +
                "space station moon " +
                "hybrid fuel cars " +
                "radioactive waste " +
                "organic soil enhancement " +
                "orphan drugs ";
        String [] arr = s.split(" ");
        System.out.println(Arrays.toString(arr));
        System.out.println(arr.length+" words");
       for (String st : arr) {
           hash_Set.add(st);
        }

        Set <String> ans = af.getSetOfQuery(hash_Set);
        System.out.println(Arrays.toString(ans.toArray()));
        System.out.println(ans.size());

    }


    // word and score attributes are from DataMuse API


    private static Pair<String, Double> getIdentical(String s) throws Exception {

        final String USER_AGENT = "Mozilla/5.0";
        HttpURLConnectionExample http = new HttpURLConnectionExample();
        String wordToSearch = s;
        //http.searchSynonym(wordToSearch);
        System.out.println("Sending request...");
       // String url = "https://api.datamuse.com/words?rel_syn=" + wordToSearch;
       // String url = "https://api.datamuse.com/words?rd=" + wordToSearch;
        String url = "https://api.datamuse.com/words?ml=" + wordToSearch;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = con.getResponseCode();
        // ordering the response
        StringBuilder response;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
        }
        System.out.println(s);
        if (response.toString().isEmpty() || response.toString().contains("word")) {
            int w = response.indexOf("word");
            int sc = response.indexOf("score");
            int end = response.indexOf(",",sc);
            //System.out.println(response.indexOf("word"));
            //System.out.println(response.indexOf("score"));
            //System.out.println(response.substring(w + 7, sc - 3));
            //System.out.println(response.substring(sc + 7, end));
            String term = response.substring(w + 7, sc - 3);
            String score = response.substring(sc + 7, end);
            System.out.println(response.toString());
            System.out.println("t:"+term+" s:"+score);
            //return new Pair<String, Double>(term, parseDouble(score));
            return null;
        }
        return null;
    }

    public static class URLConnectionReader {

        public static void main(String[] args) throws Exception {
            URL yahoo = new URL("https://api.datamuse.com/words?ml=cat");
            URLConnection yc = yahoo.openConnection();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            yc.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null)
                System.out.println(inputLine);
            in.close();
        }
    }
}