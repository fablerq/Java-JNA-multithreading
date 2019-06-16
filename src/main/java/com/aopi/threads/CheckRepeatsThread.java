package com.aopi.threads;

import com.aopi.controllers.MainController;
import com.aopi.models.Coordinate;
import com.sun.jna.Callback;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.springframework.ui.Model;

import java.util.ArrayList;

public class CheckRepeatsThread implements Callback {
    public Boolean flag;
    private ArrayList<Coordinate> list;
    private Model model;
    private HANDLE checkRepeatsEvent;
    private Boolean isPipes = false;

    public CheckRepeatsThread(ArrayList<Coordinate> list, Model model) {
        this.list = list;
        this.model = model;
    }

    public CheckRepeatsThread(ArrayList<Coordinate> list, Model model, HANDLE checkRepeatsEvent) {
        this.list = list;
        this.model = model;
        this.checkRepeatsEvent = checkRepeatsEvent;
    }

    public CheckRepeatsThread(ArrayList<Coordinate> list, Model model, Boolean isPipes) {
        this.list = list;
        this.model = model;
        this.isPipes = isPipes;
    }

    public Boolean getFlag() {
        return flag;
    }

    public void callback() {
        if (checkRepeatsEvent != null) {
            checkRepeats(list);
            MainController.MoreKernel32.instance.SetEvent(checkRepeatsEvent);
            checkRepeatsEvent = null;
        }
        else if (isPipes)
        {
            checkRepeats(list);
        }
        else
        {
            checkRepeats(list);
        }
    }

    private void checkRepeats(ArrayList<Coordinate> list) {
        try {
            flag = false;
            for (int i = 0; i < list.size() - 1; i++) {
                if (list.get(i).abscissa.equals(list.get(i+1).abscissa)) {
                    model.addAttribute("error",
                            "Error. There are repeating abscissas in the file.");
                    break;
                }
            }
            if (!model.containsAttribute("error")) {
                flag = true;
                model.addAttribute("sortedData", list);
                model.addAttribute("maxValue", findMax(list));
            }
        }
        catch (NullPointerException e) {
            model.addAttribute("error",
                    "Invalid value passed to the CheckRepeats thread.");
        }
        catch (Exception e) {
            model.addAttribute("error",
                    "Unknown error in CheckRepeats thread.");
        }
    }

    private Integer findMax(ArrayList<Coordinate> list) {
        double max = 0.0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).abscissa > max) max = list.get(i).abscissa;
            if (list.get(i).ordinate > max) max = list.get(i).ordinate;
        }
        return (int) max;
    }
}
