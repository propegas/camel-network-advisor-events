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
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ru.atc.camel.networkadvisor.events.api.Feed2;
import ru.atc.camel.networkadvisor.events.api.RESTNetworkAdvisorEvents;

public class RESTNetworkAdvisorConsumer extends ScheduledPollConsumer {
	
	private RESTNetworkAdvisorEndpoint endpoint;

	public RESTNetworkAdvisorConsumer(RESTNetworkAdvisorEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        this.setDelay(endpoint.getConfiguration().getDelay());
	}

	@Override
	protected int poll() throws Exception {
		
		String operationPath = endpoint.getOperationPath();
		
		if (operationPath.equals("events")) return processSearchServer();
		
		// only one operation implemented for now !
		throw new IllegalArgumentException("Incorrect operation: " + operationPath);
	}
	
	private JsonObject performGetRequest(String uri) throws ClientProtocolException, IOException {
		
		System.out.println("*****************Lastid: " + endpoint.getConfiguration().getLastid());
		endpoint.getConfiguration().setLastid(70955);
		
		CloseableHttpClient httpclient = HTTPinit();
		
		//httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
	    
		String WStoken = getWStoken(httpclient);
				
		//uri = "resourcegroups/All/events?startindex=0&count=10&specialEvent=true&origin=trap";
		HttpGet request2 = new HttpGet("https://localhost:8443/rest/" + uri);
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
	    
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	    CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
	    return httpclient;
	}
	
	private String getWStoken( CloseableHttpClient httpclient ){
		
	    HttpPost request = new HttpPost("https://localhost:8443/rest/" + "login");
		request.addHeader("Accept", "application/vnd.brocade.networkadvisor+json;version=v1");
		request.addHeader("WSusername", "zsm");
		request.addHeader("WSpassword", "EG445y69of");
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
			System.out.println("Feedly API error with return code: " + response.getStatusLine().getStatusCode());
			return null;
		}
		
		return WStoken;
		
	}
	
	private int processSearchServer() throws Exception {
		
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
		
		JsonArray feeds = (JsonArray) json.get("events");
		//JsonElement serverName = json.get("serverName");
		List<RESTNetworkAdvisorEvents> feedList = new ArrayList<RESTNetworkAdvisorEvents>();
		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		
		for (JsonElement f : feeds) {
			//logger.debug(gson.toJson(i));
			RESTNetworkAdvisorEvents feed = gson.fromJson(f, RESTNetworkAdvisorEvents.class);
			feedList.add(feed);		
		}	
			
		System.out.println("*****************Lastid: " + endpoint.getConfiguration().getLastid());
		endpoint.getConfiguration().setLastid(70955);
		
		//System.out.println("*****************" + serverName);

		
		//logger.info("Starting Custom Apache Camel component example");
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(feedList, ArrayList.class);
        //exchange.getIn().setBody(serverName, Object.class);
        getProcessor().process(exchange); 
        
        return 1;
	}
	
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

}