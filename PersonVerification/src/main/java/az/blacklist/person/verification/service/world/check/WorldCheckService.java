package az.blacklist.person.verification.service.world.check;

import az.blacklist.person.verification.model.SourceSystem;
import az.blacklist.person.verification.model.world.check.WorldCheckPerson;
import az.blacklist.person.verification.service.FindPersonService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.search.spell.StringDistance;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static az.blacklist.person.verification.model.SourceSystem.WORLD_CHECK;

@Service
public class WorldCheckService extends FindPersonService<WorldCheckPerson> {

	private static final Logger logger = LoggerFactory.getLogger(WorldCheckService.class);

	private static final String ELEMENT_RECORD = "record";
	private static final String ELEMENT_ALIASES = "aliases";
	private static final String ELEMENT_FIRST_NAME = "first_name";
	private static final String ELEMENT_LAST_NAME = "last_name";
	private static final String ELEMENT_DOB = "dob";
	private static final String ELEMENT_ALIAS = "alias";
	private static final String ATTRIBUTE_CATEGORY = "category";
	private static final String ATTRIBUTE_SUB_CATEGORY = "sub-category";
	private static final String ATTRIBUTE_UID = "uid";
	private static final String ELEMENT_UID = "uid";

	public WorldCheckService(ObjectMapper objectMapper, RestHighLevelClient restHighLevelClient,
			StringDistance stringDistance) {
		super(objectMapper, restHighLevelClient, stringDistance);
	}

	@Override
	public SourceSystem getSourceSystem() {
		return WORLD_CHECK;
	}

	public void processUpdateFile(File file) {
		logger.info("Started parsing file {} with update", file.getName());

		try (FileInputStream fileInputStream = new FileInputStream(file)) {

			XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(fileInputStream);

			WorldCheckPerson update = null;
			List<String> aliases = null;
			String tagContent = null;
			List<WorldCheckPerson> worldCheckPeople = new ArrayList<>();
			String firstName = null;
			String lastName = null;

			try {
				int n = 0;
				while (reader.hasNext()) {
					
					n += 1;
					if (n%1000000 == 0)
						logger.info("N rows update: {}", n);
					
					int event = reader.next();

					switch (event) {
					case XMLStreamConstants.START_ELEMENT:
						switch (reader.getLocalName()) {
						case ELEMENT_RECORD:
							update = new WorldCheckPerson();
							update.setCategory(reader.getAttributeValue(null, ATTRIBUTE_CATEGORY));
							update.setSubCategory(reader.getAttributeValue(null, ATTRIBUTE_SUB_CATEGORY));
							update.setUid(Long.valueOf(reader.getAttributeValue(null, ATTRIBUTE_UID)));
							break;
						case ELEMENT_ALIASES:
							aliases = new ArrayList<>();
							break;
						default:
							break;
						}
						break;

					case XMLStreamConstants.CHARACTERS:
						tagContent = reader.getText().trim();
						break;

					case XMLStreamConstants.END_ELEMENT:
						switch (reader.getLocalName()) {
						case ELEMENT_RECORD:
							logger.trace("parsed update: {}", update);
							Objects.requireNonNull(update).setFirstName(firstName);
							Objects.requireNonNull(update).setLastName(lastName);
							Objects.requireNonNull(update).setFullName1(String.format("%s %s", firstName, lastName));
							Objects.requireNonNull(update).setFullName2(String.format("%s %s", lastName, firstName));
							worldCheckPeople.add(update);
							update = null;
							aliases = null;
							firstName = null;
							lastName = null;
							break;
						case ELEMENT_FIRST_NAME:
							firstName = tagContent;
							break;
						case ELEMENT_LAST_NAME:
							lastName = tagContent;
							break;
						case ELEMENT_DOB:
							if (!StringUtils.isEmpty(tagContent)) {
								Objects.requireNonNull(update).setDateOfBirth(tagContent);
							}
							break;
						case ELEMENT_ALIASES:
							Objects.requireNonNull(update).setAliases(aliases);
							aliases = null;
							break;
						case ELEMENT_ALIAS:
							if (!StringUtils.isEmpty(tagContent) && aliases != null)
								aliases.add(tagContent);
							break;
						default:
							break;
						}
						tagContent = null;
						break;
					default:
						break;
					}
				}

				logger.info("{} world check persons proceed for update", worldCheckPeople.size());
				saveWorldCheckPeople(worldCheckPeople, file.getName());
			} catch (XMLStreamException e) {
				logger.error("Can not read person", e);
			}

		} catch (IOException | XMLStreamException e) {
			logger.error("Can not read person", e);
		}
	}

