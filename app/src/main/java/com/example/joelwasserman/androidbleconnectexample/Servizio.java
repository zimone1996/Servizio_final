package com.example.joelwasserman.androidbleconnectexample;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;


import com.st.BlueSTSDK.Debug;
import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Features.FeatureAccelerationEvent;
import com.st.BlueSTSDK.Features.FeatureCOSensor;
import com.st.BlueSTSDK.Features.FeatureGyroscope;
import com.st.BlueSTSDK.Features.FeatureHumidity;
import com.st.BlueSTSDK.Features.FeatureMemsSensorFusion;
import com.st.BlueSTSDK.Features.FeatureTemperature;
import com.st.BlueSTSDK.Features.standardCharacteristics.StdCharToFeatureMap;
import com.st.BlueSTSDK.Node;
import com.st.BlueSTSDK.Utils.InvalidBleAdvertiseFormat;
import com.st.BlueSTSDK.Utils.ScanCallbackBridge;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import static android.widget.Toast.LENGTH_SHORT;
import static com.st.BlueSTSDK.Config.Register.Access.R;

/**
 * Created by simone on 12/06/2018.
 */

public class Servizio extends Service
{

    Persona persona = new Persona(null,0,0,0);


    Debug debug;


    private TreeMap<Integer, Node> mDiscoverNode = new TreeMap<>();
    //  private TreeMap<Integer, Node> mConnectedNode = new TreeMap<Integer, Node>();
    String MacIndossabile = "C0:85:44:34:5D:33";
    String stato;


    BluetoothLeScanner btScanner;
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy \n HH:mm:ss");
    Date date = new Date();

    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<BluetoothDevice>();
    private ScanCallbackBridge mScanCallBack_post21;


    private Feature.FeatureListener mHumidityListener;
    private List<FeatureHumidity> mHumidity;
    private List<FeatureTemperature> mTemperature;
    private List<FeatureAcceleration> mAcceleration;
    private List<FeatureGyroscope> mGyroscope;
    private List<FeatureMemsSensorFusion> mSensorFusion;

    BluetoothAdapter btAdapter;
    BluetoothManager btManager;
    ArrayList<String> listaMACADDRESS_indossabili = new ArrayList<>();
    private JSONObject JsonObj = new JSONObject();
    SimpleDateFormat dateFormat_ora = new SimpleDateFormat(" HH:mm:ss");
    SimpleDateFormat dateFormat_minuti = new SimpleDateFormat("HH:mm");

    File memory = Environment.getExternalStorageDirectory();
    File file = new File(memory.getAbsolutePath() + "/Reports" +"/reports_"+ dateFormat_minuti.format(date), "Report_" +
            "sensore_indossabile" + ".json");


    ///////////////////////////////////Codici di stato e pericolo ////////////////////
    private String caduta = "001";
    private String sdraiato_da_troppo_tempo = "002";
    private String in_piedi_da_troppo_tempo = "003";
    private String in_piedi = "A10";
    private String sdraiato = "A11";
    private String camminata = "A12";
    private String corsa = "A13";






