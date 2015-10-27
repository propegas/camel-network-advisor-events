package ru.atc.camel.networkadvisor.events;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
//import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
//import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.apc.stdws.xsd.isxcentral._2009._10.ISXCAlarm;
//import com.apc.stdws.xsd.isxcentral._2009._10.ISXCDevice;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ru.at_consulting.itsm.event.Event;
//import ru.atc.camel.networkadvisor.events.api.Feed2;
import ru.atc.camel.networkadvisor.events.api.RESTNetworkAdvisorEvents;

public class RESTNetworkAdvisorConsumer extends ScheduledPollConsumer {
	
	private static Logger logger = LoggerFactory.getLogger(Main.class);
	
	private RESTNetworkAdvisorEndpoint endpoint;

	public RESTNetworkAdvisorConsumer(RESTNetworkAdvisorEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        //this.afterPoll();
        this.setDelay(endpoint.getConfiguration().getDelay());
	}
	
	@Override
	protected int poll() throws Exception {
		
		String operationPath = endpoint.getOperationPath();
		
		if (operationPath.equals("events")) return processSearchEvents();
		
		// only one operation implemented for now !
		throw new IllegalArgumentException("Incorrect operation: " + operationPath);
	}
	
	private CloseableHttpClient HTTPinit(){
		
		SSLContext sslContext = null;
		//HttpClient client = HttpClientBuilder.create().build();	
		HttpClientBuilder cb = HttpClientBuilder.create();
		SSLContextBuilder sslcb = new SSLContextBuilder();
	    try {
			sslcb.loadTrustMaterial(KeyStore.getInstance(KeyStore.getDefaultType()), new TrustSelfSignedStrategy());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    try {
			cb.setSslcontext(sslcb.build());
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    try {
			sslContext = sslcb.build();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		@SuppressWarnings("deprecation")
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	    CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
	    return httpclient;
	}
	
	private String getWStoken( CloseableHttpClient httpclient ){
		
		String restapiurl = endpoint.getConfiguration().getRestapiurl();
		String WSusername = endpoint.getConfiguration().getWsusername();
		String WSpassword = endpoint.getConfiguration().getWspassword();
		
		logger.info("***************** restapiurl: " + restapiurl);
		logger.info("***************** WSusername: " + WSusername);
		logger.info("***************** WSpassword: " + WSpassword);
		
		HttpPost request = new HttpPost(restapiurl + "login");
		request.addHeader("Accept", "application/vnd.brocade.networkadvisor+json;version=v1");
		request.addHeader("WSusername", WSusername);
		request.addHeader("WSpassword", WSpassword);
		HttpResponse response = null;
		try {
			response = httpclient.execute(request);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		Header head = response.getFirstHeader("WStoken");
		String WStoken = head.getValue();
		System.out.println("***************** WStoken: " + WStoken);
	
		if (response.getStatusLine().getStatusCode() != 200) {
			//throw new RuntimeException("Feedly API error with return code: " + response.getStatusLine().getStatusCode());
			System.out.println("Network Advisor RST API error with return code: " + response.getStatusLine().getStatusCode());
			return null;
		}
		
		return WStoken;
		
	}

	private JsonObject performGetRequest(String uri) throws ClientProtocolException, IOException {
		
		String restapiurl = endpoint.getConfiguration().getRestapiurl();
		
		System.out.println("*****************Lastid: " + endpoint.getConfiguration().getLastid());
		//endpoint.getConfiguration().setLastid(70955);
		
		CloseableHttpClient httpclient = HTTPinit();
		
		//httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
	    
		String WStoken = getWStoken(httpclient);
		if (WStoken == null)
			throw new RuntimeException("Failed while WSToken retrieving.");
				
		//uri = "resourcegroups/All/events?startindex=0&count=10&specialEvent=true&origin=trap";
		HttpGet request2 = new HttpGet(restapiurl + uri);
		request2.addHeader("Accept", "application/vnd.brocade.networkadvisor+json;version=v1");
		request2.addHeader("WStoken", WStoken);
		HttpResponse response2 = httpclient.execute(request2);
		
		JsonParser parser = new JsonParser();
		InputStreamReader sr = new InputStreamReader(response2.getEntity().getContent(), "UTF-8");
		BufferedReader br = new BufferedReader(sr);
		JsonObject json = (JsonObject) parser.parse(br);
		br.close();
		sr.close();
		
		return json;
	}

	private int processSearchEvents() throws Exception {
		
		//Long timestamp;
		
		String eventsuri = endpoint.getConfiguration().getEventsuri();
		
		System.out.println("***************** eventsuri: " + eventsuri);
		
		int startindex = endpoint.getConfiguration().getStartindex();
		int count = endpoint.getConfiguration().getCount();
		String specialEvent = endpoint.getConfiguration().getSpecialEvent();
		String uri = String.format("%sevents?startindex=%d&count=%d&specialEvent=%s", 
									eventsuri,startindex,count,specialEvent);
		
		System.out.println("***************** URL: " + uri);
		
		JsonObject json = performGetRequest(uri);
		
		System.out.println("*****************  JSON: " + json);
		
		JsonArray events = (JsonArray) json.get("events");
		//JsonElement serverName = json.get("serverName");
		//List<RESTNetworkAdvisorEvents> eventList = new ArrayList<RESTNetworkAdvisorEvents>();
		List<Event> eventList = new ArrayList<Event>();
		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		
		int EventId = 0;
		int lastEventId = 0;
		int i = 0;
		
		System.out.println("*****************lastEventId: " + lastEventId);
		int storedLastId = endpoint.getConfiguration().getLastid();
		System.out.println("*****************storedLastId: " + storedLastId);
		
		for (JsonElement f : events) {
			i++;
			//logger.debug(gson.toJson(i));
			
			
			RESTNetworkAdvisorEvents event = gson.fromJson(f, RESTNetworkAdvisorEvents.class);
			Event genevent = new Event();
			
			EventId = getEventId(event.getKey());
			if ( i == 1 )
				lastEventId = EventId;
			
			System.out.println("*****************EventId: " + EventId);
			if (EventId != -1){
				if (EventId > storedLastId){
					
					genevent = genEventObj( event );
					eventList.add(genevent);
				}
				else {
					break;
				}
			}
			
					
		}	
		
		Collections.reverse(eventList);
			
		endpoint.getConfiguration().setLastid(lastEventId) ;
		
				
		logger.info("Create Exchange container");
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(eventList, ArrayList.class);
        //exchange.getIn().setHe
        System.out.println("There is an exchange going on.");
        //System.out.println(exchange.getIn().getHeader("CamelFileName"));
        System.out.println(exchange.getIn().getBody());
        System.out.println(exchange.getIn().getBody().getClass());
        //exchange.getIn().setBody(serverName, Object.class);
        getProcessor().process(exchange); 
        
        return 1;
	}
	
	public static Event genEventObj( RESTNetworkAdvisorEvents event ) {
		Event genevent;
		//Long timestamp;
		
		genevent = new Event();
		genevent.setHost(event.getSourceName());
		//genevent.setParametr(event.getEventCategory());
		genevent.setObject(event.getNodeWwn());
		genevent.setCategory("HARDWARE");
		genevent.setExternalid(event.getKey());
		genevent.setMessage(event.getDescription());
		genevent.setSeverity(event.getSeverity());
		genevent.setStatus("OPEN");
		genevent.setTimestamp(event.getFirstOccurrenceHostTime()/1000);
		genevent.setEventsource("BSNA");
		genevent.setService("BSNA");
		genevent.setCi(event.getNodeWwn());
		//System.out.println(event.toString());
		
		//logger.info(genevent.toString());
		
		return genevent;
				
	}

	private int getEventId(String key) {
		int id = -1;
		Pattern p = Pattern.compile("(edbid-)(.*)");
		Matcher matcher = p.matcher(key);
		//String output = "";
		if (matcher.matches())
			id = Integer.parseInt(matcher.group(2));
		//System.out.println(matcher.group(2));
		id = Integer.parseInt(matcher.group(2).toString());
		//System.out.println(id);
		return id;
	}
	
	/*
	private int processSearchFeeds() throws Exception {
		
		String query = endpoint.getConfiguration().getQuery();
		String uri = String.format("login?query=%s", query);
		JsonObject json = performGetRequest(uri);
		
		//JsonArray feeds = (JsonArray) json.get("results");
		JsonArray feeds = (JsonArray) json.get("ServerName");
		List<Feed2> feedList = new ArrayList<Feed2>();
		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		for (JsonElement f : feeds) {
			//logger.debug(gson.toJson(i));
			Feed2 feed = gson.fromJson(f, Feed2.class);
			feedList.add(feed);		
		}		
		
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(feedList, ArrayList.class);
        getProcessor().process(exchange); 
        
        return 1;
	}
	*/

}