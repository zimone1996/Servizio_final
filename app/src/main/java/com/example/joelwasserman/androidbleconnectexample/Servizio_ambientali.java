package com.example.joelwasserman.androidbleconnectexample;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;
import com.st.BlueSTSDK.Debug;
import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureCOSensor;
import com.st.BlueSTSDK.Features.FeatureHumidity;
import com.st.BlueSTSDK.Features.FeatureTemperature;
import com.st.BlueSTSDK.Features.standardCharacteristics.StdCharToFeatureMap;
import com.st.BlueSTSDK.Node;
import com.st.BlueSTSDK.Utils.InvalidBleAdvertiseFormat;
import com.st.BlueSTSDK.Utils.ScanCallbackBridge;
import net.time4j.android.ApplicationStarter;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import static android.widget.Toast.LENGTH_SHORT;



public class Servizio_ambientali extends Service {

    public Servizio_ambientali() {
    }

    //TODO caricare file csv delle posizioni dei sensori

    private MyFTPClientFunctions ftpclient = null;

    Debug mDebugConsole;

    double posizione_indossabile_x;
    double posizione_indossabile_y;

    private TreeMap<Integer, Node> mDiscoverNode = new TreeMap<>();
    private TreeMap<Long, Node> connessi = new TreeMap<>();

    TreeMap<String, ArrayList<String>> mappa_posizioni = new TreeMap<>();

    private HashMap<Node, Humidity_Listener> nodeTomHumidityListener = new HashMap<>();
    private HashMap<Node, TemperatureListener> nodeTomTemperatureListener = new HashMap<>();
    private HashMap<Node, CO_listener> nodeTomCOlistener = new HashMap<>();

    private HashMap<Node, List<FeatureHumidity>> listaHumidity = new HashMap<>();
    private HashMap<Node, List<FeatureTemperature>> listaTemperature = new HashMap<>();
    private HashMap<Node, List<FeatureCOSensor>> listaCOgas = new HashMap<>();


    private HashMap<Node,Double> mappa_pesi = new HashMap<>();

    private HashMap<String, Integer> mappa_MacAddress = new HashMap<>();

    private HashMap<Node, JSONObject> JsonObj = new HashMap<>();

    private JSONObject allarmi = new JSONObject();

    private JSONObject posizione = new JSONObject();

    private HashMap<Node, File> Node2File = new HashMap<>();

    private ArrayList<Node> listadigiaconnessi = new ArrayList<>();

    private HashMap<Node, Boolean> datipresi = new HashMap<>();

    File memory = Environment.getExternalStorageDirectory();




    SimpleDateFormat dateFormat_ora = new SimpleDateFormat("HH:mm:ss");
    SimpleDateFormat dateFormat_minuti = new SimpleDateFormat("HH:mm");



    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<BluetoothDevice>();
    private ScanCallbackBridge mScanCallBack_post21;


    BluetoothAdapter btAdapter;
    BluetoothManager btManager;

    ArrayList<String> listaMACADDRESS_ambientali = new ArrayList<>();

    String path_cartella;

    private HashMap<Node, Float> lista_umidita = new HashMap<Node, Float>();
    private HashMap<Node, Float> lista_temperatura = new HashMap<Node, Float>();
    private HashMap<Node, Float> lista_CO = new HashMap<Node, Float>();


    String nome_utente;



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    private boolean connessoftp=false;

    Date date = new Date();
    String minuti;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        ApplicationStarter.initialize(this, true); // with prefetch on background thread


       // readExcelFile(getApplicationContext(), "prova.xlsx",mappa_posizioni);

        ftpclient = new MyFTPClientFunctions();

        if (isOnline(Servizio_ambientali.this)) {
            connectToFTPAddress();
        } else {
            Toast.makeText(Servizio_ambientali.this,
                    "Please check your internet connection!",
                    Toast.LENGTH_LONG).show();
        }



            //creo cartella con tempo

/*
            File f1 = new File(Environment.getExternalStorageDirectory()+"/Reports", "reports_"+minuti);
            f1.mkdirs();*/

        File f1;




/////Aggiunta MacAddress e mappa posizioni


        String path = Environment.getExternalStorageDirectory().toString() + "/MACADDRESS/Mac.json";

 /*       File directory = new File(path);

        File[] files = directory.listFiles();

        BufferedReader br;
        String riga = null;
        for (File f : files) {
            if (f.isFile() && f.getPath().endsWith(".json")) {
                System.out.println("QWER"+f.getName().toString());
                try {
                    //lettura file
                    br = new BufferedReader(new FileReader(f));

                    riga = br.readLine();
                    int i=1;
                    while (riga != null) {
                        listaMACADDRESS_ambientali.add(riga);

                        mappa_MacAddress.put(riga,i);
                        riga = br.readLine();
                        i++;
                    }

                    br.close();
                } catch (IOException e) {
                    Log.d("Exception", e.toString());
                }
            }
        }*/


                try {
                    File yourFile = new File(path);
                    FileInputStream stream = new FileInputStream(yourFile);
                    String jsonStr = null;
                    try {
                        FileChannel fc = stream.getChannel();
                        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

                        jsonStr = Charset.defaultCharset().decode(bb).toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        stream.close();
                    }


                    JSONObject jsonObj = new JSONObject(jsonStr);


                    // Getting data JSON Array nodes
                    JSONArray data  = jsonObj.getJSONArray("Sensori_Ambientali");

                    // looping through All nodes
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject c = data.getJSONObject(i);
                        String id = c.getString("Mac_Address");
                        JSONObject pos = c.getJSONObject("Posizione");
                        String pos_x = pos.getString("X");
                        String pos_y = pos.getString("Y");

                        // adding each child node to HashMap key => value
                        listaMACADDRESS_ambientali.add(id);
                        ArrayList<String> position = new ArrayList<>();
                        position.add(pos_x);
                        position.add(pos_y);
                        mappa_posizioni.put(id,position);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }




//NUOVOOOO




