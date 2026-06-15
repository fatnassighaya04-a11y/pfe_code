package com.pfe.docextraction.entity;

import com.pfe.docextraction.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_settings")
@Getter
@Setter
public class AppSettings extends BaseEntity {

    @Column(name = "organisation_nom", nullable = false)
    private String organisationNom;

    @Column(name = "organisation_email", nullable = false)
    private String organisationEmail;

    @Column(name = "organisation_telephone", nullable = false)
    private String organisationTelephone;

    @Column(name = "organisation_pays", nullable = false)
    private String organisationPays;

    @Column(name = "extraction_seuil_auto", nullable = false)
    private Integer extractionSeuilAuto;

    @Column(name = "extraction_langue", nullable = false)
    private String extractionLangue;

    @Column(name = "extraction_score_min", nullable = false)
    private Integer extractionScoreMin;

    @Column(name = "notifications_email", nullable = false)
    private Boolean notificationsEmail;

    @Column(name = "notifications_alerte_erreur", nullable = false)
    private Boolean notificationsAlerteErreur;

    @Column(name = "notifications_apprentissage_continu", nullable = false)
    private Boolean notificationsApprentissageContinu;

    @Column(name = "notifications_ameliorer_modele", nullable = false)
    private Boolean notificationsAmeliorerModele;

    @Column(name = "securite_deux_facteurs", nullable = false)
    private Boolean securiteDeuxFacteurs;

    @Column(name = "securite_deux_facteurs_type", nullable = false)
    private String securiteDeuxFacteursType;

    @Column(name = "securite_duree_session", nullable = false)
    private Integer securiteDureeSession;
}