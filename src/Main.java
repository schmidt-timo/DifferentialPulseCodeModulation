// based on Exercise 1
// modified by Timo Schmidt for Exercise 6
//
// Copyright (C) 2019-2020 by Klaus Jung
// All rights reserved.
// Date: 2020-04-08

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		BorderPane root = (BorderPane)FXMLLoader.load(getClass().getResource("AppView.fxml"));
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Differential Pulse Code Modulation (DPCM) - SS2020 - Timo Schmidt");
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
