package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class UserAccount {
    private int id ;
    private String name ;
    private String email ;
    private String password ;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    public UserAccount(int id , String email,String name ,DataOutputStream outputStream,DataInputStream inputStream) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.outputStream = outputStream;
        this.inputStream = inputStream;

    }

    public String getEmail() {
        return email;
    }




}
