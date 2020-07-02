// based on Exercise 1
// modified by Timo Schmidt for Exercise 6
//
// Copyright (C) 2019-2020 by Klaus Jung
// All rights reserved.
// Date: 2020-06-26

import java.io.File;
import java.util.*;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class RasterImage {
	
	private static final int gray  = 0xffa0a0a0;

	public int[] argb;	// pixels represented as ARGB values in scanline order

	public int[] argb_error;
	public int[] argb_reconstructed;

	public int width;	// image width in pixels
	public int height;	// image height in pixels
	public PredictorType selectedPredictor;

	public RasterImage(RasterImage src) {
		// copy constructor
		this.width = src.width;
		this.height = src.height;
		argb = src.argb.clone();
	}

	public RasterImage(File file) {
		// creates an RasterImage by reading the given file
		Image image = null;
		if(file != null && file.exists()) {
			image = new Image(file.toURI().toString());
		}
		if(image != null && image.getPixelReader() != null) {
			width = (int)image.getWidth();
			height = (int)image.getHeight();
			argb = new int[width * height];
			image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), argb, 0, width);
		} else {
			// file reading failed: create an empty RasterImage
			this.width = 256;
			this.height = 256;
			argb = new int[width * height];
			Arrays.fill(argb, gray);
		}
	}
	
	public RasterImage(ImageView imageView) {
		// creates a RasterImage from that what is shown in the given ImageView
		Image image = imageView.getImage();
		width = (int)image.getWidth();
		height = (int)image.getHeight();
		argb = new int[width * height];
		image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), argb, 0, width);
	}
	
	public void setToView(ImageView imageView) {
		// sets the current argb pixels to be shown in the given ImageView
		if(argb != null) {
			WritableImage wr = new WritableImage(width, height);
			PixelWriter pw = wr.getPixelWriter();
			pw.setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), argb, 0, width);
			imageView.setImage(wr);
		}
	}

	/**
	 * Convert colors to gray
 	 */
	public void convertToGray() {
		// Loop through image
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pos = y * width + x;
				int rgb = argb[pos];

				int r = (rgb >> 16)	& 0xFF;
				int g = (rgb >> 8) 	& 0xFF;
				int b = (rgb)		& 0xFF;

				// Calculate average (= gray value)
				int gray = (r + g + b) / 3;

				// Write back values
				argb[pos] = 0xFF << 24 | gray << 16 | gray << 8 | gray;
			}
		}
	}

	/**
	 * Method to get the color/gray value at predictor position
	 * @param x - current x value
	 * @param pos - current position
	 * @param ABC - "A", "B" or "C" for predictor position
	 * @return value at A/B/C position depending on ABC String
	 */
	public int getValueAtPosition(int x, int pos, String ABC) {

		int pos_A = pos - 1;
		int pos_B = pos - width;
		int pos_C = pos_B - 1;

		switch(ABC) {
			case "A": // Position A
				if (0 < pos_A && pos_A < argb.length) {
					if (x - 1 < 0) // for left border
						return 128;
					else
						return argb[pos_A] & 0xFF;
				}
				break;
			case "B": // Position B
				if (0 < pos_B && pos_B < argb.length)
					return argb[pos_B] & 0xFF;
				break;
			case "C": // Position C
				if (0 < pos_C && pos_C < argb.length) {
					if (x - 1 < 0) // for left border
						return 128;
					else
						return argb[pos_C] & 0xFF;
				}
				break;
		}

		// else
		return 128;

	}

	/**
	 * Do all the calculations for both images
	 */
	public void calculateImages() {

		argb_error = new int[width * height];
		argb_reconstructed = new int[width * height];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				int pos = y * width + x;
				int gray = argb[pos] & 0xFF;

				int A = getValueAtPosition(x, pos, "A");
				int B = getValueAtPosition(x, pos, "B");
				int C = getValueAtPosition(x, pos, "C");

				// For Error Prediction Image
				int error = gray - getPredictor(A, B, C); // Encode Algorithm: Signal – Predictor
				int errorImage = fixOverflow(error + 128);
				argb_error[pos] = (0xFF << 24) | (errorImage << 16) | (errorImage << 8) | errorImage;

				// For Reconstructed Image
				int reconstructed = fixOverflow(error + getPredictor(A, B, C)); // Decode Algorithm: Error + Predictor
				argb_reconstructed[pos] = (0xFF << 24) | (reconstructed << 16) | (reconstructed << 8) | reconstructed;
			}
		}
	}

	/**
	 * Replace argb array with error argb
	 */
	public void encode() {
		calculateImages();
		argb = argb_error;
	}

	/**
	 * Replace argb array with reconstructed argb
	 */
	public void decode() {
		calculateImages();
		argb = argb_reconstructed;
	}

	/**
	 * Method to get predictor
	 * @param A, B, C
	 * @return predictor
	 */
	private int getPredictor(int A, int B, int C) {

		int predictor = 0;

		switch (selectedPredictor) {
			case A:
				predictor = A;
				break;
			case B:
				predictor = B;
				break;
			case C:
				predictor = C;
				break;
			case ABC:
				predictor = A + B - C;
				break;
			case ADAPTIVE:
				predictor = Math.abs(A - C) < Math.abs(B - C) ? B : A;
				break;
		}

		return predictor;

	}

	/**
	 * Calculate entropy
	 * @return entropy value
	 */
	public double calculateEntropy() {

		// Create histogram + fill
		int[] histogram = new int[256];
		Arrays.fill(histogram, 0);

		for (int i = 0; i < argb.length; i++) {
			int value = argb[i] & 0xFF;
			histogram[value]++;
		}

		// Entropy formula
		int sum = width * height;
		double entropy = 0;
		for (int value : histogram) {
			if (value > 0) {
				double prob = value / (double) sum;
				entropy -= prob * (Math.log(prob) / Math.log(2)); // log2(prob)
			}
		}
		return entropy;
	}

	/**
	 * Method to fix overflow
	 * @param value - color value
	 * @return fixed value
	 */
	private int fixOverflow(int value) {
		if (value > 255) value = 255;
		if (value < 0) value = 0;
		return value;
	}

	/**
	 * Setter for currently selected PredictorType
	 * @param selectedPredictor - the currently selected Predictor
	 */
	public void setSelectedPredictor(PredictorType selectedPredictor) {
		this.selectedPredictor = selectedPredictor;
	}

}
