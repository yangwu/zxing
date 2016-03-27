/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android.result;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EncodingUtils;

import com.google.zxing.Result;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.ParsedResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Message;

/**
 * This class handles TextParsedResult as well as unknown formats. It's the fallback handler.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class TextResultHandler extends ResultHandler {

  private static final int[] buttons = {
	  R.string.upload,
      R.string.button_web_search,
      R.string.button_share_by_email,
      R.string.button_share_by_sms,
      R.string.button_custom_product_search,
  };

  public TextResultHandler(Activity activity, ParsedResult result, Result rawResult) {
    super(activity, result, rawResult);
  }

  @Override
  public int getButtonCount() {
    return hasCustomProductSearch() ? buttons.length : buttons.length - 1;
  }

  @Override
  public int getButtonText(int index) {
    return buttons[index];
  }

  @Override
  public void handleButtonPress(int index) {
    String text = getResult().getDisplayResult();
    switch (index) {
      case 0:
    	  if(isNetworkAvailable(this.getActivity())){
    		  NetworkThread t = new NetworkThread();
        	  t.setContent(text);
        	  new Thread(t).start();  
    	  }else{
    		  AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
      	    builder.setTitle(R.string.app_name);
      	    builder.setPositiveButton(R.string.button_ok, null);
      		builder.setMessage(R.string.network_off);
    	    builder.show();
    	  }
    	  break;
      case 1:
        webSearch(text);
        break;
      case 2:
        shareByEmail(text);
        break;
      case 3:
        shareBySMS(text);
        break;
      case 4:
        openURL(fillInCustomSearchURL(text));
        break;
    }
  }
  
  private boolean isNetworkAvailable(Activity activity){
	  ConnectivityManager connectMgr = (ConnectivityManager) activity
	          .getSystemService(activity.CONNECTIVITY_SERVICE);
	   if(connectMgr != null){
		   NetworkInfo[] infos = connectMgr.getAllNetworkInfo();
		   if(infos != null){
			   for (int k=0;k<infos.length;k++){
				   if(infos[k].getState() == NetworkInfo.State.CONNECTED)
					   return true;
			   }
		   }
	   }
		   return false;
	   
  }

  @Override
  public int getDisplayTitle() {
    return R.string.result_text;
  }
  
  class NetworkThread implements Runnable{
	  private String content;
		 

		public String getContent() {
			return content;
		}


		public void setContent(String content) {
			this.content = content;
		}


		@Override
		public void run() {
			// TODO Auto-generated method stub
			  try {
				System.out.println("start to upload:************");
				//120.25.88.137
				URL url = new URL("http://wishconsole.com/addTrackingData.php");
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.setUseCaches(false);
				conn.setRequestMethod("POST");
				OutputStream out = (OutputStream)conn.getOutputStream();
				String params = "device_id=" + Build.BOARD + Build.BRAND + Build.DEVICE + Build.HARDWARE + Build.ID + "&user_id=1&tracking_number=" + getContent();
				out.write(params.getBytes());
				out.flush();
				out.close();
				InputStream in = conn.getInputStream();
				ByteArrayBuffer bArray = new ByteArrayBuffer(1024);
				byte[] bt = new byte[1024];
				int i = 0;
				while(-1!= (i = in.read(bt))){
					bArray.append(bt,0,i);
				}
				String webcontent = EncodingUtils.getString(bArray.toByteArray(), "utf-8");
				System.out.println("RESULT:************" + webcontent);
				 CaptureActivity mainActivity = (CaptureActivity)getActivity();
				  Message message = Message.obtain(mainActivity.getHandler(), R.id.upload_content);
				  message.obj = webcontent;
				  mainActivity.getHandler().sendMessage(message);
			      
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
  }
}
