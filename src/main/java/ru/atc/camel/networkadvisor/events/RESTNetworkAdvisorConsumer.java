package ru.atc.camel.networkadvisor.events;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

//import com.apc.stdws.xsd.isxcentral._2009._10.ISXCAlarmSeverity;
//import com.apc.stdws.xsd.isxcentral._2009._10.ISXCAlarm;
//import com.apc.stdws.xsd.isxcentral._2009._10.ISXCDevice;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ru.at_consulting.itsm.event.Event;
import ru.atc.camel.networkadvisor.events.api.RESTNetworkAdvisorDevice;
//import ru.atc.camel.networkadvisor.events.api.Feed2;
import ru.atc.camel.networkadvisor.events.api.RESTNetworkAdvisorEvents;

public class RESTNetworkAdvisorConsumer extends ScheduledPollConsumer {
	
	private static Logger logger = LoggerFactory.getLogger(Main.class);
	
	private RESTNetworkAdvisorEndpoint endpoint;
	
	private static String SavedWStoken;
	
	public enum PersistentEventSeverity {
	    OK, INFO, WARNING, MINOR, MAJOR, CRITICAL;
		
	    public String value() {
	        return name();
	    }

	    public static PersistentEventSeverity fromValue(String v) {
	        return valueOf(v);
	    }
	}

	public RESTNetworkAdvisorConsumer(RESTNetworkAdvisorEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        //this.afterPoll();
        this.setTimeUnit(TimeUnit.MINUTES);
        this.setInitialDelay(0);
        this.setDelay(endpoint.getConfiguration().getDelay());
	}
	
	@Override
	protected int poll() throws Exception {
		
		String operationPath = endpoint.getOperationPath();
		
		if (operationPath.equals("events")) return processSearchEvents();
		
		// only one operation implemented for now !
		throw new IllegalArgumentException("Incorrect operation: " + operationPath);
	}
	
	@Override
	public long beforePoll(long timeout) throws Exception {
		
		logger.info("*** Before Poll!!!");
		// only one operation implemented for now !
		//throw new IllegalArgumentException("Incorrect operation: ");
		
		//send HEARTBEAT
		genHeartbeatMessage(getEndpoint().createExchange());
		
		return timeout;
	}
	
