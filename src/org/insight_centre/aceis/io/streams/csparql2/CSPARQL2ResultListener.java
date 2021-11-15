package org.insight_centre.aceis.io.streams.csparql2;

import it.polimi.sr.rsp.csparql.sysout.ConstructSysOutDefaultFormatter;
import it.polimi.sr.rsp.csparql.sysout.SelectSysOutDefaultFormatter;
import it.polimi.yasper.core.format.QueryResultFormatter;
import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.FmtUtils;
import org.insight_centre.aceis.observations.SensorObservation;
import org.insight_centre.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CSPARQL2ResultListener extends QueryResultFormatter {
	private String uri;
	private static final Logger logger = LoggerFactory.getLogger(CSPARQL2ResultListener.class);
	public static Set<String> capturedObIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	public static Set<String> capturedResults = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());


	private final SelectSysOutDefaultFormatter sf;
	private final ConstructSysOutDefaultFormatter cf;
	long last_result = -1L;

	public CSPARQL2ResultListener(String string, String format, boolean distinct) {
		super(format, distinct);
		this.cf = new ConstructSysOutDefaultFormatter(format, distinct);
		this.sf = new SelectSysOutDefaultFormatter(format, distinct);
		setUri(string);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof Binding) {
			this.sf.format((Binding)arg);
		} else if (arg instanceof Graph) {
			this.cf.format((Graph)arg);
		} else if (arg instanceof Table) {
			Table result = (Table)arg;

			List<String> names = result.getVarNames();
			//logger.info(Arrays.toString(names.toArray()));
			List<Integer> indexes = new ArrayList<Integer>();
			Map<String, Long> latencies = new HashMap<String, Long>();
			for (int i = 0; i < names.size(); i++) {
				if (names.get(i).contains("obId"))
					indexes.add(i);
			}
			// logger.info("Indexes: " + indexes);
			int cnt = 0;
			QueryIterator qIter = result.iterator(null) ;
			for (; qIter.hasNext();) {
				Binding binding = qIter.nextBinding() ;

				String resultString = "";
				Iterator iter = binding.vars();

				while(iter.hasNext()) {
					resultString += FmtUtils.stringForNode(binding.get((Var) iter.next())).replaceAll("\t", " ").trim() + " ";
				}
				//logger.info("ResultsString: " + resultString + "\n");

				/*if (capturedResults.contains(resultString)) {
					logger.debug("Early return");
					continue;
				}*/
				//capturedResults.add(resultString);

				String[] resultArr = resultString.split(" ");

				cnt += 1;
				for (int i : indexes) {
					// String obid = t.get(i);
					String obid = resultArr[i].substring(1, resultArr[i].length()-1);
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
						}
						else {
							long creationTime = so.getSysTimestamp().getTime();
							latencies.put(obid, (System.currentTimeMillis() - creationTime));
						}
					}
				}
			}
			if (cnt > 0)
				CityBench.pm.addResults(getUri(), latencies, cnt, capturedObIds.size());

			// System.out.println();
		}
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

}
