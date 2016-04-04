package ru.atc.camel.networkadvisor.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.at_consulting.itsm.event.Event;
import ru.at_consulting.itsm.event_rules.EnrichRule;
import ru.at_consulting.itsm.event_rules.EventRules;
import ru.at_consulting.itsm.event_rules.RuleInput;
import ru.at_consulting.itsm.event_rules.RuleOutput;
import ru.at_consulting.itsm.event_rules.Statement;
import ru.atc.camel.networkadvisor.events.api.RESTNetworkAdvisorDevice;
import ru.atc.camel.networkadvisor.events.api.RESTNetworkAdvisorEvents;

import javax.net.ssl.SSLContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RESTNetworkAdvisorConsumer extends ScheduledPollConsumer {

    private static final int HTTP_OK_STATUS = 200;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static String SavedWStoken;
    private static List<EnrichRule> enrichRules;
    private final RESTNetworkAdvisorEndpoint endpoint;

    public RESTNetworkAdvisorConsumer(RESTNetworkAdvisorEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        //this.afterPoll();
        this.setTimeUnit(TimeUnit.MINUTES);
        this.setInitialDelay(0);
        this.setDelay(endpoint.getConfiguration().getDelay());
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

    }

    public static String getSavedWStoken() {
        return SavedWStoken;
    }

    public void setSavedWStoken(String savedWStoken) {
        SavedWStoken = savedWStoken;
    }

    @Override
    protected int poll() throws Exception {

        String operationPath = endpoint.getOperationPath();

        if ("events".equals(operationPath)) return processSearchEvents();

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

        exchange.getIn().setHeader("EventIdAndStatus", "Error_" + timestamp);
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

    private CloseableHttpClient initHttp() {

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
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        return HttpClients.custom().setSSLSocketFactory(sslsf).build();
    }

    private String getWStoken(CloseableHttpClient httpclient) throws IOException {

        String restapiUrl = endpoint.getConfiguration().getRestapiurl();
        String wsUsername = endpoint.getConfiguration().getWsusername();
        String wsPassword = endpoint.getConfiguration().getWspassword();

        logger.info("***************** restapiurl: " + restapiUrl);
        logger.info("***************** WSusername: " + wsUsername);
        logger.info("***************** WSpassword: " + wsPassword);

        HttpPost request = new HttpPost(restapiUrl + "login");
        request.addHeader("Accept", "application/vnd.brocade.networkadvisor+json;version=v1");
        request.addHeader("WSusername", wsUsername);
        request.addHeader("WSpassword", wsPassword);
        HttpResponse response;
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
        String wsToken = head.getValue();
        //System.out.println("***************** WStoken: " + WStoken);

        if (response.getStatusLine().getStatusCode() != HTTP_OK_STATUS) {
            //throw new RuntimeException("Feedly API error with return code: " + response.getStatusLine().getStatusCode());
            System.out.println("Network Advisor RST API error with return code: " + response.getStatusLine().getStatusCode());
            //return null;
            throw new RuntimeException("Failed while HTTP API connect.");
        }

        setSavedWStoken(wsToken);

        return wsToken;

    }

    private JsonObject performGetRequest(String uri, String wsToken) throws IOException {

        String restapiurl = endpoint.getConfiguration().getRestapiurl();

        //System.out.println("*****************Lastid: " + endpoint.getConfiguration().getLastid());
        //endpoint.getConfiguration().setLastid(70955);

        CloseableHttpClient httpClient = initHttp();

        //httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

        //System.out.println("*****************WSToken: " + wsToken);

        String wsTokenFinal = null;
        if (wsToken == null) {
            wsTokenFinal = getWStoken(httpClient);
            if (wsTokenFinal == null) {
                httpClient.close();
                throw new RuntimeException("Failed while WSToken retrieving.");
            }

        }
        //System.out.println("*****************WSToken: " + wsToken);

        //System.out.println("*****************URL: " + restapiurl + uri);

        //uri = "resourcegroups/All/events?startindex=0&count=10&specialEvent=true&origin=trap";
        HttpGet request2 = new HttpGet(restapiurl + uri);
        request2.addHeader("Accept", "application/vnd.brocade.networkadvisor+json;version=v1");
        request2.addHeader("WStoken", wsTokenFinal);
        HttpResponse response2 = httpClient.execute(request2);

        JsonParser parser = new JsonParser();
        InputStreamReader sr = new InputStreamReader(response2.getEntity().getContent(), "UTF-8");
        BufferedReader br = new BufferedReader(sr);
        JsonObject json = (JsonObject) parser.parse(br);
        br.close();
        sr.close();

        httpClient.close();

        return json;
    }

    private RESTNetworkAdvisorDevice getDeviceByWwn(String wwn, String wsToken) throws IOException {
        String uri = String.format("resourcegroups/All/fcswitches/%s", wwn);

        //System.out.println("***************** URL2: " + uri);

        JsonObject json;
        try {
            json = performGetRequest(uri, wsToken);
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

        //gendevice = genDeviceObj( device_na );

        return gson.fromJson(f, RESTNetworkAdvisorDevice.class);

    }

    private int processSearchEvents() throws Exception {

        //Long timestamp;

        EventRules eventRules = initEnrichRules("enrichment-rules.xml");
        enrichRules = eventRules.getEnrichRules();
        logger.info("Parsed " + enrichRules.size() + " Rules.");

        //System.exit(-1);

        String eventsuri = endpoint.getConfiguration().getEventsuri();

        //System.out.println("***************** eventsuri: " + eventsuri);

        int startindex = endpoint.getConfiguration().getStartindex();
        int initcount = endpoint.getConfiguration().getInitcount();
        String specialEvent = endpoint.getConfiguration().getSpecialEvent();
        String uri = String.format("%sevents?startindex=%d&count=%d&specialEvent=%s",
                eventsuri, startindex, initcount, specialEvent);

        //System.out.println("***************** URL: " + uri);

        //CloseableHttpClient httpclient = initHttp();

        logger.info("Try to get " + initcount + " Events.");

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
            // List<Event> eventList = new ArrayList<>();
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

            int eventId;
            lastEventId = 0;
            int i = 0;
            j = 0;
            findlast = false;

            //System.out.println("*****************lastEventId: " + lastEventId);
            int storedLastId = endpoint.getConfiguration().getLastid();
            //System.out.println("*****************storedLastId: " + storedLastId);

            logger.info("Received " + events.size() + " Total Events.");

            for (JsonElement f : events) {
                i++;
                logger.debug(f.toString());

                RESTNetworkAdvisorEvents event = gson.fromJson(f, RESTNetworkAdvisorEvents.class);
                Event genevent;

                eventId = getEventId(event.getKey());
                if (i == 1)
                    lastEventId = eventId;

                System.out.println("*****************eventId: " + eventId);

                if (eventId != -1) {
                    if (eventId > storedLastId) {
                        j++;
                        genevent = genEventObj(event);

                        genevent = processEnrichmentRules(genevent);

                        //eventList.add(genevent);

                        logger.info("Create Exchange container");
                        Exchange exchange = getEndpoint().createExchange();
                        exchange.getIn().setBody(genevent, Event.class);
                        exchange.getIn().setHeader("eventId", event.getKey());

                        getProcessor().process(exchange);

                    } else {
                        findlast = true;
                        break;
                    }
                }

            }
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.error(String.format("Error while get Events from API: %s ", e));
            genErrorMessage(e.getMessage() + " " + e.toString());
            //dataSource.close();
            return 0;
        } catch (Error e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.error(String.format("Error while get Events from API: %s ", e));
            genErrorMessage(e.getMessage() + " " + e.toString());
            //dataSource.close();
            return 0;
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.error(String.format("Error while get Events from API: %s ", e));
            genErrorMessage(e.getMessage() + " " + e.toString());
            //dataSource.close();
            return 0;
        }

        logger.info("Received " + j + " new Events.");

        //Collections.reverse(eventList);
        endpoint.getConfiguration().setLastid(lastEventId);
        int count = endpoint.getConfiguration().getCount();

        //endpoint.getConfiguration().setLastid(lastEventId) ;
        if (findlast) {

            endpoint.getConfiguration().setInitcount(count);
        } else {

            endpoint.getConfiguration().setInitcount(count * 2);
        }

        return 1;
    }

    private Event processEnrichmentRules(Event event) {
        //enrichRules = eventRules.getEnrichRules();
        for (EnrichRule enrichRule : enrichRules) {
            logger.info(enrichRule.getId() + "");
            logger.info("******* Inputs: ");
            RuleInput inputs = enrichRule.getRuleInput();

            List<Statement> inputStatements = inputs.getStatements();
            //System.out.println("******* InputStatements: " + inputStatements);
            boolean statementsMatches = true;
            for (Statement statement : inputStatements) {
                logger.info("******* statement field name: " + statement.getFieldName());
                logger.info("******* statement field value: " + statement.getFieldValue());

                String methodSuffix = Character.toUpperCase(statement.getFieldName().charAt(0)) + statement.getFieldName().substring(1);
                Method method = null;
                try {
                    method = Event.class.getDeclaredMethod("get" + methodSuffix);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    logger.error(String.format("Error while invoke Enrichment Rules: %s ", e));
                }
                //method.invoke();
                if (method != null) {
                    String eventValue = null;
                    try {
                        eventValue = (String) method.invoke(event);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    logger.info("******* eventValue: " + eventValue);
                    if (eventValue != null && eventValue.matches(statement.getFieldValue())) {
                        logger.info("******* Statement MATCHES !!!!!: " + statement.getId());

                    } else {
                        statementsMatches = false;
                        logger.info("******* Statement DOES NOT MATCHES !!!!!");
                        break;
                    }
                }

            }

            logger.info("******* Outputs: ");
            RuleOutput outputs = enrichRule.getRuleOutput();

            List<Statement> outputStatements = outputs.getStatements();
            //logger.debug("******* outputStatements: " + outputStatements);
            if (statementsMatches) {
                for (Statement outputStatement : outputStatements) {
                    logger.info("******* statement field name: " + outputStatement.getFieldName());
                    logger.info("******* statement field value: " + outputStatement.getFieldValue());

                    String methodSuffix = Character.toUpperCase(outputStatement.getFieldName().charAt(0)) + outputStatement.getFieldName().substring(1);
                    Method reflectSetMethod = null;
                    try {
                        reflectSetMethod = Event.class.getDeclaredMethod("set" + methodSuffix, String.class);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                        logger.error(String.format("Error while invoke Enrichment Rules: %s ", e));
                    }
                    if (reflectSetMethod != null) {
                        try {
                            reflectSetMethod.invoke(event, outputStatement.getFieldValue());
                            logger.info("******* NewEventValue: " + outputStatement.getFieldValue());
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            logger.error(String.format("Error while invoke Enrichment Rules: %s ", e));

                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                            logger.error(String.format("Error while invoke Enrichment Rules: %s ", e));

                        }

                    }
                }
            }
        }
        return event;
    }

    private EventRules initEnrichRules(String filePath) {
        EventRules eventRules = null;

        logger.info(String.format("Parsing Enrichment Rules xml: %s ", filePath));
        try {

            //RESTNetworkAdvisorEvents rawEvent = new RESTNetworkAdvisorEvents();

            File file = new File(filePath);
            JAXBContext jaxbContext = JAXBContext.newInstance(EventRules.class);

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            eventRules = (EventRules) jaxbUnmarshaller.unmarshal(file);

        } catch (JAXBException e) {
            e.printStackTrace();
            logger.error(String.format("Error while invoke Enrichment Rules: %s ", e));
        }
        return eventRules;
    }

    private Event genEventObj(RESTNetworkAdvisorEvents event) throws IOException {
        Event genevent;
        genevent = new Event();
        String wwn;
        RESTNetworkAdvisorDevice networkAdvisorDevice;
        //Long timestamp;

        wwn = event.getNodeWwn();
        //logger.info("Create Exchange container");
        //System.out.println("***************** wwn: " + wwn);

        String hostName;
        hostName = event.getSourceName();
        if (wwn.length() != 0) {

            //System.out.println("***************** wwn3: ***" + wwn.length() + "***");
            networkAdvisorDevice = getDeviceByWwn(wwn, RESTNetworkAdvisorConsumer.getSavedWStoken());
            logger.info("***************** networkAdvisorDevice.getName(): " + networkAdvisorDevice.getName());
            logger.info("***************** event.getSourceName(): " + event.getSourceName());
            //System.out.println("***************** networkAdvisorDevice.getName(): " + networkAdvisorDevice.getName());
            //System.out.println("***************** event.getSourceName(): " + event.getSourceName());
            if (networkAdvisorDevice.getName().length() != 0)
                hostName = networkAdvisorDevice.getName();
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
        genevent.setTimestamp(event.getFirstOccurrenceHostTime() / 1000);
        genevent.setEventsource("BSNA");
        genevent.setService("BSNA");
        genevent.setCi(String.format("%s:%s", endpoint.getConfiguration().getSource(), event.getNodeWwn()));
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
        //id = Integer.parseInt(matcher.group(2));
        //System.out.println(id);
        return id;
    }

    public String setRightSeverity(String severity) {
        String newseverity;
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
            case "EMERGENCY":
                newseverity = PersistentEventSeverity.CRITICAL.name();
                break;
            case "ALERT":
                newseverity = PersistentEventSeverity.CRITICAL.name();
                break;
            case "CRITICAL":
                newseverity = PersistentEventSeverity.CRITICAL.name();
                break;
            case "ERROR":
                newseverity = PersistentEventSeverity.MAJOR.name();
                break;
            case "WARNING":
                newseverity = PersistentEventSeverity.WARNING.name();
                break;
            case "NOTICE":
                newseverity = PersistentEventSeverity.INFO.name();
                break;
            case "INFO":
                newseverity = PersistentEventSeverity.INFO.name();
                break;
            case "DEBUG":
                newseverity = PersistentEventSeverity.INFO.name();
                break;
            case "UNKNOWN":
                newseverity = PersistentEventSeverity.INFO.name();
                break;
            default:
                newseverity = PersistentEventSeverity.INFO.name();
                break;

        }
        System.out.println("***************** severity: " + severity);
        System.out.println("***************** newseverity: " + newseverity);
        return newseverity;
    }

    public enum PersistentEventSeverity {
        OK, INFO, WARNING, MINOR, MAJOR, CRITICAL;

        public static PersistentEventSeverity fromValue(String v) {
            return valueOf(v);
        }

        public String value() {
            return name();
        }
    }

}