/**
 * this class connect between the controller and the model
 */
package ViewModel;

import Model.Model;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

public class ViewModel extends Observable implements Observer {
    Model myModel;

    public ViewModel(Model myModel) {
        this.myModel = myModel;

    }

    @Override
    public void update(Observable o, Object arg) {

    }
    /**
     * this function send to the model the path of the corpus
     * @param path
     */
    public void setCorpusPath(String path) {
        myModel.setCorpusPath(path);
    }
    /**
     * this function send to the model the path of the posting files
     * @param path
     */
    public void setPostingPath(String path) {
         myModel.setPostingPath(path);
    }
    /**
     * this function triggers the start indexing action in the model
     */
    public void startToIndex() {
        myModel.startToIndex();
    }
    /**
     * this function trigger the reset processing in the model
     * @return true if all the memory deleted
     */
    public int resetProcess() {
        return myModel.resetProcess();
    }

    /**
     * this function trigger the load dictionary in the model
     */
    public void loadDictionaryFromDisk() {
        myModel.loadDictionaryFromDisk();
    }

    /**
     * this function set the stemming pararmeter in the model
     * @param is_stemmig
     */
    public void isStemming(boolean is_stemmig) {
        this.myModel.setStemming(is_stemmig);
    }
    /**
     *
     * @return the dictionary
     */
    public ConcurrentHashMap<String, Pair<String, Integer>> getDictionary(){
        return myModel.getDictionary();
    }


    /**
     * set use word2vec
     * @param withSemnatic
     */
    public void semanticTreatment(boolean withSemnatic) {
        myModel.semanticTreatment(withSemnatic);
    }

    /**
     * start the retrieval process
     * @param b
     * @param q
     */
    public void runQuery(boolean b, String q) {
        myModel.runQuery(b,q);
    }

    /**
     * set the use in synonym api
     * @param is_click
     */
    public void API_synonym(boolean is_click) {
        myModel.is_API_synonym(is_click);
    }

    /**
     *
     * @return query result
     */
    public HashMap<String, List<Pair<String,Double>>> getResult() {
         return myModel.getResult();
    }

    /**
     *
     * @return query result in string for saving file
     */
    public StringBuilder queryToString() {
        return myModel.queryToString();
    }

    /**
     *
     * @param docname
     * @return the top5 entity of doc
     */
    public StringBuilder showEntitySearch(String docname){

        return myModel.showEntitySearch(docname);
    }

    /**
     *
     * @return true get if there is use in word2vec model
     */
    public boolean getSemanticTreatment(){
       return myModel.getSemanticTreatment();
    }

    /**
     *
     * @return true- if there is use in stemming
     */
    public boolean getIsStem() {
        return myModel.getIsStem();
    }

    /**
     *
     * @return true if there is use in api synonym
     */
    public boolean getIsApiSyn() {
      return  myModel.getIsAPIsyn();

    }
}
