package az.blacklist.person.verification.model;

import az.blacklist.person.verification.model.black.list.BlackListPerson;
import az.blacklist.person.verification.model.world.check.WorldCheckPerson;

import java.util.Arrays;
import java.util.List;

public enum SourceSystem {
    WORLD_CHECK("world_check",
            "worldCheck",
            WorldCheckPerson.class,
            "fullName1", "fullName2", "aliases"),
    BLACK_LIST("black_list",
            "blackList",
            BlackListPerson.class,
            "fullName");

    private String indexName;
    private String name;
    private List<String> columns;
    private Class type;

    SourceSystem(String indexName,
                 String name,
                 Class type,
                 String... columns) {
        this.indexName = indexName;
        this.name = name;
        this.type = type;
        this.columns = Arrays.asList(columns);
    }

    public String getIndexName() {
        return indexName;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return type;
    }

    public List<String> getColumns() {
        return columns;
    }
}
