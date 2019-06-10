package az.blacklist.person.verification.model.black.list;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BlackListPerson {

    private String number;

    private String fullName;

    private String category;

    private String dateOfBirth;

    private String subCategory;

    private String note;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer percentage;
}
