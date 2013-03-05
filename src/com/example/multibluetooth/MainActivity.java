package com.example.multibluetooth;
//*************************//
//多個藍芽連線範例
//(noni血氧儀 FORMAT)
//************************//
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.UUID;
import java.lang.Math.*;


import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity {
	private DateFormat dateFormat;
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter1;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter2;
	private ListView pairedListView1;
	private ListView pairedListView2;
	private TextView HR1,SPO1;
	private TextView HR2,SPO2;
	private ImageView redheart;
	private int hr1value=0,hr2value=0,spo21value=0,spo22value=0;
	private BluetoothDevice device;
	private BluetoothSocket mmSocket,mmSocket2;
	private InputStream mmInStream,mmInStream2;
    private OutputStream mmOutStream,mmOutStream2;
    private byte[] buffer = new byte[1024];
    private byte[] buffer2 = new byte[1024];
    private int bytes;
    private StringBuffer sbHR,sbHR2;
    private StringBuffer sbSPO2,sbSPO22;
    private StringBuffer sbEI,sbEI2;
    private Thread mthread;
    private boolean deviceflag = true;
    String address;
    int hr1start=0,hr2start=0;
    
    final Handler handler = new Handler();
    final Runnable callback = new Runnable(){
		public void run() {
			HR1.setText("HR1: "+sbHR.toString());
        	SPO1.setText("SPo2_1: "+sbSPO2.toString());
			}
		
    };
    
    final Runnable callback2 = new Runnable(){
		public void run() {
        	HR2.setText("HR2: "+sbHR2.toString());
        	SPO2.setText("SPo2_2: "+sbSPO22.toString());
			}
    };
    
    final Runnable heartalpha = new Runnable(){
		@TargetApi(11)
		@SuppressWarnings("deprecation")
		public void run() {
			if(Math.abs(hr1value-hr2value)<3)
				redheart.setAlpha(225);
			else if(Math.abs(hr1value-hr2value)<6)
				redheart.setAlpha(150);
			else if(Math.abs(hr1value-hr2value)<9)
				redheart.setAlpha(75);
			else if(Math.abs(hr1value-hr2value)<12)
				redheart.setAlpha(37);
			else 
				redheart.setAlpha(1);
			
			}
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 
        
        HR1 = (TextView) findViewById(R.id.HR1);
        HR2 = (TextView) findViewById(R.id.HR2);
        SPO1 = (TextView) findViewById(R.id.SPO1);
        SPO2 = (TextView) findViewById(R.id.SPO2);
        HR1.setVisibility(8);
        HR2.setVisibility(8);
        SPO1.setVisibility(8);
        SPO2.setVisibility(8);
        //動畫設定
        redheart =(ImageView) findViewById(R.id.heart);
        redheart.setAlpha(0);
        final Animation am = new ScaleAnimation(1f,1.3f,1f,1.3f,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        am.setDuration( 1000 );
        //am.setRepeatCount( -1 );
        redheart.setAnimation(am);
        final Animation am2 = new ScaleAnimation(1.3f,1f,1.3f,1f,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        am2.setDuration( 1000 );
        //連續動畫
        am.setAnimationListener(new AnimationListener(){

			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				redheart.setAnimation(am2);
				am2.start();
			}
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
			}
        	
        });
        am2.setAnimationListener(new AnimationListener(){

			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				redheart.setAnimation(am);
				am.start();
			}
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
			}
        	
        });
        
        am.startNow();

        //裝置一的清單設定
        mPairedDevicesArrayAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked);
        pairedListView1 = (ListView) findViewById(R.id.deviceList1);
        pairedListView1.setAdapter(mPairedDevicesArrayAdapter1);
        pairedListView1.setOnItemClickListener(mDeviceClickListener);
        
        //裝置二的清單設定
        mPairedDevicesArrayAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked);
        pairedListView2 = (ListView) findViewById(R.id.deviceList2);
        pairedListView2.setAdapter(mPairedDevicesArrayAdapter2);
        pairedListView2.setOnItemClickListener(mDeviceClickListener2);
        
        //-----線狀態監聽
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);
        
        //----------檢查藍芽是否開啟
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "NO BT", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
      
        mBtAdapter.startDiscovery();//開始掃描
        
        //---------藍芽訊號接收與處裡
        mthread = new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				for(;;){
					try {
						//Log.i("thread","callback");
						if(hr1start==1||hr2start==1){
							handler.post(heartalpha);
							if(hr1start==1){
								try {
									bytes = mmInStream.read(buffer);
						        	sbHR = new StringBuffer();
						        	sbSPO2 = new StringBuffer();
						        	sbEI = new StringBuffer();
						        	hr1value=(buffer[1]+(buffer[0]-0x80)*128 & 0xff);
						        	spo21value=(buffer[2] & 0xff);
						        	sbHR.append(Integer.toString(hr1value ,10));
						        	sbSPO2.append(Integer.toString(spo21value ,10));
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
			        			handler.post(callback);
							}
							if(hr2start==1){
								try {
									bytes = mmInStream2.read(buffer2);
									sbHR2 = new StringBuffer();
						        	sbSPO22 = new StringBuffer();
						        	sbEI2 = new StringBuffer();
						        	hr2value=(buffer2[1]+(buffer2[0]-0x80)*128 & 0xff);
						        	spo22value=(buffer2[2] & 0xff);
						        	sbHR2.append(Integer.toString(hr2value ,10));
						        	sbSPO22.append(Integer.toString(spo22value ,10));
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								handler.post(callback2);
							}
						}
						sleep(1000);
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}		
					
				}
			}
		};
		try{
			mthread.start();
		}
		catch(Exception e){
			;
		}
		
    }
    
    
