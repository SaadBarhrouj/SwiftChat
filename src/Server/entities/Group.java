package Server.entities;


public class Group{
    private int id;
    private String name;
    private String description;
    private int adminId;

    public Group(int id, String name, String description, int admin){
        this.id = id;
        this.name = name;
        this.description = description;
        this.adminId = admin;
    }
    public int getId() { // <-- MÉTHODE AJOUTÉE
        return this.id;
    }


    public String getName(){return this.name;}
    public String getDescription(){return this.description;}

}