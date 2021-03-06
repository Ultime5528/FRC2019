package com.ultime5528.frc2019.vision;

import java.util.ArrayList;
import java.util.Random;

/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ultime5528.frc2019.vision.ConfigReader.CameraConfig;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import edu.wpi.cscore.CameraServerJNI;
import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;
import edu.wpi.cscore.VideoSource;
import edu.wpi.cscore.VideoMode.PixelFormat;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;

public final class Main {
  private static String configFile = "/boot/frc.json";

  public static int team = 5528;
  public static boolean server = false;

  public static ArrayList<UsbCamera> cameras = new ArrayList<UsbCamera>();

  public static MyPipeline pipeline;

  private static NetworkTableEntry timeEntry;
  private static NetworkTableEntry rouleauEntry;
  private static NetworkTableEntry isautoEntry;


  
  private Main() {
  }

  /**
   * Start running the camera.
   * 
   */
  public static UsbCamera startCamera(CameraConfig config) {
    System.out.println("Starting camera '" + config.name + "' on " + config.path);
    CameraServer inst = CameraServer.getInstance();
    UsbCamera camera = new UsbCamera(config.name, config.path);
    //MjpegServer server = inst.startAutomaticCapture(camera);

    Gson gson = new GsonBuilder().create();

    camera.setConfigJson(gson.toJson(config.config));
    camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

    /*if (config.streamConfig != null) {
      server.setConfigJson(gson.toJson(config.streamConfig));
    }*/

    return camera;
  }
  
  /**
   * Main.
   * 
   * @throws InterruptedException
   */
  public static void main(String... args) throws InterruptedException {
    // if (args.length > 0) {
    //   configFile = args[0];
    //   ConfigReader.configFile = configFile;
    // }

    // // lire la congig
    // if (!ConfigReader.readConfig()) {
    //   return;
    // }

    // démarre NetworkTables

    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();

    System.out.println("Setting up NetworkTables client for team " + team);
    ntinst.startClientTeam(team);

    //crée pipeline de vision
    pipeline = new MyPipeline(ntinst);

    NetworkTable table = ntinst.getTable("Vision");
    timeEntry = table.getEntry("TIME");
    rouleauEntry = table.getEntry("ROULEAU_ON");
    isautoEntry = table.getEntry("IS_AUTO");

    
    // rouleauEntry = table.getEntry("ROULEAU_ON");
    //démarre caméra
    // for (CameraConfig config : ConfigReader.getCameraConfigs()) {
    //   cameras.add(startCamera(config));
    // }
    //camera = startCamera();

    //démarre la loop() de vision
    loop();
  }
  
  private static void loop(){
    //Vision

    UsbCamera camVision = new UsbCamera("CamVision", K.VISION_CAMERA_PORT);
    camVision.setVideoMode(VideoMode.PixelFormat.kMJPEG, K.WIDTH_VISION, K.HEIGHT_VISION, K.VISION_FPS);
    camVision.setExposureManual(0);
    camVision.getProperty("contrast").set(100);
    camVision.getProperty("saturation").set(50);
    camVision.setWhiteBalanceManual(6500);
    camVision.setBrightness(0);
    // camVision.setFPS(K.VISION_FPS);
    
    UsbCamera camPilote = new UsbCamera("CamPilote", K.PILOTE_CAMERA_PORT);
    camPilote.setVideoMode(PixelFormat.kMJPEG, K.WIDTH, K.HEIGHT, K.PILOTE_FPS);
    camPilote.setExposureHoldCurrent();

    // Tentative de connexion...

    NetworkTableEntry startVisionEntry = NetworkTableInstance.getDefault().getTable("Vision").getEntry("START_VISION");
    int i = 0;
    while(!startVisionEntry.getBoolean(false)) {
      i++;
      System.out.println("Tentative de connexion #" + i + "...");
      try {
        Thread.sleep(1000);
      } catch (Exception e) { }

      if(i == 5)
        throw new RuntimeException("Impossible de se connecter. Redémarrage...");
    }

    // camPilote.setBrightness(100);
    // camPilote.getProperty("contrast").set(50);
    // camPilote.getProperty("saturation").set(50);
    // camPilote.setWhiteBalanceManual(500);
    // camPilote.setExposureManual(50);
    camPilote.setFPS(K.PILOTE_FPS);
    // new Thread(() -> {
    //   try {
    //     Thread.sleep(10000);
    //     camPilote.setExposureHoldCurrent();
    //     System.out.println("Exposure hold set 1");
    //     Thread.sleep(10000);
    //     camPilote.setExposureHoldCurrent();
    //     System.out.println("Exposure hold set 2");
    //   } catch(Exception e) { }
    // }).start();
    

    CvSink sourceVision =  CameraServer.getInstance().getVideo(camVision);
    CvSource outputVideoVision = CameraServer.getInstance().putVideo("OutputVision", K.WIDTH_VISION, K.HEIGHT_VISION);
    outputVideoVision.setFPS(K.VISION_FPS);
    
    MjpegServer serverVision = (MjpegServer) CameraServer.getInstance().getServer("serve_OutputVision");
    
    serverVision.setCompression(50);
    serverVision.setFPS(K.VISION_FPS);

    Mat inputVision = new Mat(K.HEIGHT,K.WIDTH,CvType.CV_8UC3);    

    //Pilote
    CvSink sourcePilote = CameraServer.getInstance().getVideo(camPilote);
    
    CvSource outputVideoPilote = CameraServer.getInstance().putVideo("OutputPilote", K.WIDTH,(int)(K.HEIGHT * (1 + K.TIME_BAR_PROPORTION)));
    outputVideoPilote.setFPS(K.PILOTE_FPS);

    MjpegServer serverPilote = (MjpegServer) CameraServer.getInstance().getServer("serve_OutputPilote");
    
    // serverPilote.setCompression(100);
    serverPilote.setFPS(K.PILOTE_FPS);

    Mat inputPilote = new Mat(K.HEIGHT,K.WIDTH,CvType.CV_8UC3); 
    Mat outputPilote = new Mat((int)(K.HEIGHT * (1 + K.TIME_BAR_PROPORTION)),K.WIDTH,CvType.CV_8UC3); 
    //Mat outputPilote = new Mat((int)(K.HEIGHT),K.WIDTH,CvType.CV_8UC3); 

    // Checkup FPS Pilote


    new Thread( () -> {

      int currentTime;
      boolean rouleauON;
      boolean isauto;
      while(!Thread.currentThread().isInterrupted()){

        

        sourcePilote.grabFrame(inputPilote);
        
        currentTime = (int)timeEntry.getDouble(135);
        rouleauON = rouleauEntry.getBoolean(false);
        isauto = isautoEntry.getBoolean(false);
        
        //écrire les infos sur la vision du pilote
        inputPilote.copyTo(outputPilote.rowRange(0, K.HEIGHT));
        PiloteView.write(outputPilote, currentTime, rouleauON, isauto);
        
        outputVideoPilote.putFrame(outputPilote);
        
      }
      
    } ).start();
    
    while(true){
      try {

        //obtenir l'image des caméras
        sourceVision.grabFrame(inputVision);

        // // //traiter l'image de la vision
        pipeline.process(inputVision);

        // if(pipeline.pause) {
        //   long time = System.currentTimeMillis();
        //   while(System.currentTimeMillis() - time <= 4000) {

        //     outputVideoVision.putFrame(inputVision);
        //   }
        //   pipeline.pause = false;
        // }

        // // //afficher l'image
        outputVideoVision.putFrame(inputVision);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
