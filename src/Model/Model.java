/*
  this class connect the ui to the program
 */
package Model;

import javafx.scene.control.Alert;
import javafx.stage.StageStyle;
import javafx.util.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Model extends Observable {

    //region Class Fields
    //------------Static Fields----------------------------
    //                         term     postingfile name,total_tf
    static ConcurrentHashMap<String, Pair<String, Integer>> terms = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, DocInfo> docs = new ConcurrentHashMap<>();


    //protected static String corpus_Path;
    protected static String corpus_Path;

    protected static String posting_Path;
    protected static boolean isStem;

    //part B
    static boolean is_one_query;

    protected static boolean isSemanticTreatment;
    protected static boolean is_API_synonym;

    protected HashMap<String, List<Pair<String, Double>>> multiQueryResult = new HashMap<>();
    protected HashMap<String, List<Pair<String, Double>>> sortedResultDocName = new HashMap<>();
    protected HashMap<String, String> fromDocNameToDocID= new HashMap<>();

    //endregion

    public Model() {

    }
    //region PartA
    /**
     * this function start the creation of the inverted index
     */
    public void startToIndex() {

        Alert alert2 = new Alert(Alert.AlertType.INFORMATION);
        alert2.setTitle("Indexing...");
        alert2.setContentText("Indexing...\nPlease wait");
        alert2.setHeaderText(null);
        alert2.show();

        long startTime = System.currentTimeMillis();

        //---------init dictionary in memory--------------------
        docs = new ConcurrentHashMap<>();
        terms = new ConcurrentHashMap<>();
        Parse.DocCounter = 0;
        Parse.moreDocsToBeRead = true;

        //----------create folders in Dest folder-------------
        createDirectoriesForIndexing();

        //----------init FileReader---------------------------
        runReadFile();

        //----------init 4 threads of parser and wait for it to end------------------
        runParsers();

        //----------init 4 threads of Indexers and wait for them to end---------------------
        runIndexers();

        //-----------delete junk terms----------------------
        //region remove junk words
        terms.remove("CELLRULE");
        terms.remove("ROWRULE");
        terms.remove("TABLECELL");
        terms.remove("CHJ");
        terms.remove("CVJ");
        terms.remove("mr");
        terms.remove("TABLECELL-CHJ-CVJ-C");
        terms.remove("TABLECELL-CHJ-R-CVJ-C");
        terms.remove("F-P");
        //endregion

        //-----------save indexing time--------------------
        long stopTime = System.currentTimeMillis();
        long elp = (stopTime - startTime);
        int minutes = (int) ((elp / (1000.0 * 60)) % 60);
        int seconds = (int) ((elp / 1000.0) - (minutes * 60));

        //----------------save terms dictionary to file----------
        saveDictionaryFiles();
        System.out.println("finish saving term and doc dictionary to file");

        //----------------delete TempFiles----------------
        deleteTempFiles();

        //----------------reset static vars----------------
        int docCount = Parse.DocCounter;
        Parse.DocCounter = 0;
        Parse.moreDocsToBeRead = true;

        //-----------show message with indexing info------------
        alert2.close();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information about the Dictionary");
        alert.setHeaderText("Information");
        alert.setContentText("Numbers od docs in index: " + docCount + "\n" + "Numbers of Terms: " + terms.size() + "\n" + "Duration: " + minutes + " min and " + seconds + " sec");
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();


        //---------Export Dictionary as csv-------------------
        //ExportToCsv();

    }

    public ConcurrentHashMap<String, Pair<String, Integer>> getDictionary() {
        return terms;
    }

    /**
     * this function set the path of the corpus
     *
     * @param path corpus path
     */
    public void setCorpusPath(String path) {
        corpus_Path = path;
    }

    /**
     * this function set the path of the temp files, posting files, dictionary
     *
     * @param path posting path
     */
    public void setPostingPath(String path) {
        posting_Path = path;
    }

    /**
     * this function deletes the temp files we save
     */
    private void deleteTempFiles() {

        String stemTempFoldferPath = posting_Path + "\\WithStem\\Temp";
        String WOstemTempFoldferPath = posting_Path + "\\WithOutStem\\Temp";

        Path pathStem = Paths.get(stemTempFoldferPath);
        Path pathWOStem = Paths.get(WOstemTempFoldferPath);

        try { //delete al folder and sub folders and files
            Files.walk(pathStem)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            Files.walk(pathWOStem)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * this function save the dictionary files
     */
    private void saveDictionaryFiles() {
        String filePath = (isStem) ? posting_Path + "\\WithStem\\Dictionary\\" : posting_Path + "\\WithOutStem\\Dictionary\\";
        try {

            FileOutputStream fileOut = new FileOutputStream(filePath + "terms" + ".dic"); // add is stem
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(terms);
            objectOut.flush();
            objectOut.close();
            FileOutputStream fileOut1 = new FileOutputStream(filePath + "docs" + ".dic"); // add is stem
            ObjectOutputStream objectOut1 = new ObjectOutputStream(fileOut1);
            objectOut1.writeObject(docs);
            objectOut1.flush();
            objectOut1.close();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * this function load the dictionary files from the disk
     */
    public void loadDictionaryFromDisk() {
        String filePath = (isStem) ? posting_Path + "\\WithStem\\Dictionary\\" : posting_Path + "\\WithOutStem\\Dictionary\\";
        try {
            FileInputStream is = new FileInputStream(filePath + "terms" + ".dic");
            ObjectInputStream ois = new ObjectInputStream(is);
            System.out.println("starting reading term dic...");
            terms = (ConcurrentHashMap<String, Pair<String, Integer>>) ois.readObject();
            System.out.println("finish raeding term dictionary");
            ois.close();
            is.close();
            FileInputStream is2 = new FileInputStream(filePath + "docs" + ".dic");
            ObjectInputStream ois2 = new ObjectInputStream(is2);
            System.out.println("starting reading docs dic...");
            docs = (ConcurrentHashMap<String, DocInfo>) ois2.readObject();
            System.out.println("finish raeding docs dictionary");
            ois2.close();
            ois2.close();
            ExportToCsv();
            System.out.println("finish append to dictionary");

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(" ");
            alert.setHeaderText(null);
            alert.setContentText("Could not find any Dictionary in this path");
            alert.showAndWait();
        }
        Alert alert2 = new Alert(Alert.AlertType.INFORMATION);
        alert2.setTitle("Finish loading terms dictionary");
        alert2.setHeaderText(null);
        alert2.setContentText("now you can display it!");
        alert2.showAndWait();

    }

    /**
     * this function deletes the memory of the program
     *
     * @return if reset was performed
     */
    public int resetProcess() {
        boolean didDeletePosting = false;
        boolean didDeleteDicts = false;

        //---------delete all directory-------------------
        String stemFolderPath = posting_Path + "\\WithStem";
        String WOatenFolderPath = posting_Path + "\\WithOutStem";
        Path pathStem = Paths.get(stemFolderPath);
        Path pathWOStem = Paths.get(WOatenFolderPath);

        try { //delete al folder and sub folders and files
            Files.walk(pathStem)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            Files.walk(pathWOStem)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            didDeletePosting = true;
        } catch (Exception e) {
            System.out.println(e);        }

        //-----------------delete pathes-------------------------
        posting_Path = "";
        corpus_Path = "";

        //----------delete Parse data structures
        Parse.stopWordList = new ArrayList<>();
        Parse.DocumentsQueue = new ArrayList<Document>();
        Parse.DocCounter = 0;
        Parse.moreDocsToBeRead = true;


        //------------delete all of data structures------------
        if (!terms.isEmpty()) {
            terms = new ConcurrentHashMap<>();
            didDeleteDicts = true;
        }
        if (!docs.isEmpty()) {
            docs = new ConcurrentHashMap<String, DocInfo>();
            didDeleteDicts = true;
        }
        if (!sortedResultDocName.isEmpty()) {
            sortedResultDocName = new HashMap<>();
            didDeleteDicts = true;
        }
        if (!multiQueryResult.isEmpty()) {
            multiQueryResult = new HashMap<>();
            didDeleteDicts = true;
        }
        if (!fromDocNameToDocID.isEmpty()) {
            fromDocNameToDocID = new HashMap<>();
            didDeleteDicts = true;
        }

        int wahtWasDeleted = 0;

        if(didDeleteDicts && didDeletePosting) wahtWasDeleted =3;
        else if(!didDeleteDicts && !didDeletePosting) wahtWasDeleted= 0;
        else wahtWasDeleted= (didDeleteDicts) ? 1:2;

        //what was deleted:
        //0= nothing
        //1 = only dicts
        //2 = only posting files
        //3 = both dict and posting files
        return wahtWasDeleted;
    }

    /**
     * this function export the dictionary to csv files
     */
    private void ExportToCsv() {
        String numbers = "0123456789";
        List<String> sortedKeys = Collections.list(terms.keys());
        //Collections.sort(sortedKeys);
        //find the index for abc
        StringBuilder biggerThanOne = new StringBuilder();
        StringBuilder onlyOneCount = new StringBuilder();

        for (String s : sortedKeys) {
            int n = terms.get(s).getValue();

            s = s.replace(",", "#");
            if (n > 1) {
                biggerThanOne.append(s).append(",").append(n).append("\n");
            } else {
                onlyOneCount.append(s).append(",").append(n).append("\n");
            }


        }
        try {
            String filePath = (isStem) ? posting_Path + "\\WithStem\\Dictionary\\" : posting_Path + "\\WithOutStem\\Dictionary\\";
            PrintWriter writer = new PrintWriter(new File(filePath + "biggerThanOne.csv"));
            writer.append(biggerThanOne);
            writer.flush();
            writer.close();
            PrintWriter writer2 = new PrintWriter(new File(filePath + "onlyOneCount.csv"));
            writer2.append(onlyOneCount);
            writer2.flush();
            writer2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this function set the stemming parameter
     *
     * @param is_Stemming is_stem
     */
    public void setStemming(boolean is_Stemming) {
        isStem = is_Stemming;
    }

    /**
     * this function creates the directories for all the files we save
     */
    private void createDirectoriesForIndexing() {


        String DirectoryPath = posting_Path + "\\";
        try {
            //create stem Folders
            File dirStem = new File(DirectoryPath + "WithStem");
            File dirDicStem = new File(DirectoryPath + "WithStem\\Dictionary");
            File dirPostingStem = new File(DirectoryPath + "WithStem\\Posting");
            File dirTempStem = new File(DirectoryPath + "WithStem\\Temp");
            File dirNumbersStem = new File(DirectoryPath + "WithStem\\Temp\\numbers");
            dirStem.mkdirs();
            dirDicStem.mkdirs();
            dirPostingStem.mkdirs();
            dirTempStem.mkdirs();
            dirNumbersStem.mkdirs();

            //create no stem folders
            File dirWOStem = new File(DirectoryPath + "WithOutStem");
            File dirDicWOStem = new File(DirectoryPath + "WithOutStem\\Dictionary");
            File dirWOPostingWOStem = new File(DirectoryPath + "WithOutStem\\Posting");
            File dirWOTempWOStem = new File(DirectoryPath + "WithOutStem\\Temp");
            File dirNumbersWOStem = new File(DirectoryPath + "WithOutStem\\Temp\\numbers");
            dirWOStem.mkdirs();
            dirDicWOStem.mkdirs();
            dirWOPostingWOStem.mkdirs();
            dirWOTempWOStem.mkdirs();
            dirNumbersWOStem.mkdirs();

            for (char c = 'A'; c <= 'Z'; c++) {
                File lettersStem = new File(DirectoryPath + "\\WithStem\\Temp\\" + c);
                File lettersWOStem = new File(DirectoryPath + "\\WithOutStem\\Temp\\" + c);
                lettersStem.mkdirs();
                lettersWOStem.mkdirs();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this function call the read file thread to begin its running
     */
    private void runReadFile() {
        ReadFile rf = new ReadFile(corpus_Path);
        rf.start();
    }

    /**
     * this function call the parsers threads to start parsing
     */
    private void runParsers() {
        Parse.setIsStem(isStem);

        Thread p1 = new Parse(posting_Path, corpus_Path);
        p1.setName("1");
        p1.start();
        Thread p2 = new Parse(posting_Path, corpus_Path);
        p2.setName("2");
        p2.start();
        Thread p3 = new Parse(posting_Path, corpus_Path);
        p3.setName("3");
        p3.start();
        Thread p4 = new Parse(posting_Path, corpus_Path);
        p4.setName("4");
        p4.start();

        try {
            p1.join();
            p2.join();
            p3.join();
            p4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * this function call the indexer threads to start indexing
     */
    private void runIndexers() {
        String[] s1 = {"A", "B", "C", "D"};
        String[] s2 = {"E", "F", "G", "H", "I", "J", "K", "L"};
        String[] s3 = {"M", "N", "O", "P", "Q", "R"};
        String[] s4 = {"S", "T", "U", "V", "W", "X", "Y", "Z", "numbers"};

        Indexer i1 = new Indexer(posting_Path, s1, isStem);
        Indexer i2 = new Indexer(posting_Path, s2, isStem);
        Indexer i3 = new Indexer(posting_Path, s3, isStem);
        Indexer i4 = new Indexer(posting_Path, s4, isStem);

        i1.start();
        i2.start();
        i3.start();
        i4.start();


        try {
            i1.join();
            i2.join();
            i3.join();
            i4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
//endregion

    //region Part B

    /**
     * set use in synonym api
     * @param is_click
     */
    public void is_API_synonym(boolean is_click) {
        is_API_synonym = is_click;
    }

    /**
     * set use in word2vec model
     * @param isSemantic
     */
    public void semanticTreatment(boolean isSemantic) {
        isSemanticTreatment = isSemantic;
    }

    /**
     * this method start to query search
     * @param isOneQuery
     * @param q
     */
    public void runQuery(boolean isOneQuery, String q) {
        fromDocNameToDocID = new HashMap<>();
        if (isOneQuery) {

            Searcher searcher = new Searcher(posting_Path);
            HashMap<String, List<Pair<String, Double>>> MultyQueryResult = new HashMap<>();
            List<Pair<String, Double>> ans = searcher.getRankedDocsFromQuery(q, "");
            for (Pair<String, Double> p : ans) {

                fromDocNameToDocID.put((docs.get(p.getKey())).getDocName(), p.getKey());
            }
            MultyQueryResult.put("123", ans);
            is_one_query = true;
            this.multiQueryResult = MultyQueryResult;
            this.sortedResultDocName = sortAndUpdateResult();
        } else { // multy
            Searcher searcher = new Searcher(posting_Path);
            is_one_query = false;
            HashMap<String, Pair<String, String>> MultyQuery = parseMultiQuery(q); // pair of String query, description

            HashMap<String, List<Pair<String, Double>>> MultyQueryResult = new HashMap<>();

            for (Map.Entry<String, Pair<String, String>> entry : MultyQuery.entrySet()) {
                //long s = System.currentTimeMillis();
                List<Pair<String, Double>> ans = searcher.getRankedDocsFromQuery(entry.getValue().getKey(), entry.getValue().getValue());
                System.out.println("finish run query #"+entry.getKey());
                // long f = System.currentTimeMillis();
                // long dur = f - s;
                // System.out.println(entry.getKey() + ": " + dur);
                MultyQueryResult.put(entry.getKey(), ans);
                for (Pair<String, Double> p : ans) {
                    fromDocNameToDocID.put((docs.get(p.getKey())).getDocName(), p.getKey());
                }
            }
            this.multiQueryResult = MultyQueryResult;
            this.sortedResultDocName = sortAndUpdateResult();


        }
    }

    /**
     * this method parse the query file and split all the queries
     * @param path
     * @return all the query with description
     */
    private HashMap<String, Pair<String, String>> parseMultiQuery(String path) {
        File multiQuery = new File(path); // current directory
        LinkedHashMap<String, Pair<String, String>> extractQuery = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(multiQuery))) {
            String line;
            String[] query = new String[3];
            query[0] = "";
            query[1] = "";
            query[2] = "";
            String desc = "";
            boolean descLine = false;
            while ((line = br.readLine()) != null) {

                if (line.contains("<num> Number:")) {
                    query[0] = line.substring(13).trim();
                }
                if (line.contains("<title>")) {
                    query[1] = line.substring(7).trim();
                }
                if (descLine) {
                    desc += line;
                    if (line.contains("<narr>")) {
                        descLine = false;
                        int n = desc.indexOf("<narr>");
                        //query[2] = cleanDescription(desc.trim().substring(0, n));
                        query[2] = desc.trim().substring(0, n);
                        desc = "";

                    }
                }
                if (line.contains("<desc> Description:")) {
                    descLine = true;

                }

                if (query[0].compareTo("") != 0 && query[1].compareTo("") != 0 && query[2].compareTo("") != 0 && !descLine) { // not empty
                    extractQuery.put(query[0], new Pair(query[1], query[2]));
                    // System.out.println("num: "+query[0]+" title: "+query[1]);
                    //System.out.println("desc: " + query[2]);
                    query[1] = "";
                    query[0] = "";
                    query[2] = "";
                }
            }

        } catch (IOException io) {
            io.printStackTrace();
        }
        //Set <String> set = extractQuery.keySet();


        return extractQuery;
    }

    /**
     *
     * @param desc
     * @return the description of the query without unnecessary words
     */
    private String cleanDescription(String desc) {

        if (desc.toLowerCase().contains("identify documents that discuss")) {
            desc = desc.toLowerCase().replace("identify documents that discuss", "");
            //return desc;
        }
        if (desc.toLowerCase().contains("find documents that discuss")) {
            desc = desc.toLowerCase().replace("find documents that discuss", "");
            //return desc;
        }
        if (desc.toLowerCase().contains("documents")) {
            desc = desc.toLowerCase().replace("documents", "");

        }
        if (desc.toLowerCase().contains("identify")) {
            desc = desc.toLowerCase().replace("identify", "");

        }
        return desc;
    }

    /**
     * this function sort the query result
     * @return
     */
    private HashMap<String, List<Pair<String, Double>>> sortAndUpdateResult() {

        HashMap<String, List<Pair<String, Double>>> ansQuery = new HashMap<String, List<Pair<String, Double>>>();
        //ArrayList<String> num = Arrays.tomultyQureyresult.keySet().toArray();

        if (null != multiQueryResult) {
            for (Map.Entry<String, List<Pair<String, Double>>> entry : multiQueryResult.entrySet()) {

                List<Pair<String, Double>> list = new ArrayList<>();
                for (Pair p : entry.getValue()) {
                    list.add(new Pair((docs.get(p.getKey())).getDocName(), p.getValue()));
                }
                ansQuery.put(entry.getKey(), list);
            }


        }

        LinkedHashMap<String, List<Pair<String, Double>>> result = ansQuery.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        return result;

    }

    /**
     *
     * @param docName
     * @return top 5 entity of the current document
     */
    public StringBuilder showEntitySearch(String docName) {
        StringBuilder ans = new StringBuilder();
        //ans.append(docName + ":\n\n");

        String docId = fromDocNameToDocID.get(docName);
        EntitiesFinder ef = new EntitiesFinder(posting_Path);
        List<Pair<String, Double>> topEntities = ef.getTop5Entities(docId);
        DecimalFormat df2 = new DecimalFormat("#.###");

        for (Pair<String, Double> p : topEntities) {
            ans.append(p.getKey() + " : rank=" + df2.format(p.getValue()) + "\n");
        }


        return ans;

    }

    /**
     *
     * @return the query result as string
     */
    public StringBuilder queryToString() {

        StringBuilder ans = new StringBuilder();

        HashMap<String, List<Pair<String, Double>>> ansQuery = getResult();
        if (ansQuery != null) {
            for (Map.Entry<String, List<Pair<String, Double>>> entry : ansQuery.entrySet()) {

                for (Pair p : entry.getValue()) {
                    // ans.append(entry.getKey() + " 0 " + p.getKey() + " 1 " + "\n");
                    //or long way?
                    ans.append(entry.getKey() + " 0 " + p.getKey() + " 1 12.1 mt" + "\n");
                }
            }
            return ans;
        }
        return null;
    }

    /**
     *
     * @return the query result
     */
    public HashMap<String, List<Pair<String, Double>>> getResult() {
        return sortedResultDocName;
    }


    public boolean getSemanticTreatment() {
        return isSemanticTreatment;
    }

    public boolean getIsStem() {
        return isStem;
    }

    public boolean getIsAPIsyn() {
        return is_API_synonym;
    }
//endregion

}
