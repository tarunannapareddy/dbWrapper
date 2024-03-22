package dao;

import dbConnector.DBConnector;
import marketplace.pojos.User;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class UserDAO {

    private Connection conn;

    public UserDAO() {
        this.conn = DBConnector.getCustomerConnection();
    }

    public Integer createUser(String userName, String password) {
        User user = getUser(userName);
        int val =-1;
        if(user !=null) {
            return -1;
        }
        Statement statement;
        try {
            String query=String.format("insert into account (user_id,password) values ('%s','%s');", userName, password);
            statement=conn.createStatement();
            statement.executeUpdate(query);
            val = getUser(userName).getId();
            System.out.println("Row Inserted row"+ userName+" "+password);
        }catch (Exception e){
            System.out.println(e);
        }
        return val;
    }

    public User getUser(String userName){
        Statement statement;
        User user = null;
        try {
            String query=String.format("select * from account where user_id= '%s' ",userName);
            statement=conn.createStatement();
            ResultSet res =statement.executeQuery(query);
            while(res.next()) {
                user = new User(res.getString("user_id"), res.getString("password"), res.getInt("id"));
            }
        }catch (Exception e){
            System.out.println(e);
        }
        return user;
    }

    public User getUser(String userName, String password){
        Statement statement;
        User user = null;
        try {
            String query=String.format("select * from account where user_id= '%s' and password = '%s' ",userName, password);
            statement=conn.createStatement();
            ResultSet res =statement.executeQuery(query);
            if(res.next()) {
                user = new User(res.getString("user_id"), res.getString("password"), res.getInt("id"));
            }
        }catch (Exception e){
            System.out.println(e);
        }
        return user;
    }

}
