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

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.EditText;


public class SetupDialog extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SpannableString ss = new SpannableString(getString(R.string.app_name) + " " +
            getString(R.string.setup));
    ss.setSpan(new ForegroundColorSpan(getColor(R.color.primary)),
            0, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    ss.setSpan(new TypefaceSpan("sans-serif-condensed"),
            0, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    ss.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    setTitle(ss);
    setContentView(R.layout.activity_setup);
    Intent i = getIntent();
    ((EditText) findViewById(R.id.inputIP)).setText(i.getStringExtra("ipAddress"));
    ((EditText) findViewById(R.id.inputPort)).setText(String.valueOf(i.getIntExtra("port", 0)));
  }


  public void okClicked(View view) {
    Intent result = new Intent();
    result.putExtra("ipAddress",
                    ((EditText)findViewById(R.id.inputIP)).getText().toString());
    result.putExtra("port",
                    Integer.valueOf(((EditText)findViewById(R.id.inputPort)).getText().toString()));
    setResult(RESULT_OK, result);
    finish();
  }
}
