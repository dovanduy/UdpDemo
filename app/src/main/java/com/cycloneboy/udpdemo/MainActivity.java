package com.cycloneboy.udpdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private  static  String Tag = "UDP";

    TextView tvIpInfo,txt_Info;
    Button  btn_Send,btn_UdpConn,btn_UdpClose;
    Button  btnForward,btnTurnLeft,btnTurnRight,btnStop,btnBackward;
    EditText edit_Send,editTextIP,editTextPort;
    private UDPClient client = null;
    public static Context context;
    private final MyHandler myHandler = new MyHandler(this);
    private StringBuffer udpRcvStrBuf=new StringBuffer(),udpSendStrBuf=new StringBuffer();

    private  byte[] sendCmdBuf = new byte[1024];
    MyBtnClick myBtnClick = new MyBtnClick();

    // 添加获取GPS
    private TextView postionView;
    private LocationManager locationManager;
    private String locationProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("sys init " + getLocalIp() + " : " + client.CLIENT_PORT);

        tvIpInfo = (TextView)findViewById(R.id.tvIpInfo);
        tvIpInfo.append(getLocalIp() + " : " + client.CLIENT_PORT);


        context = this;
        bindWidget();       //控件绑定
        bindListening();    //监听事件
        bindReceiver();     //注册broadcastReceiver接收器
        iniWidget();        //初始化控件状态
        initLocation();     //初始化定位GPS相关
    }


    private void bindWidget(){
        txt_Info = (TextView)findViewById(R.id.txt_Info);
        btn_Send = (Button)findViewById(R.id.btn_Send);
        btn_UdpConn = (Button)findViewById(R.id.btn_udpConn);
        btn_UdpClose = (Button)findViewById(R.id.btn_udpClose);

        edit_Send = (EditText)findViewById(R.id.edit_Send);
        editTextPort = (EditText)findViewById(R.id.editTextPort);
        editTextIP = (EditText)findViewById(R.id.editTextIP);

        btnForward = (Button)findViewById(R.id.btnForward);
        btnTurnLeft = (Button)findViewById(R.id.btnTurnLeft);
        btnTurnRight= (Button)findViewById(R.id.btnTurnRight);
        btnStop= (Button)findViewById(R.id.btnStop);
        btnBackward= (Button)findViewById(R.id.btnBackward);

    }

    private void bindListening(){
        btn_Send.setOnClickListener(myBtnClick);
        btn_UdpConn.setOnClickListener(myBtnClick);
        btn_UdpClose.setOnClickListener(myBtnClick);

        btnForward.setOnClickListener(myBtnClick);
        btnTurnLeft.setOnClickListener(myBtnClick);
        btnTurnRight.setOnClickListener(myBtnClick);
        btnStop.setOnClickListener(myBtnClick);
        btnBackward.setOnClickListener(myBtnClick);

    }

    private void bindReceiver(){
        IntentFilter udpRcvIntentFilter = new IntentFilter("udpRcvMsg");
        registerReceiver(broadcastReceiver,udpRcvIntentFilter);
    }

    private void iniWidget(){
        btn_Send.setEnabled(false);
    }

    private void initLocation(){
        //获取显示地理位置信息的TextView
        postionView = (TextView) findViewById(R.id.positionView);
        //获取地理位置管理器
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //获取所有可用的位置提供器
        List<String> providers = locationManager.getProviders(true);
        for (String string : providers) {
            System.out.println("所有定位方式：>>>"+string);
        }
        if(providers.contains(LocationManager.GPS_PROVIDER)){
            //如果是GPS
            locationProvider = LocationManager.GPS_PROVIDER;
        }else if(providers.contains(LocationManager.NETWORK_PROVIDER)){
            //如果是Network
            locationProvider = LocationManager.NETWORK_PROVIDER;
        }else{
            Toast.makeText(this, "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
            return ;
        }
        //获取Location
        Location location = locationManager.getLastKnownLocation(locationProvider);
        if(location!=null){
            //不为空,显示地理位置经纬度
            showLocation(location);
        }
        //监视地理位置变化
        locationManager.requestLocationUpdates(locationProvider, 3000, 1, locationListener);
    }

    /**
     * 显示地理位置经度和纬度信息
     * @param location
     */
    private void showLocation(Location location){
        //获取位置变化结果
        float accuracy = location.getAccuracy();//精确度，以密为单位
        double altitude = location.getAltitude();//获取海拔高度
        double longitude = location.getLongitude();//经度
        double latitude = location.getLatitude();//纬度
        float speed = location.getSpeed();//速度

        //显示位置信息
        StringBuffer sb= new StringBuffer();
        sb.append("accuracy:"+accuracy+" ");
        sb.append("altitude:"+altitude+"");
        sb.append("longitude:"+longitude+" ");
        sb.append("latitude:"+latitude+" ");
        sb.append("speed:"+speed+" ");


        String locationStr = "维度：" + location.getLatitude() +"\n"
                + "经度：" + location.getLongitude();
        postionView.setText(locationStr);
        System.out.println(locationStr + sb.toString());
    }

    /**
     * LocationListern监听器
     * 参数：地理位置提供器、监听位置变化的时间间隔、位置变化的距离间隔、LocationListener监听器
     */
    LocationListener locationListener =  new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle arg2) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        //位置改变的时候调用，这个方法用于返回一些位置信息
        @Override
        public void onLocationChanged(Location location) {
            //如果位置发生变化,重新显示
            showLocation(location);

        }
    };

    public class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;
        String strTemp = null;
        public MyHandler(MainActivity activity){
            mActivity = new WeakReference<MainActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case ConstParam.MSG_TYPE_SHOW_RCV:
                    udpRcvStrBuf.append(msg.obj.toString());
                    strTemp = "接收到：" + msg.obj.toString();
                    txt_Info.setText(strTemp);
                    break;
                case ConstParam.MSG_TYPE_SEND:
                    udpSendStrBuf.append(msg.obj.toString());
                    strTemp = "发送：" + msg.obj.toString();
                    txt_Info.setText(strTemp);
                    //txt_Info.setText(udpSendStrBuf.toString());
                    break;
                case 3:
                    strTemp = "接收到：" + msg.obj.toString();
                    txt_Info.setText(strTemp);
                    //txt_Info.setText(udpRcvStrBuf.toString());
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            //移除监听器
            locationManager.removeUpdates(locationListener);
        }
    }

        // 获取本机IP
    private String getLocalIp() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        Log.i(Tag, "int ip "+ipAddress);

        if (ipAddress == 0) return null;
        return ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "."
                + (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff));
    }


    // 设置广播接收器
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("udpRcvMsg"))  {
                Message message = new Message();
                message.obj = intent.getStringExtra("udpRcvMsg");
                message.what = ConstParam.MSG_TYPE_SHOW_RCV;
                Log.i("主界面Broadcast","收到"+message.obj.toString());
                myHandler.sendMessage(message);
            }
        }
    };

    public void DisplayToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }



    // 按键消息处理函数
    class MyBtnClick implements Button.OnClickListener{

        @Override
        public void onClick(View v) {
            Thread thread = null;
            switch (v.getId()) {
//            case R.id.btn_CleanRecv:
//                udpRcvStrBuf.delete(0,udpRcvStrBuf.length());
//                Message message = new Message();
//                message.what = 3;
//                myHandler.sendMessage(message);
//                break;
                case R.id.btn_udpConn:
                    System.out.println(" btn_udpConn click once");
                    //建立线程池
                    ExecutorService exec = Executors.newCachedThreadPool();

                    String desIp = editTextIP.getText().toString();
                    int desPort = Integer.valueOf(editTextPort.getText().toString());
                    System.out.println( " des ip and port " + desIp + " : " + desPort);

                    client = new UDPClient(desIp,desPort);
                    exec.execute(client);
                    btn_UdpClose.setEnabled(true);
                    btn_UdpConn.setEnabled(false);
                    btn_Send.setEnabled(true);
                    editTextIP.setEnabled(false);
                    editTextPort.setEnabled(false);
                    break;
                case R.id.btn_udpClose:
                    System.out.println(" btn_udpClose click once");
                    client.setUdpLife(false);
                    btn_UdpConn.setEnabled(true);
                    btn_UdpClose.setEnabled(false);
                    btn_Send.setEnabled(false);
                    editTextIP.setEnabled(true);
                    editTextPort.setEnabled(true);
                    break;
                case R.id.btn_Send:
                    System.out.println(" btn_Send click once");
                    thread= new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Message message = new Message();
                            message.what = ConstParam.MSG_TYPE_SEND;
                            if (edit_Send.getText().toString()!=""){
                                client.send(edit_Send.getText().toString());
                                message.obj = edit_Send.getText().toString();
                                myHandler.sendMessage(message);
                            }

                        }
                    });
                    thread.start();
                    break;
                case R.id.btnForward:
                    System.out.println(" btnForward click once");
                    sendCmdBuf[0] = 1;
                    thread = sendCmd(ConstParam.SEND_CMD_TURN_FORWARD,sendCmdBuf,1);
