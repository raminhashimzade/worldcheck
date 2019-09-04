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

        String fullName = translitFromAz(request.getFullName());
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
    
    private String translitFromAz(String fullName) {
    	String res = "";
    	res = fullName.replace("Ə", "A");
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
    	
    	return res;
    }	    

}
