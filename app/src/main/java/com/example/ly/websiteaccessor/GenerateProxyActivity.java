package com.example.ly.websiteaccessor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.menglvren.visit.NetConfig;
import com.menglvren.visit.R;
import com.menglvren.visit.model.IpListCache;
import com.menglvren.visit.model.Server;
import com.menglvren.visit.model.VipListCache;
import com.menglvren.visit.test.TestUnit;
import com.menglvren.visit.util.DataCleanManager;
import com.menglvren.visit.util.ProxySetting;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 生成代理：1 默认 手动+上次缓存的有效ip(含vip) 2 vip 手动+上次缓存的vip
 * 检测代理：1 默认 访问相应网页 2 socket（建议）
 */
public class  GenerateProxyActivity extends Activity {
    Button generate,check,start,clearCache;
    TextView generate_text,check_text,log;
    RadioButton manual,vip;
    RadioButton socket,filter;

    private Handler handler;
    private int mStatus=-1;
    private static final int GENERATE_PROXY=0;
    private static final int CHECK_UPDATE=1;
    private static final int CHECK_FINISH=2;
    private final int MSG_ARG_UPDATE_VALID=0;
    private final int MSG_ARG_UPDATE_INVALID=1;

    private final int GET_PROXYS_TIME_OUT =3*1000;
    private final int CHECK_PROXYS_TIME_OUT =2*1000;
    //private final int VIP_VALID_MAX=100;

    private int generate_click_count=0;
    private boolean isLocalVipLoaded=false;
    private boolean isLocalIpLoaded=false;
    private boolean isInterrupted =false;
    private String currentSize;

    ExecutorService service= Executors.newFixedThreadPool(10);
    private AtomicInteger mCurrentCheckCount=new AtomicInteger(0);

