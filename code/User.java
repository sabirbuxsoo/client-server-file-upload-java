import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;


//Author Sabir Buxsoo
//CSC3002F Assignment 1
//Class to handle users
public class User{
    private HashMap<String, String> users = new HashMap<String, String>();
    User(){
        //Get list of users from database and put into hashtbale
        try(BufferedReader in = new BufferedReader(new FileReader("../Server/DB/users.txt"))) {
            String str;
            while ((str = in.readLine()) != null) {
                String[] tokens = str.split(",");
                users.put(tokens[0], tokens[1]);

            }
        }
        catch (IOException e) {
            System.out.println("File Read Error");
        }
    }

    //Return list of all users
    public HashMap<String, String> getUsers(){
        return users;
    }
    
    //Check if user exits
    public boolean userExists(String username){
        if(users.containsKey(username)){
            return true;
        }
        return false;
    }

    //Check password of user
    public boolean checkPassword(String username, String password){
        // String encrypted_password = hashPass(username, password);
        if(users.get(username).equals(password)){
            return true;
        }
        return false;
    }


}