	private void genErrorMessage(String message) {
		// TODO Auto-generated method stub
		long timestamp = System.currentTimeMillis();
		timestamp = timestamp / 1000;
		String textError = "Возникла ошибка при работе адаптера: ";
		Event genevent = new Event();
		genevent.setMessage(textError + message);
		genevent.setEventCategory("ADAPTER");
		genevent.setSeverity(PersistentEventSeverity.CRITICAL.name());
		genevent.setTimestamp(timestamp);
		genevent.setEventsource("BSNA_EVENT_ADAPTER");
		genevent.setStatus("OPEN");
		genevent.setHost("adapter");
		
		logger.info(" **** Create Exchange for Error Message container");
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(genevent, Event.class);
        
        exchange.getIn().setHeader("EventIdAndStatus", "Error_" +timestamp);
        exchange.getIn().setHeader("Timestamp", timestamp);
        exchange.getIn().setHeader("queueName", "Events");
        exchange.getIn().setHeader("Type", "Error");

        try {
			getProcessor().process(exchange);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}

	
	public static void genHeartbeatMessage(Exchange exchange) {
		// TODO Auto-generated method stub
		long timestamp = System.currentTimeMillis();
		timestamp = timestamp / 1000;
		//String textError = "Возникла ошибка при работе адаптера: ";
		Event genevent = new Event();
		genevent.setMessage("Сигнал HEARTBEAT от адаптера");
		genevent.setEventCategory("ADAPTER");
		genevent.setObject("HEARTBEAT");
		genevent.setSeverity(PersistentEventSeverity.OK.name());
		genevent.setTimestamp(timestamp);
		genevent.setEventsource("BSNA_EVENT_ADAPTER");
		
		logger.info(" **** Create Exchange for Heartbeat Message container");
        //Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(genevent, Event.class);
        
        exchange.getIn().setHeader("Timestamp", timestamp);
        exchange.getIn().setHeader("queueName", "Heartbeats");
        exchange.getIn().setHeader("Type", "Heartbeats");
        exchange.getIn().setHeader("Source", "BSNA_EVENT_ADAPTER");

        try {
        	//Processor processor = getProcessor();
        	//.process(exchange);
        	//processor.process(exchange);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} 
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
	
	private String getWStoken( CloseableHttpClient httpclient ) throws ClientProtocolException, IOException {
		
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
			throw e;
			//return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
			//return null;
		}
		
		Header head = response.getFirstHeader("WStoken");
		String WStoken = head.getValue();
		//System.out.println("***************** WStoken: " + WStoken);
	
		if (response.getStatusLine().getStatusCode() != 200) {
			//throw new RuntimeException("Feedly API error with return code: " + response.getStatusLine().getStatusCode());
			System.out.println("Network Advisor RST API error with return code: " + response.getStatusLine().getStatusCode());
			//return null;
			throw new RuntimeException("Failed while HTTP API connect.");
		}
		
		setSavedWStoken(WStoken);
		
		return WStoken;
		
	}

	private JsonObject performGetRequest(String uri, String WStoken) throws ClientProtocolException, IOException {
		
		String restapiurl = endpoint.getConfiguration().getRestapiurl();
		
		//System.out.println("*****************Lastid: " + endpoint.getConfiguration().getLastid());
		//endpoint.getConfiguration().setLastid(70955);
		
		CloseableHttpClient	httpclient = HTTPinit();
		
		//httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
	    
		//System.out.println("*****************WSToken: " + WStoken);
		
		if (WStoken == null){
			WStoken = getWStoken(httpclient);
			if (WStoken == null) {
				httpclient.close();
				throw new RuntimeException("Failed while WSToken retrieving.");
			}
				
		}
		//System.out.println("*****************WSToken: " + WStoken);
		
		//System.out.println("*****************URL: " + restapiurl + uri);
		
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
		
		httpclient.close();
		
		return json;
	}
	
	private  RESTNetworkAdvisorDevice getDeviceByWwn( String wwn, String WStoken ) throws ClientProtocolException, IOException {
		String uri = String.format("resourcegroups/All/fcswitches/%s", wwn);
		
		//System.out.println("***************** URL2: " + uri);
		
		JsonObject json = null ;
		try {
			json = performGetRequest(uri, WStoken);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
			
		}
		
		JsonArray devices = (JsonArray) json.get("fcSwitches");
		//JsonElement serverName = json.get("serverName");
		//List<RESTNetworkAdvisorEvents> eventList = new ArrayList<RESTNetworkAdvisorEvents>();
		//Device device = new Device();
		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		
		JsonElement f = devices.get(0);
		
		logger.debug(f.toString());
			
		RESTNetworkAdvisorDevice device_na = gson.fromJson(f, RESTNetworkAdvisorDevice.class);
		//gendevice = genDeviceObj( device_na );
		
		return device_na;
		
	}
	
	private int processSearchEvents() throws ClientProtocolException, IOException, Exception {
		
		//Long timestamp;
		
		String eventsuri = endpoint.getConfiguration().getEventsuri();
		
		//System.out.println("***************** eventsuri: " + eventsuri);
		
		int startindex = endpoint.getConfiguration().getStartindex();
		int initcount = endpoint.getConfiguration().getInitcount();
		String specialEvent = endpoint.getConfiguration().getSpecialEvent();
		String uri = String.format("%sevents?startindex=%d&count=%d&specialEvent=%s", 
									eventsuri,startindex,initcount,specialEvent);
		
		//System.out.println("***************** URL: " + uri);
		
		//CloseableHttpClient httpclient = HTTPinit();
		
		logger.info("Try to get " + initcount + " Events." );
		
		logger.info("Get events URL: " + uri);
		
		int lastEventId;
		int j;
		boolean findlast;
		try {
			JsonObject json = performGetRequest(uri, null);
			
			//System.out.println("*****************  JSON: " + json);
			
			JsonArray events = (JsonArray) json.get("events");
			//JsonElement serverName = json.get("serverName");
			//List<RESTNetworkAdvisorEvents> eventList = new ArrayList<RESTNetworkAdvisorEvents>();
			List<Event> eventList = new ArrayList<Event>();
			Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
			
			int EventId = 0;
			lastEventId = 0;
			int i = 0;
			j = 0;
			findlast = false;
			
			//System.out.println("*****************lastEventId: " + lastEventId);
			int storedLastId = endpoint.getConfiguration().getLastid();
			//System.out.println("*****************storedLastId: " + storedLastId);
			
			logger.info("Received " + events.size() + " Total Events." );
			
			for (JsonElement f : events) {
				i++;
				logger.debug(f.toString());
				
				
				RESTNetworkAdvisorEvents event = gson.fromJson(f, RESTNetworkAdvisorEvents.class);
				Event genevent = new Event();
				
				EventId = getEventId(event.getKey());
				if ( i == 1 )
					lastEventId = EventId;
				
				System.out.println("*****************EventId: " + EventId);
				
				if (EventId != -1){
					if (EventId > storedLastId){
						j++;
						genevent = genEventObj( event );
						eventList.add(genevent);
						
						logger.info("Create Exchange container");
				        Exchange exchange = getEndpoint().createExchange();
				        exchange.getIn().setBody(genevent, Event.class);
				        exchange.getIn().setHeader("EventId", event.getKey());
				        //exchange.getIn().setHe
				        //System.out.println("There is an exchange going on.");
				        //System.out.println(exchange.getIn().getHeader("CamelFileName"));
				        //System.out.println(exchange.getIn().getBody());
				        //System.out.println(exchange.getIn().getBody().getClass());
				        //exchange.getIn().setBody(serverName, Object.class);
				        getProcessor().process(exchange); 
						
					}
					else {
						findlast = true;
						break;
					}
				}
				
						
			}
		}  catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error( String.format("Error while get Events from SQL: %s ", e));
			genErrorMessage(e.toString());
			//dataSource.close();
			return 0;
		}
		catch (Error e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error( String.format("Error while get Events from SQL: %s ", e));
			genErrorMessage(e.toString());
			//dataSource.close();
			return 0;
		}
		catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error( String.format("Error while get Events from SQL: %s ", e));
			genErrorMessage(e.getMessage() + " " + e.toString());
			//dataSource.close();
			return 0;
		}
		finally
		{
			//dataSource.close();
			//return 0;
		}	
		
		logger.info("Received " + j + " new Events." );
		
		//Collections.reverse(eventList);
		endpoint.getConfiguration().setLastid(lastEventId) ;
		int count = endpoint.getConfiguration().getCount();
			
		//endpoint.getConfiguration().setLastid(lastEventId) ;
		if (findlast){
		
			endpoint.getConfiguration().setInitcount(count);
		}
		else {
			
			endpoint.getConfiguration().setInitcount(count * 2);
		}
				
		/*
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
        */
        
        return 1;
	}
	
