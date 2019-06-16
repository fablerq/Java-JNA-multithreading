package com.aopi.controllers;

import com.aopi.models.Coordinate;
import com.aopi.models.dataRequest;
import com.aopi.pipes.PipeClient;
import com.aopi.pipes.PipeServer;
import com.aopi.threads.*;
import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD.SIZE_T;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.W32APIOptions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.sun.jna.platform.win32.WinBase.INFINITE;

@Controller
public class MainController {

    public interface MoreKernel32 extends Kernel32 {
        MoreKernel32 instance = Native.loadLibrary(
            "kernel32",
            MoreKernel32.class,
            W32APIOptions.DEFAULT_OPTIONS);
        HANDLE CreateThread(Pointer lpThreadAttributes,
                            SIZE_T dwStackSize,
                            Callback lpStartAddress,
                            LPVOID lpParameter,
                            DWORD dwCreationFlags,
                            DWORDByReference lpThreadId);

    }

    HANDLE mutex =
        MoreKernel32.instance.CreateMutex(null, true, "AOPI");

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @RequestMapping(value = "/data", method = RequestMethod.POST)
    public String setData(Model model, @RequestParam Integer n) {
        model.addAttribute("coordinates", createCoordinates(n));
        return "setData";
    }

    @RequestMapping(value = "/handle", method = RequestMethod.POST)
    public String handle(Model model, dataRequest request) {
        if (request.type.equals("mutex"))
        {
            handleMUTEX(model, request);
        }
        else if (request.type.equals("events"))
        {
            handleEVENT(model, request);
        }
        else if (request.type.equals("pipes"))
        {
            handlePIPES(model, request);
        }
        else {
            model.addAttribute("error", "Error. Invalid synchronization type.");
        }
        return "chart";
    }

