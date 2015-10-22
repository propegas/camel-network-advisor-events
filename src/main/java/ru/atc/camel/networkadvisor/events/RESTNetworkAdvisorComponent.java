package ru.atc.camel.networkadvisor.events;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;

public class RESTNetworkAdvisorComponent extends UriEndpointComponent {

	public RESTNetworkAdvisorComponent() {
		super(RESTNetworkAdvisorEndpoint.class);
	}

	@Override
	protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
		
		RESTNetworkAdvisorEndpoint endpoint = new RESTNetworkAdvisorEndpoint(uri, remaining, this);		
		RESTNetworkAdvisorConfiguration configuration = new RESTNetworkAdvisorConfiguration();
		
		// use the built-in setProperties method to clean the camel parameters map
		setProperties(configuration, parameters);
		
		endpoint.setConfiguration(configuration);		
		return endpoint;
	}
}