package iohandling;

import simulation.FitnessResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by colander on 2/5/17.
 * Logs debug messages and the results of an evolution run.
 */
public class Logger {
    public final static String RESULTS_DIRECTORY = "results/";
    private final String runDir;

    private BufferedWriter logWriter;

    public Logger() {
        runDir = RESULTS_DIRECTORY + System.currentTimeMillis();
        IOHandler.createDirectory(runDir);
        File logFile = new File(runDir + "/evolution.log");
        try {
            logWriter = new BufferedWriter(new FileWriter(logFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logGeneration(ArrayList<FitnessResult> results, int generationNo) {
        String genFolder = runDir + "/" + (generationNo > 10 ? generationNo : "0" + generationNo);
        IOHandler.createDirectory(genFolder);
        for (int i = 0; i < results.size(); i++) {
            IOHandler.writeFile(genFolder + "/" + (i > 10 ? i : "0" + i) + ".gtp", results.get(i).result + "\n" + results.get(i).genotype.serialize());
        }
    }

    public void log(String message) {
        String[] split = message.split("\n");
        for (int i = 0; i < split.length; i++) {
            System.out.println(System.currentTimeMillis() + "|" + split[i]); //TODO REMOVE when the project is done
            try {
                logWriter.write(System.currentTimeMillis() + "|" + split[i]);
                logWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void flush() {
        try {
            logWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
