package ru.atc.camel.networkadvisor.events.api;

public class RESTNetworkAdvisorEvents {

    private String key;
    private String severity;
    private String description;
    private String sourceAddr;
	
	
	@Override
	public String toString() {
		return key + " " + severity + " " + description;
	}


	public String getKey() {
		return key;
	}


	public void setKey(String key) {
		this.key = key;
	}


	public String getSeverity() {
		return severity;
	}


	public void setSeverity(String severity) {
		this.severity = severity;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public String getSourceAddr() {
		return sourceAddr;
	}


	public void setSourceAddr(String sourceAddr) {
		this.sourceAddr = sourceAddr;
	}
}