    public Servizio()
    {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {




        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        mp = MediaPlayer.create(getApplicationContext(), com.st.BlueSTSDK.R.raw.alarm);

        /////Aggiunta MacAddress
        String path = Environment.getExternalStorageDirectory().toString()+"/MACADDRESS_INDOSSABILE";

        File directory = new File(path);

        File[] files = directory.listFiles();

        BufferedReader br;
        String riga=null;
        for (File f : files)
        {
            if (f.isFile() && f.getPath().endsWith(".txt")) {
                try {
                    //lettura file
                    br = new BufferedReader(new FileReader(f));
                    riga = br.readLine();
                    while (riga != null) {
                        listaMACADDRESS_indossabili.add(riga);
                        riga=br.readLine();
                        System.out.println("AAAAA"+listaMACADDRESS_indossabili.size());
                    }

                    br.close();
                } catch (IOException e) {
                    Log.d("Exception",e.toString());
                }
            }
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startScanning();
        Toast.makeText(this, "Servizio indossabile partito", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent,flags,startId);
    }


    @Override
    public void onDestroy()
    {
        stopBleScan();
        Toast.makeText(getApplicationContext(), "Servizio interrotto", Toast.LENGTH_SHORT).show();
        disconnectDeviceSelected(mDiscoverNode.get(0));
        Toast.makeText(getApplicationContext(),"Disconnesso da sensore " +
                "indossabile: " + mDiscoverNode.get(0).getTag(),Toast.LENGTH_SHORT).show();
        Log.i("PROVA SERVICE", "Distruzione Service");
        mp.stop();
    }



    ///////////////////////METODO AGGIUNTA NODO/////////////////


    public boolean addNode(final Node newNode) {
        boolean aggiunta=false;
        synchronized (mDiscoverNode) {
            String newTag = newNode.getTag();

      /*      for (Integer k : mDiscoverNode.keySet()) {
                if (mDiscoverNode.get(k).getTag()==newTag)
                    return false;
            }*/


            if (newNode.getTag().equals(listaMACADDRESS_indossabili.get(0))) {
                mDiscoverNode.put(0, newNode);
                aggiunta = true;
            }
        }//synchronized

        return aggiunta;
    }//addNode

    int a=0;

    //////////////////////////SCANSIONE E AGGIUNTA NODO////////////////////////////
    private BluetoothAdapter.LeScanCallback mScanCallBack_pre21 = new BluetoothAdapter.LeScanCallback() {

        /**
         * call when an advertise package is received,
         * <p>it will notify a new node only the first time that the advertise is received,
         * and only for the nodes with a compatible advertise message.
         * if device is already build we update its the rssi value.
         * </p>
         * @param device Android remote ble device
         * @param rssi signal power
         * @param advertisedData device advertise package
         */
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] advertisedData) {
            final String deviceAddr = device.getAddress();
            //       TreeMap<Integer, Node> disponibili;
            synchronized (mDiscoverNode) {
                for (Integer k : mDiscoverNode.keySet()) {
                    if (mDiscoverNode.get(k).getTag().equals(deviceAddr)) {
                        mDiscoverNode.get(k).isAlive(rssi);
                        mDiscoverNode.get(k).upDateAdvertising(advertisedData);
                        return;
                    }

                    System.out.println("ciao"+mDiscoverNode.size());

                    if (mDiscoverNode.get(k).getState() == Node.State.Dead) {
                        mDiscoverNode.get(k).connect(getApplicationContext());
                        Toast.makeText(getApplicationContext(), "Disconnesso da sensore indossabile: "
                                + mDiscoverNode.get(0).getTag(), LENGTH_SHORT).show();
                        a = 1;
                    }
                    if (a == 1 && mDiscoverNode.get(k).getState() == Node.State.Connected) {
                        enableNeededNotification(mDiscoverNode.get(k));
                        startPlotFeature(mDiscoverNode.get(k));
                        a = 0;
                        Toast.makeText(getApplicationContext(), "Riconnesso al sensore indossabile: "
                                + mDiscoverNode.get(0).getTag(), LENGTH_SHORT).show();
                    }
                    if(mDiscoverNode.get(k).isConnected()){
                        debug =  mDiscoverNode.get(k).getDebug();


                        stopBleScan();
                    }


                }//for

            }
                try {
                    final Node newNode = new Node(device, rssi, advertisedData);
                    System.out.println("ciao");

                    if (addNode(newNode)) {

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                connectToDeviceSelected(newNode);
                                Toast.makeText(getApplicationContext(), "Connesso al sensore indossabile: "
                                        + mDiscoverNode.get(0).getTag(), Toast.LENGTH_SHORT).show();
                            }
                        }, 1000);

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                enableNeededNotification(mDiscoverNode.get(0));
                                startPlotFeature(mDiscoverNode.get(0));
                                Toast.makeText(getApplicationContext(), "Dati presi dal sensore indossabile: "
                                        + mDiscoverNode.get(0).getTag(), Toast.LENGTH_SHORT).show();
                            }
                        }, 3000);

                    }


                } catch (InvalidBleAdvertiseFormat e) {
                }


        }//onLeScan

    };//LeScanCallback



    public void startScanning() {
        System.out.println("start scanning");
        devicesDiscovered.clear();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mScanCallBack_post21 = new ScanCallbackBridge(mScanCallBack_pre21);
                btAdapter.getBluetoothLeScanner().startScan( mScanCallBack_post21);
            }
        });
    }




    public void connectToDeviceSelected(Node n) {
        if (!n.isConnected()) {
            n.addExternalCharacteristics(new StdCharToFeatureMap());
            n.connect(this);


        }
    }


    public void disconnectDeviceSelected(Node n) {
        if(n.isConnected()){
            n.disconnect();
        }
    }


