package ru.atc.camel.networkadvisor.events;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
//import org.apache.camel.dataformat.xstream.JsonDataFormat;
import org.apache.camel.dataformat.xstream.XStreamDataFormat;
import org.apache.camel.model.ExpressionNode;
import org.apache.camel.model.SplitDefinition;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
//import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spi.DataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

import ru.at_consulting.itsm.event.Event;

public class Main {
	
	private static Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		
		logger.info("Starting Custom Apache Camel component example");
		logger.info("Press CTRL+C to terminate the JVM");
		

		
		org.apache.camel.main.Main main = new org.apache.camel.main.Main();
		main.enableHangupSupport();
		
		main.addRouteBuilder(new RouteBuilder() {
			
			@Override
			public void configure() throws Exception {
				
				/*
				XStream xstream = new XStream();
				xstream.processAnnotations(Event.class);

				XStreamDataFormat xStreamDataFormat = new XStreamDataFormat();
				xStreamDataFormat.setXStream(xstream);
				
				DataFormat myJaxb =
					      new JaxbDataFormat("ru.at_consulting.itsm.event");
				*/
				JsonDataFormat myJson = new JsonDataFormat();
				myJson.setPrettyPrint(true);
				myJson.setLibrary(JsonLibrary.Jackson);
				myJson.setJsonView(Event.class);
				//myJson.setPrettyPrint(true);
				
				PropertiesComponent properties = new PropertiesComponent();
				properties.setLocation("classpath:restna.properties");
				getContext().addComponent("properties", properties);
				
				ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://192.168.157.73:61616");		
				getContext().addComponent("activemq", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
				
				ExpressionNode test = from("restna://events?"
		    			+ "delay={{delay}}&"
		    			+ "restapiurl={{restapiurl}}&"
		    			+ "wsusername={{wsusername}}&"
		    			+ "wspassword={{wspassword}}&"
		    			+ "eventsuri={{eventsuri}}&"
		    			+ "startindex={{startindex}}&"
		    			+ "count={{count}}&"
		    			+ "specialEvent={{specialEvent}}")
		    	
		    	//.split(body().convertTo(Event.class));
				.split(body()).convertBodyTo(Event.class);
				
				//String "getKey";
				//ExpressionNode id = test.bean(Event.class, "getExternalid");
				
				/*
				from("restna://events?"
		    			+ "delay={{delay}}&"
		    			+ "restapiurl={{restapiurl}}&"
		    			+ "wsusername={{wsusername}}&"
		    			+ "wspassword={{wspassword}}&"
		    			+ "eventsuri={{eventsuri}}&"
		    			+ "startindex={{startindex}}&"
		    			+ "count={{count}}&"
		    			+ "specialEvent={{specialEvent}}")
		    	
		    	//.split(body().convertTo(Event.class))
				.split(body())
				*/
				
				//ValueBuilder vvv = new ValueBuilder(simple("${body.externalid}"));
				//logger.info("vvv " + vvv.toString());
				//System.out.println("vvv " + vvv.toString());

				//test.getExpression("${body.externalid}");
				//test.transform().simple("OrderId: ${bean:orderIdGenerator?method=generateId}");
				//${body.getExternalid}${body.externalid}
				
		    	//	.log(body().toString())
		    	//.log("*-*-*-*-*-*-*-*-* Processing ${id}")
		    	//.log("${id} ${body.getKey} ${body.key} ")
		    	//myJson.
		    	//.marshal().json(JsonLibrary.Jackson,Event.class)
		    		test.marshal(myJson)
		    	//.marshal(myJaxb)
		    		.to("file://out?fileName=event-${id}")
		    		.to("activemq:BSNA-tgc1-san-Event.queue");
				}
		});
		
		main.run();
	}
}