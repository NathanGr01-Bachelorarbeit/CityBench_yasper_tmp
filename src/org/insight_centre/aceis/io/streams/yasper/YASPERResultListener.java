package org.insight_centre.aceis.io.streams.yasper;

import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.FmtUtils;
import org.insight_centre.aceis.observations.SensorObservation;
import org.insight_centre.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.streamreasoning.rsp4j.api.operators.s2r.execution.assigner.Consumer;
import org.streamreasoning.rsp4j.yasper.querying.operators.r2r.BindingImpl;
import org.streamreasoning.rsp4j.yasper.querying.operators.r2r.Var;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class YASPERResultListener<O> implements Consumer<O> {
	private String uri;
	private static final Logger logger = LoggerFactory.getLogger(YASPERResultListener.class);
	public static Set<String> capturedObIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	public static Set<String> capturedResults = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	private List<O> received = new ArrayList<O>();
	int cnt = 0;

	@Override
	public void notify(O event, long ts) {
		if (event instanceof BindingImpl) {
			BindingImpl result = (BindingImpl) event;

			Set<Var> variables = result.variables();
			//logger.info(Arrays.toString(names.toArray()));
			Set<Var> indexes = new HashSet<>();
			Map<String, Long> latencies = new HashMap<String, Long>();
			for (Var var : variables) {
				if (var.name().contains("obId"))
					indexes.add(var);
			}
			// logger.info("Indexes: " + indexes);

			cnt += 1;
			for (Var var : indexes) {
				// String obid = t.get(i);
				String obid = result.value(var).toString();
					/*try {
						FileOutputStream out = new FileOutputStream(new File("Test2.log"), true);
						out.write((obid + "\n").getBytes(StandardCharsets.UTF_8));
					}
					catch (Exception e) {
						e.printStackTrace();
					}*/
				//logger.info(obid);
				if (obid == null)
					logger.error("NULL ob Id detected.");
				if (!capturedObIds.contains(obid)) {
					capturedObIds.add(obid);
					//logger.info(obid);
					// uncomment for testing the completeness, i.e., showing how many observations are captured
					//logger.info("CSPARQL result arrived " + capturedResults.size() + ", obs size: " + capturedObIds.size() + ", result: " + result);
					SensorObservation so = CityBench.obMap.get(obid);
					if (so == null) {
						logger.info("Cannot find observation for: " + obid);
					} else {
						long creationTime = so.getSysTimestamp().getTime();
						latencies.put(obid, (System.currentTimeMillis() - creationTime));
					}

				}
			}
			if (cnt > 0)
				CityBench.pm.addResults(getUri(), latencies, cnt, capturedObIds.size());

			// System.out.println();
		}
	}

	public YASPERResultListener(String string) {
		super();
		setUri(string);
	}

	public void update(Observable o, Object arg) {

	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

}
