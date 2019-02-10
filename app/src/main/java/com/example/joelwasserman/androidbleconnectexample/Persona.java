package com.example.joelwasserman.androidbleconnectexample;

import com.st.BlueSTSDK.Node;

import java.util.ArrayList;

public class Persona {

    ArrayList<Node> listanodi = new ArrayList();
    String stato;
    float temperature;
    float humidity;
    float CO;

    public Persona(String state, float gas, float T, float H){

        this.CO = gas;
        this.temperature = T;
        this.stato = state;
        this.humidity = H;

            }


            public float getCO(){
                return this.CO;
            }
    public float getTemperature(){
        return this.temperature;
    }
    public float getHumidity(){
        return this.humidity;
    }
    public String getStato(){
        return this.stato;
    }

    public void setCO(float CO){
        this.CO = CO;
    }

    public void setTemperature(float T){
         this.temperature = T;
    }
    public void setHumidity(float H){
         this.humidity = H;
    }
    public void setStato(String stato){
         this.stato = stato;
    }
}
