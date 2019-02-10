package com.example.joelwasserman.androidbleconnectexample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;

public class NomeUtente extends Activity {


    String nome_utente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inserisci_nome);



    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                EditText simpleEditText = (EditText) findViewById(R.id.nome);
                nome_utente = simpleEditText.getText().toString();
                if(nome_utente.length()==3) {
                    Intent intent = new Intent(NomeUtente.this, MainActivity.class);
                    intent.putExtra("Nome", nome_utente);
                    Toast.makeText(NomeUtente.this,"Nome inserito correttamente",Toast.LENGTH_LONG).show();
                    startActivity(intent);
                }
                else{
                    final AlertDialog.Builder builder = new AlertDialog.Builder(NomeUtente.this);
                    builder.setTitle("Attenzione!");
                    builder.setMessage("Il nome utente deve essere di almeno tre caratteri");
                    builder.show();
                }
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}
