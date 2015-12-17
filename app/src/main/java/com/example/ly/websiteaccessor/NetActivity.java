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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.update.UmengUpdateAgent;

public class NetActivity extends Activity {
	Button visit,generate;
	TextView show,generate_text;
	LinearLayout webview_container;
	//WebView web,web2,web3;
	int WEB_NUM;

	//HttpClient hClient=new DefaultHttpClient();
	ExecutorService eService=Executors.newSingleThreadExecutor();
	
	List<Task> tasks=new ArrayList<Task>();	
	private int total=0;
	private int valid_ip_num =0;
	private int success_visit_num =0;
	private int generate_click_count=0;

	private final int TASK_REFRESH=0;
	private final int TASK_FINISH=1;
	private final int GENERATE_PROXY=2;
	private final int TASK_SHOW_VALIDIPNUM=3;
	
	private final int TASK_UNIT=10*1000;
	private final int TIME_OUT=5*1000;

	private  Handler handler=null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_net);

		UmengUpdateAgent.update(this);
		UmengUpdateAgent.setDeltaUpdate(false);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

		handler=new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
					case TASK_FINISH:
						show.setText("完成!\n成功次数 "+ success_visit_num);
//						total=0;
//						success_visit_num=0;
						//finish();
						//success_ping_num =0;
						break;
					case TASK_SHOW_VALIDIPNUM:
						generate_text.setText("代理数量："+NetConfig.servers.size() +"\n有效代理："+NetConfig.validIps.size());

					case TASK_REFRESH:
						show.setText("请求次数：" + total + "" +
								//" ping num= " + success_ping_num+"" +
								"\n成功次数："+success_visit_num+
								" (pv: "+success_visit_num*WEB_NUM+")");

						Bundle data=msg.getData();
						if(data==null || TextUtils.isEmpty(data.getString("url"))){
							break;
						}
						String loc=data.getString("url");
						String ip=data.getString("ip");
						int port=Integer.parseInt(data.getString("port"));

						CookieSyncManager.createInstance(NetActivity.this);
						CookieSyncManager.getInstance().startSync();
						CookieManager.getInstance().removeSessionCookie();
						CookieManager.getInstance().removeAllCookie();

						int child_num=webview_container.getChildCount();
						for(int i=0;i<child_num;i++){
							WebView web= (WebView) webview_container.getChildAt(i);
							configWebview(web, ip, port);
							web.loadUrl(loc);
						}
						break;
					case GENERATE_PROXY:
						generate_text.setText("手机版本："+Build.VERSION.SDK_INT+"\n代理数量："+NetConfig.servers.size());
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
				return true;
			}
		});

		show=(TextView) findViewById(R.id.show);
		generate_text=(TextView) findViewById(R.id.generate_text);
		
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
				show.setText("请求网络...\n成功次数：" + success_visit_num);
				new Thread(){
					@Override
					public void run() {
						int task_num=0;
						for (Task task : tasks) {
							if(NetConfig.badIps.contains(task.mIp)){
								Log.i("ly", "badIp="+task.mIp+" continue");
								continue;
							}else if(NetConfig.validIps.contains(task.mIp)){
								Log.i("ly", "validIp="+task.mIp+" direct submit");
								eService.submit(task);
								task_num++;
							}else{
								if(isValidIP(task.mUrl,task.mIp,task.mPort)){
									Log.i("ly", "check ip="+task.mIp+" valid,submit");
									eService.submit(task);
									task_num++;
									NetConfig.validIps.add(task.mIp);
								}else{
									Log.i("ly", "check ip="+task.mIp+" bad,record");
									NetConfig.badIps.add(task.mIp);
								}
							}
							Log.i("ly","task num="+task_num);

						}
						valid_ip_num=NetConfig.validIps.size();
						handler.sendEmptyMessageDelayed(TASK_SHOW_VALIDIPNUM, 0);

					}
				}.start();

				//eService.shutdown();
			}
		});

		webview_container= (LinearLayout) findViewById(R.id.webview_container);
		int child_num=webview_container.getChildCount();
		WEB_NUM=child_num;
		for(int i=0;i<child_num;i++){
			WebView web= (WebView) webview_container.getChildAt(i);
			initWebview(web,0);
		}

	}
	private void configWebview(WebView web,String ip,int port){
		if(Build.VERSION.SDK_INT== Build.VERSION_CODES.KITKAT){
			ProxySetting.setKitKatWebViewProxy(web.getContext().getApplicationContext(),ip,port);
		}else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN
				&& Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT){
			ProxySetting.setProxyICSPlus(web,ip,port,"");
		}else{
			Toast.makeText(NetActivity.this,"仅支持android版本4.1-4.4",Toast.LENGTH_LONG).show();
		}

		//
		web.clearCache(true);
		web.clearHistory();
		web.clearFormData();
	}

	private void initWebview(final WebView web,int agent_index){
		web.getSettings().setJavaScriptEnabled(true);
		web.getSettings().setUserAgentString(NetConfig.agents[agent_index]);
		web.getSettings().setAppCacheEnabled(false);
		web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		web.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				web.loadUrl(url);
				return true;
			}

			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

				super.onReceivedError(view, errorCode, description, failingUrl);
				Log.i("ly", description);
				//success_visit_num--;
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				//Log.i("ly", "finish");
				//success_visit_num++;