        /////////////////////



        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        //    MacAddress.add(MacAmbientale);
        //    MacAddress.add(MacAmbientale2);
    }



    long ora_inizio;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        ora_inizio =   System.currentTimeMillis();

        nome_utente = intent.getStringExtra("Nome");
        System.out.println("stampato"+nome_utente);
        minuti = intent.getStringExtra("Minuti");
        File f1 = new File(memory.getAbsolutePath() + "/Reports" + "/reports_" + minuti);
        path_cartella = f1.getAbsolutePath();
        System.out.println("path"+path_cartella);

        startScanning();
        Toast.makeText(this, "Servizio ambientale partito", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);

    }


    @Override
    public void onDestroy() {
        stopBleScan();
        Toast.makeText(getApplicationContext(), "Servizio interrotto", Toast.LENGTH_SHORT).show();
        for (int i : mDiscoverNode.keySet()) {
            disconnectDeviceSelected(mDiscoverNode.get(i));
            Toast.makeText(getApplicationContext(), "Disconnesso da sensore ambientale: " +
                    mDiscoverNode.get(i).getTag(), Toast.LENGTH_SHORT).show();


        }
        mDiscoverNode.clear();
        connessi.clear();
    }


    ///////////////////////METODO AGGIUNTA NODO NEI DISCOVERED/////////////////


    public boolean addNode(final Node newNode) {
        boolean aggiunta = true;
        synchronized (mDiscoverNode) {
            String newTag = newNode.getTag();


            for (Integer k : mDiscoverNode.keySet())
                if (mDiscoverNode.get(k).getTag() == newTag) {
                  return false;
                }

            /*     for (long k : connessi.keySet())
                if (connessi.get(k).getTag() == newTag) {
                  return false;
                }
*/

            for (String s : listaMACADDRESS_ambientali) {
                if (newNode.getTag().equals(s)) {
                    mDiscoverNode.put(mDiscoverNode.size(), newNode);
                    nodeTomHumidityListener.put(newNode, new Humidity_Listener(getResources(), newNode));
                    nodeTomTemperatureListener.put(newNode, new TemperatureListener(getResources(), newNode));
                    nodeTomCOlistener.put(newNode, new CO_listener(getResources(), newNode));
                    aggiunta = true;

                    datipresi.put(newNode, false);

                    JsonObj.put(newNode, new JSONObject());
                }
            }


        }//synchronized
        return aggiunta;
    }//addNode

    int a = 0;

    int entrato = 0;

    long ora = 0;


    int []value = new int [10]; //array di 10 elementi
    String sup[] = new String[10];
    ArrayList<Integer> numeri = new ArrayList<Integer>();
    String stringa_finale_da_iviare = "";
    //////////////////////////SCANSIONE, AGGIUNTA NODO E CONNESSIONE////////////////////////////
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
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] advertisedData) {


                    final String deviceAddr = device.getAddress();
            //       TreeMap<Integer, Node> disponibili;
            synchronized (mDiscoverNode) {
                synchronized (connessi) {




                    for (final Integer k : mDiscoverNode.keySet()) {



                        mDiscoverNode.get(k).readRssi();
                        System.out.println("PPPPPP"+mDiscoverNode.get(k).getLastRssiUpdateDate());


                        ////////////LOCALIZZAZIONE/////////////////
                        //prendo peso e lo metto nella mappa
                        Localizzazione loc = new Localizzazione(mDiscoverNode.get(k).getLastRssi());
                        mappa_pesi.put(mDiscoverNode.get(k),loc.getWeight());
                        if(!mappa_pesi.isEmpty())
                            System.out.println("peso"+mappa_pesi.get(mDiscoverNode.get(k)));

                //        mDiscoverNode.get(k).readRssi();
                 //       System.out.println("peso" + mDiscoverNode.get(k).getLastRssi());




                        ////////////////////

                        if (mDiscoverNode.get(k).getTag().equals(deviceAddr)) {
                            mDiscoverNode.get(k).isAlive(rssi);
                            mDiscoverNode.get(k).upDateAdvertising(advertisedData);




                            return;
                        }

//condizione temporale


                        if(-ora_inizio + System.currentTimeMillis() > 5000) {


                            if (!mDiscoverNode.get(k).isConnected() && !connessi.isEmpty() && connessi.size() == 5) { //5

                                gestisci(mDiscoverNode.get(k));

                                if (mDiscoverNode.get(k).isConnected()) {
                                    enableNeededNotification(mDiscoverNode.get(k));
                                    startPlotFeature(mDiscoverNode.get(k));
                                    Toast.makeText(getApplicationContext(), "Dati presi dal sensore ambientale"
                                            + mDiscoverNode.get(k).getTag(), Toast.LENGTH_SHORT).show();
                                }
                            }


                            //aggiunta per ricollegarsi a nodi persi se entrato in RR almeno una volta
                            if (sonoentrato) {
                                if (connessi.size() < 5) {
                                    try {
                                        //NON DEVO AGGIUNGERE CONDIZIONE SU PRESENZA IN CONNESSI?
                                        if (!presentenellamappa(mDiscoverNode.get(k), connessi)) {
                                            connectToDevice(mDiscoverNode.get(k));
                                            connessi.put(System.currentTimeMillis(), mDiscoverNode.get(k));
                                        }
                                    } catch (Exception e) {

                                    }
                                    //         connessi.put(System.currentTimeMillis(),mDiscoverNode.get(k));
                                    datipresi.remove(mDiscoverNode.get(k));
                                    datipresi.put(mDiscoverNode.get(k), false);
                                }
                            }

                            //riconnessione automatica se non caso di RR
                            if (mDiscoverNode.get(k).getState() == Node.State.Dead && connessi.size() <= 5 && !sonoentrato) { //5


                                if (presentenellamappa(mDiscoverNode.get(k), connessi))
                                    rimuovidallamappa(mDiscoverNode.get(k), connessi);

                                Toast.makeText(getApplicationContext(), "Disconnesso da sensore ambientale: " + mDiscoverNode.get(k).getTag()
                                        , LENGTH_SHORT).show();

                                connectToDevice(mDiscoverNode.get(k));
                                datipresi.remove(mDiscoverNode.get(k));
                                datipresi.put(mDiscoverNode.get(k), false);
                            }

                            System.out.println("Stato:" + mDiscoverNode.get(k).getTag() + " " + mDiscoverNode.get(k).getState());


                            if (mDiscoverNode.get(k).isConnected()) {


                                //verifica sui dati se presi
                                if (!datipresi.get(mDiscoverNode.get(k))) {
                                    enableNeededNotification(mDiscoverNode.get(k));
                                    startPlotFeature(mDiscoverNode.get(k));
                                    Toast.makeText(getApplicationContext(), "Dati presi dal sensore ambientale"
                                            + mDiscoverNode.get(k).getTag(), Toast.LENGTH_SHORT).show();
                                    datipresi.put(mDiscoverNode.get(k), true);
                                }

                                //se connesso e non presente in connessi lo aggiungo
                                if (presentenellamappa(mDiscoverNode.get(k), connessi) == false) {
                                    connessi.put(System.currentTimeMillis(), mDiscoverNode.get(k));
                                }

                                //aggiungo nella lista già connessi
                                if (!presentenellalista(mDiscoverNode.get(k), listadigiaconnessi) && connessi.size() <= 5) //5
                                    listadigiaconnessi.add(mDiscoverNode.get(k));

//                                Toast.makeText(getApplicationContext(), "Connesso al sensore ambientale:"
//                                        + mDiscoverNode.get(k).getTag(), Toast.LENGTH_SHORT).show();
                            }


                            //se non collegato rimuovo
                            if (!mDiscoverNode.get(k).isConnected() && mDiscoverNode.get(k).getState() != Node.State.Connecting && sonoentrato) {

                                //
                                if (presentenellamappa(mDiscoverNode.get(k), connessi)) {
                                    rimuovidallamappa(mDiscoverNode.get(k), connessi);

                                    ///aggiunta di prova
                                }

                            }
                        }

                        System.out.println("Lista connessi" + connessi.size());
                        System.out.println("Lista discovered" + mDiscoverNode.size());
                        System.out.println("Lista già connessi" + listadigiaconnessi.size());
                        System.out.println("Dati presi" + datipresi.size());


                        for(Node i: listadigiaconnessi)  {
                            for (String n : mappa_MacAddress.keySet()) {//mi prendo i MacAddress presenti in file esterno
                                if (i.getTag().equals(n)) {  // confronto quest MacAddress con quelli presenti in già connessi
                                    int numb = mappa_MacAddress.get(n); //prendo il numero corrispondente ala MacAddress
                                    if (!numeri.contains(numb)) //verifico se il numero corrispondente sia già presente in numeri
                                         numeri.add(mappa_MacAddress.get(n)); //aggiungo il numero alla lista numeri
                                }
                            }
                        }

                        if(numeri != null ) {
                            for (int i : numeri) {
                                switch (i) { //in base al numero, cambio array value
                                    case 1:
                                        value[i-1] = 1;
                                    case 2:
                                        value[i-1] = 1;
                                    case 3:
                                        value[i-1] = 1;
                                    case 4:
                                        value[i-1] = 1;
                                    case 5:
                                        value[i-1] = 1;
                                    case 6:
                                        value[i-1] = 1;
                                    case 7:
                                        value[i-1] = 1;
                                    case 8:
                                        value[i-1] = 1;
                                    case 9:
                                        value[i-1] = 1;
                                    case 10:
                                        value[i-1] = 1;
                                }
                            }
                        }

                        for(int i=0; i<10; i++){
                            sup[i] = Integer.toString(value[i]);

                        }

                        if(sup!=null)
                            stringa_finale_da_iviare = sup[0]+sup[1]+sup[2]+sup[3]+sup[4]+sup[5]+sup[6]+sup[7]+sup[8]+sup[9];
                        System.out.println("stringa"+stringa_finale_da_iviare);

                    }//for


////PROBELMA: SE SONO IN RR E STACCO NODO CONNESSO, APP NON SI RICOLLEGA A QUELLO NON COLLEGATO PER ROUND ROBIN

                try {
                        //controllo su numero connessi
                        if(connessi.size() == 6){ //6
                            Object massimo = connessi.keySet().toArray()[0];
                            for (Object i : connessi.keySet().toArray()) {
                                if (Long.valueOf((Long) i) > Long.valueOf((Long) massimo))
                                    massimo = i;
                            }

                            connessi.get(massimo).disconnect();
                            connessi.remove(massimo);
                        }

                    final Node newNode = new Node(device, rssi, advertisedData);
                    if (addNode(newNode)) {
                       final Handler handler = new Handler();
                         handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (connessi.size() < 5) {//5
                       //             connessi.put(System.currentTimeMillis(),newNode);
                       //             listadigiaconnessi.add(newNode);
              //                      int i = 0;
                                   /* if(!connessi.isEmpty()) {
                                        for (long index : connessi.keySet()) {
                                            if (connessi.get(index).getState() != Node.State.Connected)
                                                i = i++;
                                        }
*/
                                    connectToDevice(newNode);

                                    //    }
                            //   }
                                }

                                //prima connessione

                            /*    if (newNode.getState() == Node.State.Idle && connessi.size() < 2) { //5

                                    newNode.connect(getApplicationContext());
                                    datipresi.put(newNode,false);
                                }
*/
                            }
                        }, 1000);
                  /*      handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                enableNeededNotification(newNode);
                                startPlotFeature(newNode);

                            }
                        }, 1000);*/

                    }

                } catch (InvalidBleAdvertiseFormat e) {
                        System.out.println("EEEE"+ e);
                }

            }
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
                btAdapter.getBluetoothLeScanner().startScan(mScanCallBack_post21);
            }
        });
    }


    public void connectToDevice(Node n) {
        if (!n.isConnected()) {
            n.addExternalCharacteristics(new StdCharToFeatureMap());
            n.connect(this);
        }
    }


    public void disconnectDeviceSelected(Node n) {
        if (n.isConnected()) {
            n.disconnect();
        }
    }

    public boolean presentenellalista(Node n, ArrayList<Node> lista) {
        Boolean presente = false;
        int i = 0;
        if (!lista.isEmpty()) {
            while (i < lista.size() && !presente) {
                if (n.equals(lista.get(i)))
                    presente = true;
                i++;
            }
        } else
            presente = false;
        return presente;
    }


    long ultimoRR = 0;
    boolean primavolta = true;

    boolean sonoentrato;


    private synchronized void gestisci(Node node) {



        int indice=-1;

        System.out.println("GESTISCI");



//se presente nella lista dei già connessi mi prendo l'indice del nodo da discovered
        if(presentenellalista(node, listadigiaconnessi))
               indice = verifica();

        //se indice è diverso da -1 sostituisco il nodo
            if(indice != -1)
              node =  mDiscoverNode.get(indice);




        Object minimo = connessi.keySet().toArray()[0];
        for (Object i : connessi.keySet().toArray()) {
            if (Long.valueOf((Long) i) < Long.valueOf((Long) minimo))
                minimo = i;
        }


        long ora = System.currentTimeMillis();
        System.out.println("MM" + (ora - Long.valueOf((Long) minimo)));

        if ((ora - Long.valueOf((Long) minimo)) > 30000  && (ora - ultimoRR > 20000 || primavolta)) {


            sonoentrato=true;


            ultimoRR = System.currentTimeMillis();

            primavolta = false;
            connessi.get(minimo).disconnect();
            Toast.makeText(getApplicationContext(), "Disconnesso da RR" + connessi.get(minimo).getTag(), Toast.LENGTH_SHORT).show();
            connessi.remove(minimo);

            connectToDevice(node);

  //          while (node.getState() != Node.State.Connected) {


                //          if(node.isConnected()) {


                ////////////////////////////PROVA!!///////////////////

                   /* ArrayList<Node> lista = new ArrayList<>();
                    for (long i : connessi.keySet()) {
                        lista.add(connessi.get(i));
                    }*/

                    //   Toast.makeText(getApplicationContext(), "Disconnesso da RR" + connessi.get(minimo).getTag(), Toast.LENGTH_SHORT).show();


           //         connessi.clear();

                    if (node.getState() == Node.State.Connecting) {
                        connessi.put(System.currentTimeMillis(), node);
                        Toast.makeText(getApplicationContext(), "Connesso a RR" + node.getTag(), Toast.LENGTH_SHORT).show();
                        if(!presentenellalista(node, listadigiaconnessi))
                            listadigiaconnessi.add(node);
                    }


                    //     }

                    //  listadigiaconnessi.add(node);
         /*   for (int i : mDiscoverNode.keySet()) {
                if (mDiscoverNode.get(i).isConnected()) {
                    System.out.println("random" + randomWithRange(5, 15));
                    connessi.put(System.currentTimeMillis() + randomWithRange(5, 15), mDiscoverNode.get(i));
                }
            }*/

                   /* for (Node i : lista) {
                        System.out.println("random" + randomWithRange(5, 15));
                        connessi.put(System.currentTimeMillis() + randomWithRange(5, 15), i);
                    }*/
           //         lista.clear();

            datipresi.remove(node);
            datipresi.put(node,false);


                }
         //   }
        }



    int randomWithRange(int min, int max) {
        int range = (max - min) + 1;
        return (int) (Math.random() * range) + min;
    }



    public boolean presentenellamappa(Node nodo, TreeMap<Long, Node> lista) {

        boolean presente = false;
        for(long i : lista.keySet()) {
            if(lista.get(i).getTag().equals(nodo.getTag())) {
                presente = true;
                break;
            }
            else
                presente = false;
        }
      return presente;
    }


    public Node presentenellamappa_indice(Float max, HashMap<Node, Float> lista) {

        Node n = null;
        for (Node i : lista.keySet()) {
            if (lista.get(i) == max) {
                n = i;
                break;
            }
        }
            return n;
    }


    public void rimuovidallamappa(Node n, TreeMap<Long, Node> lista){

        long indice=0;
        for(long i : lista.keySet()) {
            if(lista.get(i).getTag().equals(n.getTag())) {
                indice = i;
                break;
            }
        }
        lista.remove(indice);
    }



    public int verifica() {
        boolean esiste = false;
        int i=0;
        for(Integer k : mDiscoverNode.keySet()) {
            if (!mDiscoverNode.get(k).isConnected() && !presentenellalista(mDiscoverNode.get(k),listadigiaconnessi)) {
                esiste = true;
                i = k;
            }
        }
        if(esiste)
            return i;
        else
            return -1;
    }



