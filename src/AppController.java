// based on Exercise 1
// modified by Timo Schmidt for Exercise 6
//
// Copyright (C) 2019-2020 by Klaus Jung
// All rights reserved.
// Date: 2020-06-26

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class AppController {

	private static final String initialFileName = "test1.jpg";
	private static File fileOpenPath = new File(".");

    @FXML
    private ImageView originalImageView;

	@FXML
	private Label originalEntropy;

    @FXML
    private ImageView predictionImageView;

	@FXML
	private Label predictionEntropy;

	@FXML
	private ComboBox<PredictorType> predictorSelection;

/*	@FXML
	private Label quantizationLabel;

	@FXML
	private Slider quantizationSlider;*/

    @FXML
    private ImageView reconstructedImageView;

	@FXML
	private Label reconstructedEntropy;

	@FXML
	private Label MSE;

    @FXML
    private Label messageLabel;

	@FXML
    void openImage() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(fileOpenPath); 
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images (*.jpg, *.png, *.gif)", "*.jpeg", "*.jpg", "*.png", "*.gif"));
		File selectedFile = fileChooser.showOpenDialog(null);
		if(selectedFile != null) {
			fileOpenPath = selectedFile.getParentFile();
			RasterImage img = new RasterImage(selectedFile);
			img.convertToGray();
			img.setToView(originalImageView);
	    	processImages();
	    	messageLabel.getScene().getWindow().sizeToScene();;
		}
    }
    
	@FXML
	public void initialize() {
		// set combo boxes items
		predictorSelection.getItems().addAll(PredictorType.values());
		predictorSelection.setValue(PredictorType.A);

		// load and process default image
		RasterImage img = new RasterImage(new File(initialFileName));
		img.convertToGray();
		img.setToView(originalImageView);
		processImages();
	}
	
	private void processImages() {
		if(originalImageView.getImage() == null)
			return; // no image: nothing to do
		
		long startTime = System.currentTimeMillis();

		// Original image view
		RasterImage origImg = new RasterImage(originalImageView);
		originalEntropy.setText(setEntropyValue(origImg.calculateEntropy()));

		// Prediction Error image view
		RasterImage predictionImg = new RasterImage(origImg);
		predictionImg.setSelectedPredictor(getPredictorType());
		predictionImg.encode();
		predictionEntropy.setText(setEntropyValue(predictionImg.calculateEntropy()));
		predictionImg.setToView(predictionImageView);

		// Reconstructed image view
		RasterImage reconstructedImg = new RasterImage(origImg);
		reconstructedImg.setSelectedPredictor(getPredictorType());
		reconstructedImg.decode();
		reconstructedEntropy.setText(setEntropyValue(reconstructedImg.calculateEntropy()));
		reconstructedImg.setToView(reconstructedImageView);

		// MSE
		double mse = calculateMSE(origImg.argb, reconstructedImg.argb);
		MSE.setText(setMSEValue(mse));

		// Processing time
	   	messageLabel.setText("Processing time: " + (System.currentTimeMillis() - startTime) + " ms");
	}

	public void predictorChanged() {
    	processImages();
	}

	public PredictorType getPredictorType() {
		return predictorSelection.getValue();
	}

	public String setEntropyValue(double value) {
    	return ("Entropy: " + String.format("%.3f", value));
    }

	public String setMSEValue(double value) {
		return ("MSE: " + String.format("%.1f", value));
	}

	/**
	 * Calculate MSE
	 * Source: https://www.oreilly.com/library/view/mastering-java-for/9781782174271/f2aad1aa-9f16-4fd1-9bbc-295cf52da842.xhtml#:~:text=your%20free%20trial-,MSE,predicted%3B%20int%20n%20%3D%20actual.
	 */
    public double calculateMSE(int[] actual, int[] predicted) {
		int n = actual.length; double sum = 0.0;

		for (int i = 0; i < n; i++) {
			int diff = actual[i] - predicted[i];
			sum = sum + diff * diff;
		}

		return sum / n;
	}

}