    //synchronization with pipes
    private Model handlePIPES(Model model, dataRequest request)  {
        DWORD flag = new DWORD(0);
        SIZE_T size = new SIZE_T(0);

        WriteToFileThread funcWriting =
            new WriteToFileThread(request, model, true);
        HANDLE writingThread = MoreKernel32.instance.CreateThread(
            null, size, funcWriting,
            null, flag, null);
        MoreKernel32.instance.WaitForSingleObject(writingThread, INFINITE);

        PipeServer writingPipeServer = new PipeServer("write");
        PipeClient writingPipeClient = new PipeClient("write");
        HANDLE hWritingPipeServer = writingPipeServer.createPipe();
        byte[] dataToWritingPipe = doSerialization(funcWriting.getFlag(), model);

        if(model.containsAttribute("error")) {
          return model;
        }

        writingPipeClient.callPipe(dataToWritingPipe, model);
        byte[] writingRes = writingPipeServer.getPipeServerOutput(
            hWritingPipeServer,
            model);

        if(model.containsAttribute("error")) {
          return model;
        }

        boolean writingPipeResult =
            (boolean) doDeserialization(writingRes, model);

        if(model.containsAttribute("error")) {
          return model;
        }

        if (writingPipeResult && !model.containsAttribute("error")) {
            ReadFromFileThread funcReading =
                new ReadFromFileThread(model, true);
            HANDLE readingThread =
                MoreKernel32.instance.CreateThread(
                    null, size, funcReading,
                    null, flag, null);
            MoreKernel32.instance.WaitForSingleObject(readingThread, INFINITE);

            PipeServer readingPipeServer = new PipeServer("read");
            PipeClient readingPipeClient = new PipeClient("read");
            HANDLE hReadingPipeServer = readingPipeServer.createPipe();
            byte[] dataToReadingPipe = doSerialization(funcReading.getList(), model);

            if(model.containsAttribute("error")) {
              return model;
            }

            readingPipeClient.callPipe(dataToReadingPipe, model);
            byte[] readingRes = writingPipeServer.getPipeServerOutput(
                hReadingPipeServer,
                model);

            if(model.containsAttribute("error")) {
              return model;
            }

            ArrayList<Coordinate> readingPipeResult =
                    (ArrayList<Coordinate>) doDeserialization(readingRes, model);

            if(model.containsAttribute("error")) {
              return model;
            }

            if (!readingPipeResult.isEmpty() && !model.containsAttribute("error")) {
                ArrayList<Coordinate> list = readingPipeResult;

                SortingThread funcSorting =
                    new SortingThread(list, model, true);
                HANDLE sortingThread =
                    MoreKernel32.instance.CreateThread(
                        null, size, funcSorting,
                        null, flag, null);
                MoreKernel32.instance.WaitForSingleObject(sortingThread, INFINITE);

                PipeServer sortingPipeServer = new PipeServer("sort");
                PipeClient sortingPipeClient = new PipeClient("sort");
                HANDLE hSortingPipeServer = sortingPipeServer.createPipe();
                byte[] dataToSortingPipe = doSerialization(funcSorting.getSortedList(), model);

                if(model.containsAttribute("error")) {
                  return model;
                }

                sortingPipeClient.callPipe(dataToSortingPipe, model);
                byte[] sortingRes = sortingPipeServer.getPipeServerOutput(
                    hSortingPipeServer,
                    model);

                if(model.containsAttribute("error")) {
                  return model;
                }

                ArrayList<Coordinate> sortingPipeResult =
                    (ArrayList<Coordinate>) doDeserialization(sortingRes, model);

                if(model.containsAttribute("error")) {
                  return model;
                }

                if (!sortingPipeResult.isEmpty() && !model.containsAttribute("error")) {
                    list = sortingPipeResult;

                    CheckRepeatsThread funcRepeats =
                        new CheckRepeatsThread(list, model, true);
                    HANDLE checkRepeatsThread =
                        MoreKernel32.instance.CreateThread(
                            null, size, funcRepeats,
                            null, flag, null);

                    MoreKernel32.instance.WaitForSingleObject(checkRepeatsThread, INFINITE);

                    PipeServer checkingPipeServer = new PipeServer("check");
                    PipeClient checkingPipeClient = new PipeClient("check");
                    HANDLE hCheckingPipeServer = checkingPipeServer.createPipe();
                    byte[] dataToCheckingPipe = doSerialization(funcRepeats.getFlag(), model);

                    if(model.containsAttribute("error")) {
                      return model;
                    }

                    checkingPipeClient.callPipe(dataToCheckingPipe, model);
                    byte[] checkingRes = checkingPipeServer.getPipeServerOutput(
                        hCheckingPipeServer,
                        model);

                    if(model.containsAttribute("error")) {
                      return model;
                    }

                    boolean checkingPipeResult =
                        (boolean) doDeserialization(checkingRes, model);

                    if(model.containsAttribute("error")) {
                      return model;
                    }

                    if (checkingPipeResult && !model.containsAttribute("error")) {

                        InterpolationThread funcInterpolation =
                            new InterpolationThread(list, model);
                        HANDLE interpolationThread =
                            MoreKernel32.instance.CreateThread(
                                null, size, funcInterpolation,
                                null, flag, null);

                        MoreKernel32.instance.WaitForSingleObject(interpolationThread, INFINITE);
                    }
                    else
                    if (!model.containsAttribute("error")) {
                        model.addAttribute("error",
                            "Synchronization error. CheckRepeats thread " +
                                "hasn't terminated.");
                    }
                }
                else
                if (!model.containsAttribute("error")) {
                    model.addAttribute("error",
                        "Synchronization error. Sorting thread " +
                            "hasn't terminated.");
                }
            }
            else
            if (!model.containsAttribute("error")) {
                model.addAttribute("error",
                    "Synchronization error. ReadFromFile thread " +
                        "hasn't terminated.");
            }
        }
        else
        if (!model.containsAttribute("error")) {
            model.addAttribute("error",
                "Error while working with pipes in ReadFromFile thread");
        }

        return model;
    }