//                    thread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Message message = new Message();
//                            message.what = ConstParam.MSG_TYPE_SEND;
//                            int cmdLength = 7;
//                            byte[] cmd = new byte[cmdLength];
//                            cmd[0] = (byte) 0xa5;  // 报头
//                            cmd[1] = (byte) 0xa5;
//                            cmd[2] = (byte) 0x01;  // 报文ID
//                            cmd[3] = (byte) 0x01;  // 报文内容长度 n （总的报文长度 n + 6)
//                            cmd[4] = (byte) 0x01;  // 报文内容
//                            cmd[5] = (byte) 0xc8;  // 报尾
//                            cmd[6] = (byte) 0xc8;
//
//                            client.send(cmd,cmdLength);
//                            message.obj = new String(cmd);
//                            myHandler.sendMessage(message);
//
//                        }
//                    });
                    thread.start();
                    break;
                case R.id.btnTurnLeft:{
                    System.out.println(" btnTurnLeft click once");
                    sendCmdBuf[0] = 1;
                    thread = sendCmd(ConstParam.SEND_CMD_TURN_LEFT,sendCmdBuf,1);
                    thread.start();
                    break;
                }
                case R.id.btnTurnRight:{
                    System.out.println(" btnTurnRight click once");
                    sendCmdBuf[0] = 1;
                    thread = sendCmd(ConstParam.SEND_CMD_TURN_RIGHT,sendCmdBuf,1);
                    thread.start();
                    break;
                }
                case R.id.btnStop:{
                    System.out.println(" btnStop click once");
                    sendCmdBuf[0] = 1;
                    thread = sendCmd(ConstParam.SEND_CMD_TURN_STOP,sendCmdBuf,1);
                    thread.start();
                    break;
                }
                case R.id.btnBackward:{
                    System.out.println(" btnBackward click once");
                    sendCmdBuf[0] = 1;
                    thread = sendCmd(ConstParam.SEND_CMD_TURN_BACKWARD,sendCmdBuf,1);
                    thread.start();
                    break;
                }
                default:
                    break;
            }
        }
    }


     // 发送控制命令函数
    public Thread sendCmd(final byte cmdId, final byte[] cmdContent,final int cmdContentLength){

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = ConstParam.MSG_TYPE_SEND;
                byte[] cmd = new byte[cmdContentLength +6];
                cmd[0] = (byte) 0xa5;  // 报头
                cmd[1] = (byte) 0xa5;
                cmd[2] =  cmdId;  // 报文ID
                cmd[3] = (byte) cmdContentLength;  // 报文内容长度 n （总的报文长度 n + 6)
                for (int i = 0; i < cmd[3]; i++) {
                    cmd[i+4] =cmdContent[i];
                }
                //cmd[4+ cmdContentLength] = (byte) 0x01;  // 报文内容
                cmd[4+ cmdContentLength] = (byte) 0xc8;  // 报尾
                cmd[5+ cmdContentLength] = (byte) 0xc8;

                client.send(cmd,cmd[3] + 6);
                message.obj = new String(" 发送控制命令：" + cmdId);
                myHandler.sendMessage(message);

            }
        });
        return thread;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }


}