    IpListCache ipListCache;
    HashSet<Server> ipList;
    VipListCache vipListCache;
    HashSet<Server> vipList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_proxy);

        initServer();
        initIpCache();
        initHandler();
        initView();
        test();
    }
    private void test(){
        //TestUnit.showHTMLPage(GenerateProxyActivity.this);
        //TestUnit.testContain();
    }

    private void initServer(){
        NetConfig.servers.clear();
        NetConfig.validIps.clear();
        NetConfig.badIps.clear();
        NetConfig.validIpIndex =0;
    }
    private void initIpCache(){
        ipListCache=new IpListCache(this);
        ipList =ipListCache.getIpList();
        if(ipList ==null){
            ipList =new HashSet<>();
        }

        vipListCache=new VipListCache(this);
        vipList=vipListCache.getVipList();
        if(vipList==null){
            vipList=new HashSet<>();
        }
    }
    private void initHandler(){
        handler=new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what){
                    case GENERATE_PROXY:
                        mStatus=GENERATE_PROXY;
                        generate_text.setText("代理数量："+NetConfig.servers.size());
                        check.setEnabled(true);
                        generate.setEnabled(!manual.isChecked());
                        break;
                    case CHECK_UPDATE:
                        mStatus=CHECK_UPDATE;
                        if(msg.arg1==MSG_ARG_UPDATE_VALID){
                            check_text.setText("有效代理：" + NetConfig.validIps.size());
                            if(NetConfig.validIps.size()>0){
                                start.setEnabled(true);
                            }
                            String htmlStr = "<font color=\"#ff0000\">"+msg.obj.toString()+"</font><br>";
                            log.append(Html.fromHtml(htmlStr));
                        }else{
                            log.append(msg.obj.toString());
                        }
                        break;
                    case CHECK_FINISH:
                        mStatus=CHECK_FINISH;
                        check_text.setText("有效代理："+NetConfig.validIps.size()+" done!");
                        launchMain();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }
    private void initView(){
        generate= (Button) findViewById(R.id.generate);
        check= (Button) findViewById(R.id.check);
        start= (Button) findViewById(R.id.start);
        generate_text= (TextView) findViewById(R.id.generate_text);
        check_text= (TextView) findViewById(R.id.check_text);
        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(manual.isChecked()){
                    generateManualProxys();
                }else if(vip.isChecked()){
                    generateVIPProxys();
                }else{
                    generateProxys(generate_click_count++);
                }
            }
        });
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                check.setEnabled(false);
                generate.setEnabled(false);
                if(socket.isChecked()){
                   checkProxysBySocket();
                }else{
                   checkProxys();
                }
            }
        });
        check.setEnabled(false);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchMain();
            }
        });
        start.setEnabled(false);

        manual = (RadioButton) findViewById(R.id.manual);
        vip= (RadioButton) findViewById(R.id.vip);
        vip.setEnabled(false);
        filter = (RadioButton) findViewById(R.id.check_filter);
        socket=(RadioButton) findViewById(R.id.check_socket);

        log= (TextView) findViewById(R.id.log);

        clearCache= (Button) findViewById(R.id.clear_cache);
        currentSize = DataCleanManager.getTotalCacheSize(GenerateProxyActivity.this);
        clearCache.setText("清理缓存 "+currentSize);
        clearCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(ipList !=null){
                    ipList.clear();
                }
                if(vipList!=null){
                    vipList.clear();
                }
                DataCleanManager.cleanInternalCache(GenerateProxyActivity.this);
                currentSize = DataCleanManager.getTotalCacheSize(GenerateProxyActivity.this);
                Toast.makeText(GenerateProxyActivity.this, "缓存已清除", Toast.LENGTH_SHORT).show();
                clearCache.setText("清理缓存 "+currentSize);
            }
        });
    }
    private void checkProxysByPing(){
        NetConfig.validIps.clear();
        NetConfig.badIps.clear();
        for (Server server : NetConfig.servers){
            service.submit(new PingTask(server));
        }
    }
    private void checkProxysBySocket(){
        NetConfig.validIps.clear();
        NetConfig.badIps.clear();
        for (Server server : NetConfig.servers){
            service.submit(new SocketTask(server));
        }
    }
    private void checkProxys(){
        NetConfig.validIps.clear();
        NetConfig.badIps.clear();
        new Thread() {
            @Override
            public void run() {
                for (Server server : NetConfig.servers) {
                    if (isInterrupted) {
                        break;
                    }
                    Message msg = new Message();
                    msg.what=CHECK_UPDATE;
                    if (isValidIP(server.ip, server.port)) {
                        Log.i("ly", "valid ip-->" + server.ip);
                        NetConfig.validIps.add(server);

                        msg.arg1=MSG_ARG_UPDATE_VALID;
                        msg.obj = "valid ip-->" + server.ip + "\n";
                    } else {
                        Log.i("ly", "bad ip-->" + server.ip);
                        NetConfig.badIps.add(server);

                        msg.arg1=MSG_ARG_UPDATE_INVALID;
                        msg.obj = "bad ip-->" + server.ip + "\n";
                    }
                    handler.sendMessage(msg);
                }
                if (!isInterrupted) handler.sendEmptyMessage(CHECK_FINISH);
            }
        }.start();
    }
    private void generateManualProxys(){
        /*测试vip花刺代理*/
        for(String line:NetConfig.manualProxys){
            int index=line.indexOf(":");
            String ip=line.substring(0, index);
            String port=line.substring(index+1);
            NetConfig.servers.add(new Server(ip, port));
        }

        handler.sendEmptyMessage(GENERATE_PROXY);
    }
    private  void generateVIPProxys(){
        if(!isLocalVipLoaded){
            Log.i("ly","load local vip proxy");
            isLocalVipLoaded=true;
            //本地（手动vip+上次有效的vip）
            for(String line:NetConfig.manualProxys){
                int index=line.indexOf(":");
                String ip=line.substring(0, index);
                String port=line.substring(index+1);
                vipList.add(new Server(ip, port));
            }

            NetConfig.servers.addAll(vipList);
            vipList.clear();

            handler.sendEmptyMessage(GENERATE_PROXY);
            return;
        }
        new Thread(){
            public void run() {
                HttpGet get=new HttpGet(NetConfig.VIPURL_GETPROXY);
                get.setHeader("Cache-Control", "no-cache");
                get.setHeader("User-Agent", NetConfig.agent);

                HttpParams params = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(params, GET_PROXYS_TIME_OUT); //设置连接超时
                HttpConnectionParams.setSoTimeout(params, GET_PROXYS_TIME_OUT); //设置请求超时
                get.setParams(params);

                HttpClient hClient = null;

                try {

                    hClient=new DefaultHttpClient();
                    HttpResponse hResponse=hClient.execute(get);
                    if(hResponse.getStatusLine().getStatusCode()==200){

                        BufferedReader br = new BufferedReader(new InputStreamReader(hResponse.getEntity().getContent(),"utf-8"));
                        StringBuilder sbBuilder=new StringBuilder();
                        String line = null;

                        while ((line = br.readLine()) != null){
                            sbBuilder.append(line + "\n");
                            if(line.contains(":")){
                                int index=line.indexOf(":");
                                String ip=line.substring(0, index);
                                String port=line.substring(index+1);
                                NetConfig.servers.add(new Server(ip, port));
                            }

                        }
                        TestUnit.savedVIP(GenerateProxyActivity.this,sbBuilder);
                        br.close();

                        handler.sendEmptyMessage(GENERATE_PROXY);
                    }else{
                        Log.e("ly", "err, get servers from local");
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            };
        }.start();
    }
    private void generateProxys(final int click_count){
        if(!isLocalIpLoaded){
            Log.i("ly","load local ip proxy");
            isLocalIpLoaded=true;
            //本地（手动vip+上次有效的ip(含vip)）
            for(String line:NetConfig.manualProxys){
                int index=line.indexOf(":");
                String ip=line.substring(0, index);
                String port=line.substring(index+1);
                ipList.add(new Server(ip, port));
            }

            NetConfig.servers.addAll(ipList);
            ipList.clear();

            handler.sendEmptyMessage(GENERATE_PROXY);
            return;
        }
        final int page=click_count;
        new Thread(){
            public void run() {

                HttpGet get=new HttpGet(NetConfig.URL_GETPROXY+page);
                get.setHeader("Cache-Control", "no-cache");
                get.setHeader("User-Agent", NetConfig.agent);

                HttpParams params = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(params, GET_PROXYS_TIME_OUT); //设置连接超时
                HttpConnectionParams.setSoTimeout(params, GET_PROXYS_TIME_OUT); //设置请求超时
                get.setParams(params);

                HttpClient hClient = null;

                try {

                    hClient=new DefaultHttpClient();
                    HttpResponse hResponse=hClient.execute(get);
                    if(hResponse.getStatusLine().getStatusCode()==200){

                        BufferedReader br = new BufferedReader(new InputStreamReader(hResponse.getEntity().getContent(),"utf-8"));
                        StringBuilder sbBuilder=new StringBuilder();
                        String line = null;

                        while ((line = br.readLine()) != null){
                            sbBuilder.append(line + "\n");
                        }
                        br.close();

                        Pattern p = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)[\\s\\S]+?(\\d+)");
                        Matcher m = p.matcher(sbBuilder);

                        while(m.find()) {

                            Log.i("ly", "ip:" + m.group(1));
                            Log.i("ly", "port:" + m.group(2));
                            NetConfig.servers.add(new Server(m.group(1), m.group(2)));
                        }
                        handler.sendEmptyMessage(GENERATE_PROXY);
                    }else{
                        Log.e("ly", "err, get servers from local");
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            };
        }.start();
    }
    private boolean isValidIPBySocket(final String ip,final String port){
        Boolean ret=false;
        Socket s=new Socket();
        try {
            s.setSoTimeout(CHECK_PROXYS_TIME_OUT);
            s.connect(new InetSocketAddress(ip, Integer.parseInt(port)), CHECK_PROXYS_TIME_OUT);
            ret=s.isConnected();
        } catch (IOException e) {
            Log.i("ly","connect error-->"+e.toString());
            e.printStackTrace();
        }finally {
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ret;
        }
    }
    private boolean isValidIP(final String ip,final String port){
        if(filter.isChecked()){
            if(ipList.contains(new Server(ip,port))){
                Log.i("ly","in white list");
                return true;
            }else{
                /*新ip，重新检测*/
            }
        }
        HttpGet get=new HttpGet(NetConfig.home);
        get.setHeader("User-Agent", NetConfig.agent);
        get.setHeader("Cache-Control", "no-cache");

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, GET_PROXYS_TIME_OUT); //设置连接超时
        HttpConnectionParams.setSoTimeout(params, GET_PROXYS_TIME_OUT); //设置请求超时
        get.setParams(params);

        System.setProperty("http.proxyHost", ip);
        System.setProperty("http.proxyPort", port);
        System.setProperty("https.proxyHost", ip);
        System.setProperty("https.proxyPort", port);

        HttpClient hClient = null;
        try {
            hClient=new DefaultHttpClient();
            HttpResponse  hResponse=hClient.execute(get);
            if(hResponse.getStatusLine().getStatusCode()==200){
                if(filter.isChecked()){
                    ipList.add(new Server(ip,port));
                    Log.i("ly", "add ip list");
                }
                return true;
            }else{

                return false;
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }
    private void launchMain(){
        if(NetConfig.validIps.size()==0){
            Toast.makeText(this,"未生成有效代理",Toast.LENGTH_SHORT).show();
            return;
        }
        clearAndSave();
        Intent it=new Intent(GenerateProxyActivity.this,MainActivity.class);
        it.putExtra("isVip",vip.isChecked());
        startActivity(it);

    }
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        clearAndSave();
    }
    private void clearAndSave(){
        handler.removeCallbacksAndMessages(null);
        isInterrupted =true;
        service.shutdownNow();

        ProxySetting.cancelProxy();

        if(mStatus==CHECK_FINISH){
            HashSet<Server> temp=new HashSet<>();
            temp.addAll(NetConfig.validIps);
            ipListCache.saveIpList(temp);
            temp.clear();
        }
        if (vip.isChecked() && mStatus==CHECK_FINISH){
            HashSet<Server> temp=new HashSet<>();
            temp.addAll(NetConfig.validIps);//每次仅保留有效的vip回写到本地
            vipListCache.saveVipList(temp);
            temp.clear();
        }

        finish();
        Log.i("ly", "finish");
    }
    class PingTask implements Runnable{
        Server mServer;
        PingTask(Server server){
           mServer=server;
        }
        @Override
        public void run() {
            Message msg = new Message();
            msg.what = CHECK_UPDATE;
            try
            {
                Process p = Runtime.getRuntime().exec(
                        "ping -c 1 -w "+ CHECK_PROXYS_TIME_OUT /1000+" " + mServer.ip);

                int status = p.waitFor();
                if (status == 0)
                {
                    Log.i("ly", "valid ip-->" + mServer.ip);
                    NetConfig.validIps.add(mServer);

                    msg.arg1=MSG_ARG_UPDATE_VALID;
                    msg.obj = "ping valid ip-->" + mServer.ip + "\n";

                } else
                {
                    Log.i("ly", "bad ip-->" + mServer.ip+" status:"+status);
                    NetConfig.badIps.add(mServer);

                    msg.arg1=MSG_ARG_UPDATE_INVALID;
                    msg.obj = "ping bad ip-->" + mServer.ip + "\n";
                }


            } catch (Exception e)
            {
                Log.i("ly", "bad ip-->" + mServer.ip);
                NetConfig.badIps.add(mServer);

                msg.arg1=MSG_ARG_UPDATE_INVALID;
                msg.obj = "exception bad ip-->" + mServer.ip + "\n";
            }finally {
                handler.sendMessage(msg);
            }
        }
    }
    class SocketTask implements Runnable{
        Server mServer;
        SocketTask(Server server){
            mServer=server;
        }
        @Override
        public void run() {
            Message msg = new Message();
            msg.what=CHECK_UPDATE;

            if (isValidIPBySocket(mServer.ip,mServer.port))
            {
                Log.i("ly", "valid ip-->" + mServer.ip);
                NetConfig.validIps.add(mServer);
                if(vip.isChecked()){
                    vipList.add(mServer);
                }
                msg.arg1=MSG_ARG_UPDATE_VALID;
                msg.obj = "socket valid ip-->" + mServer.ip + "\n";

            } else {
                Log.i("ly", "bad ip-->" + mServer.ip);
                NetConfig.badIps.add(mServer);

                msg.arg1=MSG_ARG_UPDATE_INVALID;
                msg.obj = "socket bad ip-->" + mServer.ip + "\n";
            }
            handler.sendMessage(msg);

            if (mCurrentCheckCount.incrementAndGet()==NetConfig.servers.size()){
               handler.sendEmptyMessage(CHECK_FINISH);
            }

        }
    }
}
