package org.insight_centre.aceis.io.streams.yasper;

import com.csvreader.CsvReader;
import it.polimi.yasper.core.stream.data.DataStreamImpl;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.insight_centre.aceis.eventmodel.EventDeclaration;
import org.insight_centre.aceis.io.rdf.RDFFileManager;
import org.insight_centre.aceis.io.streams.DataWrapper;
import org.insight_centre.aceis.observations.AarhusTrafficObservation;
import org.insight_centre.aceis.observations.PollutionObservation;
import org.insight_centre.aceis.observations.SensorObservation;
import org.insight_centre.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.streamreasoning.rsp4j.api.RDFUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class YASPERAarhusPollutionStream extends YASPERSensorStream implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	CsvReader streamData;
	EventDeclaration ed;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
	private Date startDate = null;
	private Date endDate = null;


	public YASPERAarhusPollutionStream(String uri, String txtFile, EventDeclaration ed) throws IOException {
		super(uri);
		streamData = new CsvReader(String.valueOf(txtFile));
		this.ed = ed;
		streamData.setTrimWhitespace(false);
		streamData.setDelimiter(',');
		streamData.readHeaders();
	}

	public YASPERAarhusPollutionStream(String uri, String txtFile, EventDeclaration ed, Date start, Date end)
			throws IOException {
		super(uri);

		streamData = new CsvReader(String.valueOf(txtFile));
		this.ed = ed;
		streamData.setTrimWhitespace(false);
		streamData.setDelimiter(',');
		streamData.readHeaders();
		this.startDate = start;
		this.endDate = end;
	}

	@Override
	public void run() {
		logger.info("Starting sensor stream: " + this.stream_uri);
		try {
			while (streamData.readRecord() && !stop) {
				Date obTime = sdf.parse(streamData.get("timestamp"));
				if (this.startDate != null && this.endDate != null) {
					if (obTime.before(this.startDate) || obTime.after(this.endDate)) {
						logger.debug(this.stream_uri + ": Disgarded observation @" + obTime);
						continue;
					}
				}
				// logger.info("Reading data: " + streamData.toString());
				PollutionObservation po = (PollutionObservation) this.createObservation(streamData);
				// logger.debug("Reading data: " + new Gson().toJson(po));
				Graph graph = getGraph(po);
				try {
					this.put(graph, System.currentTimeMillis());
					//new Thread(new Putter(this, graph)).start();

					logger.debug(this.stream_uri + " Streaming: " + graph.toString());

				} catch (Exception e) {
					e.printStackTrace();
					logger.error(this.stream_uri + " YASPER streamming error.");
				}
				CityBench.pm.addNumberOfStreamedStatements((int)graph.size());
				try {
					if (this.getRate() == 1.0)
						Thread.sleep(sleep);
				} catch (Exception e) {

					e.printStackTrace();
					this.stop();
				}

			}
		} catch (Exception e) {
			logger.error("Unexpected thread termination");
			e.printStackTrace();

		} finally {
			logger.info("Stream Terminated: " + this.stream_uri);
			this.stop();
		}

	}

	@Override
	protected Graph getGraph(SensorObservation so) {
		org.apache.commons.rdf.api.RDF instance = RDFUtils.getInstance();
		Graph graph = instance.createGraph();
		if (ed != null)
			for (String s : ed.getPayloads()) {
				IRI observation = instance.createIRI(RDFFileManager.defaultPrefix + so.getObId() + UUID.randomUUID());
				// so.setObId(RDFFileManager.defaultPrefix + observation.toString());
				CityBench.obMap.put(observation.toString(), so);
				graph.add(instance.createTriple(observation, instance.createIRI(RDF.type.getURI()), instance.createIRI(RDFFileManager.ssnPrefix + "Observation")));

				graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.ssnPrefix + "observedBy"), instance.createIRI(ed.getServiceId())));
				graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.ssnPrefix + "observedProperty"), instance.createIRI(s.split("\\|")[2])));
				graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.saoPrefix + "hasValue"), instance.createLiteral(Double.toString((((PollutionObservation) so).getApi())), instance.createIRI("http://www.w3.org/2001/XMLSchema#double"))));
			}
		return graph;
	}

	@Override
	protected SensorObservation createObservation(Object data) {
		try {
			// CsvReader streamData = (CsvReader) data;
			int ozone = Integer.parseInt(streamData.get("ozone")), particullate_matter = Integer.parseInt(streamData
					.get("particullate_matter")), carbon_monoxide = Integer.parseInt(streamData.get("carbon_monoxide")), sulfure_dioxide = Integer
					.parseInt(streamData.get("sulfure_dioxide")), nitrogen_dioxide = Integer.parseInt(streamData
					.get("nitrogen_dioxide"));
			Date obTime = sdf.parse(streamData.get("timestamp"));
			PollutionObservation po = new PollutionObservation(0.0, 0.0, 0.0, ozone, particullate_matter,
					carbon_monoxide, sulfure_dioxide, nitrogen_dioxide, obTime);
			// logger.debug(ed.getServiceId() + ": streaming record @" + po.getObTimeStamp());
			po.setObId("AarhusPollutionObservation-" + (int) Math.random() * 10000);
			DataWrapper.waitForInterval(currentObservation, po, startDate, getRate());
			this.currentObservation = po;
			return po;
		} catch (NumberFormatException | IOException | ParseException e) {
			e.printStackTrace();
		}
		return null;

	}
	public void setWritable(DataStreamImpl<org.apache.jena.graph.Graph> e) {
		this.s = e;
	}
}
