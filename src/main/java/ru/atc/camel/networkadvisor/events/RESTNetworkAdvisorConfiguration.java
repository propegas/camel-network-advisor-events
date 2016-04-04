package ru.atc.camel.networkadvisor.events;

import org.apache.camel.spi.UriParam;

public class RESTNetworkAdvisorConfiguration {

    private static final int EVENT_COUNT = 50;
    private static final int INIT_EVENT_COUNT = 50;
    private static final int DELAY_IN_MILLISECONDS = 60000;
    private String eventsuri;
    private String wsusername;
    private String restapiurl;
    private String wspassword;
    private String source;
    private int lastid;

    @UriParam
    private String query;

    @UriParam(defaultValue = "60000")
    private int delay = DELAY_IN_MILLISECONDS;

    @UriParam(defaultValue = "0")
    private int startindex;

    @UriParam(defaultValue = "50")
    private int initcount = INIT_EVENT_COUNT;

    @UriParam(defaultValue = "50")
    private int count = EVENT_COUNT;

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

    public int getInitcount() {
        return initcount;
    }

    public void setInitcount(int initcount) {
        this.initcount = initcount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}