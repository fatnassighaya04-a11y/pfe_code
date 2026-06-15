package com.pfe.docextraction.service.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.admin.email}")
    private String adminEmail;

    public java.util.Map<String, String> buildAccountApprovedPreview() {
        return java.util.Map.of(
                "subject", "Votre compte a été approuvé",
                "intro", "Bonjour [Nom d'utilisateur],",
                "body", "Votre compte a été approuvé par l'administrateur. Vous pouvez maintenant vous connecter à la plateforme.",
                "closing", "Cordialement,\nPlateforme Extraction Documents IA"
        );
    }

    public java.util.Map<String, String> buildAccountRejectedPreview() {
        return java.util.Map.of(
                "subject", "Votre compte a été rejeté",
                "intro", "Bonjour [Nom d'utilisateur],",
                "body", "Votre demande de compte a été rejetée par l'administrateur. Pour plus d'informations, contactez l'administrateur.",
                "closing", "Cordialement,\nPlateforme Extraction Documents IA"
        );
    }

    public java.util.Map<String, String> buildAccountModificationPreview() {
        return java.util.Map.of(
                "subject", "Votre compte a été modifié",
                "intro", "Bonjour,",
                "body", "Votre compte a été modifié par l'administrateur. Consultez la plateforme pour voir les détails de cette modification.",
                "closing", "Cordialement,\nPlateforme Extraction Documents IA"
        );
    }


    // Notifier l'admin qu'un nouveau compte attend approbation
    @Async
    public void sendNewAccountNotification(String username, String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@docextraction.com");
            message.setTo(adminEmail);
            message.setSubject("Nouveau compte en attente d'approbation");
            message.setText(
                "Bonjour Administrateur,\n\n" +
                "Un nouveau compte vient d'être créé et attend votre approbation :\n\n" +
                "Nom d'utilisateur : " + username + "\n" +
                "Email : " + email + "\n\n" +
                "Connectez-vous à la plateforme pour approuver ou rejeter ce compte.\n\n" +
                "Cordialement,\n" +
                "Plateforme Extraction Documents IA"
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur envoi email : " + e.getMessage());
        }
    }

    // Notifier l'utilisateur que son compte est approuvé
    @Async
    public void sendAccountApprovedNotification(String username, String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@docextraction.com");
            message.setTo(email);
            message.setSubject("Votre compte a été approuvé");
            message.setText(
                "Bonjour " + username + ",\n\n" +
                "Votre compte a été approuvé par l'administrateur.\n\n" +
                "Vous pouvez maintenant vous connecter à la plateforme.\n\n" +
                "Cordialement,\n" +
                "Plateforme Extraction Documents IA"
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur envoi email : " + e.getMessage());
        }
    }

    // Notifier l'utilisateur que son compte est rejeté
    @Async
    public void sendAccountRejectedNotification(String username, String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@docextraction.com");
            message.setTo(email);
            message.setSubject("Votre compte a été rejeté");
            message.setText(
                "Bonjour " + username + ",\n\n" +
                "Votre demande de compte a été rejetée par l'administrateur.\n\n" +
                "Pour plus d'informations, contactez l'administrateur.\n\n" +
                "Cordialement,\n" +
                "Plateforme Extraction Documents IA"
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur envoi email : " + e.getMessage());
        }
    }

    // Notifier l'utilisateur qu'un administrateur a modifié son compte
    @Async
    public void sendAccountModificationNotification(String email, String title, String details) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@docextraction.com");
            message.setTo(email);
            message.setSubject(title);
            message.setText(
                "Bonjour,\n\n" +
                details + "\n\n" +
                "Si vous pensez qu'il s'agit d'une erreur, contactez l'administrateur.\n\n" +
                "Cordialement,\n" +
                "Plateforme Extraction Documents IA"
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur envoi email modification : " + e.getMessage());
        }
    }
    // Envoyer le code de réinitialisation
    @Async
    public void sendResetCode(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@docextraction.com");
            message.setTo(email);
            message.setSubject("Code de réinitialisation de mot de passe");
            message.setText(
                "Bonjour,\n\n" +
                "Votre code de réinitialisation de mot de passe est :\n\n" +
                "        " + code + "\n\n" +
                "Ce code est valable 15 minutes.\n\n" +
                "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.\n\n" +
                "Cordialement,\n" +
                "Plateforme Extraction Documents IA"
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur envoi email reset : " + e.getMessage());
        }
    }
}