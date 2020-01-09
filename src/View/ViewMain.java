package View;

import Model.Model;
import ViewModel.ViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Optional;

public class ViewMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {


        //ViewModel -> Model
        Model model = new Model();
        ViewModel viewModel = new ViewModel(model);
        model.addObserver(viewModel);
        //--------------
        //Loading Main Windows
        primaryStage.setTitle("לבדוק לך תמנוע");
        FXMLLoader fxmlLoader1 = new FXMLLoader();
        Parent rootWelcome = fxmlLoader1.load(getClass().getResource("ViewXML2.fxml").openStream());
        Scene welcomeScene = new Scene(rootWelcome, 600, 750);
        //welcomeScene.getStylesheets().add(getClass().getResource("ViewCSS.css").toExternalForm());
        primaryStage.setScene(welcomeScene);



        //View -> ViewModel
        ViewController myviewController = fxmlLoader1.getController();
        myviewController.set_ViewModel(viewModel);
        viewModel.addObserver(myviewController);

        SetStageCloseEvent(primaryStage);
        primaryStage.getIcons().add(new Image("file:View/engine-icon-1.jpg"));
        primaryStage.setResizable(false);
        //show main window
        primaryStage.show();


    }

    private void SetStageCloseEvent(Stage primaryStage) {
        primaryStage.setOnCloseRequest(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Exit");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to exit?");
            alert.initStyle(StageStyle.UTILITY);

            Optional<ButtonType> result = alert.showAndWait();
            // ... user chose CANCEL or closed the dialog
            if (result.get() == ButtonType.OK) {
                // ... user chose OK

                // Close program
            } else e.consume();
        });
    }

    private void startMusic() {
        //String musicFile = "resources/Music/opening.mp3";     // For example
        //Media sound = new Media(new File(musicFile).toURI().toString());
        //WelcomeView.mediaPlayer = new MediaPlayer(sound);
        //WelcomeView.mediaPlayer.setCycleCount(INDEFINITE);
        //WelcomeView.mediaPlayer.play();
    }

    public static void main(String[] args) {

        //System.out.println(Arrays.toString(getSynonyms("dog")));
        launch(args);
    }
}
