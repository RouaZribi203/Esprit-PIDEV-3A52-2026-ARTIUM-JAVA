package entities;

import java.time.LocalDateTime;

public class LocationLivre {
    private Integer id;
    private LocalDateTime dateDebut;
    private String etat;
    private Integer nombreDeJours;
    private Integer userId;
    private Integer livreId;

    public LocationLivre() {
    }

    public Integer getId() {
        return id;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public Integer getNombreDeJours() {
        return nombreDeJours;
    }

    public void setNombreDeJours(Integer nombreDeJours) {
        this.nombreDeJours = nombreDeJours;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getLivreId() {
        return livreId;
    }

    public void setLivreId(Integer livreId) {
        this.livreId = livreId;
    }
}

