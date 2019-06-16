package com.aopi.threads;

import com.aopi.controllers.MainController;
import com.aopi.models.Coordinate;
import com.sun.jna.Callback;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.springframework.ui.Model;

import java.util.ArrayList;

public class SortingThread implements Callback {
    private ArrayList<Coordinate> list;
    public ArrayList<Coordinate> sortedList = new ArrayList<>();
    private Model model;
    private HANDLE sortingEvent;
    private Boolean isPipes = false;

    public SortingThread(ArrayList<Coordinate> list, Model model) {
        this.list = list;
        this.model = model;
    }

    public SortingThread(ArrayList<Coordinate> list, Model model, HANDLE sortingEvent) {
        this.list = list;
        this.model = model;
        this.sortingEvent = sortingEvent;
    }

    public SortingThread(ArrayList<Coordinate> list, Model model, Boolean isPipes) {
        this.list = list;
        this.model = model;
        this.isPipes = isPipes;
    }

    public void callback() {
        if (sortingEvent != null) {
            extractSorting(list);
            MainController.MoreKernel32.instance.SetEvent(sortingEvent);
            sortingEvent = null;
        } else if (isPipes)
        {
            extractSorting(list);
        }
        else {
            extractSorting(list);
        }
    }

    public ArrayList<Coordinate> getSortedList() {
        return sortedList;
    }

    private void extractSorting(ArrayList<Coordinate> list) {
        try {
            sortedList.clear();
            for (int i = 0; i < list.size() - 1; i++) {
                int min = i;
                for (int j = i + 1; j < list.size(); j++) {
                    if (list.get(j).abscissa < list.get(min).abscissa) min = j;
                }
                Coordinate temp = list.get(i);
                list.set(i, list.get(min));
                list.set(min, temp);
            }
            this.sortedList = list;
        }
        catch (NullPointerException e) {
            model.addAttribute("error",
                               "Invalid value passed to the Sorting thread.");
        }
        catch (Exception e) {
            model.addAttribute("error",
                    "Unknown error in Sorting thread.");
        }
    }

}
