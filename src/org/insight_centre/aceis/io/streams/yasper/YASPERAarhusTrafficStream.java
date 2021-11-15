package org.insight_centre.aceis.io.streams.yasper;

import com.csvreader.CsvReader;
import it.polimi.yasper.core.stream.data.DataStreamImpl;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.simple.Types;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.insight_centre.aceis.eventmodel.EventDeclaration;
import org.insight_centre.aceis.eventmodel.TrafficReportService;
import org.insight_centre.aceis.io.rdf.RDFFileManager;
import org.insight_centre.aceis.io.streams.DataWrapper;
import org.insight_centre.aceis.observations.AarhusTrafficObservation;
import org.insight_centre.aceis.observations.SensorObservation;
import org.insight_centre.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.streamreasoning.rsp4j.api.RDFUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class YASPERAarhusTrafficStream extends YASPERSensorStream implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(YASPERAarhusTrafficStream.class);
	static long time1;
	EventDeclaration ed;
	private boolean forJWSTest = false;
	private List<String> lines = new ArrayList<String>();
	private long messageCnt, byteCnt;
	String p1Street, p1City, p1Lat, p1Lon, p2Street, p2City, p2Lat, p2Lon, p1Country, p2Country, distance, id;
	// private QosSimulationMode qosSimulationMode = QosSimulationMode.none;
	// long sleep = 1000; // default frequency is 1.0
	// boolean stop = false;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd-k-m-s");
	private Date startDate = null, endDate = null;
	CsvReader streamData, metaData;
	private List<String> subscribers = new ArrayList<String>();
	String txtFile;
	private int cnt = 0;

	public YASPERAarhusTrafficStream(String uri, String txtFile, EventDeclaration ed) throws IOException {
		super(uri);
		String fileName = "";
		messageCnt = 0;
		byteCnt = 0;
		this.txtFile = txtFile;
		this.ed = ed;
		// time1 = time.getTime();
		streamData = new CsvReader(String.valueOf(txtFile));
		streamData.setTrimWhitespace(false);
		streamData.setDelimiter(',');
		streamData.readHeaders();
		// streamData.skipRecord();
		metaData = new CsvReader("dataset/MetaData/trafficMetaData.csv");
		metaData.readHeaders();
		streamData.readRecord();
		while (metaData.readRecord()) {
			if (streamData.get("REPORT_ID").equals(metaData.get("REPORT_ID"))) {

				distance = metaData.get("DISTANCE_IN_METERS");
				if (ed instanceof TrafficReportService)
					((TrafficReportService) ed).setDistance(Integer.parseInt(distance));


				metaData.close();
				break;
			}
		}
	}

	public YASPERAarhusTrafficStream(String uri, String txtFile, EventDeclaration ed, Date start, Date end)
			throws IOException {
		super(uri);
		logger.info("IRI: " + this.stream_uri.split("#")[1] + ed.getInternalQos());
		this.startDate = start;
		this.endDate = end;
		messageCnt = 0;
		byteCnt = 0;
		this.txtFile = txtFile;
		this.ed = ed;
		streamData = new CsvReader(String.valueOf(txtFile));
		streamData.setTrimWhitespace(false);
		streamData.setDelimiter(',');
		streamData.readHeaders();
		metaData = new CsvReader("dataset/MetaData/trafficMetaData.csv");
		metaData.readHeaders();
		streamData.readRecord();
		while (metaData.readRecord()) {
			if (streamData.get("REPORT_ID").equals(metaData.get("REPORT_ID"))) {

				distance = metaData.get("DISTANCE_IN_METERS");
				if (ed instanceof TrafficReportService)
					((TrafficReportService) ed).setDistance(Integer.parseInt(distance));

				metaData.close();
				break;
			}
		}
	}

	public synchronized void addSubscriber(String s) {
		this.subscribers.add(s);
	}

	@Override
	protected SensorObservation createObservation(Object objData) {
		// SensorObservation so = DataWrapper.getAarhusTrafficObservation((CsvReader) objData, ed);
		// DataWrapper.waitForInterval(currentObservation, so, startDate, getRate());
		// this.currentObservation = so;
		// return so;
		try {
			// CsvReader streamData = (CsvReader) objData;
			AarhusTrafficObservation data;
			// if (!this.txtFile.contains("mean"))
			data = new AarhusTrafficObservation(Double.parseDouble(streamData.get("REPORT_ID")),
					Double.parseDouble(streamData.get("avgSpeed")), Double.parseDouble(streamData.get("vehicleCount")),
					Double.parseDouble(streamData.get("avgMeasuredTime")), 0, 0, null, null, 0.0, 0.0, null, null, 0.0,
					0.0, null, null, streamData.get("TIMESTAMP"));
			String obId = "AarhusTrafficObservation-" + streamData.get("_id");
			Double distance = Double.parseDouble(((TrafficReportService) ed).getDistance() + "");
			if (data.getAverageSpeed() != 0)
				data.setEstimatedTime(distance / data.getAverageSpeed());
			else
				data.setEstimatedTime(-1.0);
			if (distance != 0)
				data.setCongestionLevel(data.getVehicle_count() / distance);
			else
				data.setCongestionLevel(-1.0);
			data.setObId(obId);
			DataWrapper.waitForInterval(currentObservation, data, startDate, getRate());
			this.currentObservation = data;
			return data;
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Date getEndDate() {
		return endDate;
	}

	// public QosSimulationMode getQosSimulationMode() {
	// return qosSimulationMode;
	// }

	public Date getStartDate() {
		return startDate;
	}

	@Override
	protected Graph getGraph(SensorObservation data) throws NumberFormatException, IOException {
		// return DataWrapper.getAarhusTrafficStatements((AarhusTrafficObservation) data, ed);
		org.apache.commons.rdf.api.RDF instance = RDFUtils.getInstance();
		Graph graph = instance.createGraph();
		if (ed != null)
			for (String pStr : ed.getPayloads()) {
				// if (s.contains("EstimatedTime")) {
				// Resource observedProperty = m.createResource(s);
				String obId = data.getObId();
				IRI observation = instance.createIRI(RDFFileManager.defaultPrefix + obId + UUID.randomUUID());
				CityBench.obMap.put(observation.toString(), data);
				// data.setObId(observation.toString());
				// System.out.println("OB: " + observation.toString());
				IRI xsdDouble = instance.createIRI("http://www.w3.org/2001/XMLSchema#double");
				graph.add(instance.createTriple(observation, instance.createIRI(RDF.type.getURI()), instance.createIRI(RDFFileManager.ssnPrefix + "Observation")));
				graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.ssnPrefix + "observedBy"), instance.createIRI(ed.getServiceId())));
				graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.ssnPrefix + "observedProperty"), instance.createIRI(pStr.split("\\|")[2])));

				// System.out.println("Annotating: " + observedProperty.toString());
				if (pStr.contains("AvgSpeed"))
					graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.saoPrefix + "hasValue"), instance.createLiteral(Double.toString(((AarhusTrafficObservation) data).getAverageSpeed()), xsdDouble)));
				else if (pStr.contains("VehicleCount")) {
					graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.saoPrefix + "hasValue"), instance.createLiteral(Double.toString(((AarhusTrafficObservation) data).getVehicle_count()), xsdDouble)));
				} else if (pStr.contains("MeasuredTime"))
					graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.saoPrefix + "hasValue"), instance.createLiteral(Double.toString(((AarhusTrafficObservation) data).getAvgMeasuredTime()), xsdDouble)));
				else if (pStr.contains("EstimatedTime"))
					graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.saoPrefix + "hasValue"), instance.createLiteral(Double.toString(((AarhusTrafficObservation) data).getEstimatedTime()), xsdDouble)));
				else if (pStr.contains("CongestionLevel"))
					graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.saoPrefix + "hasValue"), instance.createLiteral(Double.toString(((AarhusTrafficObservation) data).getCongestionLevel()), xsdDouble)));
				// break;
				// }
			}
		return graph;
	}

	public boolean isForJWSTest() {
		return forJWSTest;
	}

	public void run() {
		logger.info("Starting sensor stream: " + this.stream_uri + " " + this.startDate + ", " + this.endDate
				+ " distance: " + ((TrafficReportService) this.ed).getDistance());
		// logger.info("EventDeclaration: " + this.ed);
		try {
			// Reads csv document for traffic metadata
			boolean completed = false;
			while (streamData.readRecord() && !stop) {

				Date obTime;

				if (!this.txtFile.contains("mean"))
					obTime = sdf.parse(streamData.get("TIMESTAMP"));
				else
					obTime = sdf2.parse(streamData.get("startTime"));
				// logger.info("obTime: " + obTime);
				logger.debug("Reading data: " + streamData.toString());
				if (this.startDate != null && this.endDate != null) {
					if (obTime.before(this.startDate) || obTime.after(this.endDate)) {
						//logger.debug(this.stream_uri + ": Disgarded observation observed at: " + obTime);
						continue;
					}
				}

				AarhusTrafficObservation data = (AarhusTrafficObservation) this.createObservation(streamData);
				cnt += 1;
				if (cnt >= 1000)
					try {
						if (!completed) {
							logger.info("My mission completed: " + this.stream_uri);
							completed = true;
						}
						Thread.sleep(sleep);
						continue;
					} catch (InterruptedException e) {

						e.printStackTrace();

					}
				Graph graph = this.getGraph(data);
				long messageByte = 0;
				try {
					this.put(graph, System.currentTimeMillis());
					//logger.debug(this.stream_uri + " Streaming: " + model.getGraph().toString());

				} catch (Exception e) {
					e.printStackTrace();
					logger.error(this.stream_uri + " YASPER streamming error.");
					messageByte += graph.toString().getBytes().length;
				}
				CityBench.pm.addNumberOfStreamedStatements((int)graph.size());

				this.messageCnt += 1;
				this.byteCnt += messageByte;
				if (sleep > 0) {
					try {
						if (this.getRate() == 1.0)
							Thread.sleep(sleep);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			logger.info("Stream Terminated: " + this.stream_uri + " total bytes sent: " + this.byteCnt);
			this.stop();
		}
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public void setForJWSTest(boolean forJWSTest) {
		this.forJWSTest = forJWSTest;
	}

	// public void setQosSimulationMode(QosSimulationMode qosSimulationMode) {
	// this.qosSimulationMode = qosSimulationMode;
	// }

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public void setWritable(DataStreamImpl<org.apache.jena.graph.Graph> e) {
		this.s = e;
	}
}