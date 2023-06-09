package com.groupfour.testcoveragetool.group.elasticsearch;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupfour.testcoveragetool.controller.CoverageController;
import com.groupfour.testcoveragetool.controller.EndpointInfo;
import org.apache.http.HttpHost;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.*;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.groupfour.testcoveragetool.group.APIType;

public class ElasticSearchReader {

	private boolean debugMode;
	private int timeDeltaSec;
	private static final String ELASTICHOST = "192.168.3.122";
	private static final int ELASTICPORT = 9200;
	private static final String INDEXNAME = "jaeger-span-*";
	
	@SuppressWarnings("deprecation")
	private RestHighLevelClient rhlc = new RestHighLevelClient(RestClient.builder(new HttpHost(ELASTICHOST, ELASTICPORT)));
	private SearchRequest request = new SearchRequest(INDEXNAME); //match all indices
	private SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


	public void ElasticSearch(boolean debug, int timeDeltaSeconds) {
		ElasticSearch();
		debugMode = debug;
		timeDeltaSec = timeDeltaSeconds;
	}
	
	public void ElasticSearch() {
		debugMode = false;
		timeDeltaSec = 0;
	}

	public boolean isDebug() {
		return debugMode;
	}
	
	public ElasticSearchReader setDebugMode(boolean debug) {
		debugMode = debug;
		
		return this;
	}


	/**
	 * gets the endpoints hit in a specific time window (automatically corrects the window based on timeDeltaSeconds)
	 * calls getLogsInTimeRange,
	 * @param from start time
	 * @param to end time
	 * @return a HashSet of all the endpoints hit in the time window
	 * @throws IOException
	 * @throws Exception
	 */
	public HashSet<String> getEndpointsHit(Date from, Date to) throws IOException, Exception {
		List<String> logs = getLogsInTimeRange(from, to);

		HashSet<String> l = new HashSet<>(logs);

		CoverageController.setSelenium(EndpointInfo.convertFromStrings(l));
		System.err.println("hit setter");

		CoverageController.setMavenLock(false);

		return l;
	}

	/**
	 * gets all Endpoints within a specific time range, including duplicates, for all regexes provided
	 * @param start this system's start time
	 * @param stop this system's stop time
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	private List<String> getLogsInTimeRange(Date start, Date stop) throws IOException, Exception {
		List<String> logs = new ArrayList<String>();
		logs.addAll(getLogs(start, stop));
		
		return logs;
	}


	/**
	 * gets all logs within a time range for a given regex
	 * @param start
	 * @param stop
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	private List<String> getLogs(Date start, Date stop) throws IOException, Exception {

		//fix the timestamps using the delta
		long startTime = start.toInstant().toEpochMilli();
		startTime += (timeDeltaSec * 1000) ;


		long endTime = stop.toInstant().toEpochMilli();
		endTime += (timeDeltaSec * 1000);


		List<String> restLogs = queryLogs(startTime, endTime);

		if(isDebug()) {
			for (String s : restLogs) {
				System.out.println(s);
			}

			System.out.println("Number of endpoints: " + restLogs.size());
		}

		rhlc.close();
		return restLogs;
	}


	private  List<String> queryLogs(long start, long stop) throws Exception {
		List<String> restLogs = new ArrayList<String>();
		QueryBuilder queryBuilder = buildQuery(start, stop);

		searchSourceBuilder.query(queryBuilder);
		//retrieve the maximum number of logs
		searchSourceBuilder.size(10000);
		request.source(searchSourceBuilder);

		SearchResponse searchResponse = rhlc.search(request, RequestOptions.DEFAULT); //perform the request
		SearchHits hits = searchResponse.getHits();

		for (SearchHit hit : hits) {
			String sourceAsString = hit.getSourceAsString();
			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, Object> sourceAsMap = objectMapper.readValue(sourceAsString, new TypeReference<Map<String, Object>>() {});
			List<Map<String, Object>> tags = (List<Map<String, Object>>) sourceAsMap.get("tags");

			String method = null;
			String url = null;

			for (Map<String, Object> tag : tags) {
				String key = (String) tag.get("key");
				if (key.equals("http.method")) {
					method = (String) tag.get("value");
				} else if (key.equals("http.url")) {
					url = (String) tag.get("value");
				}

				if (method != null && url != null) {
					break;
				}
			}

			if (method != null && url != null) {
				String methodUrl = method + " " + url;
				restLogs.add(shortenURL(methodUrl));
			}
		}

		return restLogs;
	}

	private QueryBuilder buildQuery(long startTime, long endTime) {
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(QueryBuilders.nestedQuery("tags", QueryBuilders.termQuery("tags.key", "http.method"), ScoreMode.None))
				.must(QueryBuilders.nestedQuery("tags", QueryBuilders.termQuery("tags.key", "http.url"), ScoreMode.None))
				.filter(QueryBuilders.rangeQuery("startTimeMillis").gte(startTime).lte(endTime));

		return queryBuilder;
	}


	private String shortenURL(String s) throws Exception {
		String[] parts = s.split("\\s+");
		String method = parts[0];
		String url = parts[1].replaceAll(" ", "%20");
		URI uri = new URI(url);
		String path = uri.getPath();
		return method + " " + path;
	}

}
