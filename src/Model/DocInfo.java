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
    private int uniqeWords;
    private String title;
    private String date;
    private String fileName;
    //endregion

    DocInfo(String docID, String docName, int max_tf, int uniqueWords, String title, String date, String fileName) {
        this.docID = docID;
        this.docName = docName;
        this.max_tf = max_tf;
        this.uniqeWords = uniqueWords;
        this.title = title;
        this.date = date;
        this.fileName = fileName;
    }

    /**
     *
     * @return the max ocuurrences of specific word
     */
    public int getMax_tf() {
        return max_tf;
    }
    /**
     *
     * @return num of uniqe words
     */
    public int getUniqeWords() {
        return uniqeWords;
    }

    /**
     *
     * @returndoc name
     */
    public String getDocName() {
        return docName;
    }

    /**
     *
     * @return title of doc
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     * @return File Name
     */
    public String getFileName() {
        return fileName;
    }

}

