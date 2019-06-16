package com.aopi.threads;

import com.aopi.controllers.MainController;
import com.aopi.models.Coordinate;
import com.sun.jna.Callback;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.springframework.ui.Model;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

import static com.sun.jna.platform.win32.WinBase.INFINITE;

public class ReadFromFileThread implements Callback {
    private ArrayList<Coordinate> list= new ArrayList<>();
    private Model model;
    private HANDLE readingEvent;
    private HANDLE mutex;
    private Boolean isPipes = false;

    public ArrayList<Coordinate> getList() {
        return list;
    }

    public ReadFromFileThread(Model model, HANDLE readingEvent) {
        this.model = model;
        this.readingEvent = readingEvent;
    }

    public ReadFromFileThread(Model model, HANDLE readingEvent, HANDLE mutex) {
        this.model = model;
        this.readingEvent = readingEvent;
        this.mutex = mutex;
    }

    public ReadFromFileThread(Model model, boolean isPipes) {
        this.model = model;
        this.isPipes = isPipes;
    }

    public void callback() {
        if (readingEvent != null) {
            readData();
            MainController.MoreKernel32.instance.SetEvent(readingEvent);
            readingEvent = null;
        }
        else if (isPipes)
        {
            readData();
        }
        else {
            MainController.MoreKernel32.instance.WaitForSingleObject(mutex, INFINITE);
            readData();
            MainController.MoreKernel32.instance.ReleaseMutex(mutex);
            mutex = null;
        }
    }

    //функция для чтения с файла
    private void readData() {
        try {
            list.clear();
            FileReader reader = new FileReader("src/main/resources/coordinates.txt");
            Scanner scan = new Scanner(reader);
            while(scan.hasNextLine()) {
                String[] str = scan.nextLine().split(" ");
                if (str.length != 2) {
                    model.addAttribute("error", "Error. Invalid coordinate " +
                        "format in file");
                    return;
                }
                try {
                    double abs = Double.parseDouble(str[0]);
                    double ord = Double.parseDouble(str[1]);
                    if ((ord > 1000000) | (ord < -1000000) | (abs > 1000000) | (abs < -1000000)) {
                        model.addAttribute("error", "Error. File has too " +
                            "large coordinates.");
                        return;
                    }
                    Coordinate cor = new Coordinate(abs, ord);
                    list.add(cor);
                }
                catch (Exception e) {
                    model.addAttribute("error", "Error. There are non-numeric " +
                        "coordinates in the file.");
                    return;
                }
            }
        }
        catch (FileNotFoundException e) {
            model.addAttribute("error",
                "ReadFromFile thread can't find required file.");
        }
        catch (Exception e) {
            model.addAttribute("error",
                "Unknown error in ReadFromFile thread.");
        }
    }
}
