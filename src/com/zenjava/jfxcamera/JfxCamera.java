package com.zenjava.jfxcamera;

import com.lti.civil.*;
import com.lti.civil.awt.AWTImageConverter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

// estas librerias fueron usadas en POO

import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class JfxCamera extends Application
{
    private TextArea logTextArea;
    private ChoiceBox<CaptureDeviceInfo> deviceCombo;
    private Label messageLabel;
    private CaptureSystem system;
    private CaptureStream captureStream;
    private Button initSystemButton;
    private Button startCaptureButton;
    private Button stopCaptureButton;
    private Button shutdownSystemButton;
    private Button stressTestButton;
    private ImageView imageView;
    private Button takeSnapshotButton;
    private VBox snapshotPane;
    private javafx.scene.image.Image lastImage;

    public static void main(String[] args)
    {
        launch(args);
    }

    public void start(Stage stage) throws Exception
    {
        Scene scene = new Scene(buildView(), 1200, 768);
        stage.setScene(scene);
        stage.show();
    }

    protected void initialiseSystem()
    {
        try
        {
            initSystemButton.setDisable(true);

            log("Creating camera system factory");
            CaptureSystemFactory factory = DefaultCaptureSystemFactorySingleton.instance();
            system = factory.createCaptureSystem();
            log("Initialising camera system");
            system.init();
            log("Camera system initialised");

            log("Loading capture devices");
            deviceCombo.getItems().clear();
            List list = system.getCaptureDeviceInfoList();
            log("Found " + list.size() + " capture devices");
            for (int i = 0, listSize = list.size(); i < listSize; i++)
            {
                Object device = list.get(i);
                CaptureDeviceInfo info = (CaptureDeviceInfo) device;
                log("CAPTURE DEVICE " + (i+1) + ":");
                log("  Available Device ID: " + info.getDeviceID());
                log("  Available Description: " + info.getDescription());
                deviceCombo.getItems().add(info);
            }
            deviceCombo.getSelectionModel().selectFirst();
            log("Done loading capture devices");

            shutdownSystemButton.setDisable(false);
            if (deviceCombo.getItems().size() > 0)
            {
                startCaptureButton.setDisable(false);
                messageLabel.setText("Choose camera from drop down and click 'Start Capture'");
            }
            else
            {
                messageLabel.setText("No camera found! Do you have one installed and plugged in?");
                log("No capture devices, you need a supported camera to stream video");
            }

            stressTestButton.setDisable(false);
        }
        catch (CaptureException e)
        {
            initSystemButton.setDisable(false);
            logErrror("Unable to initialise system", e);
            messageLabel.setText("Init failed");
        }
    }

    protected void startCapture()
    {
        try
        {
            startCaptureButton.setDisable(true);
            shutdownSystemButton.setDisable(true);

            log("Opening capture stream");
            CaptureDeviceInfo selectedItem = deviceCombo.getSelectionModel().getSelectedItem();
            CaptureDeviceInfo deviceInfo = selectedItem;
            captureStream = system.openCaptureDeviceStream(deviceInfo.getDeviceID());
            log("Capture stream opened");

            log("Starting camera capture");
            captureStream.setObserver(new CaptureHandler());
            captureStream.start();
            log("Capture capture started");

            messageLabel.setText("Showing video from " + deviceInfo.getDescription());
            stopCaptureButton.setDisable(false);
            takeSnapshotButton.setDisable(false);
        }
        catch (CaptureException e)
        {
            startCaptureButton.setDisable(false);
            shutdownSystemButton.setDisable(false);
            logErrror("Unable to start capture", e);
            messageLabel.setText("Error starting capture");
        }
    }

    protected void stopCapture()
    {
        try
        {
            stopCaptureButton.setDisable(true);
            takeSnapshotButton.setDisable(true);

            log("Stopping camera capture");
            captureStream.stop();
            captureStream.dispose();
            log("Capture stream stopped");

            startCaptureButton.setDisable(false);
            shutdownSystemButton.setDisable(false);
            messageLabel.setText("Video capture stopped, want to go again?");
        }
        catch (CaptureException e)
        {
            stopCaptureButton.setDisable(false);
            logErrror("Unable to stop capture", e);
            messageLabel.setText("Error stopping capture");
        }
    }

    protected void shutdownSystem()
    {
        try
        {
            shutdownSystemButton.setDisable(true);
            startCaptureButton.setDisable(true);
            stopCaptureButton.setDisable(true);

            log("Disposing camera stream");
            system.dispose();
            log("Camera system disposed");

            initSystemButton.setDisable(false);
            messageLabel.setText("Camera system shutdown, but you can start it up again if you want");
        }
        catch (CaptureException e)
        {
            shutdownSystemButton.setDisable(false);
            logErrror("Unable to shutdown camera system", e);
            messageLabel.setText("Error shutting down system");
        }
    }

    protected void startStressTest()
    {
        StressTester stressTester = new StressTester();
        stressTester.start();
    }

    protected void log(String message)
    {
        logTextArea.appendText(message);
        logTextArea.appendText("\n");
    }

    protected void logErrror(String message, Throwable e)
    {
        log(message);
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        log(writer.toString());
    }

    private Parent buildView()
    {
        BorderPane pane = new BorderPane();

        messageLabel = new Label("Click 'Init Camera System' to start");
        messageLabel.setMaxWidth(Integer.MAX_VALUE);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setStyle("-fx-font-size: 20pt; -fx-padding: 20px; -fx-background-color: #ffffdd; -fx-border-color: gray");
        pane.setTop(messageLabel);

        pane.setLeft(buildButtonArea());
        pane.setCenter(buildPreviewArea());
        pane.setRight(buildSnapshotArea());

        pane.setBottom(buildLogArea());

        return pane;
    }

    private Pane buildButtonArea()
    {
        VBox box = new VBox(10);
        box.setStyle("-fx-padding: 10");
        box.setFillWidth(true);

        initSystemButton = new Button("Init Camera System");
        initSystemButton.setMaxWidth(Integer.MAX_VALUE);
        initSystemButton.setOnAction(new EventHandler<ActionEvent>()
        {
            public void handle(ActionEvent event)
            {
                initialiseSystem();
            }
        });
        box.getChildren().add(initSystemButton);

        deviceCombo = new ChoiceBox<CaptureDeviceInfo>();
        deviceCombo.setMaxWidth(Integer.MAX_VALUE);
        deviceCombo.setConverter(new StringConverter<CaptureDeviceInfo>() {
            public String toString(CaptureDeviceInfo deviceInfo)
            {
                return deviceInfo.getDescription();
            }

            public CaptureDeviceInfo fromString(String s)
            {
                throw new UnsupportedOperationException("Converting device from string is not supported");
            }
        });
        box.getChildren().add(deviceCombo);

        startCaptureButton = new Button("Start Capture");
        startCaptureButton.setMaxWidth(Integer.MAX_VALUE);
        startCaptureButton.setDisable(true);
        startCaptureButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                startCapture();
            }
        });
        box.getChildren().add(startCaptureButton);

        stopCaptureButton = new Button("Stop Capture");
        stopCaptureButton.setMaxWidth(Integer.MAX_VALUE);
        stopCaptureButton.setDisable(true);
        stopCaptureButton.setOnAction(new EventHandler<ActionEvent>()
        {
            public void handle(ActionEvent event)
            {
                stopCapture();
            }
        });
        box.getChildren().add(stopCaptureButton);

        shutdownSystemButton = new Button("Shutdown Camera System");
        shutdownSystemButton.setMaxWidth(Integer.MAX_VALUE);
        shutdownSystemButton.setDisable(true);
        shutdownSystemButton.setOnAction(new EventHandler<ActionEvent>()
        {
            public void handle(ActionEvent event)
            {
                shutdownSystem();
            }
        });
        box.getChildren().add(shutdownSystemButton);

        stressTestButton = new Button("Start Stress Test");
        stressTestButton.setMaxWidth(Integer.MAX_VALUE);
        stressTestButton.setOnAction(new EventHandler<ActionEvent>()
        {
            public void handle(ActionEvent event)
            {
                startStressTest();
            }
        });
        box.getChildren().add(stressTestButton);

        return box;
    }

    private Pane buildPreviewArea()
    {
        StackPane pane = new StackPane();
        pane.setStyle("-fx-background-color: #ddd; -fx-border-color: gray");
        imageView = new ImageView();
        pane.getChildren().add(imageView);
        return pane;
    }

    private Pane buildSnapshotArea()
    {
        BorderPane pane = new BorderPane();
        pane.setPrefWidth(180);
        pane.setStyle("-fx-padding: 20px");

        takeSnapshotButton = new Button("Take Snapshot");
        takeSnapshotButton.setDisable(true);
        takeSnapshotButton.setMaxWidth(Integer.MAX_VALUE);
        takeSnapshotButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event)
            {
                ImageView snapshot = new ImageView(lastImage);
                snapshot.setStyle("-fx-border-color: gray");
                snapshot.setFitWidth(155);
                snapshot.setPreserveRatio(true);
                snapshotPane.getChildren().add(0, snapshot);
            }
        });
        BorderPane.setMargin(takeSnapshotButton, new Insets(0, 0, 10, 0));
        pane.setTop(takeSnapshotButton);

        snapshotPane = new VBox(10);
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(snapshotPane);
        pane.setCenter(scrollPane);

        return pane;
    }


    private Pane buildLogArea()
    {
        BorderPane pane = new BorderPane();
        logTextArea = new TextArea();
        pane.setCenter(logTextArea);
        return pane;
    }


    //-------------------------------------------------------------------------

    private class CaptureHandler implements CaptureObserver
    {
        int count;

        public void onNewImage(CaptureStream captureStream, com.lti.civil.Image image)
        {
            if (++count % 100 == 0)
            {
                log("Streamed " + count + " frames of video (you should be seeing stuff)");
            }

            BufferedImage bufferedImage = AWTImageConverter.toBufferedImage(image);
            lastImage = javafx.scene.image.Image.impl_fromExternalImage(bufferedImage);
            imageView.setImage(lastImage);
        }

        public void onError(CaptureStream captureStream, CaptureException e)
        {
            logErrror("Error during capture", e);
        }
    }

    //-------------------------------------------------------------------------

    private class StressTester extends Thread
    {
        private boolean active;

        public synchronized void start() {
            active = true;
            super.start();
        }

        public void stopStressTesting()
        {
            active = false;
            interrupt();
        }

        public void run()
        {
            Platform.runLater(new Runnable() {
                public void run() {
                    initialiseSystem();
                }
            });

            try { Thread.sleep(1000); } catch (Exception e) {};

            int rounds = 0;

            while (active)
            {
                try
                {
                    Platform.runLater(new Runnable() {
                        public void run() {
                            startCapture();
                        }
                    });

                    Thread.sleep(3000);

                    Platform.runLater(new Runnable() {
                        public void run() {
                            stopCapture();
                        }
                    });

                    System.out.println("*** Stress test completed " + ++rounds + " rounds");
                }
                catch (Exception e)
                {
                    System.out.println("Stress Test caught exception: " + e);
                    e.printStackTrace();
                }
            }
        }
    }
}
