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
import ru.atc.adapters.type.Event;
import ru.atc.camel.networkadvisor.events.api.RESTNetworkAdvisorDevice;
import ru.atc.camel.networkadvisor.events.api.RESTNetworkAdvisorEvents;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.atc.adapters.message.CamelMessageManager.genAndSendErrorMessage;
import static ru.atc.adapters.message.CamelMessageManager.genHeartbeatMessage;

public class RESTNetworkAdvisorConsumer extends ScheduledPollConsumer {

    private static final int HTTP_OK_STATUS = 200;
    private static final Logger logger = LoggerFactory.getLogger("mainLogger");
    private static final Logger loggerErrors = LoggerFactory.getLogger("errorsLogger");
    private static String savedWStoken;
    //private static List<EnrichRule> enrichRules;
    private final RESTNetworkAdvisorEndpoint endpoint;

    public RESTNetworkAdvisorConsumer(RESTNetworkAdvisorEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        //this.afterPoll();
        this.setTimeUnit(TimeUnit.MINUTES);
        this.setInitialDelay(0);
        this.setDelay(endpoint.getConfiguration().getDelay());
    }

    public static String getSavedWStoken() {
        return savedWStoken;
    }

    public void setSavedWStoken(String wsToken) {
        logger.debug("*** Saving savedWStoken: " + wsToken);
        savedWStoken = wsToken;
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

        //send HEARTBEAT
        genHeartbeatMessage(getEndpoint().createExchange(), endpoint.getConfiguration().getAdaptername());

        return timeout;
    }

    private void genErrorMessage(String message) {
        genAndSendErrorMessage(this, message, new RuntimeException("No additional exception's text."),
                endpoint.getConfiguration().getAdaptername());
    }

    private void genErrorMessage(String message, Exception exception) {
        genAndSendErrorMessage(this, message, exception,
                endpoint.getConfiguration().getAdaptername());
    }

    private CloseableHttpClient initHttp() throws KeyManagementException {

        SSLContext sslContext = null;
        HttpClientBuilder cb = HttpClientBuilder.create();
        SSLContextBuilder sslcb = new SSLContextBuilder();
        try {
            sslcb.loadTrustMaterial(KeyStore.getInstance(KeyStore.getDefaultType()), new TrustSelfSignedStrategy());
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            loggerErrors.error("HTTP SSL KeyStore Error", e);
        }

        try {
            sslContext = sslcb.build();
        } catch (NoSuchAlgorithmException e) {
            loggerErrors.error("HTTP SSL KeyStore Error", e);
        }

        cb.setSslcontext(sslContext);

        @SuppressWarnings("deprecation")
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        return HttpClients.custom().setSSLSocketFactory(sslsf).build();
    }

    private String getWStoken(CloseableHttpClient httpclient) throws IOException {

        String restapiUrl = endpoint.getConfiguration().getRestapiurl();
        String wsUsername = endpoint.getConfiguration().getWsusername();
        String wsPassword = endpoint.getConfiguration().getWspassword();

        logger.debug("***************** restapiurl: " + restapiUrl);
        logger.debug("***************** WSusername: " + wsUsername);
        logger.debug("***************** WSpassword: " + wsPassword);

        HttpPost request = new HttpPost(restapiUrl + "login");
        request.addHeader("Accept", "application/vnd.brocade.networkadvisor+json;version=v1");
        request.addHeader("WSusername", wsUsername);
        request.addHeader("WSpassword", wsPassword);
        HttpResponse response;
        try {
            response = httpclient.execute(request);
        } catch (IOException e) {
            logger.error("Error while get WS Token ", e);
            genErrorMessage("Error while get WS Token ", e);
            throw new RuntimeException("Error while get WS Token ");
        }

        Header head = response.getFirstHeader("WStoken");
        String wsToken = head.getValue();

        logger.debug("***************** WStoken: " + wsToken);

        if (response.getStatusLine().getStatusCode() != HTTP_OK_STATUS) {
            genErrorMessage("Failed while HTTP API connect.");
            throw new RuntimeException("Failed while HTTP API connect.");
        }

        logger.debug("******* Save WSToken: " + wsToken);
        setSavedWStoken(wsToken);

        return wsToken;

    }