///////////////////////////////////////////////TEMPERATURA///////////////////////////




    float max_T;

    //   String dataString;
    Float dataString;
    //   String datastring2;
    private final static ExtractDataFunction sExtractDataTemp = new ExtractDataFunction() {
        public float getData(Feature.Sample s) {
            return FeatureTemperature.getTemperature(s);
        }//getData
    };
    public static final String FEATURE_TEMPERATURE = "Temperature";

    private final Feature.FeatureListener mTemperatureListener = new Feature.FeatureListener() {


        long startTime4 = System.currentTimeMillis();


        @Override
        public void onUpdate(Feature f, Feature.Sample sample) {

            String unit = mTemperature.get(0).getFieldsDesc()[0].getUnit();
            final float data[] = extractData(mTemperature, sExtractDataTemp);
            float T = data[1];
            //   float T1 = data[2];
            //       dataString = FEATURE_TEMPERATURE + "  :  " + String.format("%.2f", T) + "[" + unit + "]";


            if(T>max_T) {
                max_T = T;
                persona.setTemperature(max_T);
   //             debug.write("#"+persona.getTemperature());
                System.out.println("# "+persona.getTemperature());
            }
            persona.setStato(stato);

            System.out.println("PERSONA " + persona.getStato());

            dataString =   data[1];
            //  datastring2 = FEATURE_TEMPERATURE + " : " + String.format("%.2f", T1) + "[" + unit + "]";


            final String dateTime = dateFormat.format(date);
            final String dataOra = dateFormat_ora.format(date);



            File file = new File(getApplicationContext().getFilesDir(), "stato.txt");
            FileOutputStream fos;

            try {
                //scrittura
                fos = new FileOutputStream(file);
                fos.write(stato.getBytes());
                fos.close();


            } catch (Exception e) {
                e.printStackTrace();
            }

            }

    };

