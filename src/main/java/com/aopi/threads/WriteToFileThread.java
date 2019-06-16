package com.aopi.threads;

import com.aopi.controllers.MainController;
import com.aopi.models.dataRequest;
import com.sun.jna.Callback;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.springframework.ui.Model;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import static com.sun.jna.platform.win32.WinBase.INFINITE;

public class WriteToFileThread implements Callback {
    private dataRequest data;
    private Model model;
    private Boolean flag = false;
    private HANDLE writingEvent;
    private HANDLE mutex;
    private Boolean isPipes = false;

    public WriteToFileThread(dataRequest data, Model model, HANDLE writingEvent) {
        this.data = data;
        this.model = model;
        this.writingEvent = writingEvent;
    }

    public WriteToFileThread(dataRequest data, Model model, HANDLE writingEvent, HANDLE mutex) {
        this.data = data;
        this.model = model;
        this.writingEvent = writingEvent;
        this.mutex = mutex;
    }

    public WriteToFileThread(dataRequest data, Model model, Boolean isPipes) {
        this.data = data;
        this.model = model;
        this.isPipes = isPipes;
    }

    public Boolean getFlag() {
        return flag;
    }

    public void callback() {
        try {
            if (writingEvent != null) {
                recordData(data);
                MainController.MoreKernel32.instance.SetEvent(writingEvent);
                writingEvent = null;
            }
            else if (isPipes) {
                recordData(data);
            }
            else {
                MainController.MoreKernel32.instance.WaitForSingleObject(mutex, INFINITE);
                recordData(data);
                MainController.MoreKernel32.instance.ReleaseMutex(mutex);
                mutex = null;
            }
        } catch (Exception e) {
            System.out.println("error="+e.getMessage());
        }
    }

    //функция для записи в файл
    private void recordData(dataRequest data) {
        try {
            flag = false;
            BufferedWriter writer =
                new BufferedWriter(new FileWriter("src/main/resources/coordinates.txt"));
            for (int i = 0; i < data.abscissas.size(); i++) {
                writer.write(
                    data.abscissas.get(i) + " " +
                        data.ordinates.get(i) + "\n");
            }
            writer.close();
            flag = true;
        }
        catch (IOException e) {
            model.addAttribute("error", "Error with writing to file");
        }
        catch (NullPointerException e) {
            model.addAttribute("error",
                "Invalid value passed to the WriteToFile thread.");
        }
        catch (Exception e) {
            model.addAttribute("error",
                "Unknown error in WriteToFile thread.");
        }
    }
}
