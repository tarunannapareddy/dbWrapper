import com.fasterxml.jackson.databind.ObjectMapper;
import dao.UserDAO;
import pojos.QueryMessage;

import java.util.Map;

public class QueryMap {
    private UserDAO userDAO = new UserDAO();

    public Object execute(QueryMessage queryMessage){
        if(queryMessage.table.equals("user")){
            return executeUserRequests(queryMessage.function, queryMessage.input);
        }
        return null;
    }

    public Object executeUserRequests(String function, Map<String, Object> input){
        if(function.equals("getUser")){
            return userDAO.getUser((String)input.get("userName"));
        }else if(function.equals("createUser")){
            return userDAO.createUser((String)input.get("userName"),(String)input.get("password"));
        } else if(function.equals("getUser_with_details")){
            return userDAO.getUser((String)input.get("userName"),(String)input.get("password"));
        }
        return null;
    }
}
