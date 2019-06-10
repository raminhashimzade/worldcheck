package az.blacklist.person.verification.model.black.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlackListResponse {

    private Integer matchCount;

    private List<BlackListPerson> blackListPeople;
}
