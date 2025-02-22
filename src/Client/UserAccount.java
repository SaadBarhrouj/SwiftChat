package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UserAccount {
    private int id ;
    private String name ;
    private String email ;
    private String password ;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private Lock lock=new ReentrantLock();


    public UserAccount(int id , String email,String name ,DataOutputStream outputStream,DataInputStream inputStream) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.outputStream = outputStream;
        this.inputStream = inputStream;

    }

    public void lockMe() {
        this.lock.lock();
    }

    public void unlockMe() {
        this.lock.unlock();
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }


}
