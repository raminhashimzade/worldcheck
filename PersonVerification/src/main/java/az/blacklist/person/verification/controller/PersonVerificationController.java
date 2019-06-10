package az.blacklist.person.verification.controller;

import az.blacklist.person.verification.model.PersonVerificationRequest;
import az.blacklist.person.verification.service.PersonVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/person/verification")
public class PersonVerificationController {
    private final Logger logger = LoggerFactory.getLogger(PersonVerificationController.class);

    private final PersonVerificationService personVerificationService;

    public PersonVerificationController(PersonVerificationService personVerificationService) {
        this.personVerificationService = personVerificationService;
    }

    @PostMapping
    public Map<String, Object> getPerson(@RequestBody @Valid PersonVerificationRequest request) {
        logger.info("Get person");
        return personVerificationService.getPerson(request);
    }

}