    //synchronization with events
    private Model handleEVENT(Model model, dataRequest request)  {
        DWORD flag = new DWORD(0);
        SIZE_T size = new SIZE_T(0);

        HANDLE writingEvent = MoreKernel32.instance.CreateEvent(
            null, false, false, "write");
        WriteToFileThread funcWriting =
            new WriteToFileThread(request, model, writingEvent);
        HANDLE writingThread = MoreKernel32.instance.CreateThread(
            null, size, funcWriting,
            null, flag, null);

        MoreKernel32.instance.WaitForSingleObject(writingEvent, INFINITE);
        MoreKernel32.instance.CloseHandle(writingEvent);

        if (funcWriting.getFlag() && !model.containsAttribute("error")) {
            HANDLE readingEvent = MoreKernel32.instance.CreateEvent(
                null, false, false, "read");
            ReadFromFileThread funcReading =
                new ReadFromFileThread(model, readingEvent);
            HANDLE readingThread =
                MoreKernel32.instance.CreateThread(
                    null, size, funcReading,
                    null, flag, null);
            MoreKernel32.instance.WaitForSingleObject(readingEvent, INFINITE);
            MoreKernel32.instance.CloseHandle(readingEvent);

            if (!funcReading.getList().isEmpty() && !model.containsAttribute("error")) {
                ArrayList<Coordinate> list = funcReading.getList();

                HANDLE sortingEvent = MoreKernel32.instance.CreateEvent(
                    null, false, false, "sort");
                SortingThread funcSorting =
                    new SortingThread(list, model, sortingEvent);
                HANDLE sortingThread =
                    MoreKernel32.instance.CreateThread(
                        null, size, funcSorting,
                        null, flag, null);
                MoreKernel32.instance.WaitForSingleObject(sortingEvent, INFINITE);
                MoreKernel32.instance.CloseHandle(sortingEvent);

                if (!funcSorting.getSortedList().isEmpty() && !model.containsAttribute("error")) {
                    list = funcSorting.getSortedList();

                    HANDLE checkingRepeatsEvent = MoreKernel32.instance.CreateEvent(
                        null, false, false, "check");
                    CheckRepeatsThread funcRepeats =
                        new CheckRepeatsThread(list, model, checkingRepeatsEvent);
                    HANDLE checkRepeatsThread =
                        MoreKernel32.instance.CreateThread(
                            null, size, funcRepeats,
                            null, flag, null);

                    MoreKernel32.instance.WaitForSingleObject(checkingRepeatsEvent, INFINITE);
                    MoreKernel32.instance.CloseHandle(checkingRepeatsEvent);

                    if (funcRepeats.getFlag() && !model.containsAttribute("error")) {
                        HANDLE interpolationEvent = MoreKernel32.instance.CreateEvent(
                            null, false, false, "interpolate");
                        InterpolationThread funcInterpolation =
                            new InterpolationThread(list, model, interpolationEvent);
                        HANDLE interpolationThread =
                            MoreKernel32.instance.CreateThread(
                                null, size, funcInterpolation,
                                null, flag, null);

                        MoreKernel32.instance.WaitForSingleObject(interpolationEvent, INFINITE);
                        MoreKernel32.instance.CloseHandle(interpolationEvent);
                    }
                    else
                    if (!model.containsAttribute("error")) {
                        model.addAttribute("error",
                            "Synchronization error. CheckRepeats thread " +
                                "hasn't terminated.");
                    }
                }
                else
                if (!model.containsAttribute("error")) {
                    model.addAttribute("error",
                        "Synchronization error. Sorting thread " +
                            "hasn't terminated.");
                }
            }
            else
            if (!model.containsAttribute("error")) {
                model.addAttribute("error",
                    "Synchronization error. ReadFromFile thread " +
                        "hasn't terminated.");
            }
        }
        else
            if (!model.containsAttribute("error")) {
                model.addAttribute("error",
                    "Synchronization error. WriteToFile thread " +
                        "hasn't terminated.");
            }

        return model;
    }

