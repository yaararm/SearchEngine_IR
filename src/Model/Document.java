/**
 * this class represent the Document we read from the file, before parsing
 */
package Model;

public class Document {
    private String docName;
    private String title;
    private String text;
    private String date;
    private String fileName;



    public Document(String docName, String title, String text, String date,String fileName) {
        this.docName = docName;
        this.title = title;
        this.text = text;
        this.date = date;
        this.fileName = fileName;
    }

    /**
     *
     * @return document name
     */
    public String getDocName() {
        return docName;
    }

    /**
     *
     * @return document title
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     * @return the tsxt of the document
     */
    public String getText() {
        return text;
    }

    /**
     *
     * @return the date of the document
     */
    public String getDate() {
        return date;
    }

    public String getFileName() {
        return fileName;
    }
}
