package org.insight_centre.aceis.io.streams.yasper;

import it.polimi.yasper.core.stream.data.DataStreamImpl;
import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.insight_centre.aceis.eventmodel.EventDeclaration;
import org.insight_centre.aceis.io.rdf.RDFFileManager;
import org.insight_centre.aceis.observations.AarhusTrafficObservation;
import org.insight_centre.aceis.observations.SensorObservation;
import org.insight_centre.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.streamreasoning.rsp4j.api.RDFUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class YASPERLocationStream extends YASPERSensorStream implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(YASPERLocationStream.class);
	private String txtFile;
	private EventDeclaration ed;

	// private ContextualFilteringManager cfm;

	public YASPERLocationStream(String uri, String txtFile, EventDeclaration ed) {
		super(uri);
		this.txtFile = txtFile;
		this.ed = ed;

	}

	// public UserLocationStream(ExecContext context, String uri, String txtFile, EventDeclaration ed,
	// ContextualFilteringManager cfm) {
	// super(context, uri);
	// this.txtFile = txtFile;
	// this.ed = ed;
	// this.cfm = cfm;
	//
	// }

	@Override
	public void run() {
		logger.info("Starting sensor stream: " + this.stream_uri);
		try {
			if (txtFile.contains("Location")) {
				BufferedReader reader = new BufferedReader(new FileReader(txtFile));
				String strLine;
				while ((strLine = reader.readLine()) != null && (!stop)) {

					SensorObservation so = this.createObservation(strLine);
					Graph graph = this.getGraph(so);
					try {
						this.put(graph, System.currentTimeMillis());
						logger.debug(this.stream_uri + " Streaming: " + graph.toString());

					} catch (Exception e) {
						e.printStackTrace();
						logger.error(this.stream_uri + " YASPER streamming error.");
					}
					CityBench.pm.addNumberOfStreamedStatements((int)graph.size());

					// this.messageCnt += 1;
					// this.byteCnt += messageByte;
					// cw.write(new SimpleDateFormat("hh:mm:ss").format(new Date()));
					// cw.write(this.messageCnt + "");
					// cw.write(this.byteCnt + "");
					// cw.endRecord();
					// cw.flush();
					// stream(n(RDFFileManager.upPrefix + "007"), n(RDFFileManager.ctPrefix + "detectedAt"),
					// n(RDFFileManager.defaultPrefix + "FoI-c6edd705-66a5-45c1-9d93-220da78f9421"));
					if (sleep > 0) {
						try {
							Thread.sleep(sleep);
						} catch (InterruptedException e) {

							e.printStackTrace();

						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Graph getGraph(SensorObservation so) throws NumberFormatException, IOException {
		org.apache.commons.rdf.api.RDF instance = RDFUtils.getInstance();
		Graph graph = instance.createGraph();
		String userStr = so.getFoi();
		String coordinatesStr = so.getValue().toString();
		Model m = ModelFactory.createDefaultModel();
		double lat = Double.parseDouble(coordinatesStr.split(",")[0]);
		double lon = Double.parseDouble(coordinatesStr.split(",")[1]);
		Resource serviceID = m.createResource(ed.getServiceId());
		//
		// Resource user = m.createResource(userStr);

		IRI observation = instance.createIRI(RDFFileManager.defaultPrefix + so.getObId() + UUID.randomUUID());
		CityBench.obMap.put(observation.toString(), so);
		graph.add(instance.createTriple(observation, instance.createIRI(RDF.type.getURI()), instance.createIRI(RDFFileManager.ssnPrefix + "Observation")));
		// observation.addProperty(RDF.type, m.createResource(RDFFileManager.saoPrefix + "StreamData"));

		// location.addProperty(RDF.type, m.createResource(RDFFileManager.ctPrefix + "Location"));


		// observation.addProperty(m.createProperty(RDFFileManager.ssnPrefix + "featureOfInterest"), user);
		graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.ssnPrefix + "observedBy"), instance.createIRI(ed.getServiceId())));
		graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.ssnPrefix + "observedProperty"), instance.createIRI(ed.getPayloads().get(0).split("\\|")[2])));
		// fake fixed foi

		graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.ssnPrefix + "featureOfInterest"), instance.createIRI("http://iot.ee.surrey.ac.uk/citypulse/datasets/aarhusculturalevents/culturalEvents_aarhus#context_do63jk2t8c3bjkfb119ojgkhs7")));


		IRI xsdDouble = instance.createIRI("http://www.w3.org/2001/XMLSchema#double");
		BlankNode coordinates = instance.createBlankNode();
		graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.saoPrefix + "hasValue"), coordinates));
		graph.add(instance.createTriple(coordinates, instance.createIRI(RDFFileManager.ctPrefix + "hasLatitude"), instance.createLiteral(Double.toString(lat), xsdDouble)));
		graph.add(instance.createTriple(coordinates, instance.createIRI(RDFFileManager.ctPrefix + "hasLongitude"), instance.createLiteral(Double.toString(lon), xsdDouble)));
		// System.out.println("transformed: " + m.listStatements().toList().size());s
		return graph;
	}

	//@Override
	protected SensorObservation createObservation(Object data) {
		String str = data.toString();
		String userStr = str.split("\\|")[0];
		String coordinatesStr = str.split("\\|")[1];
		SensorObservation so = new SensorObservation();
		so.setFoi(userStr);
		// so.setServiceId(this.getURI());
		so.setValue(coordinatesStr);
		so.setObTimeStamp(new Date());
		so.setObId("UserLocationObservation-" + (int) Math.random() * 10000);
		// return so;
		//this.currentObservation = so;
		return so;
	}

	public void setWritable(DataStreamImpl<org.apache.jena.graph.Graph> e) {
		this.s = e;
	}
}
