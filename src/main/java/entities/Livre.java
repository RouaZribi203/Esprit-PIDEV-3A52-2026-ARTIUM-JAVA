package entities;

public class Livre extends Oeuvre {
    private String categorie;
    private Double prixLocation;
    private byte[] fichierPdf;

    public Livre() {
        setClasse("livre");
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public Double getPrixLocation() {
        return prixLocation;
    }

    public void setPrixLocation(Double prixLocation) {
        this.prixLocation = prixLocation;
    }

    public byte[] getFichierPdf() {
        return fichierPdf;
    }

    public void setFichierPdf(byte[] fichierPdf) {
        this.fichierPdf = fichierPdf;
    }
}
