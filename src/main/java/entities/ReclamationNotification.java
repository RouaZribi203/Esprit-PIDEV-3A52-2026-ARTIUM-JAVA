package entities;

import java.time.LocalDateTime;

public class ReclamationNotification {
    private Integer reponseId;
    private Integer reclamationId;
    private String reponseContenu;
    private LocalDateTime reponseDate;
    private String reclamationType;
    private String reclamationTexte;

    public Integer getReponseId() {
        return reponseId;
    }

    public void setReponseId(Integer reponseId) {
        this.reponseId = reponseId;
    }

    public Integer getReclamationId() {
        return reclamationId;
    }

    public void setReclamationId(Integer reclamationId) {
        this.reclamationId = reclamationId;
    }

    public String getReponseContenu() {
        return reponseContenu;
    }

    public void setReponseContenu(String reponseContenu) {
        this.reponseContenu = reponseContenu;
    }

    public LocalDateTime getReponseDate() {
        return reponseDate;
    }

    public void setReponseDate(LocalDateTime reponseDate) {
        this.reponseDate = reponseDate;
    }

    public String getReclamationType() {
        return reclamationType;
    }

    public void setReclamationType(String reclamationType) {
        this.reclamationType = reclamationType;
    }

    public String getReclamationTexte() {
        return reclamationTexte;
    }

    public void setReclamationTexte(String reclamationTexte) {
        this.reclamationTexte = reclamationTexte;
    }
}

