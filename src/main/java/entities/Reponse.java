package entities;

import java.time.LocalDateTime;

public class Reponse {
    private Integer id;
    private String contenu;
    private LocalDateTime dateReponse;
    private Integer reclamationId;
    private Integer userAdminId;

    public Reponse() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateReponse() {
        return dateReponse;
    }

    public void setDateReponse(LocalDateTime dateReponse) {
        this.dateReponse = dateReponse;
    }

    public Integer getReclamationId() {
        return reclamationId;
    }

    public void setReclamationId(Integer reclamationId) {
        this.reclamationId = reclamationId;
    }

    public Integer getUserAdminId() {
        return userAdminId;
    }

    public void setUserAdminId(Integer userAdminId) {
        this.userAdminId = userAdminId;
    }
}

