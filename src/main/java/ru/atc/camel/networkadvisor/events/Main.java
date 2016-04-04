package ru.atc.camel.networkadvisor.events;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.processor.idempotent.FileIdempotentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.at_consulting.itsm.event.Event;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.util.Objects;

public final class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static String activemq_port;
    private static String activemq_ip;

    private Main() {

    }

    public static void main(String[] args) throws Exception {

        logger.info("Starting Custom Apache Camel component example");
        logger.info("Press CTRL+C to terminate the JVM");

        if (args.length == 2) {
            activemq_port = args[1];
            activemq_ip = args[0];
        }

        if (activemq_port == null || Objects.equals(activemq_port, ""))
            activemq_port = "61616";
        if (activemq_ip == null || Objects.equals(activemq_ip, ""))
            activemq_ip = "172.20.19.195";

        logger.info("activemq_ip: " + activemq_ip);
        logger.info("activemq_port: " + activemq_port);

        org.apache.camel.main.Main main = new org.apache.camel.main.Main();
        main.enableHangupSupport();

        main.addRouteBuilder(new RouteBuilder() {

            @Override
            public void configure() throws Exception {

                JsonDataFormat myJson = new JsonDataFormat();
                myJson.setPrettyPrint(true);
                myJson.setLibrary(JsonLibrary.Jackson);
                myJson.setJsonView(Event.class);
                //myJson.setPrettyPrint(true);

                PropertiesComponent properties = new PropertiesComponent();
                properties.setLocation("classpath:restna.properties");
                getContext().addComponent("properties", properties);

                ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                        "tcp://" + activemq_ip + ":" + activemq_port
                );
                getContext().addComponent("activemq", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

                File cachefile = new File("sendedEvents.dat");
                //cachefile.createNewFile();

                // Heartbeats
                from("timer://foo?period={{heartbeatsdelay}}")
                        .process(new Processor() {
                            public void process(Exchange exchange) throws Exception {
                                RESTNetworkAdvisorConsumer.genHeartbeatMessage(exchange);
                            }
                        })
                        //.bean(WsdlNNMConsumer.class, "genHeartbeatMessage", exchange)
                        .marshal(myJson)
                        .to("activemq:{{heartbeatsqueue}}")
                        .log("*** Heartbeat: ${id}");

                from(new StringBuilder()
                        .append("restna://events?")
                        .append("delay={{delay}}&")
                        .append("restapiurl={{restapiurl}}&")
                        .append("wsusername={{wsusername}}&")
                        .append("wspassword={{wspassword}}&")
                        .append("eventsuri={{eventsuri}}&")
                        .append("startindex={{startindex}}&")
                        .append("source={{source}}&")
                        .append("count={{count}}&")
                        .append("initcount={{initcount}}&")
                        .append("specialEvent={{specialEvent}}")
                        .toString())

                        .choice()
                        .when(header("Type").isEqualTo("Error"))
                        .marshal(myJson)
                        .to("activemq:{{eventsqueue}}")
                        .log("Error: ${id} ${header.EventUniqId}")

                        .otherwise()

                        .idempotentConsumer(
                                header("EventId"),
                                FileIdempotentRepository.fileIdempotentRepository(cachefile)
                        )

                        .marshal(myJson)
                        .log("${id} ${header.EventId}")
                        .to("activemq:{{eventsqueue}}");
            }
        });

        main.run();
    }
}