package com.example.ly.websiteaccessor;


import java.io.BufferedReader;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;


import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.update.UmengUpdateAgent;

public class NetActivity extends Activity {
	Button visit,generate;
	TextView show;
	WebView web;
	//HttpClient hClient=new DefaultHttpClient();
	ExecutorService eService=Executors.newSingleThreadExecutor();
	
	List<Task> tasks=new ArrayList<Task>();	
	private int total=0;
	private int success_num=0;
	private int generate_click_count=0;

	private final int TASK_REFRESH=0;
	private final int TASK_FINISH=1;
	private final int GENERATE_PROXY=2;
	
	private final int TASK_UNIT=10*1000;
	private final int TIME_OUT=5*1000;
	
	
	private  Handler handler=new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case TASK_FINISH:
				show.setText("done!\nyou have successfully visited "+success_num+" times");
				total=0;
				success_num=0;
				break;
			case TASK_REFRESH:
				show.setText("connection counts:" + total + "\nyou have successfully visited " + success_num + " times");

				Bundle data=msg.getData();
				if(data==null || TextUtils.isEmpty(data.getString("url"))){
					break;
				}
				String loc=data.getString("url");
				String ip=data.getString("ip");
				int port=Integer.parseInt(data.getString("port"));

				if(Build.VERSION.SDK_INT== Build.VERSION_CODES.KITKAT){
					ProxySetting.setKitKatWebViewProxy(web.getContext().getApplicationContext(),ip,port);
				}else if(Build.VERSION.SDK_INT==Build.VERSION_CODES.JELLY_BEAN){
					ProxySetting.setProxyICSPlus(web,ip,port,"");
				}else{
					Toast.makeText(NetActivity.this,"仅支持android版本4.1-4.4",Toast.LENGTH_LONG).show();
				}

				//
				web.clearCache(true);
				web.clearHistory();
				web.clearFormData();

				CookieSyncManager.createInstance(NetActivity.this);
				CookieSyncManager.getInstance().startSync();
				CookieManager.getInstance().removeSessionCookie();
				CookieManager.getInstance().removeAllCookie();

