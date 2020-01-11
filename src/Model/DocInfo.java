/**
 * this class represent information we save about the Document
 */
package Model;

import java.io.Serializable;

public class DocInfo implements Serializable {

    //region Class Fields
    //-------Fields-----------
    private String docID;
    private String docName;
    private int max_tf;
    private int uniqueWords;
    private String title;
    private String date;
    private String fileName;
    //endregion

    DocInfo(String docID, String docName, int max_tf, int uniqueWords, String title, String date, String fileName) {
        this.docID = docID;
        this.docName = docName;
        this.max_tf = max_tf;
        this.uniqueWords = uniqueWords;
        this.title = title;
        this.date = date;
        this.fileName = fileName;
    }

    public int getMax_tf() {
        return max_tf;
    }

    public int getUniqueWords() {
        return uniqueWords;
    }

    public String getDocName() {
        return docName;
    }

    public String getTitle() {
        return title;
    }

    public String getFileName() {
        return fileName;
    }

}