///////////////////////////////////////ACCELERAZIONE/////////////////////////////////////



    private double massimoacc;
    private final static ExtractDataFunction sExtractDataAccX = new ExtractDataFunction() {
        public float getData(Feature.Sample s) {
            return FeatureAcceleration.getAccX(s);
        }//getData
    };
    private final static ExtractDataFunction sExtractDataAccY = new ExtractDataFunction() {
        public float getData(Feature.Sample s) {
            return FeatureAcceleration.getAccY(s);
        }//getData
    };
    private final static ExtractDataFunction sExtractDataAccZ = new ExtractDataFunction() {
        public float getData(Feature.Sample s) {
            return FeatureAcceleration.getAccZ(s);
        }//getData
    };

    public static final String FEATURE_ACC = "Acceleration";

    ArrayList<Float> listOfAccX = new ArrayList<Float>();
    ArrayList<Float> listOfAccY = new ArrayList<Float>();
    ArrayList<Float> listOfAccZ = new ArrayList<Float>();
    private boolean elabora = false;
    boolean verifica = false;


    float accZsecmedia;
    float accXsec;
    float accYsec;
    float accZsec;
    MediaPlayer mp;


    private FeatureAccelerationEvent.FeatureAccelerationEventListener mAccelerationListener =
            new FeatureAccelerationEvent.FeatureAccelerationEventListener() {

                long startTime = System.currentTimeMillis();

                @Override
                public void onDetectableEventChange(FeatureAccelerationEvent f,
                                                    FeatureAccelerationEvent.DetectableEvent event, boolean newStatus) {
                }

                @Override
                public void onUpdate(Feature f, Feature.Sample sample) {

                    float datax[] = extractData(mAcceleration, sExtractDataAccX);
                    float datay[] = extractData(mAcceleration, sExtractDataAccY);
                    float dataz[] = extractData(mAcceleration, sExtractDataAccZ);


                    final String acc1sec = FEATURE_ACC + ":\n" +
                            "\tData: ( X: " + String.valueOf(String.format("%.1f", datax[0])) + " Y: " +
                            String.valueOf(String.format("%.1f", datay[0]))
                            + " Z: " + String.valueOf(String.format("%.1f", dataz[0])) + " )";


                    if((System.currentTimeMillis()-startTime)<1500) {
                        listOfAccX.add(datax[0]);
                        listOfAccY.add(datay[0]);
                        listOfAccZ.add(dataz[0]);
                    }
                    else {
                        elabora=true;
                        startTime = System.currentTimeMillis();
                    }

                    if(elabora)
                        caduta();

                    Date date = new Date();
                    final String dateTime = dateFormat.format(date);



                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            // ############## DISCRIMINATION ###############
                            // CAMMINATA
                            if (massimoacc < 2000 && massimoacc > 1150 ) {
                                warning_caduta = false;
                                stato =  camminata;
                            }

                            //FERMO IN PIEDI
                            if (massimoacc < 1150) {
                                warning_caduta = false;
                                if(!warning_inpiedi)
                                    stato =  in_piedi;
                                else {
                                    stato = in_piedi_da_troppo_tempo;
                                }
                            }

                            //CORSA
                            if (massimoacc > 2000 && massimogir > 180) {
                                warning_caduta = false;
                                stato = corsa;
                                if (maxDiffx > 0.4 || maxDiffy > 0.4 || maxDiffz > 0.4) {
                                    verifica = true;
                                }
                            }

                            //SDRAIATO
                            if (!verifica && ((accZsecmedia < 1200 && accZsecmedia > 800) ||
                                    (accZsecmedia < -800 && accZsecmedia > -1200))) {
                                if(!warning_sdraiato)
                                    stato = sdraiato;
                                else{
                                    stato = sdraiato_da_troppo_tempo;
                                }
                            }




                            //CADUTA
                            if (verifica && ((accZsecmedia < 1200 && accZsecmedia > 800) ||
                                    (accZsecmedia < -800 && accZsecmedia > -1200))) {
                                stato = caduta;
                                //          stopPlotting(mDiscoverNode.get(0));
                           /*     listaStato.put(stato + dateTime);
                                try {
                                    fileJSONStato(listaStato);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }*/
                                warning_caduta = true;
                                //  verifica = false;

                                mp.start();
                                mp.setLooping(true);
                            }
                        }
                    });

                }
            };


    ArrayList<Double> accelerazionelist = new ArrayList();
    Double acc;
    int count=0;
    int count1=0;
    int count2=0;
    private boolean verifica1;
    private boolean warning_sdraiato = false;
    private boolean warning_inpiedi = false;
    private boolean warning_caduta = false;


    private void caduta() {
        accXsec=0;
        accYsec=0;
        accZsec=0;
        int i;
        accelerazionelist.clear();
        int lunghezza=listOfAccX.size();
        massimoacc = 0;
        for (i = 0; i < lunghezza; i++) {
            try {
                accXsec += listOfAccX.get(i);
                accYsec += listOfAccY.get(i);
                accZsec += listOfAccZ.get(i);

                accelerazionelist.add(Math.sqrt(Math.pow(listOfAccX.get(i), 2) + Math.pow(listOfAccY.get(i), 2) + Math.pow(listOfAccZ.get(i), 2)));
            }
            catch (Exception e){
            }
            try{
            acc =  accelerazionelist.get(i);
            }
            catch (Exception e){
            }
            if (massimoacc< acc) {
                massimoacc =  acc;
            }

        }
        if (massimoacc<1150)
            count++;
        else
            count=0;


        ///////////////////TENTATIVO!!!!///////////////
        if(count>10) {
            warning_inpiedi = true;
        }
        else {
            warning_inpiedi = false;
        }


        if ((accZsecmedia<1200 && accZsecmedia>800) || (accZsecmedia<-800 && accZsecmedia>-1200))
            count1++;
        else
            count1=0;
        if(count1>10) {
            warning_sdraiato = true;
        }
        else {
            warning_sdraiato = false;
        }


        if (massimoacc>2000 && massimogir>180 && (maxDiffx > 0.4 || maxDiffy > 0.4 || maxDiffz > 0.4))
            verifica1=true;
        if(verifica1) {
            count2++;
        }
        if(count2>3 && !warning_caduta) {
            count2 = 0;
            verifica1 = false;
            verifica = false;
        }
        if(warning_caduta)
            verifica = true;


        accZsecmedia=accZsec/lunghezza;



        elabora=false;
        listOfAccX.clear();
        listOfAccY.clear();
        listOfAccZ.clear();
    }


    public static final String FEATURE_HUMIDITY = "Humidity";
    String dataString1;
    private final static String HUM_FORMAT = "%.1f [%s]";

    TreeMap<String,String> listaStato = new TreeMap<>();



    private final static ExtractDataFunction sExtractDataHum = new ExtractDataFunction() {
        public float getData(com.st.BlueSTSDK.Feature.Sample s) {
            return FeatureHumidity.getHumidity(s);
        }//getData
    };


    float max_H=0;

    private class HumidityListener implements com.st.BlueSTSDK.Feature.FeatureListener {

        public HumidityListener(Resources res) {
        }

        TreeMap<String,Float> listaHum = new TreeMap<>();
        TreeMap<String,Float> listaTemp = new TreeMap<>();

        long startTime5 = System.currentTimeMillis();

        @Override
        public void onUpdate(com.st.BlueSTSDK.Feature f, com.st.BlueSTSDK.Feature.Sample sample) {

            String unit = mHumidity.get(0).getFieldsDesc()[0].getUnit();
            final float data[] = extractData(mHumidity, sExtractDataHum);
            dataString1 = FEATURE_HUMIDITY + "  :  " + getDisplayString(HUM_FORMAT, unit, data);

            if(data[0]>max_H) {
                max_H = data[0];
                persona.setHumidity(max_H);
              System.out.println("PERSONA " + persona.getHumidity());
            }
            Date date = new Date();
            final String dateTime = dateFormat.format(date);
            final String dataOra = dateFormat_ora.format(date);

///////////////////////////////////////////////Creazione file json////////////////////////////////////////////////////////////////////////////////////////////////////

            if((System.currentTimeMillis()-startTime5) >= 1000) {
//                listaH.put(dataString1 + dateTime);
//                try {
//                    fileJSONH(listaH);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                listaHum.put(dateTime,data[0]);

                if(listaHum.size() == 10){
                    try {
                        JsonObj.put("Acquisizioni H " +"\n" + dataOra, JsonarrayH(listaHum));
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        Log.e("VOLLEY" , "ERROR" , e);
                    }
                    listaHum.clear();
                }


                //          listaStato.put( stato + dateTime);
                //          try {
                //              fileJSONStato(listaStato);
                //         } catch (IOException e) {
                //             e.printStackTrace();
                //          }
                listaStato.put(dateTime,stato);

                if(listaStato.size() == 10){
                    try {
                        JsonObj.put("Acquisizioni Stato " +"\n" + dataOra, JsonarrayStato(listaStato));
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        Log.e("VOLLEY" , "ERROR" , e);
                    }
                    listaStato.clear();
                }






//                try {
//                    fileJSONT(listaT);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                listaTemp.put(dateTime, dataString);


                if(listaTemp.size() == 10) {
                    try {

                        JsonObj.put("Acquisizioni T " + "\n" + dataOra, JsonarrayT(listaTemp));
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        Log.e("VOLLEY" , "ERROR" , e);
                    }

                    Writer output = null;




                    try {
                        output = new BufferedWriter(new FileWriter(file));
                        output.write('\n' + JsonObj.toString());
                        output.close();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        Log.e("VOLLEY" , "ERROR" , e);
                    }

                    listaTemp.clear();
                }

                startTime5 = System.currentTimeMillis();
            }

        }
    }

   /* public void fileJSONH (JSONArray array) throws IOException {


        Writer output = null;

        // json.put(dataString);

        File sdCard = Environment.getExternalStorageDirectory();
        File file = new File (sdCard.getAbsolutePath()+"/reports" , "Humidity_report_sensore_indossabile"
                +".json");
        output = new BufferedWriter(new FileWriter(file));
        try {
            output.write('\n' + array.toString());
        }
        catch (Exception e){

        }
        output.close();
    }



    public void fileJSONT (JSONArray array) throws IOException {
        Writer output = null;

        File sdCard = Environment.getExternalStorageDirectory();
        File file = new File (sdCard.getAbsolutePath()+"/reports" , "Temperature_report_sensore_indossabile"
                + ".json");
        output = new BufferedWriter(new FileWriter(file));
        try {
            output.write('\n' + array.toString());
        }
        catch (Exception e){
        }
        output.close();
    }
*/


    /*public void fileJSONStato (JSONArray array) throws IOException {
        Writer output = null;

        File sdCard = Environment.getExternalStorageDirectory();
        File file = new File (sdCard.getAbsolutePath()+"/reports" , "State_report_sensore_indossabile"
                +".json");
        output = new BufferedWriter(new FileWriter(file));
        try {
            output.write('\n' + array.toString());
        }
        catch (Exception e){

        }
        output.close();
    }*/


    private JSONArray JsonarrayH(TreeMap<String,Float> lista) {
        JSONArray JSONH = new JSONArray();
        for(String data : lista.keySet()) {
            JSONObject umidità = new JSONObject();
            try {
                umidità.put("Valore:", lista.get(data));
                umidità.put("Unit","[%]");
                umidità.put("timestamp", data);
                umidità.put("Nodo", listaMACADDRESS_indossabili.get(0));
                JSONH.put(umidità);
            }
            catch (Exception e){
                e.printStackTrace();
                Log.e("VOLLEY" , "ERROR" , e);
            }
        }
        return JSONH;
    }



    private JSONArray JsonarrayT(TreeMap<String,Float> lista) {
        JSONArray JSONtemp = new JSONArray();
        for(String data : lista.keySet()) {
            JSONObject temperatura = new JSONObject();
            try {
                temperatura.put("Valore:", lista.get(data));
                temperatura.put("Unit","[°C]");
                temperatura.put("timestamp",data);
                temperatura.put("Nodo",listaMACADDRESS_indossabili.get(0));
                JSONtemp.put(temperatura);
            }
            catch (Exception e){
                e.printStackTrace();
                Log.e("VOLLEY" , "ERROR" , e);
            }
        }
        return JSONtemp;
    }



    private JSONArray JsonarrayStato (TreeMap<String,String> lista) throws IOException {
        JSONArray JSONstato = new JSONArray();
        for(String data : lista.keySet()) {
            JSONObject stato = new JSONObject();
            try {
                stato.put("Stato:", lista.get(data));
                stato.put("timestamp",data);
                stato.put("Nodo",listaMACADDRESS_indossabili.get(0));
                JSONstato.put(stato);
            }
            catch (Exception e){
                e.printStackTrace();
                Log.e("VOLLEY" , "ERROR" , e);
            }
        }

        return JSONstato;
    }