	public void processDeleteFile(File file) {
		logger.info("Started parsing file {} for delete", file.getName());

		try (FileInputStream fileInputStream = new FileInputStream(file)) {

			XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(fileInputStream);

			String tagContent = null;
			List<String> worldCheckPeople = new ArrayList<>();

			try {
				while (reader.hasNext()) {
					int event = reader.next();
					switch (event) {
//                        case XMLStreamConstants.START_ELEMENT:
//                            String s = reader.getLocalName();
//                            if (ELEMENT_PROFILE.equals(s)) {
//                                deletion = new WorldCheckPerson();
//                            }
//                            break;

					case XMLStreamConstants.CHARACTERS:
						tagContent = reader.getText().trim();
						break;

					case XMLStreamConstants.END_ELEMENT:
						switch (reader.getLocalName()) {
//                                case ELEMENT_PROFILE:
//                                    Objects.requireNonNull(deletion);
//                                    Objects.requireNonNull(deletion.getUid());
//                                    worldCheckPeople.add(deletion);
//                                    break;
						case ELEMENT_UID:
							if (StringUtils.isEmpty(tagContent)) {
								throw new AssertionError("uid must not be empty");
							}
							worldCheckPeople.add(tagContent);
//                                    Objects.requireNonNull(deletion)
//                                            .setUid(Long.parseLong(tagContent));
							break;
						default:
							break;
						}
						tagContent = null;
						break;
					default:
						break;
					}
				}

				logger.info("{} world check persons proceed for delete", worldCheckPeople.size());

				deleteWorldCheckPeople(worldCheckPeople, file.getName());
			} catch (XMLStreamException e) {
				logger.error("Can not read person", e);
			}

		} catch (IOException | XMLStreamException e) {
			logger.error("Can not read person", e);
		}

	}

	public List<WorldCheckPerson> findPeople(String fullName, double percentage) {
		logger.info("Find person {} in WORLD_CHECK", fullName);
		List<WorldCheckPerson> worldCheckPersonList = super.findPeople(fullName);

		worldCheckPersonList.removeIf(p -> stringDistance.getDistance(Objects.toString(p.getFullName1(), "").toLowerCase(), fullName.toLowerCase()) <= percentage
				&& stringDistance.getDistance(Objects.toString(p.getFullName2(), "").toLowerCase(), fullName.toLowerCase()) <= percentage
				&& p.getAliases().stream().allMatch(a -> stringDistance.getDistance(a, fullName.toLowerCase()) <= percentage));

		worldCheckPersonList.forEach(p -> {
			double d1 = p.getFullName1() != null
					? stringDistance.getDistance(p.getFullName1().toLowerCase(), fullName.toLowerCase())
					: 0;
			double d2 = p.getFullName2() != null
					? stringDistance.getDistance(p.getFullName2().toLowerCase(), fullName.toLowerCase())
					: 0;
			double d3 = p.getAliases().stream().mapToDouble(a -> stringDistance.getDistance(a, fullName.toLowerCase()))
					.max().orElse(0);

			double max = Math.max(d1, Math.max(d2, d3));
			p.setPercentage((int) (max * 100));
		});

		return worldCheckPersonList.stream().sorted(Comparator.comparing(WorldCheckPerson::getPercentage).reversed())
				.collect(Collectors.toList());
	}

	private void saveWorldCheckPeople(List<WorldCheckPerson> worldCheckPeople, String fileName) {
		logger.info("World Check start save people");

		final int chunkSize = 1_00;
		final AtomicInteger counter = new AtomicInteger();

		worldCheckPeople.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize)).values()
				.forEach(chunk -> {
					BulkRequest request = new BulkRequest();
					request.timeout(TimeValue.timeValueHours(1L));

					chunk.forEach(p -> {
						try {
							request.add(new IndexRequest(WORLD_CHECK.getIndexName(), "doc").id(p.getUid().toString())
									.source(objectMapper.writeValueAsString(p), XContentType.JSON));
						} catch (JsonProcessingException e) {
							logger.error("Can not save person p {}", p, e);
						}
					});

					try {
						restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
					} catch (IOException e) {
						logger.error("Can not save World Check file {}", fileName, e);
					}
				});

		logger.info("World Check end save people");
	}

	private void deleteWorldCheckPeople(List<String> worldCheckPeople, String fileName) {
		try {
			restHighLevelClient.deleteByQuery(new DeleteByQueryRequest(getSourceSystem().getIndexName())
					.setQuery(new TermsQueryBuilder("_id", worldCheckPeople)), RequestOptions.DEFAULT);
			logger.info("Records successfully deleted for file {}", fileName);
		} catch (IOException e) {
			logger.error("Can not delete records for file {}", fileName, e);
		}
	}
}