    //synchronization with mutex
    private Model handleMUTEX(Model model, dataRequest request)  {
        DWORD flag = new DWORD(0);
        SIZE_T size = new SIZE_T(0);

        WriteToFileThread funcWriting =
            new WriteToFileThread(request, model, mutex);
        ReadFromFileThread funcReading =
            new ReadFromFileThread(model, mutex);

        HANDLE mutexSyncArray[] = new HANDLE[2];

        mutexSyncArray[0] = MoreKernel32.instance.CreateThread(
            null, size, funcWriting,
            null, flag, null);

        mutexSyncArray[1] = MoreKernel32.instance.CreateThread(
            null, size, funcReading,
            null, flag, null);

        MoreKernel32.instance.WaitForMultipleObjects(
            2, mutexSyncArray, true, INFINITE);
        MoreKernel32.instance.CloseHandle(mutexSyncArray[0]);
        MoreKernel32.instance.CloseHandle(mutexSyncArray[1]);
        MoreKernel32.instance.CloseHandle(mutex);

        if (!funcReading.getList().isEmpty() && !model.containsAttribute("error")) {
            ArrayList<Coordinate> list = funcReading.getList();

            SortingThread funcSorting = new SortingThread(list, model);
            HANDLE sortingThread =
                MoreKernel32.instance.CreateThread(
                    null, size, funcSorting,
                    null, flag, null);
            MoreKernel32.instance.WaitForSingleObject(sortingThread, -1);
            MoreKernel32.instance.CloseHandle(sortingThread);

            if (!funcSorting.getSortedList().isEmpty() && !model.containsAttribute("error")) {
                list = funcSorting.getSortedList();

                CheckRepeatsThread funcRepeats = new CheckRepeatsThread(list, model);
                HANDLE checkRepeatsThread =
                    MoreKernel32.instance.CreateThread(
                        null, size, funcRepeats,
                        null, flag, null);
                MoreKernel32.instance.WaitForSingleObject(checkRepeatsThread, -1);
                MoreKernel32.instance.CloseHandle(checkRepeatsThread);

                if (funcRepeats.getFlag() && !model.containsAttribute("error")) {
                    InterpolationThread funcInterpolation =
                        new InterpolationThread(list, model);
                    HANDLE interpolationThread =
                        MoreKernel32.instance.CreateThread(
                            null, size, funcInterpolation,
                            null, flag, null);
                    MoreKernel32.instance.WaitForSingleObject(interpolationThread, -1);
                    MoreKernel32.instance.CloseHandle(interpolationThread);
                }
                else
                if (!model.containsAttribute("error")) {
                    model.addAttribute("error",
                        "Synchronization error. CheckRepeats thread " +
                            "hasn't terminated.");
                }
            }
            else
            if (!model.containsAttribute("error")) {
                model.addAttribute("error",
                    "Synchronization error. Sorting thread " +
                        "hasn't terminated.");
            }
        }
        else
        if (!model.containsAttribute("error")) {
            model.addAttribute("error",
                "Synchronization error while working with writing and reading" +
                    " to file threads");
        }

        return model;
    }

    //function for creating array with desired dimension
    private static List createCoordinates(Integer n) {
        List<Coordinate> list = new ArrayList<>();
        for(int i = 1; i <= n; i++)
        {
            list.add(new Coordinate());
        }
        return list;
    }

    private static Object doDeserialization(byte[] data, Model model)
    {
        try
        {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(in);
            return is.readObject();
        } catch(Exception e)
        {
          model.addAttribute("error",
              "Deserialization error while working with pipes");
            return null;
        }
    }

    private static byte[] doSerialization(Object obj, Model model)
    {
        try
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream os = new ObjectOutputStream(bos)) {
                os.writeObject(obj);
            }
            return bos.toByteArray();
        } catch (Exception e)
        {
          model.addAttribute("error",
              "Serialization error while working with pipes");
            return null;
        }
    }
}