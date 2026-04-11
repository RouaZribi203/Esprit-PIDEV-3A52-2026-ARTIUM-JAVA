package Test;

import Models.Personne;
import Services.ServicePersonne;
import utils.MyDatabase;

import java.sql.SQLDataException;

public class Main {
    public static void main(String[] args) {
        ServicePersonne servicePersonne = new ServicePersonne();
        try {
            servicePersonne.ajouter(new Personne("Yassine","Dhaya",90));
            servicePersonne.ajouter(new Personne("Rihem","MATTOUSI",190));
            servicePersonne.ajouter(new Personne("Falten","Foulen",70));
            servicePersonne.modifier(new Personne("TEsting","Hmed",33));
            System.out.println(servicePersonne.recuperer());
        } catch (SQLDataException e) {
            throw new RuntimeException(e);
        }
    }
}
