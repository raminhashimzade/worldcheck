package az.blacklist.person.verification.service;

import az.blacklist.person.verification.model.PersonVerificationRequest;
import az.blacklist.person.verification.model.SourceSystem;
import az.blacklist.person.verification.model.black.list.BlackListPerson;
import az.blacklist.person.verification.model.black.list.BlackListResponse;
import az.blacklist.person.verification.model.world.check.WorldCheckPerson;
import az.blacklist.person.verification.model.world.check.WorldCheckResponse;
import az.blacklist.person.verification.service.black.list.BlackListService;
import az.blacklist.person.verification.service.world.check.WorldCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PersonVerificationService {

    private final Logger logger = LoggerFactory.getLogger(PersonVerificationService.class);


    private final WorldCheckService worldCheckService;
    private final BlackListService blackListService;


    public PersonVerificationService(WorldCheckService worldCheckService,
                                     BlackListService blackListService) {
        this.worldCheckService = worldCheckService;
        this.blackListService = blackListService;
    }

    public Map<String, Object> getPerson(PersonVerificationRequest request) {
        logger.info("Person verification start {}", request);

        String fullName = request.getFullName();
        double percentage = request.getPercentage() / 100D;

        Map<String, Object> result = new HashMap<>();
        List<WorldCheckPerson> worldCheckPeople = null;
        List<BlackListPerson> blackListPeople = null;
        
        if (request.getSource().equals("W")) {
        	worldCheckPeople = worldCheckService.findPeople(fullName, percentage);
        	result.put(SourceSystem.WORLD_CHECK.getName(), new WorldCheckResponse(worldCheckPeople.size(), worldCheckPeople));
        }        
        else if (request.getSource().equals("B")) {
        	blackListPeople = blackListService.findPeople(fullName, percentage);
        	result.put(SourceSystem.BLACK_LIST.getName(), new BlackListResponse(blackListPeople.size(), blackListPeople));
        }
        else if (request.getSource().equals("WB")) {
        	worldCheckPeople = worldCheckService.findPeople(fullName, percentage);
        	blackListPeople = blackListService.findPeople(fullName, percentage);
        	result.put(SourceSystem.WORLD_CHECK.getName(), new WorldCheckResponse(worldCheckPeople.size(), worldCheckPeople));
        	result.put(SourceSystem.BLACK_LIST.getName(), new BlackListResponse(blackListPeople.size(), blackListPeople));
        }

        logger.info("Person verification end {}", request);
        return result;
    }

}
