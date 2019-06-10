package com.example.joelwasserman.androidbleconnectexample;

import android.Manifest;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.common.api.GoogleApiClient;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.LocationManager;
import android.media.JetPlayer;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.DateTimeKeyListener;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Features.FeatureAccelerationEvent;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity
{
    String minuti;
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    Button startService1;
    Button stopService;
    Button startService_amb1;
    Button stopService_amb2;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    Handler mHandler;
    SimpleDateFormat dateFormat_minuti = new SimpleDateFormat("HH:mm");

    String nome_utente;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        try {
            nome_utente = getIntent().getExtras().getString("Nome");
        }
        catch (Exception e){

        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                },
                PERMISSION_REQUEST_COARSE_LOCATION
        );


        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        startService1 = (Button) findViewById(R.id.avvia);
        stopService = (Button) findViewById(R.id.arresta);
        startService_amb1 = (Button) findViewById(R.id.button3);
        stopService_amb2 = (Button) findViewById(R.id.button4);

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

        }

        startService1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (nome_utente == null) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Attenzione!");
                    builder.setMessage("Devi inserire prima il nome utente dal menù laterale");
                    builder.show();
                }
                else
                    startService(view);
            }
        });


        startService_amb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (nome_utente == null) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Attenzione!");
                    builder.setMessage("Devi inserire prima il nome utente dal menù laterale");
                    builder.show();
                }
                else
                    startService_ambientali(view);
            }
        });


        if (nome_utente != null) {
            //creo cartella
            Date date = new Date();
            minuti = dateFormat_minuti.format(date);
            File f1 = new File(Environment.getExternalStorageDirectory()+"/Reports", "reports_"+minuti);
            f1.mkdirs();

            startService1.setVisibility(View.VISIBLE);
                startService_amb1.setVisibility(View.VISIBLE);
            }


     /*   if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }*/


        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
      /*  if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }*/

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //RelativeLayout layout = (RelativeLayout) findViewById(R.layout.activity_home);
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu,menu);

        if(nome_utente!=null){
            menu.removeItem(R.id.nome);
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId()==R.id.nome){
            Intent intent = new Intent(MainActivity.this,NomeUtente.class);
            startActivity(intent);
        }


        return super.onOptionsItemSelected(item);
    }

    public void startService(View v)
    {
        Intent intent = new Intent(MainActivity.this,Servizio.class);
        intent.putExtra("Minuti", minuti);
        startService(intent);
    }

    public void stopService(View v)
    {
        stopService(new Intent(MainActivity.this,Servizio.class));
    }



    public void startService_ambientali(View v)
    {
        Intent intent = new Intent(MainActivity.this,Servizio_ambientali.class);
        intent.putExtra("Nome", nome_utente);
        intent.putExtra("Minuti", minuti);
        startService(intent);

    }


    public void stopService_ambientali(View v)
    {
        stopService(new Intent(MainActivity.this,Servizio_ambientali.class));
    }

    public void startFtp(View v)
    {
        startService(new Intent(MainActivity.this,Servizio_ambientali.class));
    }


    public void stopFtp(View v)
    {
        stopService(new Intent(MainActivity.this,Servizio_ambientali.class));
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: { if(
                    (grantResults.length >0) &&
                            (grantResults[0]
                                    + grantResults[1]
                                    + grantResults[2]
                                    == PackageManager.PERMISSION_GRANTED
                            )
                    ){
                // Permissions are granted
                Toast.makeText(this,"Permissions granted.",Toast.LENGTH_SHORT).show();
            }else {
                // Permissions are denied
                Toast.makeText(this,"Permissions denied.",Toast.LENGTH_SHORT).show();
            }
                return;

            }

        }


    }


}