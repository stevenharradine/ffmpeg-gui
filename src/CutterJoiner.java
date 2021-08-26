import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.media.*;
import javafx.event.*;
import java.io.File;
import java.util.ArrayList;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

class CutterJoiner extends Stage {
	Duration startTime;
	Duration endTime;

	Label startTimeLabel = new Label ("");
	Label endTimeLabel = new Label ("");

	ArrayList<SceneSegment> sceneSegments;
	int currentSceneSegment;
	Label rightSide;

	public CutterJoiner (File file) {
		Media media = new Media ("file://" + file.getPath().replaceAll(" ", "%20") + ".mp4");
		MediaPlayer mediaPlayer = new MediaPlayer(media);
		MediaView mediaView = new MediaView(mediaPlayer);

		currentSceneSegment = 0;
		sceneSegments = new ArrayList<SceneSegment>();

		Slider timeSlider = new Slider();
		HBox.setHgrow(timeSlider,Priority.ALWAYS);
		timeSlider.setMinWidth(50);
		timeSlider.setMaxWidth(160);
		timeSlider.valueProperty().addListener(new InvalidationListener() {
			public void invalidated(Observable ov) {
				if (timeSlider.isValueChanging()) {
					// multiply duration by percentage calculated by slider position
					Duration duration = media.getDuration();
					double position = timeSlider.getValue();
					double seekTo = duration.toMillis() * (position / 100);
					mediaPlayer.seek(new Duration(seekTo));
				}
			}
		});

		mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
			if (!timeSlider.isValueChanging()) {
				timeSlider.setValue(newTime.toSeconds());
			}
		});

		Button pausePlayButton = new Button ("Pause / Play");
		Button beginButton = new Button ("[");
		Button endButton = new Button ("]");
		Button saveButton = new Button (">");
		HBox controlBox = new HBox (pausePlayButton, beginButton, endButton, saveButton);
		HBox currentSelection = new HBox (startTimeLabel, new Label (" -> "), endTimeLabel);

		pausePlayButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle (final ActionEvent event) {
				if (mediaPlayer.getStatus() == MediaPlayer.Status.READY || mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED || mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
					mediaPlayer.play();
				} else if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
					mediaPlayer.pause();
				} else {
					System.out.println (mediaPlayer.getStatus());
				}
			}
		});

		beginButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle (final ActionEvent event) {
				startTime = mediaPlayer.getCurrentTime();
				startTimeLabel.setText (startTime.toString());
			}
		});

		endButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle (final ActionEvent event) {
				endTime = mediaPlayer.getCurrentTime();
				endTimeLabel.setText(endTime.toString());
			}
		});

		saveButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle (final ActionEvent event) {
				String bufferedOutput = file.getPath() + ":" + startTime + " -> " + endTime;

				boolean isFileNull = file == null;
				boolean isStartTimeNull = startTime == null;
				boolean isEndTimeNull = endTime == null;
				boolean isStartLessThenEndTime = !isStartTimeNull && !isEndTimeNull && startTime.compareTo(endTime) < 0;

				if (!isFileNull && !isStartTimeNull && !isEndTimeNull && isStartLessThenEndTime) {
					SceneSegment currentSegment = new SceneSegment (file, startTime, endTime);

					sceneSegments.add (currentSegment);
					startTime = null;
					endTime = null;
					startTimeLabel.setText("");
					endTimeLabel.setText("");

					updateRightSide();
				} else {
					System.out.print ("Error: ");

					if (isFileNull) {
						System.out.print ("File undefined: ");
					}
					if (isStartTimeNull) {
						System.out.print ("startTime undefined: ");
					}
					if (isEndTimeNull) {
						System.out.print ("endTime undefined: ");
					}
					if (!isStartTimeNull && !isEndTimeNull && !isStartLessThenEndTime) {
						System.out.print ("startTime > endTime: ");
					}
				}

				System.out.println (bufferedOutput);
			}
		});

		Button encode = new Button ("Cut, Join, and Encode!");
		encode.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle (final ActionEvent event) {
				int segmentCounter;
				String concatenationString = "concat:";
				// cut into segments
				for (segmentCounter = 0; segmentCounter < sceneSegments.size(); segmentCounter++) {
					double ms = sceneSegments.get(segmentCounter).startTime.toMillis();
					//System.out.println (convertSecondsToHMmSs(ms));
					SceneSegment sceneSegment = sceneSegments.get(segmentCounter);
					System.out.println (sceneSegment.file + ":" + sceneSegment.startTime + " -> " + sceneSegment.endTime + "\n");
					try {

						String outputFileName = sceneSegment.file.getPath().replace("%20", " ") + "." + segmentCounter + ".ts";
						concatenationString += outputFileName + "|";
						String command = "ffmpeg -y -i \"" + sceneSegment.file.getPath().replace("%20", " ") + "\" -ss \"" + convertMSToHMmSs(sceneSegment.startTime.toMillis()) + "\" -to \"" + convertMSToHMmSs(sceneSegment.endTime.toMillis()) + "\" -max_muxing_queue_size 9999 -c:v copy \"" + outputFileName + "\"";
						System.out.print ("Running: " + command);
						Process process = Runtime.getRuntime().exec(new String[]{"bash", "-l", "-c",command});

						process.waitFor();
						System.out.println ("Done");

					} catch (Exception e) {
						System.out.println (e.getMessage());
					}
				}
				concatenationString = concatenationString.substring(0, concatenationString.length() - 1);
				// combine final video
				try {
					String command = "ffmpeg -y -i \"" + concatenationString + "\" -max_muxing_queue_size 9999 -c:v libx264 \"" + sceneSegments.get(0).file.getPath().replace("%20", " ") + "." + segmentCounter + ".final.mp4\"";
					System.out.print ("Running: " + command);
					Process process = Runtime.getRuntime().exec(new String[]{"bash", "-l", "-c",command});

					process.waitFor();
					System.out.println ("Done");

				} catch (Exception e) {
					System.out.println (e.getMessage());
				}
			}
		});
		VBox leftSide = new VBox (mediaView, timeSlider, controlBox, currentSelection, encode);
		rightSide = new Label ();
		HBox combined = new HBox (leftSide, rightSide);

		setTitle(file.getName());
		setScene(new Scene(combined, 320, 225));
		show();
	}
	public void updateRightSide () {
		String bufferedText = "";

		for (int i = 0; i < sceneSegments.size(); i++) {            
			bufferedText += sceneSegments.get(i).startTime + " -> " + sceneSegments.get(i).endTime + "\n";
		}

		rightSide.setText(bufferedText);
	}

	// 123 = 00:00:00.123
	// 2345 = 00:00:02.345
	// 454321 = 00:07:34
	private String convertMSToHMmSs(double totalMilliSeconds) {
		double totalSeconds = Math.floor (totalMilliSeconds / 1000);
		double milliSeconds = totalMilliSeconds % 1000;
		double totalMinutes = Math.floor (totalSeconds / 60);
		double seconds      = totalSeconds % 60;
		double totalHours   = Math.floor (totalMinutes / 60);
		double minutes      = totalMinutes % 60;
		double hours        = totalHours % 60;

		String hoursFormated = String.format("%02d", (long)hours);
		String minutesFormated = String.format("%02d", (long)minutes);
		String secondsFormated = String.format("%02d", (long)seconds);
		String milliSecondsFormated = String.format("%03d", (long)milliSeconds);

		String formatedReturn = hoursFormated + ":" + minutesFormated + ":" + secondsFormated + "." + milliSecondsFormated;

		return formatedReturn;
	}
}