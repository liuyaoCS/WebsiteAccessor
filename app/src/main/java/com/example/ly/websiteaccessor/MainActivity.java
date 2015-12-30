package com.example.ly.websiteaccessor;


import java.util.HashSet;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.menglvren.visit.NetConfig;
import com.menglvren.visit.model.IpListCache;
import com.menglvren.visit.model.Server;
import com.menglvren.visit.model.VipListCache;
import com.menglvren.visit.util.ProxySetting;
import com.umeng.update.UmengUpdateAgent;

public class MainActivity extends Activity {

	TextView show;
	LinearLayout webview_container;
	int WEB_NUM;

	private int count=0;
	private final int TASK_REFRESH=0;
	
	private final int TASK_UNIT=10*1000;

	private  Handler handler=null;

	boolean vipFlag=false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_net);

		UmengUpdateAgent.update(this);
		UmengUpdateAgent.setDeltaUpdate(false);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

		initHandler();
		initView();
		executeTask();
	}
	private void initHandler(){
		handler=new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				switch (msg.what) {

					case TASK_REFRESH:
						Bundle data=msg.getData();
						String loc=data.getString("url");
						String ip=data.getString("ip");
						int port=Integer.parseInt(data.getString("port"));

						CookieSyncManager.createInstance(MainActivity.this);
						CookieSyncManager.getInstance().startSync();
						CookieManager.getInstance().removeSessionCookie();
						CookieManager.getInstance().removeAllCookie();

						int child_num=webview_container.getChildCount();
						for(int i=0;i<child_num;i++){
							WebView web= (WebView) webview_container.getChildAt(i);
							configWebview(web, ip, port);
							web.loadUrl(loc);
						}
						show.setText("执行：" + msg.arg1* WEB_NUM+" / "+count* WEB_NUM);
						if(msg.arg1==count){
							show.setText("完成："+count* WEB_NUM);
						}
						break;
					default:
						break;
				}
				return true;
			}
		});
	}
	private void initView(){
		show=(TextView) findViewById(R.id.show);
		webview_container= (LinearLayout) findViewById(R.id.webview_container);
		int child_num=webview_container.getChildCount();
		WEB_NUM=child_num;
		for(int i=0;i<child_num;i++){
			WebView web= (WebView) webview_container.getChildAt(i);
			initWebview(web);
		}
	}
	private void executeTask(){
		for(Server server:NetConfig.validIps){
			for(String url: URLConfig.urls){
				Message msg=new Message();
				Bundle data=new Bundle();
				data.putString("url",url);
				data.putString("ip", server.ip);
				data.putString("port", server.port);
				msg.setData(data);
				msg.what=TASK_REFRESH;
				msg.arg1=count+1;
				handler.sendMessageDelayed(msg, count * TASK_UNIT);
				count++;
			}
		}
	}

	private void configWebview(WebView web,String ip,int port){
		if(Build.VERSION.SDK_INT== Build.VERSION_CODES.KITKAT){
			ProxySetting.setKitKatWebViewProxy(web.getContext().getApplicationContext(), ip, port);
		}else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN
				&& Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT){
			ProxySetting.setProxyICSPlus(web,ip,port,"");
		}else{
			Toast.makeText(MainActivity.this,"仅支持android版本4.1-4.4",Toast.LENGTH_LONG).show();
		}

		//
		web.clearCache(true);
		web.clearHistory();
		web.clearFormData();
	}

	private void initWebview(final WebView web){
		web.getSettings().setJavaScriptEnabled(true);
		web.getSettings().setUserAgentString(NetConfig.agent);
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
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
			}
		});
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		handler.removeCallbacksAndMessages(null);

		IpListCache ipListCache=new IpListCache(MainActivity.this);
		HashSet<Server> temp=new HashSet<>();
		temp.addAll(NetConfig.validIps);
		ipListCache.saveIpList(temp);

		if(vipFlag){
			VipListCache vipListCache=new VipListCache(MainActivity.this);
			HashSet<Server> temp1=new HashSet<>();
			temp1.addAll(NetConfig.validIps);
			vipListCache.saveVipList(temp1);
		}

		NetConfig.servers.clear();
		NetConfig.badIps.clear();
		NetConfig.validIps.clear();
		ProxySetting.cancelProxy();

		System.exit(0);
	}
}
