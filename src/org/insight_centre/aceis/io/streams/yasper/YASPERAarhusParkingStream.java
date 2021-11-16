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
import org.insight_centre.aceis.observations.AarhusParkingObservation;
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

public class YASPERAarhusParkingStream extends YASPERSensorStream implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	CsvReader streamData;
	EventDeclaration ed;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
	private Date startDate = null;
	private Date endDate = null;

	public YASPERAarhusParkingStream(String uri, String txtFile, EventDeclaration ed) throws IOException {
		super(uri);
		streamData = new CsvReader(String.valueOf(txtFile));
		this.ed = ed;
		streamData.setTrimWhitespace(false);
		streamData.setDelimiter(',');
		streamData.readHeaders();
	}

	public YASPERAarhusParkingStream(String uri, String txtFile, EventDeclaration ed, Date start, Date end)
			throws IOException {
		super(uri);
		logger.info("init");
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
				Date obTime = sdf.parse(streamData.get("updatetime"));
				if (this.startDate != null && this.endDate != null) {
					if (obTime.before(this.startDate) || obTime.after(this.endDate)) {
						logger.debug(this.stream_uri + ": Disgarded observation @" + obTime);
						continue;
					}
				}
				// logger.debug("Reading data: " + streamData.toString());
				AarhusParkingObservation po = (AarhusParkingObservation) this.createObservation(streamData);
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
				// messageByte += st.toString().getBytes().length;
				try {
					if (this.getRate() == 1.0)
						Thread.sleep(sleep);
				} catch (Exception e) {

					e.printStackTrace();
					this.stop();
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Unexpected thread termination");

		} finally {
			logger.info("Stream Terminated: " + this.stream_uri);
			this.stop();
		}

	}

	@Override
	protected Graph getGraph(SensorObservation so) throws NumberFormatException, IOException {
		org.apache.commons.rdf.api.RDF instance = RDFUtils.getInstance();
		Graph graph = instance.createGraph();
		IRI observation = instance.createIRI(RDFFileManager.defaultPrefix + so.getObId() + UUID.randomUUID());
		CityBench.obMap.put(observation.toString(), so);
		graph.add(instance.createTriple(observation, instance.createIRI(RDF.type.getURI()), instance.createIRI(RDFFileManager.ssnPrefix + "Observation")));

		graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.ssnPrefix + "observedBy"), instance.createIRI(ed.getServiceId())));
		graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.ssnPrefix + "observedProperty"), instance.createIRI(ed.getPayloads().get(0).split("\\|")[2])));
		graph.add(instance.createTriple(observation, instance.createIRI(RDFFileManager.saoPrefix + "hasValue"), instance.createLiteral(Double.toString(((AarhusParkingObservation) so).getVacancies()), instance.createIRI("http://www.w3.org/2001/XMLSchema#double"))));
		return graph;
	}

	@Override
	protected SensorObservation createObservation(Object data) {
		try {
			// CsvReader streamData = (CsvReader) data;
			int vehicleCnt = Integer.parseInt(streamData.get("vehiclecount")), id = Integer.parseInt(streamData
					.get("_id")), total_spaces = Integer.parseInt(streamData.get("totalspaces"));
			String garagecode = streamData.get("garagecode");
			Date obTime = sdf.parse(streamData.get("updatetime"));
			AarhusParkingObservation apo = new AarhusParkingObservation(total_spaces - vehicleCnt, garagecode, "", 0.0,
					0.0);
			apo.setObTimeStamp(obTime);
			// logger.info("Annotating obTime: " + obTime + " in ms: " + obTime.getTime());
			apo.setObId("AarhusParkingObservation-" + id);
			logger.debug(ed.getServiceId() + ": streaming record @" + apo.getObTimeStamp());
			DataWrapper.waitForInterval(this.currentObservation, apo, this.startDate, getRate());
			this.currentObservation = apo;
			return apo;
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			logger.error("ed parse error: " + ed.getServiceId());
			e.printStackTrace();
		}
		return null;

	}
	public void setWritable(DataStreamImpl<org.apache.jena.graph.Graph> e) {
		this.s = e;
	}
}
