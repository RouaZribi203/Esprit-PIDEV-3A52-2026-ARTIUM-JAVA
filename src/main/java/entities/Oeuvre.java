package entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Oeuvre {
    private Integer id;
    private String titre;
    private String description;
    private LocalDate dateCreation;
    private byte[] image;
    private String type;
    private String embedding;
    private String imageEmbedding;
    private Integer collectionId;
    private String classe;

    // Relation many-to-many via oeuvre_user (loaded by service layer).
    private List<User> favusers = new ArrayList<>();

    public Oeuvre() {
    }

    public Integer getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public String getImageEmbedding() {
        return imageEmbedding;
    }

    public void setImageEmbedding(String imageEmbedding) {
        this.imageEmbedding = imageEmbedding;
    }

    public Integer getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Integer collectionId) {
        this.collectionId = collectionId;
    }

    public String getClasse() {
        return classe;
    }

    public void setClasse(String classe) {
        this.classe = classe;
    }

    public List<User> getUsers() {
        return favusers;
    }

    public void setUsers(List<User> users) {
        this.favusers = users != null ? users : new ArrayList<>();
    }
}
