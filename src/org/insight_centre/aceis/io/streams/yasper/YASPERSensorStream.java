package org.insight_centre.aceis.io.streams.yasper;

import it.polimi.yasper.core.stream.data.DataStreamImpl;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.insight_centre.aceis.observations.SensorObservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.streamreasoning.rsp4j.yasper.examples.RDFStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class YASPERSensorStream extends RDFStream implements Runnable {
	protected Logger logger = LoggerFactory.getLogger(YASPERSensorStream.class);

	public YASPERSensorStream(String uri) {
		super(uri);
	}

	protected double rate = 1.0;
	// private int sleep = 1000;
	protected int sleep = 1000;
	protected boolean stop = false;
	protected SensorObservation currentObservation;
	protected List<String> requestedProperties = new ArrayList<String>();
	protected DataStreamImpl<Graph> s;

	public List<String> getRequestedProperties() {
		return requestedProperties;
	}

	public void setRequestedProperties(List<String> requestedProperties) {
		this.requestedProperties = requestedProperties;
	}

	public void setRate(Double rate) {
		this.rate = rate;
		if (this.rate != 1.0)
			logger.info("Streamming acceleration rate set to: " + rate);
	}

	public double getRate() {
		return rate;
	}

	public void setFreq(Double freq) {
		sleep = (int) (sleep / freq);
		if (this.rate == 1.0)
			logger.info("Streamming interval set to: " + sleep + " ms");
	}

	public void stop() {
		if (!stop) {
			stop = true;
//			logger.info("Stopping stream: " + this.getURI());
		}
		// ACEISEngine.getSubscriptionManager().getStreamMap().remove(this.getURI());
		// SubscriptionManager.
	}

	protected abstract org.apache.commons.rdf.api.Graph getGraph(SensorObservation so) throws NumberFormatException, IOException;

	protected abstract SensorObservation createObservation(Object data);

	public SensorObservation getCurrentObservation() {
		return this.currentObservation;
	}

	public void setWritable(DataStreamImpl<Graph> e) {
		this.s = e;
	}
}
