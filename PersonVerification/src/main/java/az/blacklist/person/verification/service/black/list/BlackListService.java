package az.blacklist.person.verification.service.black.list;

import static az.blacklist.person.verification.model.SourceSystem.BLACK_LIST;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.search.spell.StringDistance;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import az.blacklist.person.verification.model.SourceSystem;
import az.blacklist.person.verification.model.black.list.BlackListPerson;
import az.blacklist.person.verification.service.FindPersonService;

@Service
public class BlackListService extends FindPersonService<BlackListPerson> {

	private final Logger logger = LoggerFactory.getLogger(BlackListService.class);

	public BlackListService(ObjectMapper objectMapper, RestHighLevelClient restHighLevelClient,
			StringDistance stringDistance) {
		super(objectMapper, restHighLevelClient, stringDistance);
	}

	public void readBlackList(File file) {
        logger.info("Start read file {}", file.getName());
        List<BlackListPerson> blackListPeople = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            sheet.forEach(row -> {

                logger.info("rownum before add = " + row.getRowNum());

                if (row.getRowNum() == 0)
                    return;
                String fullName = formatter.formatCellValue(row.getCell(0));
                if (StringUtils.isEmpty(fullName))
                    return;

                String dateOfBirth = formatter.formatCellValue(row.getCell(2)).isBlank() ?
                        null : formatter.formatCellValue(row.getCell(2)).replaceAll("/", "-");

                blackListPeople.add(BlackListPerson.builder().number(formatter.formatCellValue(row.getCell(4)))
                        .fullName(translitFromAz(fullName.trim()))
                        .category(formatter.formatCellValue(row.getCell(1)))
                        .dateOfBirth(dateOfBirth)
                        .subCategory(formatter.formatCellValue(row.getCell(3)))
                        .note(String.format("%s %s",
                                formatter.formatCellValue(row.getCell(5)),
                                formatter.formatCellValue(row.getCell(6))).replaceAll("/", "-")).build());

                logger.info("rownum after add = " + row.getRowNum());
            });

            saveBlackListPeople(blackListPeople);
            logger.info("End read file {}", file.getName());
        } catch (Exception e) {
            logger.error("Can not parse Black List file {}", file.getName(), e);
        }
    }

	@Override
	public SourceSystem getSourceSystem() {
		return BLACK_LIST;
	}

	public List<BlackListPerson> findPeople(String fullName, Double percentage) {
		logger.info("Find person {} in BLACK_LIST", fullName);

		List<BlackListPerson> blackListPeople = super.findPeople(fullName);

		blackListPeople.removeIf(p -> stringDistance.getDistance(nameForDistance(p.getFullName()),
				nameForDistance(fullName)) < percentage);

		blackListPeople.forEach(p -> p.setPercentage(
				(int) (stringDistance.getDistance(nameForDistance(p.getFullName()), nameForDistance(fullName)) * 100)));
		return blackListPeople.stream().sorted(Comparator.comparing(BlackListPerson::getPercentage).reversed())
				.collect(Collectors.toList());
	}

	private void saveBlackListPeople(List<BlackListPerson> blackListPeople) {
		logger.info("Black list start save people");
		BulkRequest request = new BulkRequest();

		String blackList = BLACK_LIST.getIndexName();

		blackListPeople.forEach(p -> {
			try {
				request.add(new IndexRequest(blackList, "doc").source(objectMapper.writeValueAsString(p), XContentType.JSON));
			} catch (JsonProcessingException e) {
				logger.error("Can not save person p {}", p, e);
			}
		});

		request.timeout(TimeValue.timeValueMinutes(5));
		logger.info("Black list end save people");
		try {
			if (restHighLevelClient.indices().exists(new GetIndexRequest(blackList), DEFAULT)) {
				restHighLevelClient.indices().delete(new DeleteIndexRequest(blackList), DEFAULT);
			}
			restHighLevelClient.bulk(request, DEFAULT);
		} catch (IOException e) {
			logger.error("Can not save Black list file", e);
		}
	}

	private String nameForDistance(String fullName) {
		List<String> nameParts = Arrays.asList(fullName.toLowerCase().split(" "));
		nameParts.sort(String::compareTo);
		return String.join(" ", nameParts);
	}

	private String translitFromAz(String fullName) {
		String res = "";
		res = fullName.toUpperCase();
		res = res.replace("Ə", "A");
		res = res.replace("Ö", "O");
		res = res.replace("Ü", "U");
		res = res.replace("İ", "I");
		res = res.replace("J", "ZH");
		res = res.replace("C", "J");
		res = res.replace("Ş", "SH");
		res = res.replace("Ğ", "GH");
		res = res.replace("Ç", "CH");
		res = res.replace("X", "KH");
		res = res.replace("Q", "G");
		res = res.replace("ö", "o");
		res = res.replace("ü", "u");
		res = res.replace("ı", "i");
		res = res.replace("j", "zh");
		res = res.replace("c", "j");
		res = res.replace("ş", "sh");
		res = res.replace("ğ", "gh");
		res = res.replace("ç", "ch");
		res = res.replace("x", "kh");
		res = res.replace("q", "g");
		res = res.replace("OGHLU", "");
		res = res.replace("QIZI", "");
		res = res.trim();
		return res;
	}
}