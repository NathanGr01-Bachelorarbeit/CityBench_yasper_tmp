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
import org.insight_centre.aceis.observations.SensorObservation;
import org.insight_centre.aceis.observations.WeatherObservation;
import org.insight_centre.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.streamreasoning.rsp4j.api.RDFUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class YASPERAarhusWeatherStream extends YASPERSensorStream implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	CsvReader streamData;
	EventDeclaration ed;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private Date startDate = null;
	private Date endDate = null;

	public YASPERAarhusWeatherStream(String uri, String txtFile, EventDeclaration ed) throws IOException {
		super(uri);
		streamData = new CsvReader(String.valueOf(txtFile));
		this.ed = ed;
		streamData.setTrimWhitespace(false);
		streamData.setDelimiter(',');
		streamData.readHeaders();
	}

	public YASPERAarhusWeatherStream(String uri, String txtFile, EventDeclaration ed, Date start, Date end)
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
				// logger.info("Reading: " + streamData.toString());
				Date obTime = sdf.parse(streamData.get("TIMESTAMP").toString());
				if (this.startDate != null && this.endDate != null) {
					if (obTime.before(this.startDate) || obTime.after(this.endDate)) {
						logger.debug(this.stream_uri + ": Disgarded observation @" + obTime);
						continue;
					}
				}
				// logger.info("Reading data: " + streamData.toString());
				WeatherObservation po = (WeatherObservation) this.createObservation(streamData);
				// logger.debug("Reading data: " + new Gson().toJson(po));
				Graph graph = getGraph(po);
				try {
					this.put(graph, System.currentTimeMillis());
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
	protected Graph getGraph(SensorObservation wo) throws NumberFormatException, IOException {
		org.apache.commons.rdf.api.RDF instance = RDFUtils.getInstance();
		Graph graph = instance.createGraph();
		if (ed != null)
			for (String s : ed.getPayloads()) {
				IRI observation = instance.createIRI(RDFFileManager.defaultPrefix + wo.getObId() + UUID.randomUUID());

				CityBench.obMap.put(observation.toString(), wo);
				graph.add(instance.createTriple(observation, instance.createIRI(RDF.type.getURI()), instance.createIRI(RDFFileManager.ssnPrefix + "Observation")));
				graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.ssnPrefix + "observedBy"), instance.createIRI(ed.getServiceId())));
				graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.ssnPrefix + "observedProperty"), instance.createIRI(s.split("\\|")[2])));

				IRI xsdDouble = instance.createIRI("http://www.w3.org/2001/XMLSchema#double");
				IRI hasValue = instance.createIRI(RDFFileManager.saoPrefix + "hasValue");
				if (s.contains("Temperature"))
					graph.add(instance.createTriple(observation, hasValue, instance.createLiteral(Double.toString(((WeatherObservation) wo).getTemperature()), xsdDouble)));
				else if (s.toString().contains("Humidity"))
					graph.add(instance.createTriple(observation, hasValue, instance.createLiteral(Double.toString(((WeatherObservation) wo).getHumidity()), xsdDouble)));
				else if (s.toString().contains("WindSpeed"))
					graph.add(instance.createTriple(observation, hasValue, instance.createLiteral(Double.toString(((WeatherObservation) wo).getWindSpeed()), xsdDouble)));
			}
		return graph;
	}

	@Override
	protected SensorObservation createObservation(Object data) {
		try {
			// CsvReader streamData = (CsvReader) data;
			int hum = Integer.parseInt(streamData.get("hum"));
			double tempm = Double.parseDouble(streamData.get("tempm"));
			double wspdm = Double.parseDouble(streamData.get("wspdm"));
			Date obTime = sdf.parse(streamData.get("TIMESTAMP"));
			WeatherObservation wo = new WeatherObservation(tempm, hum, wspdm, obTime);
			logger.debug(ed.getServiceId() + ": streaming record @" + wo.getObTimeStamp());
			wo.setObId("AarhusWeatherObservation-" + (int) Math.random() * 1000);
			// this.currentObservation = wo;
			DataWrapper.waitForInterval(currentObservation, wo, startDate, getRate());
			this.currentObservation = wo;
			return wo;
		} catch (NumberFormatException | IOException | ParseException e) {
			e.printStackTrace();
		}
		return null;

	}
	public void setWritable(DataStreamImpl<org.apache.jena.graph.Graph> e) {
		this.s = e;
	}

}