///////////////////////////////////////////////TEMPERATURA////////////////////////////////////////////////



    //String dataString;

    float max_T = 0;
    float massimo_T;
    int allarme_T=0; //valore superamento soglia T
    int allarme_H=0; //valore superamento soglia T
    int allarme_CO=0; //valore superamento soglia T
    int allarme_Stato=0; //valore superamento soglia T

    public static final String FEATURE_TEMPERATURE = "Temperature";

    boolean giaesistente;


    private class TemperatureListener implements com.st.BlueSTSDK.Feature.FeatureListener {
        String dataString;
        Node node;
        JSONArray listaT = new JSONArray();

        TreeMap<String, Float> listaTemp = new TreeMap<>();


        public TemperatureListener(Resources res, Node n) {
            this.node = n;
        }

        long startTime4 = System.currentTimeMillis();


        @Override
        public void onUpdate(Feature f, Feature.Sample sample) {

            final Servizio.ExtractDataFunction sExtractDataTemp = new Servizio.ExtractDataFunction() {
                public float getData(Feature.Sample s) {
                    return FeatureTemperature.getTemperature(s);
                }//getData
            };

            String unit = listaTemperature.get(node).get(0).getFieldsDesc()[0].getUnit();
            final float data[] = extractData(listaTemperature.get(node), sExtractDataTemp);
            float T = data[1];
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            final String dateTime = dateFormat.format(date);
            dataString = FEATURE_TEMPERATURE + "  :  " + String.format("%.2f", T) + "[" + unit + "]";


            final String dataOra = dateFormat_ora.format(date);


            long secondi = System.currentTimeMillis();

            if ((secondi - startTime4) >= 1000) {
                startTime4 = System.currentTimeMillis();
                //        listaT.put(dataString + dateTime);


                try {
                     listaTemp.put(dateTime, T);
                }
                catch (Exception e){
                }
                lista_temperatura.put(node,(data[0]));



                if (listaTemp.size() == 10) {
                    max_T=0;

try {
    for (Node n : lista_temperatura.keySet()) {
        if (lista_temperatura.get(n) > max_T) {
            max_T = lista_temperatura.get(n);
            massimo_T = lista_temperatura.get(n);
        }
    }
}
catch (Exception e){

}



         /*           if(max_T > 10) {
                        for(Node n : listadigiaconnessi) {
                            try {
                                mDebugConsole = n.getDebug();
                                mDebugConsole.write("#" + nome_utente  + stringa_finale_da_iviare + max_T + max_H + max_CO + dateFormat_ora.format(date));
                            }
                            catch (Exception e){
                            }
                        }
                        System.out.println("ha inviato");
                    }
*/


                    lista_temperatura.clear();



                    try {

         //               JsonObj.get(node).put("Acquisizioni T " + "\n" + dataOra, JsonarrayT(listaTemp, node));

                         JsonarrayT(listaTemp, node);
                    } catch (Exception e) {
                    }

                    Writer output = null;

                    Random random = new Random();


                    File file = new File(path_cartella, "Report_" + node.getTag() + ".json");





                    try {
                        output = new BufferedWriter(new FileWriter(file));
                        output.write( JsonObj.get(node).toString());
                        datipresi.put(node, true);

                        //    output.write('\n' + listaJson.toString());
                        output.close();


                        new Thread(new Runnable() {
                            public void run() {
                                boolean status;
                                boolean status1;


                                if (isOnline(getApplicationContext())) {
                                    if (connessoftp) {
                                        String p = Environment.getExternalStorageDirectory()
                                                + "/Reports" + "/reports_"+minuti+"/Report_"+node.getTag()+".json";

                                        String p1 = Environment.getExternalStorageDirectory()
                                                + "/Reports" + "/reports_"+minuti+"/Report_sensore_indossabile"+".json";

                                        System.out.println("path"+p);

                                        status = ftpclient.ftpUpload(p
                                                , "Report_" + node.getTag() + ".json"
                                                , "/report/", getApplicationContext());

                                        status1 = ftpclient.ftpUpload(p1
                                                , "Report_" + "sensore_indossabile" + ".json"
                                                , "/report/", getApplicationContext());

                                    if (status && status1) {
                                            Log.d("Upload", "Upload success");

                                        Handler handler = new Handler(Looper.getMainLooper());
                                        handler.post(new Runnable() {

                                            @Override
                                            public void run() {
                                                Toast.makeText(getApplicationContext(),"File inviati a cloud FTP",Toast.LENGTH_LONG).show();                                            }
                                        });

                                            ftpclient.ftpDisconnect();
                                            connessoftp = false;
                                        } else {
                                            Log.d("Upload", "Upload failed");
                                        }
                                    } else {
                                        Log.d("Upload", "Provo a connettermi");
                                        connectToFTPAddress();
                                    }
                                } else

                                    Log.d("Upload", "Please check your internet connection!");


                            }
                        }).start();



                    } catch (Exception e) {
                    }





                    listaTemp.clear();
                }


                //listaH.put(dataString + dateTime);
                //listaStato.put(stato + dateTime);



       /*         try {
                    fileJSONT(listaT,node);
                    //       fileJSONStato(listaStato);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                */


            }
        }
    }


    //////////////////////////////////////CO GAS/////////////////////////////////

    private String dataString3;
    String FEATURE_CO = "Co gas";

    float max_CO = 0;

    float massimo_CO;

    final Servizio.ExtractDataFunction sExtractCO = new Servizio.ExtractDataFunction() {
        public float getData(Feature.Sample s) {
            return FeatureCOSensor.getGasPresence(s);
        }//getData
    };


    private class CO_listener implements com.st.BlueSTSDK.Feature.FeatureListener {
        Node node;

        private Servizio.ExtractDataFunction sExtractCO = new Servizio.ExtractDataFunction() {
            public float getData(Feature.Sample s) {
                return FeatureCOSensor.getGasPresence(s);
            }//getData
        };


        public CO_listener(Resources res, Node n) {
            this.node = n;
        }

        TreeMap<String, Float> listaCO = new TreeMap<>();
        long startTime5 = System.currentTimeMillis();

        @Override
        public void onUpdate(Feature f, Feature.Sample sample) {

            final float data[] = extractData(listaCOgas.get(node), sExtractCO);




            dataString3 = FEATURE_CO + "  :  " + data[0] + " " + "ppm";

            Log.i("GAS", dataString3);


            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            final String dateTime = dateFormat.format(date);
            final String dataOra = dateFormat_ora.format(date);



            if ((System.currentTimeMillis() - startTime5) >= 1000) {



                double num_x = 0;
                double num_y = 0;
                double somma_coefficienti = 0;

                //////////LOCALIZZAZIONE///////////
                //Calcolo posizione del sensore indossabile (lavoratore) confronto nodi con quelli connessi
                //scoro nodi in mappa_rssi; prendo quelli che sono in connessi
                for(Node n : mappa_pesi.keySet()){
                    if(presentenellamappa(n,connessi)){
                        double coeff = mappa_pesi.get(n);
                        System.out.println("coeff"+coeff);
                        somma_coefficienti = somma_coefficienti + coeff;
                        System.out.println("coeff"+somma_coefficienti);
                        for(String s : mappa_posizioni.keySet()){
                            if(s.equals(n.getTag())){
                                num_x = num_x + coeff* Double.parseDouble((mappa_posizioni.get(s).get(0)));
                                System.out.println("coeff"+num_x);
                                num_y = num_y + coeff* Double.parseDouble((mappa_posizioni.get(s).get(1)));
                            }
                        }
                    }
                }

                posizione_indossabile_x = num_x/somma_coefficienti;
                posizione_indossabile_y = num_y/somma_coefficienti;
                System.out.println("posizione"+ " " + posizione_indossabile_x+ " " + posizione_indossabile_y);



                //        listaT.put(dataString + dateTime);


                listaCO.put(dateTime, data[0]);

                lista_CO.put(node,(data[0])); //per inviare

                if (listaCO.size() == 10) {
                    max_CO=0;



                    for(Node n : lista_CO.keySet()) {
                        if (lista_CO.get(n) > max_CO) {
                            max_CO = lista_CO.get(n);
                            massimo_CO = lista_CO.get(n);
                        }
                    }



                /*    if(max_CO > 5) {
                        for(Node n : listadigiaconnessi) {
                            try {
                                mDebugConsole = n.getDebug();
                                mDebugConsole.write("#" + nome_utente + stringa_finale_da_iviare + max_T + max_H + max_CO + dateFormat_ora.format(date));
                            }
                            catch (Exception e){
                            }
                        }
                        *//*  node.getDebug().write("setName jashd");*//*
                        System.out.println("ha inviato");
                    }
*/





                    lista_CO.clear();









                    try {


               //         JsonObj.get(node).put("Acquisizioni CO " + "\n" + dataOra, JsonarrayCO(listaCO, node));
                          JsonarrayCO(listaCO, node);
                        //nuovo

                 //       posizione.put("Posizione " + "\n" + dataOra, Jsonarraypos(Double.toString(posizione_indossabile_x),Double.toString(posizione_indossabile_y)));
                        final String dataOra_p = dateFormat.format(date);
                        Jsonarraypos(Double.toString(posizione_indossabile_x),Double.toString(posizione_indossabile_y),dataOra_p);
                    } catch (Exception e) {
                    }

                          /*  Writer output = null;

                            File file = new File(memory.getAbsolutePath() + "/reports", "Report_" + node.getTag() + ".json");

                            try {
                                output = new BufferedWriter(new FileWriter(file));
                                output.write('\n' + JsonObj.get(node).toString());
                                //    output.write('\n' + listaJson.toString());
                                output.close();
                            } catch (Exception e) {
                            }*/

                    listaCO.clear();
                }
                startTime5 = System.currentTimeMillis();
            }

        }
    }



