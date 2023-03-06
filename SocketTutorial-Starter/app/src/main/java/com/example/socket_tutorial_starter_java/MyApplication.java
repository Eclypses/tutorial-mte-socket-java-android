//**************************************************************************************************
// THIS SOFTWARE MAY NOT BE USED FOR PRODUCTION.
//
// Copyright (c) Eclypses, Inc.
//
// All rights reserved.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//**************************************************************************************************



package com.example.socket_tutorial_starter_java;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;


//---------------------------------------------------------------------------------------
// NOTE TO THE SOFTWARE DEVELOPER:
//
// This application source code is educational material. In order to keep the source code
// as short as possible, many internal and unlikely errors are not communicated to the
// upper layers of the user interface.
// It is therefore suggested to run this app in an emulator or at least on a connected
// device with a live logcat facility in order to catch internal error messages which are
// written to logcat using the "debug" log level.
//---------------------------------------------------------------------------------------
public class MyApplication extends Application {

  //-------------------------------
  // Default communication settings
  //-------------------------------
  private static final String DEFAULT_IP_ADDRESS = "*** SERVER'S IP HERE! ***";
  private static final int DEFAULT_PORT = 27015;


  public static class SetupParams {
    String ipAddress;
    int port;
  }


  private final String TAG = this.getClass().getSimpleName();
  private static MyApplication singleton;
  private boolean initDone;
  private Socket socket;
  private boolean socketOpen;
  private InputStream sockIn;
  private OutputStream sockOut;
  private SocketCallback socketCallback;
  private SetupParams setupParams;


