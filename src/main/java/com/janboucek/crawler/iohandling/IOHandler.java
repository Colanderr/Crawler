package com.janboucek.crawler.iohandling;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by colander on 1/30/17.
 * Class for handling basic file IO
 */
public class IOHandler {

    static void createDirectory(String address) {
        File dir = new File(address);
        if (!dir.mkdir()) {
            System.out.println("IOHANDLER MKDIR ERROR: " + address);
        }
    }

    static void writeFile(String adddress, String text) {
        try {
            FileWriter fw = new FileWriter(adddress);
            fw.write(text);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //not efficient, works well enough
    public static String readFile(String address) {
        try {
            Scanner sc = new Scanner(new File(address));
            sc.useDelimiter("\\Z");
            return sc.next();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}