	private Event genEventObj( RESTNetworkAdvisorEvents event ) throws ClientProtocolException, IOException {
		Event genevent;
		genevent = new Event();
		String wwn = null;
		RESTNetworkAdvisorDevice device_na = null;
		//Long timestamp;
		
		wwn = event.getNodeWwn().toString();
		//logger.info("Create Exchange container");
		//System.out.println("***************** wwn: " + wwn);
		
		String hostName = "";
		hostName = event.getSourceName();
		if ( wwn.length() != 0 ){
			
			//System.out.println("***************** wwn3: ***" + wwn.length() + "***");
			device_na = getDeviceByWwn(wwn, RESTNetworkAdvisorConsumer.getSavedWStoken());
			logger.info("***************** device_na.getName(): " + device_na.getName());
			logger.info("***************** event.getSourceName(): " + event.getSourceName());
			//System.out.println("***************** device_na.getName(): " + device_na.getName());
			//System.out.println("***************** event.getSourceName(): " + event.getSourceName());
			if (device_na.getName().length() != 0)
				hostName = device_na.getName();
			else
				hostName = event.getSourceName();
		}
		genevent.setHost(hostName);
		//genevent.setParametr(event.getEventCategory());
		genevent.setObject(event.getNodeWwn());
		genevent.setCategory("HARDWARE");
		genevent.setExternalid(event.getKey());
		genevent.setMessage(event.getDescription());
		genevent.setSeverity(setRightSeverity(event.getSeverity()));
		//PersistentEventSeverity.CRITICAL.toString();
		genevent.setStatus("OPEN");
		genevent.setOrigin(event.getOrigin());
		genevent.setModule(event.getModule());
		genevent.setEventCategory(event.getEventCategory());
		genevent.setTimestamp(event.getFirstOccurrenceHostTime()/1000);
		genevent.setEventsource("BSNA");
		genevent.setService("BSNA");
		genevent.setCi(event.getNodeWwn());
		//System.out.println(event.toString());
		
		//logger.info(genevent.toString());
		
		return genevent;
				
	}
	
