package com.hugo.wifips;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Context mContext = this;
    //wifi密码保存在此此文件中：/data/misc/wifi/wpa_supplicant.conf
    String cmd = "cat /data/misc/wifi/wpa_supplicant.conf";
    private RecyclerView recyclerView;
    private List<Network> networks;
    private MyAdaper adapter;
    private String currentSsid;
    private List<Network> havePswList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestRoot();
        initData();
        initView();
        initUI();
    }

    private void initData() {
        currentSsid = getCurrentWifiInfo();
        getWifiInfo();
    }

    private void initUI() {
        adapter = new MyAdaper();
        LinearLayoutManager manger = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(manger);
        recyclerView.setAdapter(adapter);
    }

    private void initView() {
        recyclerView = (RecyclerView) findViewById(R.id.recylerview);
    }


    private void requestRoot() {
        int i = execRootCmdSilent("echo test"); // 通过执行测试命令来检测
        if (i != -1) {
            //获取权限成功
        } else {
            Toast.makeText(mContext, "请授予root权限", Toast.LENGTH_LONG).show();
        }
    }

    // 执行linux命令,即申请root权限；
    protected static int execRootCmdSilent(String paramString) {
        try {
            Process localProcess = Runtime.getRuntime().exec("su");
            Object localObject = localProcess.getOutputStream();
            DataOutputStream localDataOutputStream = new DataOutputStream(
                    (OutputStream) localObject);
            String str = String.valueOf(paramString);
            localObject = str + "\n";
            localDataOutputStream.writeBytes((String) localObject);
            localDataOutputStream.flush();
            localDataOutputStream.writeBytes("exit\n");
            localDataOutputStream.flush();
            localProcess.waitFor();
            int result = localProcess.exitValue();
            return (Integer) result;
        } catch (Exception localException) {
            localException.printStackTrace();
            return -1;
        }
    }

    //
    public static DataInputStream execRootCmd(String paramString) {

        try {
            Process process = Runtime.getRuntime().exec("su");// 经过Root处理的android系统即有su命令
            OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(
                    outputStream);
            InputStream localInputStream = process.getInputStream();
            DataInputStream localDataInputStream = new DataInputStream(
                    localInputStream);
            String str = paramString + "\n";
            dataOutputStream.writeBytes(str);
            dataOutputStream.flush();
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            process.waitFor();
            return localDataInputStream;
        } catch (Exception localException) {
            localException.printStackTrace();
            return null;
        }
    }


    public void getWifiInfo() {
        StringBuffer sb = new StringBuffer();
        DataInputStream dis = execRootCmd(cmd);
        int len = 0;

        try {
            if (dis.read() == -1) {
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String temp = null;
        try {
            while ((temp = dis.readLine()) != null) {
                sb.append(temp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String psw = sb.toString();
        networks = Parser.getNetworks(psw);
        havePswList = new ArrayList<>();
        for (Network network : networks) {
            if (network.getPsk() != null) {
                havePswList.add(network);
            }
        }
    }

    class MyAdaper extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final static int NORMAL_ITEM = 0;
        private final static int CONNECT_ITEM = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == NORMAL_ITEM) {
                return new NormalVierHolder(LayoutInflater.from(mContext).inflate(R.layout.item_normal, parent, false));
            } else {
                return new ConnectViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_wifi_connect, parent, false));
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (havePswList.get(position).getSsid().equals(currentSsid)) {
                return CONNECT_ITEM;
            } else {
                return NORMAL_ITEM;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof NormalVierHolder) {
                Network network = havePswList.get(position);
                ((NormalVierHolder) holder).wifiName.setText(network.getSsid());
                String psk = network.getPsk();
                if (psk == null) {
                    ((NormalVierHolder) holder).wifiPsw.setText("无需密码");
                } else {
                    ((NormalVierHolder) holder).wifiPsw.setText(psk);
                }
            } else {
                Network network = havePswList.get(position);
                ((ConnectViewHolder) holder).wifiName.setText(network.getSsid());
                String psk = network.getPsk();
                if (psk == null) {
                    ((ConnectViewHolder) holder).wifiPsw.setText("无需密码");
                } else {
                    ((ConnectViewHolder) holder).wifiPsw.setText(psk);
                }
            }
        }


        @Override
        public int getItemCount() {
            return havePswList.size();
        }

        class ConnectViewHolder extends RecyclerView.ViewHolder {

            TextView wifiName, wifiPsw;

            public ConnectViewHolder(View itemView) {
                super(itemView);
                wifiName = (TextView) itemView.findViewById(R.id.wifi_name);
                wifiPsw = (TextView) itemView.findViewById(R.id.wifi_psw);
            }
        }

        class NormalVierHolder extends RecyclerView.ViewHolder {
            TextView wifiName, wifiPsw;

            public NormalVierHolder(View itemView) {
                super(itemView);
                wifiName = (TextView) itemView.findViewById(R.id.wifi_name);
                wifiPsw = (TextView) itemView.findViewById(R.id.wifi_psw);
            }
        }
    }


    public String getCurrentWifiInfo() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        String ssid = connectionInfo.getSSID();
        ssid = ssid.replace("\"", "").trim();
        return ssid;
    }
}
