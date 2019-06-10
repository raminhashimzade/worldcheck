package az.blacklist.person.verification.service.black.list;

import az.blacklist.person.verification.model.SourceSystem;
import az.blacklist.person.verification.model.black.list.BlackListPerson;
import az.blacklist.person.verification.service.FindPersonService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.search.spell.StringDistance;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static az.blacklist.person.verification.model.SourceSystem.BLACK_LIST;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

@Service
public class BlackListService extends FindPersonService<BlackListPerson> {

    private final Logger logger = LoggerFactory.getLogger(BlackListService.class);

    public BlackListService(ObjectMapper objectMapper,
                            RestHighLevelClient restHighLevelClient,
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
                if (row.getRowNum() == 0) return;
                String fullName = formatter.formatCellValue(row.getCell(1));
                if (StringUtils.isEmpty(fullName)) return;

                blackListPeople.add(BlackListPerson.builder()
                        .number(formatter.formatCellValue(row.getCell(0)))
                        .fullName(fullName.trim())
                        .category(formatter.formatCellValue(row.getCell(2)))
                        .dateOfBirth(formatter.formatCellValue(row.getCell(3)))
                        .subCategory(formatter.formatCellValue(row.getCell(4)))
                        .note(formatter.formatCellValue(row.getCell(5)))
                        .build()
                );
            });

            saveBlackListPeople(blackListPeople);
            logger.info("End read file {}", file.getName());
        } catch (InvalidFormatException | IOException e) {
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
                (int) (stringDistance.getDistance(nameForDistance(p.getFullName()),
                        nameForDistance(fullName)) * 100)
                )
        );
        return blackListPeople.stream()
                .sorted(Comparator.comparing(BlackListPerson::getPercentage).reversed())
                .collect(Collectors.toList());
    }

    private void saveBlackListPeople(List<BlackListPerson> blackListPeople) {
        logger.info("Black list start save people");
        BulkRequest request = new BulkRequest();

        String blackList = BLACK_LIST.getIndexName();

        blackListPeople.forEach(
                p -> {
                    try {
                        request.add(new IndexRequest(blackList, "doc")
                                .source(objectMapper.writeValueAsString(p),
                                        XContentType.JSON));
                    } catch (JsonProcessingException e) {
                        logger.error("Can not save person p {}", p, e);
                    }
                }
        );

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
}