	/*
	private Device genDeviceObj( RESTNetworkAdvisorDevice device ) {
		Device gendevice = null;
		//Long timestamp;
		
		//String wwn = event.getNodeWwn();
		
		//getDeviceByWwn(wwn, getSavedWStoken());
		
		//gendevice = new Device();
		//gendevice.setHost(event.getSourceName());
		//genevent.setParametr(event.getEventCategory());
		//gendevice.setObject(event.getNodeWwn());
		//gendevice.setCategory("HARDWARE");

		//System.out.println(event.toString());
		
		//logger.info(genevent.toString());
		
		return gendevice;
				
	}
	*/

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

	public static String getSavedWStoken() {
		return SavedWStoken;
	}

	public void setSavedWStoken(String savedWStoken) {
		SavedWStoken = savedWStoken;
	}
	
	public String setRightSeverity(String severity)
	{
		String newseverity = "";
		/*
		 * 
		<xs:simpleType name="Severity">
		<xs:restriction base="xs:string">
		<xs:enumeration value="EMERGENCY"/>
		<xs:enumeration value="ALERT"/>
		<xs:enumeration value="CRITICAL"/>
		<xs:enumeration value="ERROR"/>
		<xs:enumeration value="WARNING"/>
		<xs:enumeration value="NOTICE"/>
		<xs:enumeration value="INFO"/>
		<xs:enumeration value="DEBUG"/>
		<xs:enumeration value="UNKNOWN"/>
		</xs:restriction>
		</xs:simpleType>
		 */
		
		
		
		switch (severity) {
        	case "EMERGENCY":  newseverity = PersistentEventSeverity.CRITICAL.name();break;
        	case "ALERT":  newseverity = PersistentEventSeverity.CRITICAL.name();break;
        	case "CRITICAL":  newseverity = PersistentEventSeverity.CRITICAL.name();break;
        	case "ERROR":  newseverity = PersistentEventSeverity.MAJOR.name();break;
        	case "WARNING":  newseverity = PersistentEventSeverity.WARNING.name();break;
        	case "NOTICE":  newseverity = PersistentEventSeverity.INFO.name();break;
        	case "INFO":  newseverity = PersistentEventSeverity.INFO.name();break;
        	case "DEBUG":  newseverity = PersistentEventSeverity.INFO.name();break;
        	case "UNKNOWN":  newseverity = PersistentEventSeverity.INFO.name();break;
        	
		}
		System.out.println("***************** severity: " + severity);
		System.out.println("***************** newseverity: " + newseverity);
		return newseverity;
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