//////////////////////////////////////////GIROSCOPIO///////////////////////////////////////


    ArrayList<Float> listOfGirX = new ArrayList<Float>();
    ArrayList<Float> listOfGirY = new ArrayList<Float>();
    ArrayList<Float> listOfGirZ = new ArrayList<Float>();
    public static final String FEATURE_QUAT = "Gyroscope";


    private final static ExtractDataFunction sExtractDataGirX = new ExtractDataFunction() {
        public float getData(Feature.Sample s) {
            return FeatureGyroscope.getGyroX(s);
        }//getData
    };
    private final static ExtractDataFunction sExtractDataGirY = new ExtractDataFunction() {
        public float getData(Feature.Sample s) {
            return FeatureGyroscope.getGyroY(s);
        }//getData
    };
    private final static ExtractDataFunction sExtractDataGirZ = new ExtractDataFunction() {
        public float getData(Feature.Sample s) {
            return FeatureGyroscope.getGyroZ(s);
        }//getData
    };

    private boolean elaborag = false;

    ArrayList<Double> moduliGir = new ArrayList();

    private double massimogir;
    private Double mG;



    private final Feature.FeatureListener mGyroscopeListener = new Feature.FeatureListener() {
        long startTime1 = System.currentTimeMillis();

        @Override
        public void onUpdate(Feature f, Feature.Sample sample) {

            float datagx[] = extractData(mGyroscope, sExtractDataGirX);
            float datagy[] = extractData(mGyroscope, sExtractDataGirY);
            float datagz[] = extractData(mGyroscope, sExtractDataGirZ);


            if((System.currentTimeMillis()-startTime1)<1500) {
                listOfGirX.add(datagx[0]);
                listOfGirY.add(datagy[0]);
                listOfGirZ.add(datagz[0]);
            }
            else {
                elaborag=true;
                startTime1 = System.currentTimeMillis();
            }

            if(elaborag)
                cadutag();


            final String girsec = FEATURE_QUAT + ":\n" +
                    "\tData: ( X: " + String.valueOf(String.format("%.1f", datagx[0]))
                    + " Y: " + String.valueOf(String.format("%.1f", datagy[0]))
                    + " Z: " + String.valueOf(String.format("%.1f", datagz[0])) + " )";


        }
    };


    private void cadutag() {

        int i;
        moduliGir.clear();
        massimogir = 0;
        int lunghezza=listOfGirX.size();
        for (i = 0; i < lunghezza; i++) {
            try {
                moduliGir.add(Math.sqrt(Math.pow(listOfGirX.get(i), 2) + Math.pow(listOfGirY.get(i), 2) + Math.pow(listOfGirZ.get(i), 2)));

                mG =  moduliGir.get(i);
                if (massimogir < mG) {
                    massimogir =  mG;
                }
            }
            catch (Exception e){

            }

        }

        elaborag=false;
        listOfGirX.clear();
        listOfGirY.clear();
        listOfGirZ.clear();
    }


    //////////////////////////////////////QUATERNIONI////////////////////////////////////////

    private final static ExtractDataFunction mSensorFusion_x = new ExtractDataFunction() {
        public float getData(Feature.Sample s) {
            return FeatureMemsSensorFusion.getQi(s);
        }//getData
    };

    private final static ExtractDataFunction mSensorFusion_y = new ExtractDataFunction() {
        public float getData(Feature.Sample s) {
            return FeatureMemsSensorFusion.getQj(s);
        }//getData
    };

    private final static ExtractDataFunction mSensorFusion_z = new ExtractDataFunction() {
        public float getData(Feature.Sample s) {
            return FeatureMemsSensorFusion.getQk(s);
        }//getData
    };

    public static final String FEATURE_GIR = "Quatermioni:";

    private boolean elaboraMems;


    ArrayList<Float> listOfQX = new ArrayList<Float>();
    ArrayList<Float> listOfQY = new ArrayList<Float>();
    ArrayList<Float> listOfQZ = new ArrayList<Float>();

    private double maxDiffx;
    private double maxDiffy;
    private double maxDiffz;


    private Feature.FeatureListener mMemesListener = new Feature.FeatureListener() {

        long startTime2 = System.currentTimeMillis();

        @Override
        public void onUpdate(Feature f, Feature.Sample sample) {

            float Qx[] = extractData(mSensorFusion, mSensorFusion_x);
            float Qy[] = extractData(mSensorFusion, mSensorFusion_y);
            float Qz[] = extractData(mSensorFusion, mSensorFusion_z);


            final String quat = FEATURE_GIR + "\n"+
                    "\tData: ( X: " + String.valueOf(String.format("%.1f", Qx[0]))
                    + " Y: " + String.valueOf(String.format("%.1f", Qy[0]))
                    + " Z: " + String.valueOf(String.format("%.1f", Qz[0])) + " )";

            if((System.currentTimeMillis()-startTime2)<1500) {
                listOfQX.add(Qx[0]);
                listOfQY.add(Qy[0]);
                listOfQZ.add(Qz[0]);
            }
            else {
                elaboraMems=true;
                startTime2 = System.currentTimeMillis();
            }

            if(elaboraMems)
                cadutaQs();

        }
    };

  private void cadutaQs() {
        try {
            maxDiffx = Collections.max(listOfQX) - Collections.min(listOfQX);
            maxDiffy = Collections.max(listOfQY) - Collections.min(listOfQY);
            maxDiffz = Collections.max(listOfQZ) - Collections.min(listOfQZ);
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e("VOLLEY" , "ERROR" , e);
        }

        elaboraMems=false;
        listOfQX.clear();
        listOfQY.clear();
        listOfQZ.clear();
    }



    public void startPlotFeature(Node node) {
        if (node == null)
            return;

        for (Feature f : mHumidity) {
            f.addFeatureListener(mHumidityListener);
            node.enableNotification(f);
        }

        for (Feature f : mTemperature) {
            f.addFeatureListener(mTemperatureListener);
            node.enableNotification(f);
        }//for

        for (Feature f : mAcceleration) {
            f.addFeatureListener(mAccelerationListener);
            node.enableNotification(f);
        }//for

        for (Feature f : mGyroscope) {
            f.addFeatureListener(mGyroscopeListener);
            node.enableNotification(f);
        }//for
        for (Feature f : mSensorFusion) {
            f.addFeatureListener(mMemesListener);
            node.enableNotification(f);
        }//for
    }

    public void stopPlotting(Node node) {
        if (node == null)
            return;
        for (Feature f : mHumidity) {
            f.removeFeatureListener(mHumidityListener);
            node.disableNotification(f);
        }//for
        for (Feature f : mTemperature) {
            f.removeFeatureListener(mTemperatureListener);
            node.disableNotification(f);
        }//for
        for (Feature f : mAcceleration) {
            f.removeFeatureListener(mAccelerationListener);
            node.disableNotification(f);
        }//for
        for (Feature f : mGyroscope) {
            f.removeFeatureListener(mGyroscopeListener);
            node.disableNotification(f);
        }//for
        for (Feature f : mSensorFusion) {
            f.removeFeatureListener(mMemesListener);
            node.disableNotification(f);
        }//for

    }


    protected void enableNeededNotification(@NonNull Node node) {

        mHumidity = node.getFeatures(FeatureHumidity.class);
        if (!mHumidity.isEmpty()) {
            mHumidityListener = new HumidityListener(getResources());
            for (Feature f : mHumidity) {
                f.addFeatureListener(mHumidityListener);
                node.enableNotification(f);
            }//for
        }

        mTemperature = node.getFeatures(FeatureTemperature.class);
        if (!mTemperature.isEmpty()) {
            for (Feature f : mTemperature) {
                f.addFeatureListener(mTemperatureListener);
                node.enableNotification(f);
            }//for
        }

        mAcceleration = node.getFeatures(FeatureAcceleration.class);
        if (!mAcceleration.isEmpty()) {
            for (Feature f : mAcceleration) {
                f.addFeatureListener(mAccelerationListener);
                node.enableNotification(f);
            }//for
        }

        mGyroscope = node.getFeatures(FeatureGyroscope.class);
        if (!mGyroscope.isEmpty()) {
            for (Feature f : mGyroscope) {
                f.addFeatureListener(mGyroscopeListener);
                node.enableNotification(f);
            }//for
        }

        mSensorFusion = node.getFeatures(FeatureMemsSensorFusion.class);
        if (!mSensorFusion.isEmpty()) {
            //   View.OnClickListener forceUpdate = new ForceUpdateFeature(mSensorFusion);
            for (Feature f : mSensorFusion) {
                f.addFeatureListener(mMemesListener);
                node.enableNotification(f);
            }//for
        }
    }

    public interface ExtractDataFunction{
        float getData(com.st.BlueSTSDK.Feature.Sample s);
    }

    private static float[] extractData(final List<? extends com.st.BlueSTSDK.Feature> features,
                                       final ExtractDataFunction extractDataFunction) {
        int nFeature = features.size();
        float data[] = new float[nFeature];
        for (int i = 0; i < nFeature; i++) {
            data[i] = extractDataFunction.getData(features.get(i).getSample());
        }//for
        return data;
    }//extractData


    private static String getDisplayString(final String format, final String unit,
                                           float values[]) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length - 1; i++) {
            sb.append(String.format(format, values[i], unit));
            sb.append('\n');
        }//for
        sb.append(String.format(format, values[values.length - 1], unit));
        return sb.toString();
    }



    private void stopBleScan(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            stopBleScan_post21();
        }else
            stopBleScan_pre21();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private void stopBleScan_pre21() {
        btAdapter.stopLeScan(mScanCallBack_pre21);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void stopBleScan_post21() {
        btAdapter.getBluetoothLeScanner().stopScan(mScanCallBack_post21);
    }
}