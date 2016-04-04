package ru.atc.camel.networkadvisor.events;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultPollingEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;

@UriEndpoint(scheme = "restna", title = "RestNA", syntax = "restna://operationPath", consumerOnly = true, consumerClass = RESTNetworkAdvisorConsumer.class, label = "restna")
public class RESTNetworkAdvisorEndpoint extends DefaultPollingEndpoint {

    private String operationPath;
    @UriParam
    private RESTNetworkAdvisorConfiguration configuration;

    public RESTNetworkAdvisorEndpoint(String uri, String operationPath, RESTNetworkAdvisorComponent component) {
        super(uri, component);
        this.operationPath = operationPath;
    }

    public Producer createProducer() throws Exception {
        throw new UnsupportedOperationException("RESTNetworkAdvisorProducer is not implemented");
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new RESTNetworkAdvisorConsumer(this, processor);
    }

    public boolean isSingleton() {
        return true;
    }

    public String getOperationPath() {
        return operationPath;
    }

    public void setOperationPath(String operationPath) {
        this.operationPath = operationPath;
    }

    public RESTNetworkAdvisorConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(RESTNetworkAdvisorConfiguration configuration) {
        this.configuration = configuration;
    }

}