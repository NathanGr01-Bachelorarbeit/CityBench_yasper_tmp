package org.insight_centre.aceis.io.streams.csparql;

//import org.insight_centre.aceis.engine.ACEISEngine;

//import org.apache.log4j.Logger;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.insight_centre.aceis.observations.SensorObservation;
import org.insight_centre.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.larkc.csparql.common.RDFTable;
import eu.larkc.csparql.common.RDFTuple;
import eu.larkc.csparql.common.streams.format.GenericObservable;
import eu.larkc.csparql.engine.RDFStreamFormatter;

public class CSPARQLResultObserver extends RDFStreamFormatter {
	private static final Logger logger = LoggerFactory.getLogger(CSPARQLResultObserver.class);
	public static Set<String> capturedObIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	public static Set<String> capturedResults = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

	public CSPARQLResultObserver(String iri) {
		super(iri);
		// TODO Auto-generated constructor stub
	}

	public void update(final GenericObservable<RDFTable> observed, final RDFTable q) {
		List<String> names = new ArrayList(q.getNames());
		//logger.info(Arrays.toString(names.toArray()));
		List<Integer> indexes = new ArrayList<Integer>();
		Map<String, Long> latencies = new HashMap<String, Long>();
		for (int i = 0; i < names.size(); i++) {
			if (names.get(i).contains("obId"))
				indexes.add(i);
		}
		// logger.info("Indexes: " + indexes);
		int cnt = 0;
		for (final RDFTuple t : q) {
			String result = t.toString().replaceAll("\t", " ").trim();
			//logger.info(this.getIRI() + " Results: " + result);
			/*try (FileOutputStream fileOutputStream = new FileOutputStream("out.txt", true)) {
				fileOutputStream.write((t.get(0) + ",  " + t.get(1) + ",  " + t.get(2) + ",  " + t.get(3) + ",  " + t.get(4) + ",  " + t.get(5) + ",  " + t.get(6) + ",  " + t.get(7) + t.get(8) +  "\n").getBytes(StandardCharsets.UTF_8));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}*/
			if (capturedResults.contains(result)) {
				continue;
			}
			capturedResults.add(result);

			String[] resultArr = result.split(" ");
			cnt += 1;
			for (int i : indexes) {
				// String obid = t.get(i);
				String obid = resultArr[i];
				try {
					FileOutputStream out = new FileOutputStream(new File("Test2.log"), true);
					out.write((obid + "\n").getBytes(StandardCharsets.UTF_8));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				//logger.info(obid);
				if (obid == null)
					logger.error("NULL ob Id detected.");
				if (!capturedObIds.contains(obid)) {
					capturedObIds.add(obid);
					//logger.info(obid);
					// uncomment for testing the completeness, i.e., showing how many observations are captured
					//logger.info("CSPARQL result arrived " + capturedResults.size() + ", obs size: " + capturedObIds.size() + ", result: " + result);
					try {
						SensorObservation so = CityBench.obMap.get(obid);
						if (so == null)
							logger.error("Cannot find observation for: " + obid);
						long creationTime = so.getSysTimestamp().getTime();
						latencies.put(obid, (System.currentTimeMillis() - creationTime));
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}

		}
		if (cnt > 0)
			CityBench.pm.addResults(getIRI(), latencies, cnt);

		// System.out.println();

	}
}
