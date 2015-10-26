package ru.atc.camel.networkadvisor.events;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.dataformat.xstream.XStreamDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
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
				
				PropertiesComponent properties = new PropertiesComponent();
				properties.setLocation("classpath:restna.properties");
				getContext().addComponent("properties", properties);
				
		    	from("restna://events?"
		    			+ "delay={{delay}}&"
		    			+ "restapiurl={{restapiurl}}&"
		    			+ "wsusername={{wsusername}}&"
		    			+ "wspassword={{wspassword}}&"
		    			+ "eventsuri={{eventsuri}}&"
		    			+ "startindex={{startindex}}&"
		    			+ "count={{count}}&"
		    			+ "specialEvent={{specialEvent}}")
		    	
		    	.split(body())
		    	
		    	.log(body().toString())
		    	.marshal().json(JsonLibrary.Jackson,Event.class)
		    	//.marshal(myJaxb)
		    	.to("file://out?fileName=splitted-${id}");
			}
		});
		
		main.run();
	}
}