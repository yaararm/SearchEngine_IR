package Model;

import javafx.util.Pair;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Indexer extends Thread {

    //region Class Fields:


    //-------------Private Fields----------------------------
    private String postingPath;
    private String[] lettersToIndex;

    //--------current PostingFile info-----------------------
    private StringBuffer currentPF = new StringBuffer();
    private int postingFilesCounter = 1; //for PF name only
    private String letterHandlingNow;
    private final int POSTING_FILE_LENGTH = 5000000;
    //endregion

    Indexer(String postingPath, String[] stringToIndex, boolean isStem) {
        this.lettersToIndex = stringToIndex;
        //this.postingPath =postingPath;
        this.postingPath = (isStem)? postingPath+"\\WithStem":postingPath+"\\WithOutStem";

    }

    @Override
    public void run() {
        startIndexing();
    }

    /**
     *
     */
    private void startIndexing() {
        for (String letter : lettersToIndex) {
            letterHandlingNow = letter;
            List<List<Term>> tempPostingFilesAsList = readParitySegmentsFiles(letter);
            mergeListsAndSendToDic(tempPostingFilesAsList);
            String lastPostingNameForLeftovers = "" + letterHandlingNow + postingFilesCounter;
            writePostFileToDisk(lastPostingNameForLeftovers);//write the leftovers terms
            postingFilesCounter = 1;

        }

    }

    /**
     * this function reads the temp files
     * @param sToRead witch letter to handle
     * @return the lists of terms from the temp files
     */
    private List<List<Term>> readParitySegmentsFiles(String sToRead) {

        List<List<Term>> lists = new ArrayList<>();

        try {
            String dirPath =  postingPath+"\\Temp\\"+ sToRead + "\\";
            File corpusDir = new File(dirPath); // current directory
            File[] files = corpusDir.listFiles();

            for (File f : Objects.requireNonNull(files)) { // iterate every directory
                FileInputStream fin = new FileInputStream(f);
                ObjectInputStream ois = new ObjectInputStream(fin);
                try {
                    ArrayList<Term> oneListOfTerms = (ArrayList<Term>) ois.readObject();
                    ois.close();
                    fin.close();

                    lists.add(oneListOfTerms);
                    System.out.println("raed File #" + letterHandlingNow + lists.size());
                } catch (Exception e) {
                    System.out.println("Error on File: " + f.getName());
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lists;
    }

    /**
     *  this function sort the lists of terms and capture them to one list
     * @param lists of terms
     */
    private void mergeListsAndSendToDic(List<List<Term>> lists) {
        int totalSize = 0; // every element in the set
        for (List<Term> l : lists) {
            totalSize += l.size();
        }

        List<Term> result = new ArrayList<>();
        List<Term> lowest;

        while (result.size() < totalSize) { // while we still have something to add
            lowest = null;

            for (List<Term> l : lists) {
                if (!l.isEmpty()) {
                    if (lowest == null) { //first- have no minimum by now
                        lowest = l;
                    } else if (l.get(0).compareTo(lowest.get(0)) == 0) {  //same term! mergeListsAndSendToDic them
                        Term t = l.remove(0);
                        if (t.isEntity()) {
                            lowest.get(0).setEntity(false); //Entity appear in more then one doc
                        }
                        lowest.get(0).mergeTerms(t);
                        totalSize--; //remove duplicate count
                    } else {//lower then current lower
                        if (l.get(0).compareTo(lowest.get(0)) < 0) {
                            lowest = l;

                        }
                    }
                }
            }
            if (lowest == null) { //finish all terms
                return;
            }
            Term min = lowest.remove(0);
            if (!min.isEntity()) {
                addTermToDictionary(min);
            }
        }
    }

    /**
     * this function add term to the final dictionary
     * @param term to be added
     */
    private void addTermToDictionary(Term term) {
        if (term == null) return;
        currentPF.append(term.toString()).append("\n");
        String postingName = "" + letterHandlingNow + postingFilesCounter;
        Model.terms.put(term.getTerm(), new Pair<>(postingName, term.getTotal_tf()));
        if (currentPF.length() > POSTING_FILE_LENGTH) {
            writePostFileToDisk(postingName);
            currentPF.setLength(0);
            postingFilesCounter++;
        }
    }

    /**
     * this function write the posting files to the disk
     * @param postingFileName - name of the posting file
     */
    private void writePostFileToDisk(String postingFileName) {
        try {
            BufferedWriter bwr = new BufferedWriter(new FileWriter(new File(postingPath+"\\Posting\\" + postingFileName + ".txt")));

            //write contents of StringBuffer to a file
            bwr.write(currentPF.toString());

            //flush the stream
            bwr.flush();

            //close the stream
            bwr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