////////////////////////////////////////////////////HUMIDITY/////////////////////////////////////////////


    float max_H = 0;
    float massimo_H;

    public static final String FEATURE_HUMIDITY = "Humidity";
    private String stato;

    private final static String HUM_FORMAT = "%.1f [%s]";



    private class Humidity_Listener implements com.st.BlueSTSDK.Feature.FeatureListener {
        JSONArray listaH = new JSONArray();
        Node node;

        TreeMap<String, Float> listaHum = new TreeMap<>();

        private Servizio.ExtractDataFunction sExtractDataHum = new Servizio.ExtractDataFunction() {
            public float getData(com.st.BlueSTSDK.Feature.Sample s) {
                return FeatureHumidity.getHumidity(s);
            }//getData
        };

        public Humidity_Listener(Resources res, Node n) {
            this.node = n;
        }

        long startTime3 = System.currentTimeMillis();

        @Override
        public void onUpdate(com.st.BlueSTSDK.Feature f, com.st.BlueSTSDK.Feature.Sample sample)  {
            String dataString1;
            String unit = listaHumidity.get(node).get(0).getFieldsDesc()[0].getUnit();
            final float data[] = extractData(listaHumidity.get(node), sExtractDataHum);
            dataString1 = FEATURE_HUMIDITY + "  :  " + getDisplayString(HUM_FORMAT, unit, data);

            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            final String dateTime = dateFormat.format(date);

            final String dataOra = dateFormat.format(date);

            lista_umidita.put(node,(data[0]));







          /*  for(int i=0; i< umi.size(); i++) {
                if (umi.get(i) > max)
                    max = umi.get(i);
            }*/



            File file = new File(getApplicationContext().getFilesDir(), "stato.txt");
            StringBuffer sb = new StringBuffer();
            FileInputStream fin;
            int ch;

            try {
                //lettura
                fin = new FileInputStream(file);
                while((ch = fin.read()) != -1) {
                    sb.append((char)ch);

                }
                stato=sb.toString();
                System.out.println("qqqqq"+sb.toString());
                fin.close();

            } catch (Exception e) {
                e.printStackTrace();
            }




///////////////////////////////////////////////Creazione file json//////////////////////////////////////////////////////////

            if ((System.currentTimeMillis() - startTime3) >= 1000) {



        //        listaH.put(dataString1 + dateTime);

                listaHum.put(dateTime, data[0]);

                if (listaHum.size() == 10) {
                    max_H=0;


                    for(Node n : lista_umidita.keySet()) {
                        if (lista_umidita.get(n) > max_H) {
                            max_H = lista_umidita.get(n);
                            massimo_H = lista_umidita.get(n);
                        }
                    }

                    System.out.println("massimo"+ max_H);


                    ///lettura stato da file


                    float CO;
                    float T;
                    float H;

                    if(max_H > 100 || max_T > 10 || max_CO > 5) {
                        for(Node n : listadigiaconnessi) {
                            if(max_CO == 0)
                                CO = massimo_CO;
                            else
                                CO =max_CO;

                            if(max_T == 0)
                                T = massimo_T;
                            else
                                T = max_T;

                            if(max_H == 0)
                                H = massimo_H;
                            else
                                H = max_H;

                            try {
                                mDebugConsole = n.getDebug();
                                mDebugConsole.write("#" + nome_utente + stringa_finale_da_iviare + T + H + CO + stato + dateFormat_ora.format(date));
                                System.out.println("#" + nome_utente + stringa_finale_da_iviare + T + H + CO + stato + dateFormat_ora.format(date));

                                  //      + stato);
                            }
                            catch (Exception e){
                            }
                        }
                        System.out.println("ha inviato");
                    }


                    //aggiungo controllo per la generazione file json su log allarmi
                    if(massimo_T > 10) {
                        allarme_T = 1;
                    }


                    //aggiungo controllo per la generazione file json su log allarmi
                    if(massimo_H > 10) {
                        allarme_H = 1;
                    }

                    //aggiungo controllo per la generazione file json su log allarmi
                    if(massimo_CO > 10) {
                        allarme_CO = 1;
                    }


                    //aggiungo controllo per la generazione file json su log allarmi
                    if(stato == "001" || stato == "002" || stato == "003") {
                        allarme_Stato = 1;

                    }



                    Writer output = null;

                    Writer output2 = null;


                    if(allarme_T == 1 || allarme_H == 1 || allarme_CO == 1 || allarme_Stato == 1){
                        try {
                            allarmi.put("temperatura", JsonLog_T(allarme_T, dataOra));
                            allarmi.put("umidita", JsonLog_H(allarme_H, dataOra));
                            allarmi.put("gas", JsonLog_CO(allarme_CO, dataOra));
                            allarmi.put("stato", JsonLog_Stato(allarme_T, dataOra));
                        }
                        catch (Exception e){
                        }
                    }

                    File file1 = new File(path_cartella, "Allarme" + ".json");

                    File file2 = new File(path_cartella, "Posizione" + ".json");

                    try {
                        output = new BufferedWriter(new FileWriter(file1));
                        output.write( allarmi.toString());

                        output.close();


                        output2 = new BufferedWriter(new FileWriter(file2));
                        output2.write( posizione.toString());
                        output2.close();
                    }
                    catch (Exception e){
                    }



                    System.out.println("umi"+lista_umidita.size());

                    lista_umidita.clear();






                    try {
                        //        listaJson.put("Acquisizioni H " +"\n" + dataOra, JsonarrayH(listaHum, node));
                    //    JsonObj.get(node).put("Acquisizioni H " + "\n" + dataOra, JsonarrayH(listaHum, node));
                          JsonarrayH(listaHum, node);
                    } catch (Exception e) {
                    }
                    listaHum.clear();
                }

            /*    try {
                    fileJSONH(listaH,node);
                    //       fileJSONStato(listaStato);
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
                startTime3 = System.currentTimeMillis();
            }
        }
    }



/*
    public void fileJSONH (JSONArray array, Node n) throws IOException {


        Writer output = null;

        // json.put(dataString);

        File sdCard = Environment.getExternalStorageDirectory();
        File file = new File (sdCard.getAbsolutePath()+"/reports" , "Humidity_report_"+n.getTag()+".json");
        output = new BufferedWriter(new FileWriter(file));
        try {
            output.write('\n' + array.toString());
        }
        catch (Exception e){

        }
        output.close();
    }



     public void fileJSONT (JSONArray array, Node n) throws IOException {
        Writer output = null;

        File sdCard = Environment.getExternalStorageDirectory();
        File file = new File (sdCard.getAbsolutePath()+"/reports" , "Temperature_report_" + n.getTag() + ".json");
        output = new BufferedWriter(new FileWriter(file));
        try {
            output.write('\n' + array.toString());
        }
        catch (Exception e){
        }
        output.close();
    }
*/


    private JSONArray JsonarrayT(TreeMap<String, Float> lista, Node n) throws JSONException {
        JSONArray JSONtemp = new JSONArray();
        for (String data : lista.keySet()) {
            JSONObject temperatura = new JSONObject();
            try {
                temperatura.put("valore", lista.get(data));
                temperatura.put("unit", "[celsius]");
                temperatura.put("timestamp", data);
                temperatura.put("nodo", n.getTag());
                JSONtemp.put(temperatura);
            } catch (Exception e) {
            }
            JsonObj.get(n).accumulate("acquisizioni_temperatura", temperatura);
        }

        return JSONtemp;
    }


    //nuovo
    private JSONObject Jsonarraypos( String posizione_x, String posizione_y, String hour) throws JSONException {
        JSONObject pos = new JSONObject();
        try {
            pos.put("x", posizione_x);
            pos.put("y", posizione_y);
            pos.put("timestamp", hour);
        }
        catch (Exception e){
        }
        posizione.accumulate("posizione",pos );
        return posizione;
    }


    private JSONArray JsonarrayH(TreeMap<String, Float> lista, Node n) throws JSONException {
        JSONArray JSONH = new JSONArray();
        for (String data : lista.keySet()) {
            JSONObject umidità = new JSONObject();
            try {
                umidità.put("valore", lista.get(data));
                umidità.put("unit", "[%]");
                umidità.put("timestamp", data);
                umidità.put("nodo", n.getTag());
                JSONH.put(umidità);
            } catch (Exception e) {
            }
               JsonObj.get(n).accumulate("acquisizioni_umidita",umidità);
        }
        return JSONH;
    }


    private JSONObject JsonLog_T(int allarmeT, String data) {

            JSONObject log_T = new JSONObject();
        try {
                log_T.put("allarme_temperatura",allarmeT);
                log_T.put("data",data);
        } catch (Exception e) {
        }
        return log_T;
    }


    private JSONObject JsonLog_H(int allarmeH, String data)  {

        JSONObject log_H = new JSONObject();
        try {
        log_H.put("allarme_umidita",allarmeH);
        log_H.put("data",data);
        } catch (Exception e) {
        }

        return log_H;
    }


    private JSONObject JsonLog_CO(int allarmeCO, String data)  {

        JSONObject log_CO = new JSONObject();
        try {
        log_CO.put("allarme_gas",allarmeCO);
        log_CO.put("data",data);
        } catch (Exception e) {
        }
        return log_CO;
    }


    private JSONObject JsonLog_Stato(int allarmeS, String data)   {

        JSONObject log_STATO = new JSONObject();
        try {
        log_STATO.put("allarme_stato",allarmeS);
        log_STATO.put("data",data);
        } catch (Exception e) {
        }
        return log_STATO;
    }



    private JSONArray JsonarrayCO(TreeMap<String, Float> lista, Node n) throws JSONException {
        JSONArray JSONCO = new JSONArray();
        for (String data : lista.keySet()) {
            JSONObject gasCO = new JSONObject();
            try {
                gasCO.put("valore", lista.get(data));
                gasCO.put("unit", "[ppm]");
                gasCO.put("timestamp", data);
                gasCO.put("nodo", n.getTag());
                JSONCO.put(gasCO);
            } catch (Exception e) {
            }
            JsonObj.get(n).accumulate("acquisizioni_gas", gasCO);
        }
        return JSONCO;
    }


    public void startPlotFeature(Node node) {
        if (node == null)
            return;

        for (Feature f : listaHumidity.get(node)) {
            f.addFeatureListener(nodeTomHumidityListener.get(node));
            node.enableNotification(f);
        }

        for (Feature f : listaTemperature.get(node)) {
            f.addFeatureListener(nodeTomTemperatureListener.get(node));
            node.enableNotification(f);
        }//for

        for (Feature f : listaCOgas.get(node)) {
            f.addFeatureListener(nodeTomCOlistener.get(node));
            node.enableNotification(f);
        }//for
    }


   /* public void stopPlotting(Node node) {
        if (node == null)
            return;
        for (Feature f : mHumidity) {
            f.removeFeatureListener(nodeTomHumidityListener.get(node));
            node.disableNotification(f);
        }//for
        for (Feature f : mTemperature) {
            f.removeFeatureListener(nodeTomTemperatureListener.get(node));
            node.disableNotification(f);
        }//for
         for (Feature f : mCOgas) {
            f.removeFeatureListener(mCOlistener);
            node.disableNotification(f);
        }//for
    }*/


    protected void enableNeededNotification(Node node) {

        Humidity_Listener mHumidityListener = null;
        listaHumidity.put(node, node.getFeatures(FeatureHumidity.class));
        if (!listaHumidity.get(node).isEmpty()) {
            mHumidityListener = nodeTomHumidityListener.get(node);
            for (Feature f : listaHumidity.get(node)) {
                f.addFeatureListener(mHumidityListener);
                node.enableNotification(f);
            }//for
        }

        TemperatureListener mTemperatureListener = null;
        listaTemperature.put(node, node.getFeatures(FeatureTemperature.class));
        if (!listaTemperature.get(node).isEmpty()) {
            mTemperatureListener = nodeTomTemperatureListener.get(node);
            for (Feature f : listaTemperature.get(node)) {
                f.addFeatureListener(mTemperatureListener);
                node.enableNotification(f);
            }//for
        }


        CO_listener mCOlistener = null;
        listaCOgas.put(node, node.getFeatures(FeatureCOSensor.class));
        if (!listaCOgas.get(node).isEmpty()) {
            mCOlistener = nodeTomCOlistener.get(node);
            for (Feature f : listaCOgas.get(node)) {
                f.addFeatureListener(mCOlistener);
                node.enableNotification(f);
            }//for
        }
    }


    public interface ExtractDataFunction {
        float getData(com.st.BlueSTSDK.Feature.Sample s);
    }

    private static float[] extractData(final List<? extends com.st.BlueSTSDK.Feature> features,
                                       final Servizio.ExtractDataFunction extractDataFunction) {
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


    private boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

    private void connectToFTPAddress() {
        new Thread(new Runnable() {
            public void run() {
                boolean status=false;
                status = ftpclient.ftpConnect( "ftp.smartbench.altervista.org",
                        "smartbench", "S97PnpVAK8kc" , 21);
                if (status == true) {
                    connessoftp=true;
                    Log.d("Connessione", "Connection Success");
                } else {
                    Log.d("Connessione", "Connection failed");
                    connessoftp=false;
                }
            }
        }).start();
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


    private static void readExcelFile(Context context, String filename, TreeMap<String, ArrayList<String>> mappa_posizioni) {

        String[][] coordinate = new String[10][2];


        ArrayList<String> MacAddress = new ArrayList<>();

        try {
            // Creating Input Stream
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/prova" + ".xlsx");
            FileInputStream myInput = new FileInputStream(file);


            // Get the first sheet from workbook

            XSSFWorkbook workbook = new XSSFWorkbook(myInput);
            XSSFSheet mySheet = workbook.getSheetAt(0);

            /** We now need something to iterate through the cells.**/
            Iterator rowIter = mySheet.rowIterator();


            while (rowIter.hasNext()) {
                XSSFRow myRow = (XSSFRow) rowIter.next();
                Iterator cellIter = myRow.cellIterator();
                while (cellIter.hasNext()) {
                    XSSFCell myCell = (XSSFCell) cellIter.next();


                    if (myCell.getColumnIndex() == 1 || myCell.getColumnIndex() == 2) {
                        coordinate[myCell.getRow().getRowNum()][myCell.getColumnIndex()-1] = myCell.toString(); //prendo i valori delle coordinate
                        //     System.out.println("AAA"+coordinate[myCell.getRow().getRowNum()][myCell.getColumnIndex()-1]);
                    }

                    if(myCell.toString().startsWith("C0")) {
                        MacAddress.add(myCell.toString());
                    }

                    Log.d("MainActivity", "Cell Value: " + myCell.toString());
             //       Toast.makeText(context, "cell Value: " + myCell.toString(), Toast.LENGTH_SHORT).show();
                }
            }

            System.out.println("MAC"+ MacAddress.get(6).toString());


            for(int i=0; i<MacAddress.size(); i++) {
                ArrayList<String> appoggio = new ArrayList<String>();
                appoggio.clear();
                appoggio.add(coordinate[i][0]);
                appoggio.add(coordinate[i][1]);
                mappa_posizioni.put(MacAddress.get(i),appoggio);
            }
            System.out.println("MAC"+mappa_posizioni.get("C0:85:2C:35:3D:32").get(0));
            System.out.println("MAC"+mappa_posizioni.get("C0:85:2C:35:3D:32").get(1));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return;
    }

}