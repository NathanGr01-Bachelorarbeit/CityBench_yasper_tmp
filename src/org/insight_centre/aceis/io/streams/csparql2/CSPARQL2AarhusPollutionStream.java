package org.insight_centre.aceis.io.streams.csparql2;

import com.csvreader.CsvReader;
import it.polimi.yasper.core.stream.data.DataStreamImpl;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.insight_centre.aceis.eventmodel.EventDeclaration;
import org.insight_centre.aceis.io.rdf.RDFFileManager;
import org.insight_centre.aceis.io.streams.DataWrapper;
import org.insight_centre.aceis.observations.PollutionObservation;
import org.insight_centre.aceis.observations.SensorObservation;
import org.insight_centre.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class CSPARQL2AarhusPollutionStream extends CSPARQL2SensorStream implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	CsvReader streamData;
	EventDeclaration ed;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
	private Date startDate = null;
	private Date endDate = null;


	public CSPARQL2AarhusPollutionStream(String uri, String txtFile, EventDeclaration ed) throws IOException {
		super(uri);
		streamData = new CsvReader(String.valueOf(txtFile));
		this.ed = ed;
		streamData.setTrimWhitespace(false);
		streamData.setDelimiter(',');
		streamData.readHeaders();
	}

	public CSPARQL2AarhusPollutionStream(String uri, String txtFile, EventDeclaration ed, Date start, Date end)
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
				Model model = this.getModel(po);
				try {
					this.s.put((org.apache.jena.graph.Graph) model.getGraph(), System.currentTimeMillis());
					logger.debug(this.stream_uri + " Streaming: " + model.getGraph().toString());

				} catch (Exception e) {
					e.printStackTrace();
					logger.error(this.stream_uri + " YASPER streamming error.");
				}
				CityBench.pm.addNumberOfStreamedStatements(model.listStatements().toList().size());
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
	protected Model getModel(SensorObservation so) {
		Model m = ModelFactory.createDefaultModel();
		if (ed != null)
			for (String s : ed.getPayloads()) {
				Resource observation = m
						.createResource(RDFFileManager.defaultPrefix + so.getObId() + UUID.randomUUID());
				// so.setObId(RDFFileManager.defaultPrefix + observation.toString());
				CityBench.obMap.put(observation.toString(), so);
				observation.addProperty(RDF.type, m.createResource(RDFFileManager.ssnPrefix + "Observation"));

				Resource serviceID = m.createResource(ed.getServiceId());
				observation.addProperty(m.createProperty(RDFFileManager.ssnPrefix + "observedBy"), serviceID);
				observation.addProperty(m.createProperty(RDFFileManager.ssnPrefix + "observedProperty"),
						m.createResource(s.split("\\|")[2]));
				Property hasValue = m.createProperty(RDFFileManager.saoPrefix + "hasValue");
				observation.addLiteral(hasValue, ((PollutionObservation) so).getApi());
			}
		return m;
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
