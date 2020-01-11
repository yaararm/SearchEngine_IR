/**
 * this class reswponsible to find the entities of specific document
 */
package Model;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntitiesFinder {
    private String posting_Path;
    private TextParse tp;
    private static final Pattern DOC_Pattern = Pattern.compile("<DOC>(.+?)</DOC>", Pattern.DOTALL),
            DOC_ID_Pattern = Pattern.compile("<DOCNO>(.+?)</DOCNO>", Pattern.DOTALL),
            TEXT_Pattern = Pattern.compile("<TEXT>(.+?)</TEXT>", Pattern.DOTALL);

    public EntitiesFinder(String postingPath) {
        this.posting_Path = postingPath;
        this.tp = new TextParse();
    }

    /**
     *
     * @param docID
     * @return this function return top5 entity for the given docID
     */
    public List<Pair<String, Double>> getTop5Entities(String docID) {

        String docName = getDocName(docID);
        String fileName = getFileName(docID);

        File doc = findDocFile(fileName);
        String docText = getDocText(doc, docName);

        tp.parseText(docText, Model.isStem);

        Hashtable<String, Integer> entities = tp.getEntities();
        Hashtable<String, Integer> terms = tp.getTerms();

        //if term is upper case -> add it to entities map
        for (String term : terms.keySet()) {
            if (term.equals(term.toUpperCase())) {
                if (Character.isLetter(term.charAt(0))) {
                    if (Model.terms.containsKey(term)) //if really entity
                        entities.put(term, terms.get(term));
                }
            }
        }

        if (entities.size() == 0) {
            return null;
        }

        Hashtable<String, Double> rankedEntities = rankEntities(entities);
        List<Pair<String, Double>> sortedEntities = sortHashTable(rankedEntities);
        if (sortedEntities.size() >= 5) {
            return sortedEntities.subList(0, 5);
        } else return sortedEntities;
    }

    /**
     *
     * @param entities
     * @return this function rate the entities of the document
     */
    private Hashtable<String, Double> rankEntities(Hashtable<String, Integer> entities) {
        Hashtable<String, Double> ans = new Hashtable<>();

        int numOfEntitiesOccupancyInDoc = 0;
        //calculate total entities occupancy in doc
        for (int i : entities.values()) {
            numOfEntitiesOccupancyInDoc += i;
        }

        for (String ent : entities.keySet()) {
            double tf = new Double(entities.get(ent));
            int total_tf = 0;
            if (Model.terms.containsKey(ent)) {
                total_tf = Model.terms.get(ent).getValue();
            }


            double dominanceInDoc = tf / numOfEntitiesOccupancyInDoc;
            double dominanceInCorpus = tf / total_tf;
            if (dominanceInCorpus > 1)
                dominanceInCorpus = 0;

            double rank = 0.8 * dominanceInDoc + 0.2 * dominanceInCorpus;
            ans.put(ent, rank);
        }


        return ans;
    }

    /**
     *
     * @param f
     * @param docName
     * @return this function
     */
    private String getDocText(File f, String docName) {

        StringBuilder rawFileAsString = getFileAsText(f);

        Matcher m = DOC_Pattern.matcher(rawFileAsString);
        String text = "";
        while (m.find()) {
            String newDocAsString = m.group(1);

            String docId = getDataFromTag(DOC_ID_Pattern, newDocAsString);
            if (!docId.equals(docName))
                continue;

            text = getDataFromTag(TEXT_Pattern, newDocAsString);
            break;

        }
        return text;
    }

    /**
     *
     * @param f
     * @return the file as string
     */
    private StringBuilder getFileAsText(File f) {
        StringBuilder res = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                res.append(line).append("\n");
            }

        } catch (IOException io) {
            io.printStackTrace();
        }
        return res;
    }

    private String getFileName(String docID) {
        return Model.docs.get(docID).getFileName();
    }

    private String getDocName(String docID) {
        return Model.docs.get(docID).getDocName();

    }

    private String getDataFromTag(Pattern p, String Data) {
        Matcher m = p.matcher(Data);
        String output = "";
        if (m.find()) {
            output = m.group(1);
        }
        return output.trim();
    }

    /**
     *
     * @param map
     * @return  all the entitie of the document sort in decreasing order
     */
    private static List<Pair<String, Double>> sortHashTable(Hashtable<String, Double> map) {
        List<Pair<String, Double>> values = new ArrayList<>();
        map.forEach((k, v) -> values.add(new Pair<>(k, v)));

        values.sort((s1, s2) -> {
            return Double.compare(map.get(s2.getKey()), map.get(s1.getKey())); //reverse order
        });

        return values;
    }

    /**
     *
     * @param docName
     * @return the original from the corpus data set
     */
    private File findDocFile(String docName) {

        try (Stream<Path> walk = Files.walk(Paths.get(Model.corpus_Path))) {

            List<Path> result = walk.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().equals(docName))
                    .collect(Collectors.toList());


            File doc = new File(result.get(0).toString());
            return doc;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}

