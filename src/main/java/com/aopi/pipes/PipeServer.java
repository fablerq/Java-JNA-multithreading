package com.aopi.pipes;

import com.aopi.controllers.MainController;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import org.springframework.ui.Model;

import static com.sun.jna.platform.win32.WinBase.*;

public class PipeServer {
  private String pipeName = null;

  public PipeServer(String pipeName) {
    this.pipeName = "\\\\.\\pipe\\"+pipeName;
  }

  public HANDLE createPipe()
  {
    HANDLE hPipe = MainController.MoreKernel32.instance.CreateNamedPipe(
        pipeName,
        Kernel32.PIPE_ACCESS_DUPLEX,
        PIPE_TYPE_BYTE | PIPE_READMODE_BYTE | PIPE_WAIT,
        1,
        1024 * 16,
        1024 * 16,
        NMPWAIT_USE_DEFAULT_WAIT,
        null);
    return hPipe;
  }

  public byte[] getPipeServerOutput(HANDLE hPipe, Model model) {
    try
    {
      byte[] buff = new byte[1024];
      IntByReference numOfBytesRead = new IntByReference(0);

      if (hPipe != INVALID_HANDLE_VALUE)
      {
        while (!MainController.MoreKernel32.instance.ConnectNamedPipe(hPipe,
            null))
        {
          System.out.println("client connected");

          MainController.MoreKernel32.instance.ReadFile(hPipe,
              buff,
              buff.length,
              numOfBytesRead,
              null);
          System.out.println("response from client: "+new String(buff));
          MainController.MoreKernel32.instance.DisconnectNamedPipe(hPipe);
          MainController.MoreKernel32.instance.CloseHandle(hPipe);
          System.out.println("connection disconnect");
          break;
        }
      } else
      {
        model.addAttribute("error", "Error in PipeServer while working with " +
            "thread" + pipeName + "' . Error during creating server");
      }
      return buff;
    } catch (Exception e)
    {
      model.addAttribute("error",
          "Unknown error in PipeServer while working with thread '" + pipeName + "'");
      return null;
    }
  }
}