				web.loadUrl(loc);
				break;
			case GENERATE_PROXY:
				generate.setText("get "+NetConfig.servers.size()+" servers"+"("+Build.VERSION.SDK_INT+")");
				for(Server server:NetConfig.servers){
					Random agents=new Random();
					int agent_index =agents.nextInt(NetConfig.agents.length);
					for(String url:NetConfig.urls){
						final Task task=new Task(url,NetConfig.agents[agent_index],server.ip,server.port);
						tasks.add(task);
					}


				}
				break;
			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_net);

		UmengUpdateAgent.update(this);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

		show=(TextView) findViewById(R.id.show);
		
		generate=(Button)findViewById(R.id.generate);
		generate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				generate_click_count++;
				generateProxys(generate_click_count);
				
			}
		});
		visit =(Button)findViewById(R.id.bt);
		visit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				show.setText("visiting...\nyou have successfully visited " + success_num + " times");
				for (Task task : tasks) {
					eService.submit(task);
				}
				//eService.shutdown();
			}
		});
		web= (WebView) findViewById(R.id.web);
		web.getSettings().setJavaScriptEnabled(true);
		web.getSettings().setUserAgentString(NetConfig.agents[0]);
		web.getSettings().setAppCacheEnabled(false);
		web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		web.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				web.loadUrl(url);
				return true;
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onBackPressed() {
		NetConfig.servers.clear();
		System.setProperty("http.proxyHost", "");
		System.setProperty("http.proxyPort", "");
		eService.shutdownNow();

		super.onBackPressed();
	}

	private void visit(String url,String agent,String ip,String port){
		
		HttpGet get=new HttpGet(url);
//		get.setHeader("User-Agent", agent);
//		get.setHeader("Cache-Control", "no-cache");
//		get.setHeader("Connection", "close");
		
		HttpParams params = new BasicHttpParams(); 
        HttpConnectionParams.setConnectionTimeout(params, TIME_OUT); //设置连接超时
        HttpConnectionParams.setSoTimeout(params, TIME_OUT); //设置请求超时
        get.setParams(params);
		
        System.setProperty("http.proxyHost", ip);
		System.setProperty("http.proxyPort",port);
		Log.i("ly", "set proxy ip="+ip);
		
		total++;
		int num=NetConfig.servers.size()*NetConfig.urls.length;
		if(num==total){
			handler.sendEmptyMessageDelayed(TASK_FINISH, TASK_UNIT);
		}
		HttpClient hClient = null;
		try {
			hClient=new DefaultHttpClient();
			HttpResponse  hResponse=hClient.execute(get);
			if(hResponse.getStatusLine().getStatusCode()==200){				
				Log.i("ly", "success"/*,parse ip="+GetNetIp(NetConfig.URL_PARSEIP)*/);
				success_num++;
				Message msg=new Message();
				Bundle data=new Bundle();
				data.putString("ip",ip);
				data.putString("port", port);
				data.putString("url",url);
				msg.setData(data);
				msg.what=TASK_REFRESH;
				handler.sendMessage(msg);
				//handler.sendEmptyMessage(TASK_REFRESH);
			}else{
				Message msg=new Message();
				msg.what=TASK_REFRESH;
				handler.sendMessage(msg);
				Log.e("ly", "response err");
			}
			Thread.sleep(TASK_UNIT);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Message msg=new Message();
			msg.what=TASK_REFRESH;
			handler.sendMessage(msg);
			Log.e("ly", "response err");
		}finally{
			//hClient.getConnectionManager().shutdown();
		}
	}
	private void generateProxys(int click_count){
		final int page=click_count;
		new Thread(){
			public void run() {
				HttpGet get=new HttpGet(NetConfig.URL_GETPROXY+page);
				
				HttpParams params = new BasicHttpParams(); 
		        HttpConnectionParams.setConnectionTimeout(params, TIME_OUT); //设置连接超时
		        HttpConnectionParams.setSoTimeout(params, TIME_OUT); //设置请求超时
		        get.setParams(params);

		        HttpClient hClient = null;
				File directory = Environment.getExternalStorageDirectory();
				File file = new File(directory, "proxy-seeds.txt");
				FileWriter fw=null;
				PrintWriter pw=null;
				try {
					fw=new FileWriter(file,true);
					pw=new PrintWriter(fw);
					hClient=new DefaultHttpClient();
					HttpResponse  hResponse=hClient.execute(get);
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
		                //NetConfig.servers.clear();
			       		while(m.find()) {  
				       		Log.i("ly","ip:"+m.group(1));  
				       		Log.i("ly","port:"+m.group(2));  
				       		NetConfig.servers.add(new Server(m.group(1), m.group(2)));
							pw.write(m.group(1) + ":" + m.group(2) + " ");
			       		}
						pw.flush();
						pw.close();
						handler.sendEmptyMessage(GENERATE_PROXY);
					 }else{
						Log.e("ly", "err, get servers from local");
						//Toast.makeText(NetActivity.this,"server error,read local cache servers",Toast.LENGTH_SHORT).show();
						File fn = new File(Environment.getExternalStorageDirectory().getPath()+File.separator+"proxy-seeds.txt");
						Scanner sca=new Scanner(fn,"utf-8");
						StringBuffer sb=new StringBuffer();
						while(sca.hasNextLine()){
							sb.append(sca.nextLine());
						}
						String[] servers=sb.toString().split(" ");
						for(String server:servers){
							String[] pair=server.split(":");
							NetConfig.servers.add(new Server(pair[0], pair[1]));
						}
						sca.close();
						handler.sendEmptyMessage(GENERATE_PROXY);

					 }
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			};
		}.start();
		
	}
	private String GetNetIp(String url){
		HttpGet get=new HttpGet(url);
		get.setHeader("Cache-Control", "no-cache");
		get.setHeader("Connection", "close");
		
		HttpParams params = new BasicHttpParams(); 
        HttpConnectionParams.setConnectionTimeout(params, TIME_OUT); //设置连接超时
        HttpConnectionParams.setSoTimeout(params, TIME_OUT); //设置请求超时
        get.setParams(params);
		
        String ret="";
        HttpClient hClient = null;
		try {
			hClient=new DefaultHttpClient();
			HttpResponse  hResponse=hClient.execute(get);
			if(hResponse.getStatusLine().getStatusCode()==200){				
				 
               BufferedReader br = new BufferedReader(new InputStreamReader(hResponse.getEntity().getContent(),"utf-8"));
               StringBuilder sbBuilder=new StringBuilder();
               String line = null;
               
               while ((line = br.readLine()) != null){ 
            	   sbBuilder.append(line + "\n");
            	   if(line.contains("My IP Address is")){
            		   ret=line;
            	   }
               }
               br.close();               
               //Log.i("ly", "set proxy ip="+System.getProperty("http.proxyHost")+",ip resolve info"+ret);
				
			}else{
				Log.e("ly", "err");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
    } 
	
	class Task implements Runnable{
		String mUrl;
		String mAgent;
		String mIp;
		String mPort;
		public Task(String url,String agent,String ip,String port){
			mUrl=url;
			mAgent=agent;
			mIp=ip;
			mPort=port;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			visit(mUrl,mAgent,mIp,mPort);
		}
		
	}

}
