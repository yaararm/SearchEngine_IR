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
    public boolean resetProcess() {
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



    public void semanticTreatment(boolean withSemnatic) {
        myModel.semanticTreatment(withSemnatic);
    }

    public void runQuery(boolean b, String q) {
        myModel.runQuery(b,q);
    }

    public void API_synonym(boolean is_click) {
        myModel.is_API_synonym(is_click);
    }

    public void set_file_for_multy_query(String file) {
        myModel.set_file_for_multy_query(file);
    }

    public HashMap<String, List<Pair<String,Double>>> getResult() {
         return myModel.getResult();
    }

    public void getFile(){
      //  return myModel.getFile();
    }

    public StringBuilder queryToString() {
        return myModel.queryToString();
    }
    public StringBuilder showEntitySearch(String docname){

        return myModel.showEntitySearch(docname);
    }
    public boolean getSemanticTreatment(){
       return myModel.getSemanticTreatment();
    }

    public boolean getIsStem() {
        return myModel.getIsStem();
    }

    public boolean getIsApiSyn() {
      return  myModel.getISApisyn();

    }
}