    private JsonObject performGetRequest(String uri, String wsToken) throws IOException, KeyManagementException {

        String restapiurl = endpoint.getConfiguration().getRestapiurl();

        CloseableHttpClient httpClient = initHttp();
        String wsTokenFinal;
        if (wsToken == null) {
            wsTokenFinal = getWStoken(httpClient);
            if (wsTokenFinal == null) {
                httpClient.close();
                genErrorMessage("Failed while WSToken retrieving.");
                throw new RuntimeException("Failed while WSToken retrieving.");
            }

        } else
            wsTokenFinal = wsToken;

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

        logger.debug("*** WSToken : " + wsToken);

        JsonObject json;
        try {
            json = performGetRequest(uri, wsToken);
        } catch (IOException | KeyManagementException e) {
            genErrorMessage("Failed while WSToken retrieving.", e);
            throw new RuntimeException("Failed while WSToken retrieving.", e);
        }
        JsonArray devices = (JsonArray) json.get("fcSwitches");

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        JsonElement f = devices.get(0);

        logger.debug(f.toString());

        return gson.fromJson(f, RESTNetworkAdvisorDevice.class);

    }

    private int processSearchEvents() {

        //EventRules eventRules = initEnrichRules("enrichment-rules.xml");
        //enrichRules = eventRules.getEnrichRules();
        //logger.info("Parsed " + enrichRules.size() + " Rules.");

        String eventsuri = endpoint.getConfiguration().getEventsuri();

        int startindex = endpoint.getConfiguration().getStartindex();
        int initcount = endpoint.getConfiguration().getInitcount();
        String specialEvent = endpoint.getConfiguration().getSpecialEvent();
        String uri = String.format("%sevents?startindex=%d&count=%d&specialEvent=%s",
                eventsuri, startindex, initcount, specialEvent);

        logger.info("Try to get " + initcount + " Events.");

        logger.info("Get events URL: " + uri);

        int lastEventId;
        int j;
        boolean findlast;
        try {
            JsonObject json = performGetRequest(uri, null);
            JsonArray events = (JsonArray) json.get("events");
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

            int eventId;
            lastEventId = 0;
            int i = 0;
            j = 0;
            findlast = false;

            int storedLastId = endpoint.getConfiguration().getLastid();

            logger.info("Received " + events.size() + " Total Events.");
            logger.debug("*** WSToken : " + getSavedWStoken());

            for (JsonElement f : events) {
                i++;
                logger.debug(f.toString());

                RESTNetworkAdvisorEvents event = gson.fromJson(f, RESTNetworkAdvisorEvents.class);
                Event genevent;

                eventId = getEventId(event.getKey());
                if (i == 1)
                    lastEventId = eventId;

                logger.debug("*****************eventId: " + eventId);

                if (eventId != -1) {
                    if (eventId > storedLastId) {
                        j++;
                        genevent = genEventObj(event);
                        //genevent = processEnrichmentRules(genevent);

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
            genErrorMessage("Error while get Events from API", e);
            return 0;
        } catch (Exception e) {
            genErrorMessage("General Error while get Events from API", e);
            return 0;
        }

        logger.info("Received " + j + " new Events.");

        endpoint.getConfiguration().setLastid(lastEventId);
        int count = endpoint.getConfiguration().getCount();

        if (findlast) {
            endpoint.getConfiguration().setInitcount(count);
        } else {
            endpoint.getConfiguration().setInitcount(count * 2);
        }

        return 1;
    }

    /*private Event processEnrichmentRules(Event event) throws IOException {

        logger.info("**** Process enrichment rules...");

        for (EnrichRule enrichRule : enrichRules) {
            logger.debug("Rule # " + enrichRule.getId());

            RuleInput inputs = enrichRule.getRuleInput();

            List<Statement> inputStatements = inputs.getStatements();
            boolean statementsMatches = true;
            for (Statement statement : inputStatements) {
                logger.debug("******* statement #: " + statement.getId());
                logger.debug("******* statement field name: " + statement.getFieldName());
                logger.debug("******* statement field value: " + statement.getFieldValue());

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
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    if (eventValue != null && eventValue.matches(statement.getFieldValue())) {
                        logger.info(String.format("******* rule %d, statement %d: Statement MATCHES!",
                                enrichRule.getId(), statement.getId()));

                    } else {
                        statementsMatches = false;
                        logger.info(String.format("******* rule %d, statement %d: Statement DOES NOT MATCH!",
                                enrichRule.getId(), statement.getId()));
                        break;
                    }
                }

            }

            logger.debug("******* Outputs: ");
            RuleOutput outputs = enrichRule.getRuleOutput();

            List<Statement> outputStatements = outputs.getStatements();

            if (statementsMatches) {
                for (Statement outputStatement : outputStatements) {
                    logger.debug("******* statement field name: " + outputStatement.getFieldName());
                    logger.debug("******* statement field value: " + outputStatement.getFieldValue());

                    String methodSuffix = Character.toUpperCase(outputStatement.getFieldName().charAt(0)) + outputStatement.getFieldName().substring(1);
                    Method reflectSetMethod = null;
                    try {
                        reflectSetMethod = Event.class.getDeclaredMethod("set" + methodSuffix, String.class);
                    } catch (NoSuchMethodException e) {
                        loggerErrors.error(String.format("Error while invoke Enrichment Rules: ", e));
                        logger.error(String.format("Error while invoke Enrichment Rules: %s ", e));
                    }
                    if (reflectSetMethod != null) {
                        try {
                            reflectSetMethod.invoke(event, outputStatement.getFieldValue());
                            logger.info(String.format("******* New Value for %s: %s",
                                    outputStatement.getFieldName(), outputStatement.getFieldValue()));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            loggerErrors.error(String.format("Error while invoke Enrichment Rules: ", e));
                            logger.error(String.format("Error while invoke Enrichment Rules: ", e));

                        }

                    }
                }
            }
        }

        logger.info("**** Stop process enrichment rules.");

        return event;
    }*/

    /*private EventRules initEnrichRules(String filePath) {
        EventRules eventRules = null;

        logger.info(String.format("Parsing Enrichment Rules xml: %s ", filePath));
        try {
            File file = new File(filePath);
            JAXBContext jaxbContext = JAXBContext.newInstance(EventRules.class);

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            eventRules = (EventRules) jaxbUnmarshaller.unmarshal(file);

        } catch (JAXBException e) {
            loggerErrors.error("Error while invoke Enrichment Rules", e);
            logger.error("Error while invoke Enrichment Rules ", e);
        }
        return eventRules;
    }*/

    private Event genEventObj(RESTNetworkAdvisorEvents event) throws IOException {

        logger.debug("******* Try to generate event... ");
        logger.debug("*** WSToken : " + getSavedWStoken());
        logger.debug("*** savedWSToken : " + savedWStoken);

        Event genevent;
        genevent = new Event();
        String wwn;
        RESTNetworkAdvisorDevice networkAdvisorDevice;

        wwn = event.getNodeWwn();

        String hostName;
        hostName = event.getSourceName();
        if (wwn.length() != 0) {

            logger.debug("******* Try to get device info by wwn... ");
            networkAdvisorDevice = getDeviceByWwn(wwn, getSavedWStoken());
            logger.info("***************** networkAdvisorDevice.getName(): " + networkAdvisorDevice.getName());
            logger.info("***************** event.getSourceName(): " + event.getSourceName());
            if (networkAdvisorDevice.getName().length() != 0)
                hostName = networkAdvisorDevice.getName();
            else
                hostName = event.getSourceName();
        }
        genevent.setHost(hostName);

        genevent.setObject(event.getNodeWwn());
        genevent.setCategory("HARDWARE");
        genevent.setExternalid(event.getKey());
        genevent.setMessage(event.getDescription());
        genevent.setSeverity(setRightSeverity(event.getSeverity()));

        genevent.setStatus("OPEN");
        genevent.setOrigin(event.getOrigin());
        genevent.setModule(event.getModule());
        genevent.setEventCategory(event.getEventCategory());
        genevent.setTimestamp(event.getFirstOccurrenceHostTime() / 1000);
        genevent.setEventsource("BSNA");
        genevent.setService("BSNA");
        genevent.setCi(String.format("%s:%s", endpoint.getConfiguration().getSource(), event.getNodeWwn()));

        logger.info("******* Event generated. ");
        logger.debug(genevent.toString());

        return genevent;

    }

    private int getEventId(String key) {
        int id = -1;
        Pattern p = Pattern.compile("(edbid-)(.*)");
        Matcher matcher = p.matcher(key);

        if (matcher.matches())
            id = Integer.parseInt(matcher.group(2));

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
                newseverity = Event.PersistentEventSeverity.CRITICAL.name();
                break;
            case "ALERT":
                newseverity = Event.PersistentEventSeverity.CRITICAL.name();
                break;
            case "CRITICAL":
                newseverity = Event.PersistentEventSeverity.CRITICAL.name();
                break;
            case "ERROR":
                newseverity = Event.PersistentEventSeverity.MAJOR.name();
                break;
            case "WARNING":
                newseverity = Event.PersistentEventSeverity.WARNING.name();
                break;
            case "NOTICE":
                newseverity = Event.PersistentEventSeverity.INFO.name();
                break;
            case "INFO":
                newseverity = Event.PersistentEventSeverity.INFO.name();
                break;
            case "DEBUG":
                newseverity = Event.PersistentEventSeverity.INFO.name();
                break;
            case "UNKNOWN":
                newseverity = Event.PersistentEventSeverity.INFO.name();
                break;
            default:
                newseverity = Event.PersistentEventSeverity.INFO.name();
                break;

        }

        return newseverity;
    }

}