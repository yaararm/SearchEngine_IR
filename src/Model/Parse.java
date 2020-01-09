package Model;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Parse extends Thread {

    //region Parser Fields:
    //--------paths-----------------
    private String postingPath;
    private String corpusPath;

    //----------one documents data structure------------
    private Hashtable<String, Integer> oneDocTerms = new Hashtable<>();
    private Hashtable<String, Integer> oneDocEntities = new Hashtable<>();

    //----------static documents data structure for batches------------
    private Hashtable<String, Term> allTerms = new Hashtable<>();
    private Hashtable<String, Term> allEntities = new Hashtable<>();

    //-------------static Parser Fields----------------------------------
    private static Hashtable<String, Pattern> patterns = new Hashtable<>();
    private static HashMap<String, String> monthsKey = new HashMap<>();
    static List<String> stopWordList = new ArrayList<>();
    static List<Document> DocumentsQueue = new ArrayList<>();
    static boolean moreDocsToBeRead = true;
    static Integer DocCounter = 0;
    private static final int BATCH_SIZE = 50000;

    //------------instance Fields for this thread
    private Stemmer stemmer = new Stemmer();
    private int docCounterForThisThread = 0;
    private int batchCounter = 0;
    private static boolean isStem;

    //----current Doc processed fields------
    private StringBuilder currentText;
    private int max_tf;
    private int unique_words;
    private String docName;
    private String docID;
    private String docTitle;
    private String docDate;
    private String docFileName;
    //endregion


    Parse(String postingPath, String corpusPath) {
        this.postingPath = postingPath + "\\";
        this.corpusPath = corpusPath + "\\";
        initPatterns();
        initMonthMap();
        init_stopWords();

    }

    @Override
    public void run() {
        startParsing();
    }

    /**
     * this function send the docs to the parse function and collect them to batch 0f 50000 docs.
     * when there are 50000 docs he send them to the function who will save them in ths disk
     */
    private void startParsing() {
        while (!isDocsQueueEmpty() || moreDocsToBeRead) { //more docs to be read
            Document currentDoc = getNextDocToProcess();
            if (currentDoc != null) {
                max_tf = 0;
                unique_words = 0;
                docName = currentDoc.getDocName();
                docID = Thread.currentThread().getName() + docCounterForThisThread;
                docTitle = currentDoc.getTitle();
                docDate = currentDoc.getDate();
                docFileName = currentDoc.getFileName();

                parseDoc(currentDoc);

                if (batchCounter == BATCH_SIZE) {
                    System.out.println("Thread #" + Thread.currentThread().getName() + " is saving temp files after " + batchCounter + " docs");
                    saveBatchToFile();
                    allTerms.clear();
                    allEntities.clear();
                    batchCounter = 0;
                }
            }
        }
        //------save terms for last leftover batch
        saveBatchToFile();
        allTerms.clear();
        allEntities.clear();
        batchCounter = 0;

        //update total number of docs processed by this thread
        synchronized (DocCounter) {
            DocCounter += docCounterForThisThread;
        }


    }

    /**
     * this function send the doc to the regex extract functions
     *
     * @param doc Document to be parse
     */
    private void parseDoc(Document doc) {
        currentText = new StringBuilder(doc.getText());
        ProcessPatterns();
        String[] splitText = splitText();
        cleanSplitText(splitText);
        addDocTermsToAllTerms();
        addDocEntitiesToAllEntity();
        batchCounter++;
        docCounterForThisThread++;
        sendDocInfoToIndexer();
        oneDocTerms.clear();
        oneDocEntities.clear();
    }

    /**
     * this function extract terms (with the help of regex )from the doc
     */
    private void ProcessPatterns() {
        findPattern_Percentage();
        findPattern_Years();
        findPattern_Dates();
        findPattern_BIGDOLLARS();
        findPattern_SMALLDOLLARS();
        findPattern_STARTWITH_$();
        findPattern_Kilometer();
        findPattern_Kilograms();
        findPattern_BMK_ByWord();
        findPattern_longNumbers("BILLION", 'B');
        findPattern_longNumbers("MILLION", 'M');
        findPattern_longNumbers("THOUSAND", 'K');
        findPattern_longNumbers("THOUSAND2", 'K');
        findPattern_BetweenNum();

    }

    /**
     * thid function split the text
     *
     * @return splited text
     */
    private String[] splitText() {
        //String[] split = currentText.toString().split("[()\\[\\]<>^&$#~+|_=;?\"*!;:\\s\\t\\n]+");
        String[] split = currentText.toString().split("([()\\[\\]<>{}^&$#@~+|_=;?\"*!:\\s\\t\\n\\r]+)|(((--)-?+)+)|(((\\.\\.)\\.?+)+)");
        return split;

    }

    /**
     * this function clean the slitted text and erase irrelevant chars/ words
     *
     * @param words current text after split
     */
    private void cleanSplitText(String[] words) {
        StringBuilder newEntity = new StringBuilder(); //temp buffer fo oneDocEntities

        //----iterate over all splited words, clean them and add to hashMap ---------------
        int lastEntityIndexEnding = 0;
        for (int i = 0; i < words.length; i++) {
            if (words[i].length() < 1) continue;

            //region Look for entities:
            if (i > lastEntityIndexEnding) {
                int numberOfWordsInEntity = 0;
                if (!words[i].isEmpty() && Character.isUpperCase(words[i].charAt(0))) { //if word capitalizes
                    String firstWord = words[i];
                    newEntity.append(firstWord);
                    numberOfWordsInEntity = 1;
                    boolean isEndWithLetter = Character.isLetter(words[i].charAt(words[i].length() - 1));
                    int index = i;
                    while (isEndWithLetter && index < words.length - 1 && words[index + 1].length() > 0 && Character.isUpperCase(words[index + 1].charAt(0))) {
                        newEntity.append("-").append(words[index + 1]);
                        index++;
                        numberOfWordsInEntity++;
                        isEndWithLetter = Character.isLetter(words[index].charAt(words[index].length() - 1));
                    }
                    if (numberOfWordsInEntity > 1 && numberOfWordsInEntity <= 5) {
                        if (!Character.isLetter(newEntity.charAt(newEntity.length() - 1))) { //remove ./, from end of entity
                            newEntity.deleteCharAt(newEntity.length() - 1);
                        }
                        addTermToHashMap(newEntity.toString(), true, false);
                        lastEntityIndexEnding = index - 1;
                    }
                    newEntity.setLength(0); //reset buffer
                }
            }
            //endregion

            //region Remove Punctuation from start and end of word:
            if (words[i].length() > 0 && (words[i].startsWith(",") || words[i].startsWith("."))) {
                words[i] = words[i].substring(1);
            }
            if (words[i].length() > 0 && (words[i].endsWith(",") || words[i].endsWith("."))) {
                words[i] = words[i].substring(0, words[i].length() - 1);
            }
            //endregion

            //region Remove Stop-Words:
            if (words[i].length() > 0 && stopWordList.contains(words[i].toLowerCase())) {
                continue;
            }
            //endregion

            //region Stem Word if necessary:
            if (isStem) {
                stemmer.add(words[i].toCharArray(), words[i].length());
                stemmer.stem();
                words[i] = stemmer.toString();
            }
            //endregion

            //region Final cleaning for word:
            if (words[i].length() > 0) {
                //delete spesipic junk on corpus
                if (words[i].matches(".*([%\\s,'`./\\-�&{}()<>|¥]).*")) {
                    //delete delimiter from start and end
                    if (words[i].length() > 0 && (words[i].startsWith(".") || words[i].startsWith("-") || words[i].startsWith("%") || words[i].startsWith(" "))) {
                        words[i] = words[i].substring(1);
                    }
                    if (words[i].length() > 0 && (words[i].endsWith(".") || words[i].endsWith("-") || words[i].endsWith("%") || words[i].startsWith(" "))) {
                        words[i] = words[i].substring(0, words[i].length() - 1);
                    }
                    String[] sWords = words[i].split("[\\s%&,/'`{}()<>|�¥]+");
                    for (String w : sWords) {
                        //remove dot from end and start only!
                        if (w.length() > 0 && (w.startsWith(".") || w.startsWith("-") || w.startsWith("\\"))) {
                            w = w.substring(1);
                        }
                        if (w.length() > 0 && (w.endsWith(".") || w.endsWith("-"))) {
                            w = w.substring(0, w.length() - 1);
                        }
                        if (w.length() >= 1) {


                            if (w.length() == 1 && !Character.isDigit(w.charAt(0))) {
                                continue;
                            }

                            addTermToHashMap(w, false, false);
                        }
                    }
                } else {// only one word
                    if (words[i].length() == 1 && !Character.isDigit(words[i].charAt(0))) {
                        continue;
                    }
                    addTermToHashMap(words[i], false, false);
                }
            }
            //endregion
        }
    }

    /**
     * this function sends the information about the doc to the indexer
     */
    private void sendDocInfoToIndexer() {
        unique_words = oneDocTerms.size();
        if (unique_words > 0) {
            max_tf = Collections.max(oneDocTerms.values());
            Model.docs.put(docID, new DocInfo(docID, docName, max_tf, unique_words, docTitle, docDate, docFileName));

        }
    }

    /**
     * this function sort the data and save it to the temp
     */
    private void saveBatchToFile() {
        String numbers = "1234567890";
        String abc = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String pathToUse = (isStem) ? postingPath + "WithStem\\" : postingPath + "WithOutStem\\";

        ArrayList<Term> list = Collections.list(allTerms.elements());
        ArrayList<Term> listEn = Collections.list(allEntities.elements());
        list.sort(Term.Comparators.Term);
        listEn.sort(Term.Comparators.Term);

        //----------finding the numbers section------------
        int indexForNumbers = 0;
        char cNumbers = list.get(0).getTerm().charAt(0);
        while (numbers.indexOf(cNumbers) == -1) {
            indexForNumbers++;
            cNumbers = list.get(indexForNumbers).getTerm().charAt(0);
        }

        int last_index_forNumbers = indexForNumbers;
        while (numbers.indexOf(cNumbers) != -1) {
            last_index_forNumbers++;
            cNumbers = list.get(last_index_forNumbers).getTerm().charAt(0);
        }

        //--------------saving the numbers file---------
        if (last_index_forNumbers != 0) {
            ArrayList<Term> to_save = new ArrayList<Term>(list.subList(indexForNumbers, last_index_forNumbers));
            try {
                String filePath = pathToUse + "Temp\\numbers\\";
                FileOutputStream fileOut = new FileOutputStream(filePath + "numbers" + "-" + Thread.currentThread().getName() + System.currentTimeMillis() + ".ty");

                ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
                objectOut.writeObject(to_save);
                objectOut.flush();
                objectOut.close();


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //-------------find the first occurrence of letters in all terms-----------
        int startIndexForLetter = last_index_forNumbers;
        char cLetter = list.get(startIndexForLetter).getTerm().charAt(0);
        while (abc.indexOf(cLetter) == -1) {
            startIndexForLetter++;
            cLetter = list.get(startIndexForLetter).getTerm().charAt(0);
        }
        if (startIndexForLetter < list.size()) {
            list = new ArrayList<Term>(list.subList(startIndexForLetter, list.size()));
        }


        //----------find the first occurrence of letters in entity-------
        int startIndexForLetterEntiti = 0;
        char cEntiti = listEn.get(startIndexForLetterEntiti).getTerm().charAt(0);
        while (abc.indexOf(cEntiti) == -1) {
            startIndexForLetterEntiti++;
            cEntiti = listEn.get(startIndexForLetterEntiti).getTerm().charAt(0);
        }
        if (startIndexForLetter < listEn.size()) {
            listEn = new ArrayList<Term>(listEn.subList(startIndexForLetterEntiti, listEn.size()));
        }

        //-------------merge the entities and the regular terms-------------
        listEn.addAll(list);
        listEn.sort(Term.Comparators.Term);
        list.clear();

        //---------------saving the letters files-----------
        int startOfLetter = 0;
        char capital = 'A', lower = 'a';
        for (int i = 0; i < listEn.size() && capital < 91 && lower < 123; i++) {

            char firstLetter = listEn.get(i).getTerm().charAt(0);
            if ((firstLetter == capital || firstLetter == lower) && (i != listEn.size() - 1)) {
                continue;
            }

            if (i != startOfLetter && i != listEn.size() - 1) { // first ocuurence of b or B
                try {
                    ArrayList<Term> to_save = new ArrayList<Term>(listEn.subList(startOfLetter, i));
                    String filePath = pathToUse + "Temp\\" + capital + "\\";
                    FileOutputStream fileOut = new FileOutputStream(filePath + capital + "-" + Thread.currentThread().getName() + System.currentTimeMillis() + ".ty");
                    ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
                    objectOut.writeObject(to_save);
                    objectOut.flush();
                    objectOut.close();
                    startOfLetter = i;
                    capital++;
                    lower++;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }   // check which character we are
            StringBuilder test = new StringBuilder("" + lower);
            while (capital < firstLetter && lower < test.toString().toLowerCase().charAt(0)) {
                capital++;
                lower++;
            }

            //---------------last file to save----------
            if (i == listEn.size() - 1) {
                ArrayList<Term> to_save;
                //last letter
                if (startOfLetter == i) {
                    to_save = new ArrayList<Term>();
                    to_save.add(listEn.get(i));
                } //last sequence
                else {
                    to_save = new ArrayList<Term>(listEn.subList(startOfLetter, i + 1));
                }
                if (!to_save.isEmpty()) {
                    try {

                        String filePath = pathToUse + "Temp\\" + capital + "\\";
                        FileOutputStream fileOut = new FileOutputStream(filePath + capital + "-" + Thread.currentThread().getName() + System.currentTimeMillis() + ".ty");
                        ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
                        objectOut.writeObject(to_save);
                        objectOut.flush();
                        objectOut.close();
                        startOfLetter = i;
                        capital++;
                        lower++;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        //if (startOfLetter + 1 < listEn.size()) {
        //List<Term> list2 = listEn.subList(startOfLetter, listEn.size());
        //if (!list2.isEmpty()) {
        //    //System.out.println("somthing is wrong");
        //    //System.out.println(Arrays.toString(list2.toArray()));
        //}
        //}
        listEn.clear();
    }

    /**
     * this function notify the file reader
     *
     * @return Document from DocsQueue
     */
    private Document getNextDocToProcess() {
        synchronized (DocumentsQueue) {
            if (DocumentsQueue.isEmpty()) {
                return null;
            } else {
                if (DocumentsQueue.size() < 500) {
                    DocumentsQueue.notifyAll();
                }
                return DocumentsQueue.remove(0);

            }
        }
    }

    /**
     * this function return true if the current docs queue is empty
     *
     * @return isEmpty doc  queue
     */
    private boolean isDocsQueueEmpty() {
        synchronized (DocumentsQueue) {
            return DocumentsQueue.isEmpty();
        }
    }

    /**
     * this function combine the entitis of one doc to all Entiti dictionary
     */
    private void addDocEntitiesToAllEntity() {
        for (String key : oneDocEntities.keySet()) {
            int tf = oneDocEntities.get(key);
            if (allEntities.containsKey(key)) {
                allEntities.get(key).addOccurrenceToTerm(docID, tf);
            } else {
                Term t = new Term(key, true);
                t.addOccurrenceToTerm(docID, tf);
                allEntities.put(key, t);
            }
        }
    }

    /**
     * this function add the terms of the current doc to all the terms dictionary
     */
    private void addDocTermsToAllTerms() {

        for (String key : oneDocTerms.keySet()) {
            int tf = oneDocTerms.get(key);
            if (Character.isUpperCase(key.charAt(0)) || Character.isDigit(key.charAt(0))) {
                if (allTerms.containsKey(key.toLowerCase())) {
                    allTerms.get(key.toLowerCase()).addOccurrenceToTerm(docID, tf);
                } else {
                    if (allTerms.containsKey(key.toUpperCase())) {
                        allTerms.get(key.toUpperCase()).addOccurrenceToTerm(docID, tf);
                    } else { //add new term
                        Term t = null;
                        if (key.endsWith("Dollars")) {
                            t = new Term(key, false);
                        } else {
                            t = new Term(key.toUpperCase(), false);
                        }
                        t.addOccurrenceToTerm(docID, tf);
                        allTerms.put(key.toUpperCase(), t);
                    }
                }
            } else { //first letter not upper
                if (allTerms.containsKey(key.toUpperCase())) {
                    Term t = allTerms.remove(key.toUpperCase());
                    t.setTermLower();
                    if (allTerms.containsKey(key.toLowerCase())) {
                        t.mergeTerms(allTerms.get(key.toLowerCase()));
                        t.addOccurrenceToTerm(docID, tf);
                        allTerms.put(key.toLowerCase(), t);
                    } else {
                        t.addOccurrenceToTerm(docID, tf);
                        allTerms.put(key.toLowerCase(), t);
                    }
                } else {
                    if (allTerms.containsKey(key.toLowerCase())) {
                        allTerms.get(key.toLowerCase()).addOccurrenceToTerm(docID, tf);
                    } else {
                        Term t = new Term(key.toLowerCase(), false);
                        t.addOccurrenceToTerm(docID, tf);
                        allTerms.put(key.toLowerCase(), t);
                    }
                }
            }
        }

    }

    /**
     * this function add specific term to the dictionary of the specific doc
     *
     * @param term     term to be add
     * @param isEntity is it entity
     * @param isRegex  is it regex
     */
    private void addTermToHashMap(String term, boolean isEntity, boolean isRegex) {
        if (isEntity) {
            if (oneDocEntities.containsKey(term)) {
                oneDocEntities.put(term.toUpperCase(), oneDocEntities.get(term) + 1);
            } else {
                oneDocEntities.put(term.toUpperCase(), 1);
            }


        } else if (isRegex) {
            if (oneDocTerms.containsKey(term)) {
                oneDocTerms.put(term, oneDocTerms.get(term) + 1);
            } else {
                oneDocTerms.put(term, 1);
            }


        } else {//regular term
            if (Character.isUpperCase(term.charAt(0))) {
                if (oneDocTerms.containsKey(term.toLowerCase())) {
                    oneDocTerms.put(term.toLowerCase(), oneDocTerms.get(term.toLowerCase()) + 1);
                } else {
                    if (oneDocTerms.containsKey(term.toUpperCase())) {
                        oneDocTerms.put(term.toUpperCase(), oneDocTerms.get(term.toUpperCase()) + 1);
                    } else {
                        oneDocTerms.put(term.toUpperCase(), 1);
                    }

                }
            } else { //first letter not upper
                if (oneDocTerms.containsKey(term.toUpperCase())) {
                    int upper_tf = oneDocTerms.remove(term.toUpperCase());
                    if (oneDocTerms.containsKey(term.toLowerCase())) {
                        oneDocTerms.put(term.toLowerCase(), oneDocTerms.get(term.toLowerCase()) + 1 + upper_tf);
                    } else {
                        oneDocTerms.put(term.toLowerCase(), 1 + upper_tf);
                    }
                } else {
                    if (oneDocTerms.containsKey(term.toLowerCase())) {
                        oneDocTerms.put(term.toLowerCase(), oneDocTerms.get(term.toLowerCase()) + 1);
                    } else {
                        oneDocTerms.put(term.toLowerCase(), 1);
                    }
                }
            }
        }
    }

    /**
     * this function update that the read file has finish to read all files
     *
     * @param moreDocsToBeRead is more docs to be read
     */
    static void setMoreDocsToBeRead(boolean moreDocsToBeRead) {
        Parse.moreDocsToBeRead = moreDocsToBeRead;
    }

    /**
     * this function set the stemming parameter
     *
     * @param isStem stem
     */
    static void setIsStem(boolean isStem) {
        Parse.isStem = isStem;
        System.out.println(isStem);
    }

    //region Init Functions
    private void initMonthMap() {
        monthsKey.put("JAN", "01");
        monthsKey.put("JANUARY", "01");
        monthsKey.put("FEB", "02");
        monthsKey.put("FEBRUARY", "02");
        monthsKey.put("MAR", "03");
        monthsKey.put("MARCH", "03");
        monthsKey.put("APR", "04");
        monthsKey.put("APRIL", "04");
        monthsKey.put("MAY", "05");
        monthsKey.put("JUN", "06");
        monthsKey.put("JUNE", "06");
        monthsKey.put("JUL", "07");
        monthsKey.put("JULY", "07");
        monthsKey.put("AUG", "08");
        monthsKey.put("AUGUST", "08");
        monthsKey.put("SEP", "09");
        monthsKey.put("SEPTEMBER", "09");
        monthsKey.put("OCT", "10");
        monthsKey.put("OCTOBER", "10");
        monthsKey.put("NOV", "11");
        monthsKey.put("NOVEMBER", "11");
        monthsKey.put("DEC", "12");
        monthsKey.put("DECEMBER", "12");
    }

    private void init_stopWords() {
        try {
            File file = new File(corpusPath + "stop_words.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String st;

            while ((st = br.readLine()) != null) {
                stopWordList.add(st.toLowerCase());
            }
            //region add junk words
            stopWordList.add("CELLRULE".toLowerCase());
            stopWordList.add("/CELLRULE".toLowerCase());
            stopWordList.add("/ROWRULE".toLowerCase());
            stopWordList.add("ROWRULE".toLowerCase());
            stopWordList.add("/TABLECELL".toLowerCase());
            stopWordList.add("TABLECELL".toLowerCase());
            stopWordList.add("CHJ".toLowerCase());
            stopWordList.add("CVJ".toLowerCase());
            stopWordList.add("mr");
            stopWordList.add("TABLECELL-CHJ-CVJ-C".toLowerCase());
            stopWordList.add("TABLECELL-CHJ-R-CVJ-C".toLowerCase());
            stopWordList.add("F-P".toLowerCase());
            //endregion
        } catch (Exception ignored) {

        }
    }

    private void initPatterns() {
        final Pattern PERCENTAGE = Pattern.compile("\\b(([0-9])+(\\.([0-9])+)?)(\\s+)?((percent|percentage)(s)?\\b|%)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("PERCENTAGE", PERCENTAGE);

        final Pattern BMK_byWords = Pattern.compile("\\b([0-9]+)(\\.[0-9]+)?(\\s+)(billion|million|thousand)(s)?(?!%| (M )?dollar)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("BMK_byWords", BMK_byWords);

        final Pattern BILLION = Pattern.compile("(\\b(?<!\\$)([0-9]{1,3}),([0-9][0-9][0-9]),([0-9][0-9][0-9]),([0-9][0-9][0-9])(\\.[0-9]+)?)(?!%| (M )?dollar)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("BILLION", BILLION);

        final Pattern MILLION = Pattern.compile("\\b(([0-9]{1,3})),([0-9][0-9][0-9]),([0-9][0-9][0-9])(\\.[0-9]+)?(?!%| (M )?dollar)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("MILLION", MILLION);

        final Pattern THOUSAND = Pattern.compile("\\b(([1-9]{1,3})),([0-9][0-9][0-9])(\\.[0-9]+)?\\b(?!%| (M )?dollar)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("THOUSAND", THOUSAND);

        final Pattern THOUSAND2 = Pattern.compile("\\b(([1-9]))([0-9][0-9][0-9])(\\.[0-9]+)?",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("THOUSAND2", THOUSAND2);

        final Pattern STARTWITH_$ = Pattern.compile("(\\$)(\\d+)((,\\d\\d\\d)+)?((\\.\\d+)?)?( ([mb])illion(s)?)?",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("STARTWITH_$", STARTWITH_$);

        final Pattern BIGDOLLARS = Pattern.compile("((\\d+)(\\.\\d+)?(m|bn|( (m|b|tr))illion U.S.) dollar(s)?)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("BIGDOLLARS", BIGDOLLARS);

        final Pattern SMALLDOLLARS = Pattern.compile("((\\d+(\\.\\d+)?)|((\\d+)? \\d+/\\d+)|((\\d+)((,\\d\\d\\d)+)?(\\.\\d+)?))( dollar(s)?)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("SMALLDOLLARS", SMALLDOLLARS);

        final Pattern DD_MONTH = Pattern.compile("(\\b([1-9]|[1-2][0-9]|3[0-1])\\b (:?Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)\\b)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("DD_MONTH", DD_MONTH);

        final Pattern MONTH_DD = Pattern.compile("\\b(((:?Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)\\s\\d{0}([1-9]|[1-2][0-9]|3[0-1])\\b))",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("MONTH_DD", MONTH_DD);

        final Pattern MONTH_YEAR = Pattern.compile("(\\b(:?Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)\\s(1[0-9]{3}|2[0-8][0-9]{2}|29[0-8][0-9]|299[0-9])\\b)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("MONTH_YEAR", MONTH_YEAR);

        final Pattern BETWEEN_NUM = Pattern.compile("\\b((between)\\s(\\d+?)\\s *(and)\\s(\\d+?)\\b)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("BETWEEN_NUM", BETWEEN_NUM);

        final Pattern KILOMETER_P;
        KILOMETER_P = Pattern.compile("\\b([0-9]{1,3}),(([0-9]{1,3}))(\\s|\\n|-|-\\n|-\\s\\n) *(kilometer(?:s)?|km)\\b|(\\b(\\d+?)(\\s|\\n|-|-\\n|-\\s\\n) *(kilometer(?:s)?|km)\\b)|\\b *kilometer(?:s)?\\b",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("KILOMETER_P", KILOMETER_P);

        final Pattern KILOGRAM_P;
        KILOGRAM_P = Pattern.compile("\\b([0-9]{1,3}),(([0-9]{1,3}))(\\s|\\n|-|-\\n|-\\s\\n) *(kilogram(?:s)?|kg)\\b|(\\b(\\d+?)(\\s|\\n|-|-\\s\\n) *(kilogram(?:s)?|kg)\\b)|\\b *kilogram(?:s)?\\b",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        patterns.put("KILOGRAM_P", KILOGRAM_P);


    }
    //endregion

    //region Pattern processing
    private void findPattern_Percentage() {
        Matcher m = patterns.get("PERCENTAGE").matcher(currentText);
        int start = 0;
        while (m.find(start)) {
            String replacement = m.group(1) + "%";
            addTermToHashMap(replacement, false, true);
            currentText.replace(m.start(), m.end(), " ");
            start = m.start() + 1;
        }
    }

    private void findPattern_BMK_ByWord() {
        Matcher m = patterns.get("BMK_byWords").matcher(currentText);
        int start = 0;
        while (m.find(start)) {
            char MBK = m.group(4).toUpperCase().charAt(0);
            if (MBK == 'T') MBK = 'k';
            String replacement = (m.group(2) == null) ? m.group(1) + MBK : m.group(1) + m.group(2) + MBK; //group(2) is decimal digit if exist
            addTermToHashMap(replacement, false, true);
            currentText.replace(m.start(), m.end(), " ");
            start = m.start() + 1;
        }
    }

    private void findPattern_longNumbers(String patternName, char charToAdd) {
        Matcher m = patterns.get(patternName).matcher(currentText);
        int start = 0;
        while (m.find(start)) {
            String m0 = m.group(0);
            String m1 = m.group(1);
            String m2 = m.group(2);
            String m3 = m.group(3); //to be after dot
            for (int i = 2; i > 0; i--) { //delete zeros
                if (m3.charAt(i) == '0') {
                    m3 = m3.substring(0, i);
                }
            }

            String replacement = m2;
            if (m3.length() > 0) {
                replacement += "." + m3; //group(2) is decimal digit if exist
            }
            replacement += charToAdd;
            addTermToHashMap(replacement, false, true);
            currentText.replace(m.start(), m.end(), " ");
            start = m.start() + 1;

        }
    }

    private void findPattern_STARTWITH_$() {
        Matcher m = patterns.get("STARTWITH_$").matcher(currentText);
        int start = 0;
        while (m.find(start)) {
            String mainNumber = m.group(2);
            String secondaryNumber = m.group(3);
            String BMillion = m.group(7);
            int decimalLength = 0;
            if (m.group(5) != null)
                decimalLength = m.group(5).length();
            String replacement = mainNumber;
            if (secondaryNumber != null && secondaryNumber.length() > 0) {
                if (secondaryNumber.charAt(0) == '.') {
                    replacement += secondaryNumber;
                } else {// start with ,
                    int secLength = secondaryNumber.length();// - decimalLength;
                    if (secLength == 4) { //K
                        replacement += secondaryNumber;
                    } else if (secLength == 8) {
                        replacement += " M";
                    } else if (secLength == 12) {
                        replacement += secondaryNumber.substring(1, 4) + " M";
                    }
                }
            }
            if (BMillion != null) {
                char c = Character.toLowerCase(BMillion.charAt(1));
                if (c == 'm') {
                    replacement += " M";
                } else {//b
                    replacement += "000 M";
                }
            }

            replacement += " Dollars";
            addTermToHashMap(replacement, false, true);
            currentText.replace(m.start(), m.end(), " ");
            start = m.start() + 1;
        }
    }

    private void findPattern_BIGDOLLARS() {
        Matcher m = patterns.get("BIGDOLLARS").matcher(currentText);
        int start = 0;
        while (m.find(start)) {
            String type = m.group(4).toLowerCase();
            String decimal = m.group(3);
            String mainNumber = m.group(2);
            String replacement = "";
            replacement += mainNumber;
            if (type.contains("b")) {
                replacement += "000";
            } else if (type.contains("tr")) {
                replacement += "000000";
            }
            if (decimal != null && decimal.length() > 0) replacement += decimal;
            replacement += " M Dollars";
            addTermToHashMap(replacement, false, true);
            currentText.replace(m.start(), m.end(), " ");
            start = m.start() + 1;
        }
    }

    private void findPattern_SMALLDOLLARS() {
        Matcher m = patterns.get("SMALLDOLLARS").matcher(currentText);
        int start = 0;
        while (m.find(start)) {
            String mainNumber = m.group(1);
            StringBuilder replacement = new StringBuilder(mainNumber);
            if (mainNumber.contains(",")) {
                int size = (m.group(8).length()) / 4;
                if (size > 1) {//bigger than K number
                    replacement = new StringBuilder(m.group(7));
                    for (int i = 0; i < size - 2; i++) {
                        replacement.append("000");
                    }
                    replacement.append(" M");
                }
            }
            replacement.append(" Dollars");
            addTermToHashMap(replacement.toString(), false, true);
            currentText.replace(m.start(), m.end(), " ");
            start = m.start() + 1;
        }
    }

    private void findPattern_Years() {
        Matcher m = patterns.get("MONTH_YEAR").matcher(currentText);
        int start = 0;
        while (m.find(start)) {
            String month = m.group(2).toUpperCase().substring(0, 3);
            String replacement = m.group(4) + "-" + monthsKey.get(month);
            addTermToHashMap(replacement, false, true);
            currentText.replace(m.start(), m.end(), " ");
            start = m.start() + 1;
        }
    }

    private void findPattern_Dates() {

        Matcher m1 = patterns.get("MONTH_DD").matcher(currentText);

        int start = 0;
        while (m1.find(start)) {
            String day1 = m1.group(5);
            String replacement;
            String month = m1.group(2).toUpperCase().substring(0, 3);
            if (day1.length() == 1) {
                replacement = monthsKey.get(month) + "-o" + day1;
            } else {
                replacement = monthsKey.get(month) + "-" + day1;
            }
            addTermToHashMap(replacement, false, true);

            currentText.replace(m1.start(), m1.end(), " ");
            start = m1.start() + 1;
        }

        Matcher m2 = patterns.get("DD_MONTH").matcher(currentText);
        int start2 = 0;
        while (m2.find(start2)) {
            String day = m2.group(2);
            String replacement2;
            String month = m2.group(3).toUpperCase().substring(0, 3);
            if (day.length() == 1) {
                replacement2 = monthsKey.get(month) + "-0" + day;
            } else {
                replacement2 = monthsKey.get(month) + "-" + day;
            }
            addTermToHashMap(replacement2, false, true);

            currentText.replace(m2.start(), m2.end(), " ");
            start2 = m2.start() + 1;
        }

    }

    private void findPattern_BetweenNum() {

        Matcher m = patterns.get("BETWEEN_NUM").matcher(currentText);
        int start = 0;
        while (m.find(start)) {
            String replacement = m.group(3) + "-" + m.group(5);
            addTermToHashMap(replacement, false, true);
            currentText.replace(m.start(), m.end(), " ");
            start = m.start() + 1;

        }
    }

    private void findPattern_Kilometer() { //ToDo function
        Matcher m = patterns.get("KILOMETER_P").matcher(currentText);
        int start = 0;
        String replacement = "";
        while (m.find(start)) {
            String m0 = m.group(0);
            if (m.group(2) == null && m.group(7) == null) {
                replacement = "KM";
                addTermToHashMap(replacement, false, true);
                currentText.replace(m.start(), m.end(), " ");
                start = m.start() + 1;
            } else if (m.group(2) == null) { //regular number N kilometer
                replacement = m.group(7) + " KM";
                addTermToHashMap(replacement, false, true);
                currentText.replace(m.start(), m.end(), " ");
                start = m.start() + 1;
            } else if (m.group(7) == null) { // N,N kilometer
                replacement = m.group(1) + m.group(2) + " KM";
                addTermToHashMap(replacement, false, true);
                currentText.replace(m.start(), m.end(), " ");
                start = m.start() + 1;
            }

        }

    }

    private void findPattern_Kilograms() {
        Matcher m = patterns.get("KILOGRAM_P").matcher(currentText);
        int start = 0;
        String replacement = "";
        while (m.find(start)) {
            String m0 = m.group(0);
            if (m.group(2) == null && m.group(7) == null) {
                replacement = "KG";
                addTermToHashMap(replacement, false, true);
                currentText.replace(m.start(), m.end(), " ");
                start = m.start() + 1;
            } else if (m.group(2) == null) { //regular number N kilometer
                replacement = m.group(7) + " KG";
                addTermToHashMap(replacement, false, true);
                currentText.replace(m.start(), m.end(), " ");
                start = m.start() + 1;
            } else if (m.group(7) == null) { // N,N kilometer
                replacement = m.group(1) + m.group(2) + " KG";
                addTermToHashMap(replacement, false, true);
                currentText.replace(m.start(), m.end(), " ");
                start = m.start() + 1;
            }

        }
    }
    //endregion
}





