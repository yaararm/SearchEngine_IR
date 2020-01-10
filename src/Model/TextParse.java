package Model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TextParse {

    //region Parser Fields:
    //----------one documents data structure------------
    private Hashtable<String, Integer> terms = new Hashtable<>();
    private Hashtable<String, Integer> entities = new Hashtable<>();

    //-------------static Parser Fields----------------------------------
    private static Hashtable<String, Pattern> patterns;
    private static HashMap<String, String> monthsKey;
    private static List<String> stopWordList;

    //------------instance Fields for this thread
    private Stemmer stemmer = new Stemmer();
    private boolean isStem;

    //----current query processed fields------
    private StringBuilder currentText = null;
    //endregion


    TextParse()  {
        initPatterns();
        initMonthMap();
        init_stopWords();

    }

    /**
     * this function send the doc to the regex extract functions
     */
    public void parseText(String text, boolean isStem) {
        terms.clear();
        entities.clear();

        currentText = new StringBuilder(text);
        this.isStem = isStem;
        ProcessPatterns();
        String[] splitText = splitText();
        cleanSplitText(splitText);
    }

    public Hashtable<String, Integer> getTerms() {
        return new Hashtable(terms);
    }

    public Hashtable<String, Integer> getEntities() {
        return new Hashtable(entities);
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
        StringBuilder newEntity = new StringBuilder(); //temp buffer fo entities

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
     * this function add specific term to the dictionary of the specific doc
     *
     * @param term     term to be add
     * @param isEntity is it entity
     * @param isRegex  is it regex
     */
    private void addTermToHashMap(String term, boolean isEntity, boolean isRegex) {
        if (isEntity) {
            if (entities.containsKey(term)) {
                entities.put(term.toUpperCase(), entities.get(term) + 1);
            } else {
                entities.put(term.toUpperCase(), 1);
            }


        } else if (isRegex) {
            if (terms.containsKey(term)) {
                terms.put(term, terms.get(term) + 1);
            } else {
                terms.put(term, 1);
            }


        } else {//regular term
            if (Character.isUpperCase(term.charAt(0))) {
                if (terms.containsKey(term.toLowerCase())) {
                    terms.put(term.toLowerCase(), terms.get(term.toLowerCase()) + 1);
                } else {
                    if (terms.containsKey(term.toUpperCase())) {
                        terms.put(term.toUpperCase(), terms.get(term.toUpperCase()) + 1);
                    } else {
                        terms.put(term.toUpperCase(), 1);
                    }

                }
            } else { //first letter not upper
                if (terms.containsKey(term.toUpperCase())) {
                    int upper_tf = terms.remove(term.toUpperCase());
                    if (terms.containsKey(term.toLowerCase())) {
                        terms.put(term.toLowerCase(), terms.get(term.toLowerCase()) + 1 + upper_tf);
                    } else {
                        terms.put(term.toLowerCase(), 1 + upper_tf);
                    }
                } else {
                    if (terms.containsKey(term.toLowerCase())) {
                        terms.put(term.toLowerCase(), terms.get(term.toLowerCase()) + 1);
                    } else {
                        terms.put(term.toLowerCase(), 1);
                    }
                }
            }
        }
    }

    //region Init Functions
    private void initMonthMap() {
        if(monthsKey!=null){
            return;
        }
        monthsKey = new HashMap<>();

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

    private void initPatterns() {
        if(patterns!=null){
            return;
        }
        patterns = new Hashtable<>();

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

    private void init_stopWords() {
        if(stopWordList!=null){
            return;
        }

        stopWordList = new ArrayList<>();
        BufferedReader br;
        try {
            File file = new File(Model.posting_Path + "\\stop_words.txt");
            br = new BufferedReader(new FileReader(file));
            String st;
            while ((st = br.readLine()) != null) {
                stopWordList.add(st.toLowerCase());
            }

        } catch (Exception E) {
            try {
                File file = new File("Resources\\stop_words.txt");
                br = new BufferedReader(new FileReader(file));
                String st;
                while ((st = br.readLine()) != null) {
                    stopWordList.add(st.toLowerCase());
                }

            } catch (Exception e) {
                System.out.println("no stop Words Found!!!!");
            }
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