//				show.setText("请求次数：" + total + "" +
//						//" ping num= " + success_ping_num+"" +
//						"\nPV增值："+success_visit_num);

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
		NetConfig.badIps.clear();
		NetConfig.validIps.clear();
		System.setProperty("http.proxyHost", "");
		System.setProperty("http.proxyPort", "");
		eService.shutdownNow();

		super.onBackPressed();
	}
	private boolean isValidIP(String url,String ip,String port){
		HttpGet get=new HttpGet(url);
		get.setHeader("User-Agent", NetConfig.agents[0]);
		get.setHeader("Cache-Control", "no-cache");

		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, TIME_OUT); //设置连接超时
		HttpConnectionParams.setSoTimeout(params, TIME_OUT); //设置请求超时
		get.setParams(params);

		System.setProperty("http.proxyHost", ip);
		System.setProperty("http.proxyPort",port);
		//Log.i("ly", "set proxy ip="+ip);


		HttpClient hClient = null;
		try {
			hClient=new DefaultHttpClient();
			HttpResponse  hResponse=hClient.execute(get);
			if(hResponse.getStatusLine().getStatusCode()==200){
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

	private void visit(String url,String ip,String port){
		
		HttpGet get=new HttpGet(url);
		get.setHeader("User-Agent", NetConfig.agents[0]);
		get.setHeader("Cache-Control", "no-cache");

		HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIME_OUT); //设置连接超时
        HttpConnectionParams.setSoTimeout(params, TIME_OUT); //设置请求超时
		get.setParams(params);
		
        System.setProperty("http.proxyHost", ip);
		System.setProperty("http.proxyPort", port);
		Log.i("ly", "set proxy ip=" + ip);
		
		total++;
		Log.i("ly", "total="+total);

		int GOAL=valid_ip_num*NetConfig.urls.length;
		Log.i("ly", "GOAL="+valid_ip_num*NetConfig.urls.length+" ="+GOAL);
		if(GOAL>0 && total>=GOAL){
			handler.sendEmptyMessageDelayed(TASK_FINISH, TASK_UNIT);
		}

		HttpClient hClient = null;
		try {
			hClient=new DefaultHttpClient();
			HttpResponse  hResponse=hClient.execute(get);
			if(hResponse.getStatusLine().getStatusCode()==200){
				Log.i("ly", "success"/*,parse ip="+GetNetIp(NetConfig.URL_PARSEIP)*/);
				success_visit_num++;
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
	private void generateProxys(final int click_count){
		final int page=click_count;
		new Thread(){
			public void run() {
				HttpGet get=new HttpGet(NetConfig.URL_GETPROXY+page);
				//HttpPost post=new HttpPost(NetConfig.URL_GETPROXY+page);
				get.setHeader("User-Agent", NetConfig.agents[0]);
				get.setHeader("Cache-Control", "no-cache");


				HttpParams params = new BasicHttpParams(); 
		        HttpConnectionParams.setConnectionTimeout(params, TIME_OUT); //设置连接超时
		        HttpConnectionParams.setSoTimeout(params, TIME_OUT); //设置请求超时
		        get.setParams(params);

		        HttpClient hClient = null;

				String filename=FileUtil.getCachePath(NetActivity.this)+File.separator+"proxy-seeds.txt";
				File file=new File(filename);
				FileWriter fw=null;
				PrintWriter pw=null;
				try {

					hClient=new DefaultHttpClient();
					HttpResponse  hResponse=hClient.execute(get);
					if(hResponse.getStatusLine().getStatusCode()==200){

						if(click_count==1){
							file.delete();
							file = new File(FileUtil.getCachePath(NetActivity.this)+File.separator+"proxy-seeds.txt");
						}

						fw=new FileWriter(file,true);
						pw=new PrintWriter(fw);

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
						int count=0;
			       		while(m.find()) {
							if(NetConfig.debug){
								count++;
								if(count==6)break;
							}

							Log.i("ly", "ip:" + m.group(1));
							Log.i("ly", "port:" + m.group(2));
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
		public String mUrl;
		public String mIp;
		public String mPort;
		public Task(String url,String agent,String ip,String port){
			mUrl=url;
			mIp=ip;
			mPort=port;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			visit(mUrl,mIp,mPort);
		}
		
	}

}
