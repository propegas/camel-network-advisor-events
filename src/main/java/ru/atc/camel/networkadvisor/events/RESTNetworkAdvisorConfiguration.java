package ru.atc.camel.networkadvisor.events;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

@UriParams
public class RESTNetworkAdvisorConfiguration {	
    
	private String eventsuri;
	
	private String wsusername;
	
	private String restapiurl;
	
	private String wspassword;
	
	private int lastid = 0;
	
    @UriParam
    private String query;
    
    @UriParam(defaultValue = "60000")
    private int delay = 60000;
    
    @UriParam(defaultValue = "0")
    private int startindex = 0;
    
    @UriParam(defaultValue = "50")
    private int count = 50;
    
    @UriParam(defaultValue = "true")
    private String specialEvent = "true";

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public int getStartindex() {
		return startindex;
	}

	public void setStartindex(int startindex) {
		this.startindex = startindex;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getSpecialEvent() {
		return specialEvent;
	}

	public void setSpecialEvent(String specialEvent) {
		this.specialEvent = specialEvent;
	}

	public String getEventsuri() {
		return eventsuri;
	}

	public void setEventsuri(String eventsuri) {
		this.eventsuri = eventsuri;
	}

	public int getLastid() {
		return lastid;
	}

	public void setLastid(int lastid) {
		this.lastid = lastid;
	}

	public String getRestapiurl() {
		return restapiurl;
	}

	public void setRestapiurl(String restapiurl) {
		this.restapiurl = restapiurl;
	}

	public String getWsusername() {
		return wsusername;
	}

	public void setWsusername(String wsusername) {
		this.wsusername = wsusername;
	}

	public String getWspassword() {
		return wspassword;
	}

	public void setWspassword(String wspassword) {
		this.wspassword = wspassword;
	}
}