ProgressDialog myDialog ;
private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {            
            mBtAdapter.cancelDiscovery();
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            if(!info.equals(getResources().getString(R.string.noDevice))
            		&&!info.equals(getResources().getString(R.string.DeviceConnected)))
            {
            	address = info.substring(info.length() - 17);
                device = mBtAdapter.getRemoteDevice(address);
    		    if(device.getBondState()==BluetoothDevice.BOND_NONE){  
                    Method createBondMethod;
					try {
						createBondMethod = BluetoothDevice.class.getMethod("createBond");
						createBondMethod.invoke(device);   
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    		    }   		    
    		    else{
    		    	if(BluetoothAdapter.checkBluetoothAddress(address)){
    		    	  myDialog = ProgressDialog.show(MainActivity.this, "Warning...",
    		        			"裝置連線中...", true);
    		        	//---------連線THREAD
    		        	new Thread(){
    		        		@Override
    		        		public void run() {
    		    			// TODO Auto-generated method stub
    		        			try {
 			
    		        				device = mBtAdapter.getRemoteDevice(address);
    		            			if(mmSocket != null)
    		            			mmSocket.close();
    		    					mmSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
    		            			mmSocket.connect();
    		    		        	mmInStream = mmSocket.getInputStream();
    		    		        	mmOutStream = mmSocket.getOutputStream();
    		    		     		byte [] setFormatMsg = {0x02, 0x70, 0x02, 0x02, 0x08, 0x03};//傳輸格式
    		    		     		mmOutStream.write(setFormatMsg);
    		    		        		Log.i("try","  mmOutStream.write");
    		    		        	
    		    		        	hr1start=1;
    		    		            Runnable UIvisibility = new Runnable(){
    		    		        		public void run() {
    		    		    
    		    		                	pairedListView1.setVisibility(8);
    		    		                    HR1.setVisibility(0);
    		    		                    SPO1.setVisibility(0);
    		    		        			}//end of run
    		    		            };
    		    		            handler.post(UIvisibility);
    		        			} catch (Exception e) {
    		    				// TODO Auto-generated catch block
    		        			mBtAdapter.startDiscovery();
    		        			e.printStackTrace();
    		        			}finally {
    		        				Log.i("connectthreaD","connectthreaD");
    		        				myDialog.dismiss();
    		    				}
    		        			super.run();
    		    			}
    		        	}.start();
    		    	}
    		    }
            }
        }
    };
    
    
    private OnItemClickListener mDeviceClickListener2 = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {            
            mBtAdapter.cancelDiscovery();
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            if(!info.equals(getResources().getString(R.string.noDevice)))
            {
            	address = info.substring(info.length() - 17);
                device = mBtAdapter.getRemoteDevice(address);
    		    if(device.getBondState()==BluetoothDevice.BOND_NONE){  
                    Method createBondMethod;
					try {
						createBondMethod = BluetoothDevice.class.getMethod("createBond");
						createBondMethod.invoke(device);   
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    		    }   		    
    		    else{
    		    	if(BluetoothAdapter.checkBluetoothAddress(address)){
    		    	  myDialog = ProgressDialog.show(MainActivity.this, "Warning...",
    		        			"裝置2連線中...", true);
    		        	
    		        	new Thread(){
    		        		@Override
    		        		public void run() {
    		    			// TODO Auto-generated method stub
    		        			try {
    		        			
    		        				device = mBtAdapter.getRemoteDevice(address);
    		            			if(mmSocket2 != null)
    		            			mmSocket2.close();
    		    					mmSocket2 = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
    		            			mmSocket2.connect();
    		    		        	mmInStream2 = mmSocket2.getInputStream();
    		    		        	mmOutStream2 = mmSocket2.getOutputStream();
    		    		     		byte [] setFormatMsg = {0x02, 0x70, 0x02, 0x02, 0x08, 0x03};//傳輸格式
    		    		     		mmOutStream2.write(setFormatMsg);
    		    		        		Log.i("try","  mmOutStream2.write");
    		    		        	
    		    		        	hr2start=1;
    		    		            Runnable UIvisibility = new Runnable(){
    		    		        		public void run() {
    		    		    
    		    		                	pairedListView2.setVisibility(8);
    		    		                    HR2.setVisibility(0);
    		    		                    SPO2.setVisibility(0);
    		    		        			}//end of run
    		    		            };
    		    		            handler.post(UIvisibility);
    		    		        	
    		        			} catch (Exception e) {
    		    				// TODO Auto-generated catch block
    		        				mBtAdapter.startDiscovery();
    		        			e.printStackTrace();
    		        			}finally {
    		        				Log.i("connectthreaD","connectthreaD");
    		        				myDialog.dismiss();
    		    				}
    		        			super.run();
    		    			}
    		        	}.start();
    		    	}
    		    }
            }
        }
    };
    
    //------藍芽狀態處理
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                }
                else{
                	if(deviceflag==true){
                		mPairedDevicesArrayAdapter1.add(device.getName()+"\n"+device.getAddress());
                		deviceflag = false;
                	}
                	else{
                		mPairedDevicesArrayAdapter2.add(device.getName()+"\n"+device.getAddress());
                		deviceflag = true;
                	}
                }
                
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                
            } else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
        }
        }
    };
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    
    @Override
	protected void onPause() {
    	try {
    		        if (mmSocket!=null){
    		        	mmSocket.close();
    		        }
    		        if (mmSocket2!=null){
    		        	mmSocket2.close();
    		        }
    		        unregisterReceiver(mReceiver);
    		        mthread.interrupt();
    	}
    	catch(Exception e){
    	;
    	}
	super.onPause();
    }
}
