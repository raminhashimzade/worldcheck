package az.blacklist.person.verification.service;

import az.blacklist.person.verification.exceprion.PersonVerificationException;
import az.blacklist.person.verification.model.SourceSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.search.spell.StringDistance;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class FindPersonService<T> {

	private final Logger logger = LoggerFactory.getLogger(FindPersonService.class);

	protected final ObjectMapper objectMapper;
	protected final RestHighLevelClient restHighLevelClient;
	protected final StringDistance stringDistance;

	public FindPersonService(ObjectMapper objectMapper, RestHighLevelClient restHighLevelClient,
			StringDistance stringDistance) {
		this.objectMapper = objectMapper;
		this.restHighLevelClient = restHighLevelClient;
		this.stringDistance = stringDistance;
	}

	public abstract SourceSystem getSourceSystem();

	public List<T> findPeople(String fullName) {
		SourceSystem sourceSystem = getSourceSystem();
		logger.info("Find person {} in {} start", fullName, sourceSystem);
		List<T> foundPeople = new ArrayList<>();

		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.timeout(new TimeValue(600, TimeUnit.SECONDS));
		sourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
		sourceBuilder.size(10_000);
		BoolQueryBuilder query = new BoolQueryBuilder();

		query.must(new MultiMatchQueryBuilder(fullName, sourceSystem.getColumns().toArray(new String[0]))
				.cutoffFrequency(0.0001F).fuzziness(Fuzziness.ONE));

		sourceBuilder.query(query);
		SearchRequest searchRequest = new SearchRequest(sourceSystem.getIndexName());
		searchRequest.source(sourceBuilder);

		SearchResponse response;
		try {
			response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			logger.error("Can not perform search on {}", sourceSystem, e);
			throw new PersonVerificationException("Can not perform search");
		}

		response.getHits().forEach(h -> {
			try {
				foundPeople.add((T) objectMapper.readValue(h.getSourceAsString(), sourceSystem.getType()));
			} catch (IOException e) {
				logger.error("Can not read person  {} from {}", h, sourceSystem, e);
				throw new PersonVerificationException("Can not read person");
			}
		});

		logger.info("Find person {} in {} end", fullName, sourceSystem);
		return foundPeople;
	}
}
