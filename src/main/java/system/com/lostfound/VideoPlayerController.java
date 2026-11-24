
package system.com.lostfound;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

public class VideoPlayerController {

    @FXML
    private MediaView videoView;

    @FXML
    private Button playButton;

    @FXML
    private Label videoStatusLabel;

    private MediaPlayer mediaPlayer;

    @FXML
    public void initialize() {
        loadVideo();
    }

    private void loadVideo() {

        try {

            var videoResource = getClass().getResource(
                    "/videos/lspu-introduction.mp4"
            );

            if (videoResource == null) {
                videoStatusLabel.setText("Video file not found.");
                return;
            }

            Media media = new Media(videoResource.toExternalForm());

            mediaPlayer = new MediaPlayer(media);

            videoView.setMediaPlayer(mediaPlayer);
            videoView.setFitWidth(600);
            videoView.setFitHeight(300);
            videoView.setPreserveRatio(true);

            mediaPlayer.setOnReady(() -> {
                videoStatusLabel.setText("Video ready to play.");
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                playButton.setText("Replay");
                videoStatusLabel.setText("Video finished.");
            });

            mediaPlayer.setOnError(() -> {
                if (mediaPlayer.getError() != null) {
                    videoStatusLabel.setText(
                            "Video error: " +
                            mediaPlayer.getError().getMessage()
                    );
                }
            });

        } catch (Exception e) {

            videoStatusLabel.setText("Unable to load video.");
            e.printStackTrace();

        }
    }

    @FXML
    private void handlePlayPause() {

        if (mediaPlayer == null) {
            return;
        }

        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {

            mediaPlayer.pause();
            playButton.setText("Play");

        } else {

            if (mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED) {
                mediaPlayer.seek(Duration.ZERO);
            }

            mediaPlayer.play();
            playButton.setText("Pause");
        }
    }

    @FXML
    private void handleStop() {

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            playButton.setText("Play");
            videoStatusLabel.setText("Video stopped.");
        }
    }

    @FXML
    private void handleReplay() {

        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.play();
            playButton.setText("Pause");
            videoStatusLabel.setText("Playing video...");
        }
    }

    public void disposeVideo() {

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }
}
