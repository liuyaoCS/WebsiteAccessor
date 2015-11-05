package com.example.ly.websiteaccessor;
import java.util.ArrayList;

public class NetConfig {
	public static  final boolean debug=false;
	public static final String URL_GETPROXY="http://www.xicidaili.com/nn/";
	public static final String URL_PARSEIP="http://www.cmyip.com/";//"http://www.123cha.com/";
	
	public static final String[] urls=new String[]{
		"http://app.chinaso.com/",
		"http://app.chinaso.com/chinaso-app/newAppArea/xrapp_index",
		"http://app.chinaso.com/chinaso-app/newAppArea/xrapp_index?appType=1",
		"http://app.chinaso.com/chinaso-app/newAppArea/xrapp_index?appType=2",
		"http://app.chinaso.com/chinaso-app/category/app_categorys",
		"http://app.chinaso.com/chinaso-app/specialTopic/zt",
		"http://app.chinaso.com/chinaso-app/specialTopic/zt?tag=1",
		"http://app.chinaso.com/chinaso-app/specialTopic/zt?tag=2",
		"http://app.chinaso.com/chinaso-app/topLists/phb_index",
		"http://app.chinaso.com/chinaso-app/app/getById?id=bcc4270f66274168ada31fa3712c99c6",
		"http://app.chinaso.com/chinaso-app/specialTopic/ztMore/f79ba5121e844264a738fd14014debd7",
		"http://app.chinaso.com/chinaso-app/clustering/gettopAPP?id=1000226800000003109&cluId=&classPath=",
		"http://app.chinaso.com/chinaso-app/core/searchv2?word=%E7%A7%92%E6%8B%8D",
		"http://app.chinaso.com/chinaso-app/app/category_list?category=%E9%80%9A%E8%AE%AF%E7%A4%BE%E4%BA%A4&t=1",
		"http://app.chinaso.com/chinaso-app/app/category_list?category=%E6%96%B0%E9%97%BB%E8%B5%84%E8%AE%AF&t=1",
		"http://app.chinaso.com/chinaso-app/app/category_list?category=%E7%94%9F%E6%B4%BB%E4%BC%91%E9%97%B2&t=1",
		"http://app.chinaso.com/chinaso-app/app/category_list?category=%E6%95%99%E8%82%B2%E5%AD%A6%E4%B9%A0&t=1",
		"http://app.chinaso.com/chinaso-app/app/category_list?category=%E7%81%AB%E7%88%86%E8%AF%95%E5%90%AC&t=1",
		"http://app.chinaso.com/chinaso-app/app/category_list?category=%E5%8A%A8%E4%BD%9C%E5%B0%84%E5%87%BB&t=2",
		"http://app.chinaso.com/chinaso-app/app/category_list?category=%E8%B7%91%E9%85%B7%E7%AB%9E%E9%80%9F&t=2",


//		"http://www.menglvren123.icoc.cc/contact.jsp"
//		"http://www.menglvren123.icoc.cc/about.jsp"
	};
	public static ArrayList<Server> servers=new ArrayList<Server>();
	public static ArrayList<String> badIps=new ArrayList<String>();
	public static ArrayList<String> validIps=new ArrayList<String>();

	public static String[] agents=new String[]{
		"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.84 Safari/535.11 LBBROWSER",
		"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_8; en-us) AppleWebKit/534.50 (KHTML, like Gecko) Version/5.1 Safari/534.50",
		"Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0",
		"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:2.0.1) Gecko/20100101 Firefox/4.0.1",
		"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_0) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11"
	};
}
class Server{
	public String ip;
	public String port;
	public Server(String i,String p){
		ip=i;
		port=p;
	}
}
