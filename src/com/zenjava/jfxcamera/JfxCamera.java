package com.zenjava.jfxcamera;

import com.lti.civil.*;
import com.lti.civil.Image;
import com.lti.civil.awt.AWTImageConverter;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class JfxCamera extends Application
{
    private TextArea logTextArea;
    private ChoiceBox<CaptureDeviceInfo> deviceCombo;
    private CaptureSystem system;
    private CaptureStream captureStream;
    private Button initSystemButton;
    private Button startCaptureButton;
    private Button stopCaptureButton;
    private Button shutdownSystemButton;
    private ImageView imageView;

    public static void main(String[] args)
    {
        launch(args);
    }

    public void start(Stage stage) throws Exception
    {
        Scene scene = new Scene(buildView(), 1024, 768);
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

            startCaptureButton.setDisable(false);
            shutdownSystemButton.setDisable(false);

            log("Done loading capture devices");
        }
        catch (CaptureException e)
        {
            initSystemButton.setDisable(false);
            logErrror("Unable to initialise system", e);
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

            stopCaptureButton.setDisable(false);
        }
        catch (CaptureException e)
        {
            startCaptureButton.setDisable(false);
            shutdownSystemButton.setDisable(false);
            logErrror("Unable to start capture", e);
        }
    }

    protected void stopCapture()
    {
        try
        {
            stopCaptureButton.setDisable(true);

            log("Stopping camera capture");
            captureStream.stop();
            log("Capture capture stopped");

            startCaptureButton.setDisable(false);
            shutdownSystemButton.setDisable(false);
        }
        catch (CaptureException e)
        {
            stopCaptureButton.setDisable(false);
            logErrror("Unable to stop capture", e);
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
        }
        catch (CaptureException e)
        {
            shutdownSystemButton.setDisable(false);
            logErrror("Unable to shutdown camera system", e);
        }
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
        pane.setLeft(buildButtonArea());
        pane.setCenter(buildPreviewArea());
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
        startCaptureButton.setOnAction(new EventHandler<ActionEvent>()
        {
            public void handle(ActionEvent event)
            {
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

        return box;
    }

    private Pane buildPreviewArea()
    {
        StackPane pane = new StackPane();
        pane.setStyle("-fx-border-color: blue");
        imageView = new ImageView();
        pane.getChildren().add(imageView);
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
        public void onNewImage(CaptureStream captureStream, Image image)
        {
            log("New image captured");
            BufferedImage bufferedImage = AWTImageConverter.toBufferedImage(image);
            javafx.scene.image.Image lastImage = javafx.scene.image.Image.impl_fromExternalImage(bufferedImage);
            imageView.setImage(lastImage);
        }

        public void onError(CaptureStream captureStream, CaptureException e)
        {
            logErrror("Error during capture", e);
        }
    }
}
