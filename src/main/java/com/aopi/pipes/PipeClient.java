package com.aopi.pipes;

import com.aopi.controllers.MainController;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import org.springframework.ui.Model;

import static com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE;
import static com.sun.jna.platform.win32.WinBase.NMPWAIT_USE_DEFAULT_WAIT;
import static com.sun.jna.platform.win32.WinNT.*;

public class PipeClient {
  private String pipeName = null;

  public PipeClient(String pipeName) {
    this.pipeName = "\\\\.\\pipe\\"+pipeName;
  }

  public void callPipe(byte[] data, Model model) {
    try
    {
      System.out.println("trying to connect");
      MainController.MoreKernel32.instance.WaitNamedPipe(
          pipeName,
          NMPWAIT_USE_DEFAULT_WAIT);
      System.out.println("connected to the server");
      WinNT.HANDLE hPipe = MainController.MoreKernel32.instance.CreateFile(
          pipeName,
          GENERIC_READ | GENERIC_WRITE,
          0,
          null,
          OPEN_EXISTING,
          0,
          null);
      IntByReference numOfBytesWritten = new IntByReference(0);

      if (hPipe != INVALID_HANDLE_VALUE)
      {
        boolean res = MainController.MoreKernel32.instance.WriteFile(hPipe,
            data,
            data.length,
            numOfBytesWritten,
            null);
        System.out.println("client writeFile result= "+res);
        MainController.MoreKernel32.instance.CloseHandle(hPipe);
      }
      else
      {
        model.addAttribute("error", "Error in PipeClient while working with " +
            "thread" + pipeName + "' . Server not found or busy");
      }
    }
    catch (Exception e)
    {
      model.addAttribute("error",
          "Unknown error in PipeClient while working with thread '" + pipeName + "'");
    }

  }


}