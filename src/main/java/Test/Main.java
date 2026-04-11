package test;

import entities.User;
import services.ServicePersonne;

import java.sql.SQLDataException;

public class Main {
    public static void main(String[] args) {
        ServicePersonne servicePersonne = new ServicePersonne();
        try {
            servicePersonne.add(new User("Yassine","Dhaya",90));
            servicePersonne.add(new User("Rihem","MATTOUSI",190));
            servicePersonne.add(new User("Falten","Foulen",70));
            servicePersonne.update(new User("TEsting","Hmed",33));
            System.out.println(servicePersonne.getAll());
        } catch (SQLDataException e) {
            throw new RuntimeException(e);
        }
    }
}
