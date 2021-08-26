import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.event.*;
import javafx.stage.*;
import javafx.scene.media.*;
import java.io.*;

public class FFmpegGUI extends Application {
	@Override
	public void start(Stage primaryStage) {
		MenuBar menuBar = new MenuBar();
		Menu fileMenu = new Menu ("File");
		MenuItem openVideoMenuItem = new MenuItem ("Open Video");
		MenuItem exitMenuItem = new MenuItem ("Exit");

		fileMenu.getItems().add(openVideoMenuItem);
		fileMenu.getItems().add(exitMenuItem);
		menuBar.getMenus().add(fileMenu);

		FileChooser fileChooser = new FileChooser();

		openVideoMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				File file = fileChooser.showOpenDialog(primaryStage);
				if (file != null) {
					System.out.print ("Converting " + file.getPath() + " ");
					Process p = createLowQualityScrubableVideo (file);

					try {
						p.waitFor();
						System.out.println ("Done");

						new CutterJoiner (file);
					} catch (Exception exception) {
						System.out.println ("Error: " + exception.getMessage());
					}
				}
			}
		});

		exitMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				System.exit(0);
			}
		});


		VBox vBox = new VBox(menuBar);

		Scene scene = new Scene(vBox, 640, 480);

		primaryStage.setTitle("FFmpeg GUI: Cut then Join");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
	public static Process createLowQualityScrubableVideo (File file) {
		try {
			String command = "/usr/bin/ffmpeg -y -i \"" + file.getPath() + "\" -vf scale=160:-1 -c:v libx264 \"" + file.getPath() + ".mp4\"";
			Process process = Runtime.getRuntime().exec(new String[]{"bash", "-l", "-c",command});

			return process;
		} catch (Exception e) {
			System.out.println (e.getMessage());

			return null;
		}
	}
}

class SceneSegment {
	File file;
	Duration startTime;
	Duration endTime;
	SceneSegment next;

	public SceneSegment (File file) {
		this.file = file;
		next = null;
	} 

	public SceneSegment (File file, Duration startTime, Duration endTime) {
		this.file = file;
		this.startTime = startTime;
		this.endTime = endTime;
		next = null;
	}
}