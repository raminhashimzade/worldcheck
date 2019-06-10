package az.blacklist.person.verification.model.world.check;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorldCheckResponse {

    private Integer matchCount;

    private List<WorldCheckPerson> worldCheckPeople;
}
