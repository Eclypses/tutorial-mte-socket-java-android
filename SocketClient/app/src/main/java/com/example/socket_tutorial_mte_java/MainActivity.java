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



package com.example.socket_tutorial_mte_java;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements SocketCallback {

  private final String TAG = this.getClass().getSimpleName();
  private static final int SETUP_DIALOG = 100;
  private MyApplication myApp;
  enum CommStatus {Offline, Opening, Connected, Secured, Wait4Answer}
  CommStatus commStatus;

  @SuppressLint("ClickableViewAccessibility")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (BuildConfig.DEBUG)
      Log.d(TAG, "onCreate() Enter");
    myApp = MyApplication.getInstance();
    setContentView(R.layout.activity_main);
    setTitle(getString(R.string.app_name));
    // Implement the clear button functionality for userInput.
    EditText userInput = findViewById(R.id.userInput);
    userInput.setOnTouchListener((v, ev) -> {
      EditText et = (EditText)v;
      if (et == userInput) {
        if (ev.getX() >= et.getWidth() - et.getTotalPaddingRight()) {
          if (ev.getAction() == MotionEvent.ACTION_UP) {
            et.setText("");
          }
        }
      }
      return false;
    });
    // Implement an automatic call to send the data when userInput is done.
    userInput.setOnEditorActionListener((v, actionId, ev) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        sendDataClicked(findViewById(R.id.button));
      }
      return false;
    });
    // Setup the text areas, part 1
    TextView tv = findViewById(R.id.textViewEnc);
    tv.setTextColor(getColor(R.color.white));
    tv = findViewById(R.id.textViewRecvd);
    tv.setTextColor(getColor(R.color.white));
    tv.setBackgroundColor(getColor(R.color.green));
    tv = findViewById(R.id.textViewDec);
    tv.setTextColor(getColor(R.color.white));
    // And disable the "data send" button for now
    findViewById(R.id.button).setEnabled(false);
    if (!myApp.isCommOpen()) {
      // Run the communication setup dialog
      Intent i = new Intent(this, SetupDialog.class);
      MyApplication.SetupParams setupParams = myApp.getSetupParams();
      i.putExtra("ipAddress", setupParams.ipAddress);
      i.putExtra("port", setupParams.port);
      //noinspection deprecation
      startActivityForResult(i, SETUP_DIALOG);
    } else {
      // Communication is already open,
      // so lets run the MTE setup.
      if (!myApp.setupMTE(null))
        init(false);
    }
    if (BuildConfig.DEBUG)
      Log.d(TAG, "OnCreate() Exit");
  }


  private void init(boolean ok) {
    TextView tv;
    // Setup the text areas part 2
    // Start communication or toast an MTE init error
    if (ok) {
      Objects.requireNonNull(getSupportActionBar()).setSubtitle(myApp.getVersion());
      tv = findViewById(R.id.textViewEnc);
      tv.setBackgroundColor(getColor(R.color.green));
      tv.setText(R.string.enc_ready);
      tv = findViewById(R.id.textViewDec);
      tv.setBackgroundColor(getColor(R.color.green));
      tv.setText(R.string.dec_ready);
      commStatus = CommStatus.Secured;
    }
    else {
      tv = findViewById(R.id.textViewEnc);
      tv.setBackgroundColor(getColor(R.color.red));
      tv.setText(R.string.enc_not_ready);
      tv = findViewById(R.id.textViewDec);
      tv.setBackgroundColor(getColor(R.color.red));
      tv.setText(R.string.dec_not_ready);
      commStatus = CommStatus.Offline;
      Toast.makeText(this, R.string.mte_init_failed, Toast.LENGTH_LONG).show();
    }
  }


  //---------------------------------------------------------------------
  // Our child activity (setup dialog) has terminated. Decide what to do.
  //---------------------------------------------------------------------
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if ((requestCode == SETUP_DIALOG) && (resultCode == RESULT_OK)) {
      MyApplication.SetupParams setupParams = myApp.getSetupParams();
      setupParams.ipAddress = data.getStringExtra("ipAddress");
      setupParams.port = data.getIntExtra("port", setupParams.port);
      commStatus = CommStatus.Opening;
      if (!myApp.openCommunication(setupParams, this))
        init(false);
      return;
    }
    init(false);
  }


  //-----------------------------------------------------------------------
  // Our activity is being destroyed. If it is actually being finished off,
  // we call the closeCommunication() and terminate() functions to cleanup.
  //-----------------------------------------------------------------------
  @Override
  protected void onDestroy() {
    if (isFinishing()) {
      myApp.closeCommunication();
      myApp.terminate();
    }
    super.onDestroy();
  }


  public void sendDataClicked(View view) {
    if (!view.isEnabled())
      return;
    if (BuildConfig.DEBUG)
      Log.d(TAG, "sendDataClicked() Enter");
    findViewById(R.id.userInput).clearFocus();  // just take the cursor away from userInput
    // Encode the plaintext
    EditText userInput = findViewById(R.id.userInput);
    byte[] encoded = myApp.encodeData(userInput.getText().toString().getBytes(StandardCharsets.UTF_8));
    // Check the status and update the Encoder text field
    TextView tv = findViewById(R.id.textViewRecvd);
    tv.setText("");
    tv = findViewById(R.id.textViewDec);
    tv.setText("");
    tv = findViewById(R.id.textViewEnc);
    if (encoded == null) {
      tv.setBackgroundColor(getColor(R.color.red));
      tv.setText(R.string.enc_no_encode);
      if (BuildConfig.DEBUG)
        Log.d(TAG, "sendDataClicked() Exit");
      return;
    } else {
      // We take the "encoded" byte array and convert it to a Base64 string
      // so we can show it here for demonstration purposes.
      tv.setBackgroundColor(getColor(R.color.green));
      tv.setText(Base64.encodeToString(encoded, Base64.NO_WRAP));
    }
    commStatus = CommStatus.Wait4Answer;
    myApp.sendToServer('m', encoded, MyApplication.ReceiveMode.WAIT_MESSAGE);
    view.setEnabled(false);
    if (BuildConfig.DEBUG)
      Log.d(TAG, "sendDataClicked() Exit");
  }


  @SuppressLint("SetTextI18n")
  @Override
  public void answerFromServer(byte[] data) {
    String s;
    if (BuildConfig.DEBUG)
      Log.d(TAG, "answerFromServer() Enter");
    switch (commStatus) {
      case Opening:
        s = new String(data);
        if (s.equals("Ready")) {
          commStatus = CommStatus.Connected;
          if (BuildConfig.DEBUG)
            Log.d(TAG, "answerFromServer(): connection established");
        }
        else {
          commStatus = CommStatus.Offline;
          if (BuildConfig.DEBUG)
            Log.d(TAG, "answerFromServer(): " + new String(data));
        }
        if (commStatus == CommStatus.Offline)
          init(false);
        else
          myApp.setupMTE(null);
        break;
      case Connected:
        s = new String(data);
        if (s.equals("Ready")) {
          commStatus = CommStatus.Secured;
          if (BuildConfig.DEBUG)
            Log.d(TAG, "answerFromServer(): communication secured");
        }
        else {
          commStatus = CommStatus.Offline;
          if (BuildConfig.DEBUG)
            Log.d(TAG, "answerFromServer(): " + new String(data));
        }
        init(commStatus == CommStatus.Secured);
        //---------------------------------------------------
        // Communication is secured, now let's do a ping test
        //---------------------------------------------------
        EditText userInput = findViewById(R.id.userInput);
        userInput.setText("ping");
        sendDataClicked(userInput);
        break;
      case Wait4Answer:
        // Try to decode the response
        byte[] decoded = myApp.decodeData(Arrays.copyOfRange(data, 1, data.length));
        // check the result and update the fields
        // We take the "data" byte array and convert it to a Base64 string
        // so we can show it here for demonstration purposes.
        TextView tv = findViewById(R.id.textViewRecvd);
        tv.setText(Base64.encodeToString(data, Base64.NO_WRAP));
        tv = findViewById(R.id.textViewDec);
        if (decoded == null) {
          tv.setBackgroundColor(getColor(R.color.red));
          tv.setText(R.string.dec_no_decode);
        } else {
          tv.setBackgroundColor(getColor(R.color.green));
          tv.setText(new String(decoded));
        }
        commStatus = CommStatus.Secured;
        findViewById(R.id.userInput).setEnabled(true);
        findViewById(R.id.button).setEnabled(true);
        break;
      default:
        if (BuildConfig.DEBUG)
          Log.d(TAG, "answerFromServer() unknown communication status");
        break;
    }
    Log.d(TAG, "answerFromServer() Exit");
  }
}