  //------------------------------------------------------------------------------------------
  // onCreate():
  // Android creates an instance of "MyApplication" when it launches the app. This instance
  // will survive even the destruction of the main activity most of the times. It will NEVER
  // be instantiated more than once.
  // Instantiation takes places before the main activity is created. Any activity of the app
  // must never instantiate MyApplication! In order to access functions of MyApplication,
  // these functions must be declared static or called by means of utilizing the "getInstance"
  // function (see below).
  //------------------------------------------------------------------------------------------
  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "onCreate() Enter");
    singleton = this;
    setupParams = new SetupParams();
    setupParams.ipAddress = DEFAULT_IP_ADDRESS;
    setupParams.port = DEFAULT_PORT;
    socket = null;
    sockIn = null;
    sockOut = null;
    socketOpen = false;
    socketCallback = null;
    initDone = false;
    Log.d(TAG, "onCreate() Exit");
  }


  //---------------------------------------------------------------------
  // This helper function supplies an instance handle to the application.
  //---------------------------------------------------------------------
  public static MyApplication getInstance() {
    return singleton;
  }


  public SetupParams getSetupParams() {
    return setupParams;
  }


  public boolean isInitDone() {
    return initDone;
  }


  //------------------------------------------------------------------------
  // init() is called by the activity in charge of managing the app in order
  // to do basic initialization. The parameter is a set of settings used for
  // communication to the server.
  //------------------------------------------------------------------------
  @SuppressWarnings("UnusedReturnValue")
  public boolean init(SetupParams newSetupParams) {
    if (initDone) {
      Log.d(TAG, "init(): already initialized");
      return false;
    }
    Log.d(TAG, "init(): processing");
    setupParams = newSetupParams;
    //--- MTE STUFF GOES HERE ------------------------------
    // This is a good place to initialize MTE and create the
    // Encoder and Decoder objects.
    //------------------------------------------------------
    initDone = setupMTE();
    return initDone;
  }


  //-------------------------------------------------------------------------
  // terminate() sets everything back to an unconfigured state so that init()
  // could be called again. The activity in charge of managing the app may
  // call this function if it detects that it is being finished by the OS.
  //-------------------------------------------------------------------------
  public void terminate() {
    Log.d(TAG, "terminate()");
    if (!initDone) {
      Log.d(TAG, "terminate(): failed");
      return;
    }
    closeCommunication();
    setupParams.ipAddress = DEFAULT_IP_ADDRESS;
    setupParams.port = DEFAULT_PORT;
    initDone = false;
  }


  //-----------------------------------------------------------------
  // openCommunication() creates and opens a socket to the server and
  // sets shortcuts for the input and output streams.
  // It also registers the instance of "whatever" will receive the
  // callbacks.
  //-----------------------------------------------------------------
  public void openCommunication(SocketCallback socketCallback) {
    Log.d(TAG, "openCommunication(): Enter");
    if (!initDone) {
      Log.d(TAG, "openCommunication(): Exit #1");
      return;
    }
    // Cleanup if we have an open socket
    closeCommunication();
    // Store the instance providing the callback function
    this.socketCallback = socketCallback;
    //----------------------------------------------------------------
    // We have to run the opening of the socket in a different thread,
    // otherwise Android will throw a "NetworkOnMainThreadException"
    //----------------------------------------------------------------
    Thread thread = new Thread(() -> {
      // Create and open the network socket
      try {
        socket = new Socket(setupParams.ipAddress, setupParams.port);
        sockIn = socket.getInputStream();
        sockOut = socket.getOutputStream();
      } catch (IOException e) {
        Log.d(TAG, "openCommunication(): IOException creating socket and/or readers/writers");
        postAnswerToApp("FALSE".getBytes(StandardCharsets.UTF_8));
        return;
      }
      postAnswerToApp("TRUE".getBytes(StandardCharsets.UTF_8));
      socketOpen = true;
    });
    thread.start();
    Log.d(TAG, "openCommunication(): Exit #2");
  }


  //---------------------------------------------------------
  // closeCommunication() closes the socket to the server and
  // clears all associated variables.
  //---------------------------------------------------------
  public void closeCommunication() {
    if (!socketOpen)
      return;
    try {
      socket.close();
    } catch (IOException e) {
      Log.d(TAG, "closeCommunication(): IOException closing socket");
    }
    socket = null;
    sockIn = null;
    sockOut = null;
    socketOpen = false;
  }


  //----------------------------------------------------------------------------
  // sendToServer() will send the given string to the server and will also try
  // to get an answer. Processing the answer will run in a separate thread and
  // ultimately issue a callback to the application.
  //----------------------------------------------------------------------------
  public void sendToServer(byte[] data) {
    Log.d(TAG, "sendToServer(): Enter");
    //--------------------------------------------------------------
    // We must run the writing to the socket in a different thread,
    // otherwise Android will throw a "NetworkOnMainThreadException"
    //--------------------------------------------------------------
    Thread thread1 = new Thread(() -> {
      ByteBuffer dataLengthTx = ByteBuffer.allocate(4);
      // The default byte order of a ByteBuffer is always BIG Endian,
      // which is what we want - but here we go anyway!
      dataLengthTx.order(ByteOrder.BIG_ENDIAN);
      dataLengthTx.putInt(data.length);
      try {
        sockOut.write(dataLengthTx.array());
        sockOut.write(data);
      } catch (IOException e) {
        Log.d(TAG, "sendToServer(): exception writing to socket");
        return;
      }
      Log.d(TAG, "sendToServer(): data sent to server");
      //------------------------------------------------------------------
      // Reading the answer from the socket must be in a another extra
      // thread, just to avoid the dreaded "NetworkOnMainThreadException".
      //------------------------------------------------------------------
      Thread thread2 = new Thread(() -> {
        Log.d(TAG, "sendToServer(): [receiver thread] trying to read");
        byte[] dataLengthRx = new byte[4];
        int i = 0;
        int rd;
        try {
          while (i < 4) {
            rd = sockIn.read(dataLengthRx, i, 4 - i);
            if (rd < 0) {
              Log.d(TAG, "sendToServer(): [receiver thread] abort reading from socket");
              return;
            }
            i += rd;
          }
        } catch (IOException e) {
          Log.d(TAG, "sendToServer(): [receiver thread] exception reading from socket");
          return;
        }
        // The default byte order of a ByteBuffer is always BIG Endian!
        // And that is what we want anyway. So we spare us the extra work
        // of setting up a named ByteBuffer instantiation and setting
        // up the byte order to ByteOrder.BIG_ENDIAN;
        int c = ByteBuffer.wrap(dataLengthRx).getInt();
        byte[] dataBytes = new byte[c];
        i = 0;
        try {
          while (i < c) {
            rd = sockIn.read(dataBytes, i, c - i);
            if (rd < 0) {
              Log.d(TAG, "sendToServer(): [receiver thread] abort reading from socket");
              return;
            }
            i += rd;
          }
        } catch (IOException e) {
          Log.d(TAG, "sendToServer(): [receiver thread] exception reading from socket");
          return;
        }
        Log.d(TAG, "sendToServer(): [receiver thread] reading completed");
        postAnswerToApp(dataBytes);
      });
      thread2.start();
    });
    thread1.start();
  }


  //----------------------------------------------------------------------------
  // postAnswerToApp() will run the callback to get server's answers and other
  // information back to the app. It is up to the app to interpret the context
  // of the answers sent.
  // In order to make sure that the app can access its views within the callback
  // function, we make sure to run it on the main UI thread.
  //----------------------------------------------------------------------------
  private void postAnswerToApp(byte[] data) {
    if (socketCallback == null)
      return;
    // Get a handler to the main UI thread ...
    Handler handler = new Handler(Looper.getMainLooper());
    // ... and run the callback there!
    handler.post(() -> socketCallback.answerFromServer(data));
  }


  //------------------------------------------------------------------
  // setupMTE() is called from init() and will initialize MTE and then
  // create the Encoder and Decoder.
  //------------------------------------------------------------------
  private boolean setupMTE() {
    return true;
  }


  //--- MTE STUFF GOES HERE -------------------------------------
  // This is a good place to place some basic MTE functions
  // like encode(), decode() and getVersion(). These wrappers
  // can make life easier for us (e.g. incorporating data
  // type conversions).
  // These wrapper functions will also help to decouple Java from
  // dependencies against MTE.
  //-------------------------------------------------------------
}
