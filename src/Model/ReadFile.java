/**
 * this class responsible to read the entire corpus and separated the files to documents
 */
package Model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Model.Parse.DocumentsQueue;

public class ReadFile extends Thread {

    //region Class Fields
    //-------------Class Fields--------------
    private int numberOfFilesProceeded;
    private int numberOfDocsProceeded;
    private String corpusPath;
    private static final Pattern DOC_Pattern = Pattern.compile("<DOC>(.+?)</DOC>", Pattern.DOTALL),
            DOC_ID_Pattern = Pattern.compile("<DOCNO>(.+?)</DOCNO>", Pattern.DOTALL),
            DATE_Pattern = Pattern.compile("<DATE>(.+?)</DATE>", Pattern.DOTALL),
            TITLE_Pattern = Pattern.compile("<HEADLINE>(.+?)</HEADLINE>", Pattern.DOTALL),
            TEXT_Pattern = Pattern.compile("<TEXT>(.+?)</TEXT>", Pattern.DOTALL);
    //endregion

    ReadFile(String corpusPath) {
        this.corpusPath = corpusPath;
    }


    /**
     * this function iterate over the corpus path to find the files of the data
     */
    private void iterateOverCorpus(File corpus) {
        for (File f : corpus.listFiles()) {
            if (f.isDirectory()) {
                iterateOverCorpus(f);
            } else {
                numberOfFilesProceeded++;
                StringBuilder str = new StringBuilder();

                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        str.append(line).append("\n");
                    }

                } catch (IOException io) {
                    io.printStackTrace();
                }

                extractDocsFromFile(str,f.getName());
            }
        }

    }


    /**
     * this function extract the documents from the files
     *
     * @param rawFileAsString
     */

    private void extractDocsFromFile(StringBuilder rawFileAsString, String fileName) {
        Matcher m = DOC_Pattern.matcher(rawFileAsString);
        int documentsInFile = 0;
        while (m.find()) {
            numberOfDocsProceeded++;
            documentsInFile++;
            String newDocAsString = m.group(1);

            String docId = getDataFromTag(DOC_ID_Pattern, newDocAsString);
            String date = getDataFromTag(DATE_Pattern, newDocAsString);
            String title = getDataFromTag(TITLE_Pattern, newDocAsString);
            String text = getDataFromTag(TEXT_Pattern, newDocAsString);
            Document newDoc = new Document(docId, title, text, date,fileName);
            insertDocToQueue(newDoc);
        }
        System.out.println("Found " + documentsInFile + " docs in file #" + numberOfFilesProceeded);
    }

    /**
     * this function preparing the docs to insert the queue for the parsing process
     *
     * @param doc
     */
    private void insertDocToQueue(Document doc) {
        synchronized (DocumentsQueue) {
            DocumentsQueue.add(doc);
            while (DocumentsQueue.size() >= 1000) {
                try {
                    DocumentsQueue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * this function extract the specific criterion from the document
     *
     * @param p    - regex for the specific data
     * @param Data
     * @return the match of the regex
     */
    private String getDataFromTag(Pattern p, String Data) {
        Matcher m = p.matcher(Data);
        String output = "";
        if (m.find()) {
            output = m.group(1);
        }
        return output.trim();
    }

    @Override
    public void run() {
        File corpusDir = new File(corpusPath+"\\corpus"); // current directory
        iterateOverCorpus(corpusDir);
        Parse.setMoreDocsToBeRead(false);
        System.out.println("Total # of docs: " + numberOfDocsProceeded);
    }
}




