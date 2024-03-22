package pojos;

import java.util.Map;

public class QueryMessage {
    public String table;
    public String function;
    public Map<String, Object> input;

    public QueryMessage(String table, String function, Map<String, Object> input) {
        this.table = table;
        this.function = function;
        this.input = input;
    }
}
