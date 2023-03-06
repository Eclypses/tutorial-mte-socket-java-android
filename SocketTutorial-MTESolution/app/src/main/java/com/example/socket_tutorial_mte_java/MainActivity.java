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
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements SocketCallback {

  private final String TAG = this.getClass().getSimpleName();
  private static final int SETUP_DIALOG = 100;
  private MyApplication myApp;
  enum CommStatus {Disabled, WaitOpen, Connected, Wait4Answer}
  CommStatus commStatus;

  @SuppressLint("ClickableViewAccessibility")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
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
    // Get an instance handle to MyMTE and take care of the
    // system initialization
    if (!myApp.isInitDone()) {
      // Run the setup dialog
      Intent i = new Intent(this, SetupDialog.class);
      MyApplication.SetupParams setupParams = myApp.getSetupParams();
      i.putExtra("ipAddress", setupParams.ipAddress);
      i.putExtra("port", setupParams.port);
      //noinspection deprecation
      startActivityForResult(i, SETUP_DIALOG);
    } else {
      // Setup is good so lets continue with initialization
      init(true);
    }
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
      commStatus = CommStatus.WaitOpen;
      myApp.openCommunication(this);
      checkConnectStatus();
    }
    else {
      tv = findViewById(R.id.textViewEnc);
      tv.setBackgroundColor(getColor(R.color.red));
      tv.setText(R.string.enc_not_ready);
      tv = findViewById(R.id.textViewDec);
      tv.setBackgroundColor(getColor(R.color.red));
      tv.setText(R.string.dec_not_ready);
      commStatus = CommStatus.Disabled;
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
      init(myApp.init(setupParams));
    }
  }


  //-----------------------------------------------------------------------
  // Our activity is being destroyed. If it is actually being finished off,
  // we call our MyMTE.terminate() function to cleanup.
  //-----------------------------------------------------------------------
  @Override
  protected void onDestroy() {
    if (isFinishing())
      myApp.terminate();
    super.onDestroy();
  }


  private void checkConnectStatus() {
    switch (commStatus) {
      case WaitOpen:
        Log.d(TAG, "checkConnectStatus(): WaitOpen");
        final Handler handler = new Handler();
        handler.postDelayed(this::checkConnectStatus, 1000);
        break;
      case Connected:
        findViewById(R.id.button).setEnabled(true);
        Toast.makeText(this, R.string.connected, Toast.LENGTH_LONG).show();
        break;
      case Disabled:
        findViewById(R.id.button).setEnabled(false);
        Toast.makeText(this, R.string.not_connected, Toast.LENGTH_LONG).show();
        break;
    }
  }


  public void sendDataClicked(View view) {
    if (!view.isEnabled())
      return;
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
      Log.d(TAG, "sendDataClicked() Exit");
      return;
    } else {
      // We take the "encoded" byte array and convert it to a "hex" string
      // so we can show it here for demonstration purposes.
      tv.setBackgroundColor(getColor(R.color.green));
      tv.setText(Base64.encodeToString(encoded, Base64.NO_WRAP));
    }
    commStatus = CommStatus.Wait4Answer;
    myApp.sendToServer(encoded);
    view.setEnabled(false);
    Log.d(TAG, "sendDataClicked() Exit");
  }


  @Override
  public void answerFromServer(byte[] data) {
    Log.d(TAG, "answerFromServer() Enter");
    switch (commStatus) {
      case WaitOpen:
        String s = new String(data);
        if (s.equals("TRUE"))
          commStatus = CommStatus.Connected;
        else
          commStatus = CommStatus.Disabled;
        break;
      case Connected:
        Log.d(TAG, "answerFromServer(): " + new String(data));
        break;
      case Wait4Answer:
        // Try to decode the response
        byte[] decoded = myApp.decodeData(data);
        // check the result and update the fields
        // We take the "data" byte array and convert it to a "hex" string
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
        commStatus = CommStatus.Connected;
        findViewById(R.id.button).setEnabled(true);
        break;
      default:
        Log.d(TAG, "answerFromServer() unknown communication status");
        break;
    }
    Log.d(TAG, "answerFromServer() Exit");
  }
}
