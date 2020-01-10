/**
 * this class responsible on the ui side
 */
package View;

import ViewModel.ViewModel;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class ViewController implements Observer {
    //region JavaFX fields:
    @FXML
    public javafx.scene.control.Button b_corpus;
    public javafx.scene.control.Button brows_for_posting;
    public javafx.scene.control.Button start_index;
    public javafx.scene.control.Button reset_button;
    public javafx.scene.control.Button show_dictionary_button;
    public javafx.scene.control.Button load_dictionary_button;
    public javafx.scene.control.CheckBox stemming_button;
    public javafx.scene.control.TextField posting_files_path;
    public javafx.scene.control.TextField corpus_path;
    public javafx.scene.layout.GridPane grid;
    //part B
    public javafx.scene.control.TextField Query;
    public javafx.scene.control.TextField MultyQuery;
    public javafx.scene.control.Button show_Entity;
    public javafx.scene.control.CheckBox SemanticTreatment;
    public javafx.scene.control.CheckBox click_stream;
    public javafx.scene.control.Button runQuery_button;
    public javafx.scene.control.Button show_result;
    public javafx.scene.control.Button save_result;
    //endregion

    public ViewModel myViewModel;

    //region PartA

    /**
     * this function set and connect the view model
     *
     * @param vm
     */
    public void set_ViewModel(ViewModel vm) {


        this.myViewModel = vm;
    }

    /**
     * this function set the path of the corpus
     *
     * @param actionEvent
     */
    public void setCorpusPath(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select corpus Path");
        Stage window = new Stage();
        File selectedFile = directoryChooser.showDialog(window);
        String path = "";

        if (selectedFile != null) {
            path = selectedFile.toPath().toString();
        }
        corpus_path.setText(path);
    }

    /**
     * this function set the path to the posting files and dictioary
     *
     * @param actionEvent
     */
    public void setPostingPath(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Destination Folder for Posting Files");
        Stage window = new Stage();
        File selectedFile = directoryChooser.showDialog(window);
        String path = "";
        if (selectedFile != null) {
            path = selectedFile.toPath().toString();
        }
        posting_files_path.setText(path);
    }

    /**
     * this function start the index processing, and check if there are path to the files
     *
     * @param actionEvent
     */
    public void startToIndex(ActionEvent actionEvent) {
        if ((posting_files_path.getText() == null || posting_files_path.getText().trim().isEmpty()) || (corpus_path.getText() == null || corpus_path.getText().trim().isEmpty())) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("you didn't choose directories for corpus path or Posting files path");
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
        } else {
            myViewModel.setCorpusPath(corpus_path.getText());
            myViewModel.setPostingPath(posting_files_path.getText());
            myViewModel.startToIndex();
        }
    }

    /**
     * this function reset all the memory of the program
     *
     * @param actionEvent
     */
    public void resetProcess(ActionEvent actionEvent) {
        if ((posting_files_path.getText() == null || posting_files_path.getText().trim().isEmpty())) {
            //no path was selected
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Please Choose your Posting Files Path");
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
        } else {
            myViewModel.setPostingPath(posting_files_path.getText());
            Alert alert4 = new Alert(Alert.AlertType.CONFIRMATION);
            alert4.setTitle("Reset Data");
            alert4.setHeaderText(null);
            alert4.setContentText("Are you sure you want to delete all posting files dictionaries?");
            alert4.initStyle(StageStyle.UTILITY);

            Optional<ButtonType> result = alert4.showAndWait();
            if (result.get() == ButtonType.OK) {
                //user chose OK
                boolean b = myViewModel.resetProcess();
                if (b) { //deleted
                    Alert alert2 = new Alert(Alert.AlertType.INFORMATION);
                    alert2.setTitle("Reset");
                    alert2.setHeaderText(null);
                    alert2.setContentText("Posting files have been deleted successfully");
                    alert2.showAndWait();
                } else {
                    Alert alert3 = new Alert(Alert.AlertType.ERROR);
                    alert3.setTitle("Error");
                    alert3.setHeaderText(null);
                    alert3.setContentText("Could not find Posting Files to delete in this path");
                    alert3.initStyle(StageStyle.UTILITY);
                    alert3.showAndWait();
                }
            } else {
                //user chose CANCEL or closed the dialog
                actionEvent.consume();
            }
        }
    }

    /**
     * this function display the dictionary
     *
     * @param actionEvent
     */
    public void displayDictionary(ActionEvent actionEvent) {

        ConcurrentHashMap<String, Pair<String, Integer>> myDict = myViewModel.getDictionary();
        if (myDict != null) {
            List<String> sortedKeys = Collections.list(myDict.keys());
            Collections.sort(sortedKeys);
            TableView myTable = new TableView();
            TableColumn<String, keyValue> column1 = new TableColumn<>("Term");
            TableColumn<Integer, keyValue> column2 = new TableColumn<>("Total-tf");
            column1.setCellValueFactory(new PropertyValueFactory<>("term"));
            column2.setCellValueFactory(new PropertyValueFactory<>("Ttf"));
            myTable.getColumns().add(column1);
            myTable.getColumns().add(column2);
            myTable.setEditable(true);
            myTable.getSelectionModel().setCellSelectionEnabled(true);

            for (int i = 0; i < sortedKeys.size(); i++) {
                String s = sortedKeys.get(i);
                myTable.getItems().add(new keyValue(s, myDict.get(s).getValue()));
            }

            StackPane sp = new StackPane(myTable);
            Scene scene = new Scene(sp, 600, 800);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Terms Dictionary");
            stage.show();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Theres no Dictionary in the disk");
            alert.initStyle(StageStyle.UTILITY);

            alert.showAndWait();
        }
    }

    /**
     * this function load the dictionary from a file in the disk
     *
     * @param actionEvent
     */
    public void loadDictionaryFromDisk(ActionEvent actionEvent) {
        if ((posting_files_path.getText() == null || posting_files_path.getText().trim().isEmpty())) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Please Choose your Posting Files Path");
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
        } else {
            myViewModel.setPostingPath(posting_files_path.getText());
            myViewModel.loadDictionaryFromDisk();
        }
    }

    /**
     * this function set the stemming parameter
     *
     * @param actionEvent
     */
    public void isStemming(ActionEvent actionEvent) {
        if (stemming_button.isSelected()) {
            myViewModel.isStemming(true);
        } else {
            myViewModel.isStemming(false);
        }
    }
    //endregion

    //region PartB


    public void semanticTreatment(ActionEvent actionEvent) {
        if (SemanticTreatment.isSelected()) {
            myViewModel.semanticTreatment(true);
        } else {
            myViewModel.semanticTreatment(false);
        }


    }

    public void brows_for_multy_query(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("select the queries file");
        Stage window = new Stage();
        String path = "";
        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            path = file.toPath().toString();
            MultyQuery.setText(path);
            //Query.setDisable(true);
            myViewModel.set_file_for_multy_query(path);
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("there is somthing wrong with your file");
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
        }


    }

    public void is_API_synonym(ActionEvent actionEvent) {
        if (click_stream.isSelected()) {
            myViewModel.click_stream_seaech(true);
        } else {
            myViewModel.click_stream_seaech(false);
        }

    }

    public void runQuery(ActionEvent actionEvent) {


        if (MultyQuery.getText().trim().isEmpty() && Query.getText().trim().isEmpty()) {// || Query.getText().trim().isEmpty())) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("you didnt insert query");
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
        } else if ((!MultyQuery.getText().trim().isEmpty() && Query.getText().trim().isEmpty()) ||
                (MultyQuery.getText().trim().isEmpty() && !Query.getText().trim().isEmpty())) {

            if (!Query.getText().trim().isEmpty()) {
                myViewModel.runQuery(true, Query.getText());
            } else { // multy query
                myViewModel.runQuery(false, MultyQuery.getText());
            }

            Alert alert2 = new Alert(Alert.AlertType.INFORMATION);
            alert2.setTitle("Query");
            alert2.setHeaderText(null);
            alert2.setContentText("finish query, you can watch result now");
            alert2.showAndWait();

        } else if (!MultyQuery.getText().trim().isEmpty() && !Query.getText().trim().isEmpty()) {//|| !Query.getText().trim().isEmpty())) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("yot cannot insert two queries");
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();

        }
    }

    public void save_result(ActionEvent actionEvent) {

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select folder to save Query results");
        Stage window = new Stage();
        File selectedFile = directoryChooser.showDialog(window);
        String path = "";

        if (selectedFile != null) {
            path = selectedFile.toPath().toString();
            StringBuilder query = myViewModel.queryToString();
            if (query != null) {
                try {
                    String pathToSave = (myViewModel.getSemanticTreatment()) ? path + "\\query_result_semantic.txt" : path + "\\query_result.txt";
                    BufferedWriter bwr = new BufferedWriter(new FileWriter(new File(pathToSave)));

                    //write contents of StringBuffer to a file
                    bwr.write(query.toString());

                    //flush the stream
                    bwr.flush();

                    //close the stream
                    bwr.close();

                    Alert alert2 = new Alert(Alert.AlertType.INFORMATION);
                    alert2.setTitle("save");
                    alert2.setHeaderText(null);
                    alert2.setContentText("finish saving query");
                    alert2.showAndWait();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("theres no result to save");
                alert.initStyle(StageStyle.UTILITY);
                alert.showAndWait();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("theres no result to save");
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();
        }


    }

    public void show_result(ActionEvent actionEvent) {

        HashMap<String, List<Pair<String, Double>>> queryResult = myViewModel.getResult();

        if (queryResult == null) { //thers nothing to show

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Theres no Query result");
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();

        } else {
            // checks for enetity
            boolean noCorpusPath = false;
            if (corpus_path.getText().trim().isEmpty()) { // no corpus path,
                Alert alert4 = new Alert(Alert.AlertType.CONFIRMATION);
                alert4.setTitle("Entity Search");
                alert4.setHeaderText(null);
                alert4.setContentText("corpus path is necessary in order to watch TOP 5 entities\n Are you sure you want to continue without it?");
                alert4.initStyle(StageStyle.UTILITY);

                Optional<ButtonType> result = alert4.showAndWait();


                if (result.get() == ButtonType.OK) {
                    noCorpusPath = true;

                } else {

                    //user chose CANCEL or closed the dialog
                    actionEvent.consume();
                    return;
                }
            }else{
                myViewModel.setCorpusPath(corpus_path.getText());
            }
            TableView myTable = new TableView();
            TableColumn<String, rankScore> column1 = new TableColumn<>("Query");
            TableColumn<Integer, rankScore> column2 = new TableColumn<>("Rate");
            TableColumn<String, rankScore> column3 = new TableColumn<>("DocName");

            column1.setCellValueFactory(new PropertyValueFactory<>("qu"));
            column2.setCellValueFactory(new PropertyValueFactory<>("rate"));
            column3.setCellValueFactory(new PropertyValueFactory<>("docName"));
            myTable.setMinHeight(600);
            /* ScrollBar sbv = new ScrollBar();
            sbv.setOrientation(Orientation.VERTICAL);
            sbv.visibleProperty();
            VBox table  = new VBox(myTable, sbv);*/
            ScrollPane sp = new ScrollPane(myTable);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            myTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);


            myTable.getColumns().add(column1);
            myTable.getColumns().add(column2);
            myTable.getColumns().add(column3);
            myTable.setEditable(true);
            //myTable.getSelectionModel().setCellSelectionEnabled(true);
          //  myTable.getSelectionModel();

            for (Map.Entry<String, List<Pair<String, Double>>> entry : queryResult.entrySet()) {
                int i = 1;
                for (Pair p : entry.getValue()) {
                    myTable.getItems().add(new rankScore(entry.getKey(), i, (String) p.getKey()));
                    i++;
                }
            }
//----------------------labale--------------------------------------------
            Label label = new Label("Query result");
            label.setTextFill(Color.DARKBLUE);
            label.setFont(Font.font("Calibri", FontWeight.BOLD, 36));
            HBox labelHb = new HBox();
            labelHb.setAlignment(Pos.CENTER);
            labelHb.getChildren().add(label);
//-----------------------------------------select menu-----------------
            Label label2 = new Label("select doc name to see entity searc result");
            label2.setTextFill(Color.DARKBLUE);
            label2.setFont(Font.font("Calibri", FontWeight.BOLD, 20));

            labelHb.setAlignment(Pos.CENTER);
            labelHb.getChildren().add(label2);

            Button show = new Button("show Entity for Document");
            if (noCorpusPath) {
                show.setDisable(true);
            }
            show.setOnAction((ActionEvent e) -> {

                ObservableList<rankScore> olist = myTable.getSelectionModel().getSelectedItems();
                int index = myTable.getSelectionModel().getSelectedIndex();
                if (!olist.isEmpty()) {
                    String docName = olist.get(0).getdocName();
                    //System.out.println(docName);
                    String ans = myViewModel.showEntitySearch(docName).toString();
                    Alert alert2 = new Alert(Alert.AlertType.INFORMATION);
                    alert2.setTitle("Top5 Entity");
                    alert2.setHeaderText(null);
                    alert2.setContentText(ans);
                    alert2.showAndWait();
                }

                else{
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("you didnt choose specific row");
                    alert.initStyle(StageStyle.UTILITY);
                    alert.showAndWait();
                }
            });

            VBox select = new VBox();
            select.getChildren().addAll(label2, show);
            select.setAlignment(Pos.CENTER);

            // HBox buttonHb = new HBox();
            // buttonHb.setSpacing(3);
            // buttonHb.setAlignment(Pos.CENTER);
            // buttonHb.getChildren().add(select);
//-----------------------------put them all together---------------------------
            VBox vbox = new VBox();
            vbox.setSpacing(5);
            vbox.setPadding(new Insets(10, 10, 0, 10));
            //vbox.setPadding(new Insets(25, 25, 25, 25));
            vbox.getChildren().addAll(labelHb, myTable, select);
            // StackPane sp = new StackPane(buttonHb,myTable,label);
            Scene scene = new Scene(vbox, 500, 800);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Query results");
            stage.show();

        }
    }


    //endregion

    @Override
    public void update(Observable o, Object arg) {

    }

    /**
     * this class represent key value properties for the display dictionary function
     */
    public static class keyValue {
        private SimpleIntegerProperty Ttf;
        private SimpleStringProperty term;

        public keyValue(String term, Integer Ttf) {
            this.Ttf = new SimpleIntegerProperty(Ttf);
            this.term = new SimpleStringProperty(term);
        }

        public int getTtf() {
            return Ttf.get();
        }

        public SimpleIntegerProperty ttfProperty() {
            return Ttf;
        }

        public String getTerm() {
            return term.get();
        }

        public SimpleStringProperty termProperty() {
            return term;
        }

        public void setTtf(int ttf) {
            this.Ttf.set(ttf);
        }

        public void setTerm(String term) {
            this.term.set(term);
        }
    }

    public static class rankScore {
        private SimpleIntegerProperty rate;
        private SimpleStringProperty docName;
        private SimpleStringProperty qu;

        public rankScore(String qu, Integer rate, String docName) {
            this.qu = new SimpleStringProperty(qu);
            this.rate = new SimpleIntegerProperty(rate);
            this.docName = new SimpleStringProperty(docName);

        }

        public int getrate() {
            return rate.get();
        }

        public SimpleIntegerProperty rateProperty() {
            return rate;
        }

        public String getdocName() {
            return docName.get();
        }

        public SimpleStringProperty docNameProperty() {
            return docName;
        }

        public SimpleStringProperty quProperty() {
            return qu;
        }

        public String getQu() {
            return qu.get();
        }

        public void setqu(String qu) {
            this.qu.set(qu);
        }

        public void setRate(int score) {
            this.rate.set(score);
        }

        public void setdocName(String docName) {
            this.docName.set(docName);
        }

    }

    public static class entitylist {
        private SimpleStringProperty doc;
        private SimpleStringProperty entitys;

        public entitylist(String doc, String entitys) {

            this.doc = new SimpleStringProperty(doc);
            this.entitys = new SimpleStringProperty(entitys);

        }

        public SimpleStringProperty docProperty() {
            return doc;
        }

        public SimpleStringProperty entitysProperty() {
            return entitys;
        }

        public String getdoc() {
            return doc.get();
        }

        public String getentitys() {
            return entitys.get();
        }

        public void setdoc(String doc) {
            this.doc.set(doc);
        }

        public void setentitys(String entitys) {
            this.entitys.set(entitys);
        }
    }
/*
    private class AddButtonListener implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent e) {
/*
            // Create a new row after last row
            Book book = new Book("...", "...");
            data.add(book);
            int row = data.size() - 1;

            // Select the new row
            table.requestFocus();
            table.getSelectionModel().select(row);
            table.getFocusModel().focus(row);

            actionStatus.setText("New book: Enter title and author. Press .");
        }
    }
    */
}

