package com.example.ly.websiteaccessor;
import java.util.ArrayList;

public class NetConfig {	
	public static final String URL_GETPROXY="http://www.xicidaili.com/nn/";
	public static final String URL_PARSEIP="http://www.cmyip.com/";//"http://www.123cha.com/";
	
	public static final String[] urls=new String[]{
		"http://app.chinaso.com/chinaso-app/app/getById?id=0ea677ab40694f678627b212ea159dfb",
		"http://app.chinaso.com/chinaso-app/clustering/getApp?id=123&classPath=clustering/getApp",
		"http://app.chinaso.com/chinaso-app/app/app_index",
		"http://app.chinaso.com/chinaso-app/app/getByCategory?category=%E9%80%9A%E8%AE%AF%E7%A4%BE%E4%BA%A4",
		"http://app.chinaso.com/chinaso-app/game/get?id=1000226800000002833",
		"http://app.chinaso.com/chinaso-app/recommend/more?pageNow=1&category=3",
		"http://app.chinaso.com/chinaso-app/wp/more?id=0",
		"http://app.chinaso.com/chinaso-app/mobiletool/getapps",
		"http://app.chinaso.com/chinaso-app/clustering/gettopAPP?id=5b1a7d528ce945eeb5cf4de7f7ad4929&cluName=%E6%9E%81%E5%AE%A2%E8%8C%83%E5%84%BF&cluId=0&classPath=mobiletool/getapps",
		"http://app.chinaso.com/chinaso-app/specialTopic/itemAppList/afce3eebe22f4697b2425b5b1ac66ab4"

//		"http://www.menglvren123.icoc.cc/contact.jsp"
//		"http://www.menglvren123.icoc.cc/about.jsp"
	};
	public static ArrayList<Server> servers=new ArrayList<Server>();

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
