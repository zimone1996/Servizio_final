package com.example.joelwasserman.androidbleconnectexample;


import android.support.v7.app.AppCompatActivity;

import com.fuzzylite.Engine;
import com.fuzzylite.Op;
import com.fuzzylite.activation.General;
import com.fuzzylite.defuzzifier.Centroid;
import com.fuzzylite.norm.s.Maximum;
import com.fuzzylite.norm.t.AlgebraicProduct;
import com.fuzzylite.rule.Rule;
import com.fuzzylite.rule.RuleBlock;
import com.fuzzylite.term.Triangle;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;

import java.util.Scanner;

public class Localizzazione extends AppCompatActivity{

    double segnale;
    double weight;

    public Localizzazione(double rssi) {
        Engine engine = new Engine();
        this.segnale = rssi;
        engine.setName("ObstacleAvoidance");
        engine.setDescription("");

        InputVariable obstacle = new InputVariable();
        obstacle.setName("obstacle");
        obstacle.setDescription("");
        obstacle.setEnabled(true);
        obstacle.setRange(-90, -20);
        obstacle.setLockValueInRange(false);
        obstacle.addTerm(new Triangle("unusable", -113.3, -90, -66.67, 1));
        obstacle.addTerm(new Triangle("weak", -90, -66.67, -43.33, 1));
        obstacle.addTerm(new Triangle("intermidiate", -66.67, -43.33, -20, 1));
        obstacle.addTerm(new Triangle("strong", -43.33, -19.78, 3.526, 1));

        engine.addInputVariable(obstacle);


        OutputVariable mSteer = new OutputVariable();
        mSteer.setName("mSteer");
        mSteer.setDescription("");
        mSteer.setEnabled(true);
        mSteer.setRange(0.000, 1.000);
        mSteer.setLockValueInRange(false);
        mSteer.setAggregation(new Maximum());
        mSteer.setDefuzzifier(new Centroid(100));
        mSteer.setDefaultValue(Double.NaN);
        mSteer.setLockPreviousValue(false);
        mSteer.addTerm(new Triangle("small", -0.3333, 0, 0.3333, 1));
        mSteer.addTerm(new Triangle("weak", 0, 0.3333, 0.6667, 1));
        mSteer.addTerm(new Triangle("medium", 0.3333, 0.6667, 1, 1));
        mSteer.addTerm(new Triangle("high", 0.6667, 1, 1.333, 1));
        engine.addOutputVariable(mSteer);

        RuleBlock mamdani = new RuleBlock();
        mamdani.setName("mamdani");
        mamdani.setDescription("");
        mamdani.setEnabled(true);
        mamdani.setConjunction(null);
        mamdani.setDisjunction(null);
        mamdani.setImplication(new AlgebraicProduct());
        mamdani.setActivation(new General());
        mamdani.addRule(Rule.parse("if obstacle is unusable then mSteer is small", engine));
        mamdani.addRule(Rule.parse("if obstacle is weak then mSteer is weak", engine));
        mamdani.addRule(Rule.parse("if obstacle is intermidiate then mSteer is medium", engine));
        mamdani.addRule(Rule.parse("if obstacle is strong then mSteer is high", engine));
        engine.addRuleBlock(mamdani);


        StringBuilder status = new StringBuilder();

        if (!engine.isReady(status))
            throw new RuntimeException("[engine error] engine is not ready:\n" + status);

        obstacle = engine.getInputVariable("obstacle");
        OutputVariable steer = engine.getOutputVariable("mSteer");

      /*  for (int i = 1; i < 100; i++) {
            Scanner scan = new Scanner(System.in);
            System.out.println("Introduci valore:");
            Double valore = scan.nextDouble();
            obstacle.setValue(valore);
            engine.process();
            System.out.println(String.format(
                    "obstacle.input = %s -> steer.output = %s",
                    Op.str(valore), Op.str(steer.getValue())));

    }*/
        obstacle.setValue(segnale);
        engine.process();
        weight = steer.getValue();
        System.out.println(String.format(
                "obstacle.input = %s -> steer.output = %s",
                Op.str(segnale), Op.str(steer.getValue())));

    }


    public Double getWeight() {
        return weight;
    }
}
