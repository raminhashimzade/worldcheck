package az.blacklist.person.verification.model.world.check;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorldCheckPerson {

    private Long uid;

    private String firstName;

    private String lastName;

    private String fullName1;

    private String fullName2;

    private String category;

    private String subCategory;

    private String dateOfBirth;

    private List<String> aliases;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer percentage;
}
