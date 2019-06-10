package az.blacklist.person.verification.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@NotNull
public class PersonVerificationRequest {

    @NotEmpty
    private String fullName;

    @NotNull
    private Integer percentage;

   
}
