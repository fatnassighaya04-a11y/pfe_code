package com.pfe.docextraction.service.email;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final String APP_NAME = "DocExtraction";
    private static final String SUPPORT_EMAIL = "extractai.pfe@gmail.com";

    // ==================== MÉTHODES DE PRÉVISUALISATION ====================
    
    public Map<String, Object> buildAccountApprovedPreviewMap() {
        Map<String, Object> preview = new HashMap<>();
        preview.put("subject", "✅ Votre compte a été accepté");
        preview.put("intro", "Bonjour [Nom d'utilisateur],");
        preview.put("body", "Votre compte a été accepté par l'administrateur. Vous pouvez maintenant vous connecter.");
        preview.put("closing", "L'équipe DocExtraction");
        return preview;
    }

    public Map<String, Object> buildAccountRejectedPreviewMap() {
        Map<String, Object> preview = new HashMap<>();
        preview.put("subject", "❌ Votre compte a été refusé");
        preview.put("intro", "Bonjour [Nom d'utilisateur],");
        preview.put("body", "Votre demande de compte a été refusée. Contactez l'administrateur.");
        preview.put("closing", "L'équipe DocExtraction");
        return preview;
    }

    public Map<String, Object> buildAccountModificationPreviewMap() {
        Map<String, Object> preview = new HashMap<>();
        preview.put("subject", "🔄 Votre compte a été modifié");
        preview.put("intro", "Bonjour,");
        preview.put("body", "Votre compte a été modifié par l'administrateur.");
        preview.put("closing", "L'équipe DocExtraction");
        return preview;
    }

    // ==================== MÉTHODE PRIVÉE D'ENVOI D'EMAIL ====================
    
    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(new InternetAddress(fromEmail, APP_NAME));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("✅ Email envoyé avec succès à: {}", to);
        } catch (Exception e) {
            log.error("❌ Erreur envoi email à {}: {}", to, e.getMessage(), e);
        }
    }

    // ==================== EMAIL À L'ADMIN - NOUVELLE INSCRIPTION ====================
    
    @Async
    public void sendNewAccountNotification(String username, String email) {
        try {
            log.info("📧 [ADMIN] Envoi notification nouvelle inscription à: {}", adminEmail);
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                        <h2 style="color: #2c3e50; text-align: center; border-bottom: 2px solid #3498db; padding-bottom: 10px;">📝 NOUVELLE DEMANDE D'INSCRIPTION</h2>
                        <p>Bonjour Administrateur,</p>
                        <p>Un nouvel utilisateur demande l'accès à la plateforme :</p>
                        <table style="width: 100%%; margin: 15px 0; border-collapse: collapse;">
                            <tr style="background-color: #f8f9fa;">
                                <td style="padding: 10px; border: 1px solid #ddd;"><strong>👤 Nom d'utilisateur</strong></td>
                                <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; border: 1px solid #ddd;"><strong>📧 Email</strong></td>
                                <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                            </tr>
                            <tr style="background-color: #f8f9fa;">
                                <td style="padding: 10px; border: 1px solid #ddd;"><strong>📅 Date de la demande</strong></td>
                                <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                            </tr>
                        </table>
                        <div style="text-align: center; margin: 25px 0;">
                            <a href="%s/users" style="background-color: #3498db; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px;">📋 Traiter la demande</a>
                        </div>
                        <hr style="border: none; border-top: 1px solid #eee;">
                        <p style="color: #7f8c8d; font-size: 12px; text-align: center;">Cordialement,<br>%s</p>
                    </div>
                </body>
                </html>
                """, username, email, dateTime, frontendUrl, APP_NAME);
            
            sendEmail(adminEmail, "📝 Nouvelle demande d'inscription", htmlContent);
        } catch (Exception e) {
            log.error("❌ [ADMIN] Erreur: {}", e.getMessage(), e);
        }
    }

    // ==================== EMAIL À L'UTILISATEUR - ACCEPTATION ====================
    
    @Async
    public void sendAccountApprovedNotification(String username, String email) {
        if (!isValidEmail(email)) {
            log.error("❌ [ACCEPTATION] Email invalide: {}", email);
            return;
        }
        
        try {
            log.info("📧 [ACCEPTATION] Envoi à: {}", email);
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                        <h2 style="color: #27ae60; text-align: center; border-bottom: 2px solid #27ae60; padding-bottom: 10px;">✅ COMPTE ACCEPTÉ AVEC SUCCÈS</h2>
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>🎉 Félicitations ! Votre compte a été accepté par l'administrateur.</p>
                        <table style="width: 100%%; margin: 15px 0; border-collapse: collapse;">
                            <tr style="background-color: #f8f9fa;">
                                <td style="padding: 10px; border: 1px solid #ddd;"><strong>📅 Date d'approbation</strong></td>
                                <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; border: 1px solid #ddd;"><strong>📧 Email de connexion</strong></td>
                                <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                            </tr>
                        </table>
                        <div style="text-align: center; margin: 25px 0;">
                            <a href="%s/login" style="background-color: #27ae60; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px;">🔗 Se connecter</a>
                        </div>
                        <hr style="border: none; border-top: 1px solid #eee;">
                        <p style="color: #7f8c8d; font-size: 12px; text-align: center;">Cordialement,<br>%s</p>
                    </div>
                </body>
                </html>
                """, username, dateTime, email, frontendUrl, APP_NAME);
            
            sendEmail(email, "✅ Votre compte a été accepté", htmlContent);
        } catch (Exception e) {
            log.error("❌ [ACCEPTATION] Erreur pour {}: {}", email, e.getMessage(), e);
        }
    }

    // ==================== EMAIL À L'UTILISATEUR - REFUS ====================
    
    @Async
    public void sendAccountRejectedNotification(String username, String email) {
        if (!isValidEmail(email)) {
            log.error("❌ [REFUS] Email invalide: {}", email);
            return;
        }
        
        try {
            log.info("📧 [REFUS] Envoi à: {}", email);
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                        <h2 style="color: #e74c3c; text-align: center; border-bottom: 2px solid #e74c3c; padding-bottom: 10px;">❌ COMPTE REFUSÉ</h2>
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>Nous vous informons que votre demande de compte a été refusée.</p>
                        <p><strong>📅 Date de décision :</strong> %s</p>
                        <p><strong>📌 Raisons possibles :</strong></p>
                        <ul>
                            <li>Informations incomplètes</li>
                            <li>Non-respect des conditions d'utilisation</li>
                        </ul>
                        <div style="text-align: center; margin: 25px 0;">
                            <a href="mailto:%s" style="background-color: #e74c3c; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px;">📧 Contacter l'administrateur</a>
                        </div>
                        <hr style="border: none; border-top: 1px solid #eee;">
                        <p style="color: #7f8c8d; font-size: 12px; text-align: center;">Cordialement,<br>%s</p>
                    </div>
                </body>
                </html>
                """, username, dateTime, SUPPORT_EMAIL, APP_NAME);
            
            sendEmail(email, "❌ Votre compte a été refusé", htmlContent);
        } catch (Exception e) {
            log.error("❌ [REFUS] Erreur pour {}: {}", email, e.getMessage(), e);
        }
    }

    // ==================== EMAIL À L'UTILISATEUR - MODIFICATION DE COMPTE ====================
    
    @Async
    public void sendAccountModificationNotification(String email, String title, String details) {
        if (!isValidEmail(email)) {
            log.error("❌ [MODIFICATION] Email invalide: {}", email);
            return;
        }
        
        try {
            log.info("📧 [MODIFICATION] Envoi à: {}", email);
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                        <h2 style="color: #f39c12; text-align: center; border-bottom: 2px solid #f39c12; padding-bottom: 10px;">%s</h2>
                        <p>Bonjour,</p>
                        <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                            %s
                        </div>
                        <p><strong>📅 Date de modification :</strong> %s</p>
                        <div style="text-align: center; margin: 25px 0;">
                            <a href="%s/login" style="background-color: #f39c12; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px;">🔗 Accéder à la plateforme</a>
                        </div>
                        <p style="font-size: 12px; color: #7f8c8d;">Si vous pensez qu'il s'agit d'une erreur, contactez l'administrateur : <a href="mailto:%s">%s</a></p>
                        <hr style="border: none; border-top: 1px solid #eee;">
                        <p style="color: #7f8c8d; font-size: 12px; text-align: center;">Cordialement,<br>%s</p>
                    </div>
                </body>
                </html>
                """, title, details, dateTime, frontendUrl, SUPPORT_EMAIL, SUPPORT_EMAIL, APP_NAME);
            
            sendEmail(email, title, htmlContent);
        } catch (Exception e) {
            log.error("❌ [MODIFICATION] Erreur pour {}: {}", email, e.getMessage(), e);
        }
    }

    // ==================== EMAIL À L'UTILISATEUR - SUPPRESSION ====================
    
    @Async
    public void sendAccountDeletionNotification(String username, String email) {
        if (!isValidEmail(email)) {
            log.error("❌ [SUPPRESSION] Email invalide: {}", email);
            return;
        }
        
        try {
            log.info("📧 [SUPPRESSION] Envoi à: {}", email);
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                        <h2 style="color: #e74c3c; text-align: center; border-bottom: 2px solid #e74c3c; padding-bottom: 10px;">🗑️ COMPTE SUPPRIMÉ</h2>
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>Nous vous informons que votre compte a été supprimé de la plateforme.</p>
                        <p><strong>📅 Date de suppression :</strong> %s</p>
                        <p><strong>👤 Action effectuée par :</strong> Administrateur</p>
                        <p><strong>⚠️ Conséquences :</strong></p>
                        <ul>
                            <li>Vous n'avez plus accès à la plateforme</li>
                            <li>Vos données ont été supprimées conformément au RGPD</li>
                        </ul>
                        <div style="text-align: center; margin: 25px 0;">
                            <a href="mailto:%s" style="background-color: #e74c3c; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px;">📧 Contacter l'administrateur</a>
                        </div>
                        <hr style="border: none; border-top: 1px solid #eee;">
                        <p style="color: #7f8c8d; font-size: 12px; text-align: center;">Cordialement,<br>%s</p>
                    </div>
                </body>
                </html>
                """, username, dateTime, SUPPORT_EMAIL, APP_NAME);
            
            sendEmail(email, "🗑️ Votre compte a été supprimé", htmlContent);
        } catch (Exception e) {
            log.error("❌ [SUPPRESSION] Erreur pour {}: {}", email, e.getMessage(), e);
        }
    }

    // ==================== EMAIL - CODE RÉINITIALISATION ====================
    
    @Async
    public void sendResetCode(String email, String code) {
        if (!isValidEmail(email)) {
            log.error("❌ [RESET] Email invalide: {}", email);
            return;
        }
        
        try {
            log.info("📧 [RESET] Envoi code à: {}", email);
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                        <h2 style="color: #3498db; text-align: center; border-bottom: 2px solid #3498db; padding-bottom: 10px;">🔐 CODE DE RÉINITIALISATION</h2>
                        <p>Bonjour,</p>
                        <p>Vous avez demandé la réinitialisation de votre mot de passe.</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <span style="font-size: 36px; font-weight: bold; background-color: #f0f0f0; padding: 15px 30px; border-radius: 8px; letter-spacing: 5px;">%s</span>
                        </div>
                        <p><strong>⏰ Ce code est valable 15 minutes.</strong></p>
                        <p>Si vous n'avez pas fait cette demande, ignorez cet email.</p>
                        <hr style="border: none; border-top: 1px solid #eee;">
                        <p style="color: #7f8c8d; font-size: 12px; text-align: center;">Cordialement,<br>%s</p>
                    </div>
                </body>
                </html>
                """, code, APP_NAME);
            
            sendEmail(email, "🔐 Code de réinitialisation de mot de passe", htmlContent);
        } catch (Exception e) {
            log.error("❌ [RESET] Erreur pour {}: {}", email, e.getMessage(), e);
        }
    }

    // ==================== EMAIL CHANGEMENT DE RÔLE ====================
    
    @Async
    public void sendRoleChangeNotification(String username, String email, String oldRole, String newRole) {
        if (!isValidEmail(email)) {
            log.error("❌ [ROLE] Email invalide: {}", email);
            return;
        }
        
        try {
            log.info("📧 [ROLE] Envoi à: {}", email);
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                        <h2 style="color: #f39c12; text-align: center;">🔄 VOTRE RÔLE A ÉTÉ MODIFIÉ</h2>
                        <hr>
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>L'administrateur a modifié votre rôle sur la plateforme.</p>
                        <p><strong>📅 Date :</strong> %s</p>
                        <p><strong>📌 Ancien rôle :</strong> %s</p>
                        <p><strong>📌 Nouveau rôle :</strong> %s</p>
                        <div style="text-align: center; margin: 25px 0;">
                            <a href="%s/login" style="background-color: #f39c12; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px;">🔗 Se connecter</a>
                        </div>
                        <hr>
                        <p style="color: #7f8c8d; font-size: 12px; text-align: center;">Cordialement,<br>%s</p>
                    </div>
                </body>
                </html>
                """, username, dateTime, oldRole, newRole, frontendUrl, APP_NAME);
            
            sendEmail(email, "🔄 Votre rôle a été modifié", htmlContent);
        } catch (Exception e) {
            log.error("❌ [ROLE] Erreur pour {}: {}", email, e.getMessage(), e);
        }
    }

    // ==================== EMAIL CHANGEMENT DE STATUT ====================
    
    @Async
    public void sendStatusChangeNotification(String username, String email, String action) {
        if (!isValidEmail(email)) {
            log.error("❌ [STATUT] Email invalide: {}", email);
            return;
        }
        
        try {
            log.info("📧 [STATUT] Envoi à: {}", email);
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));
            
            boolean isActivate = action.equals("activate");
            String statusText = isActivate ? "activé" : "désactivé";
            String color = isActivate ? "#27ae60" : "#e74c3c";
            String subject = isActivate ? "🟢 Votre compte a été activé" : "🔴 Votre compte a été désactivé";
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                        <h2 style="color: %s; text-align: center;">%s</h2>
                        <hr>
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>L'administrateur a %s votre compte.</p>
                        <p><strong>📅 Date du changement :</strong> %s</p>
                        <p><strong>📌 Nouveau statut :</strong> %s</p>
                        <div style="text-align: center; margin: 25px 0;">
                            <a href="%s/login" style="background-color: %s; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px;">🔗 Accéder à la plateforme</a>
                        </div>
                        <hr>
                        <p style="color: #7f8c8d; font-size: 12px; text-align: center;">Cordialement,<br>%s</p>
                    </div>
                </body>
                </html>
                """, color, subject, username, statusText, dateTime, statusText.toUpperCase(), frontendUrl, color, APP_NAME);
            
            sendEmail(email, subject, htmlContent);
        } catch (Exception e) {
            log.error("❌ [STATUT] Erreur pour {}: {}", email, e.getMessage(), e);
        }
    }

    // ==================== EMAIL MODIFICATION PROFIL (UTILISATEUR) ====================
    
    @Async
    public void sendProfileModificationToUser(String username, String email, String oldData, String newData) {
        if (!isValidEmail(email)) {
            log.error("❌ [PROFIL-USER] Email invalide: {}", email);
            return;
        }
        
        try {
            log.info("📧 [PROFIL-USER] Envoi à: {}", email);
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                        <h2 style="color: #3498db; text-align: center;">✏️ CONFIRMATION MODIFICATION PROFIL</h2>
                        <hr>
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>Votre profil a été modifié avec succès.</p>
                        <p><strong>📅 Date de modification :</strong> %s</p>
                        <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                            <pre style="margin: 0; font-family: monospace; white-space: pre-wrap;">%s</pre>
                            <hr style="margin: 10px 0;">
                            <pre style="margin: 0; font-family: monospace; white-space: pre-wrap;">%s</pre>
                        </div>
                        <p>✅ Si vous êtes à l'origine de ces modifications, aucune action n'est requise.</p>
                        <p>⚠️ Si vous n'êtes pas à l'origine, contactez l'administrateur : <a href="mailto:%s">%s</a></p>
                        <hr>
                        <p style="color: #7f8c8d; font-size: 12px; text-align: center;">Cordialement,<br>%s</p>
                    </div>
                </body>
                </html>
                """, username, dateTime, oldData, newData, SUPPORT_EMAIL, SUPPORT_EMAIL, APP_NAME);
            
            sendEmail(email, "✏️ Confirmation de modification de votre profil", htmlContent);
        } catch (Exception e) {
            log.error("❌ [PROFIL-USER] Erreur pour {}: {}", email, e.getMessage(), e);
        }
    }

    // ==================== EMAIL MODIFICATION PROFIL (ADMIN) ====================
    
    @Async
    public void sendProfileModificationToAdmin(String username, String email, String oldData, String newData) {
        try {
            log.info("📧 [PROFIL-ADMIN] Envoi notification à l'admin: {}", adminEmail);
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                        <h2 style="color: #e74c3c; text-align: center;">⚠️ MODIFICATION PROFIL UTILISATEUR</h2>
                        <hr>
                        <p><strong>📅 Date de modification :</strong> %s</p>
                        <p><strong>👤 UTILISATEUR CONCERNÉ :</strong></p>
                        <ul>
                            <li><strong>Nom :</strong> %s</li>
                            <li><strong>Email :</strong> %s</li>
                        </ul>
                        <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                            <pre style="margin: 0; font-family: monospace; white-space: pre-wrap;">%s</pre>
                            <hr style="margin: 10px 0;">
                            <pre style="margin: 0; font-family: monospace; white-space: pre-wrap;">%s</pre>
                        </div>
                        <div style="text-align: center; margin: 25px 0;">
                            <a href="%s/users" style="background-color: #e74c3c; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px;">🔗 Vérifier les modifications</a>
                        </div>
                        <hr>
                        <p style="color: #7f8c8d; font-size: 12px; text-align: center;">Cordialement,<br>%s</p>
                    </div>
                </body>
                </html>
                """, dateTime, username, email, oldData, newData, frontendUrl, APP_NAME);
            
            sendEmail(adminEmail, "⚠️ Modification de profil utilisateur", htmlContent);
        } catch (Exception e) {
            log.error("❌ [PROFIL-ADMIN] Erreur: {}", e.getMessage(), e);
        }
    }

    // ==================== EMAIL NOUVELLE RÉCLAMATION (ADMIN) ====================
    
    @Async
    public void sendNewReclamationNotification(String username, String userEmail, String reclamationId, String sujet, String contenu) {
        try {
            log.info("📧 [RECLAMATION-ADMIN] Envoi notification à l'admin: {}", adminEmail);
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                        <h2 style="color: #e74c3c; text-align: center;">📢 NOUVELLE RÉCLAMATION</h2>
                        <hr>
                        <p>Bonjour Administrateur,</p>
                        <p>Un utilisateur a soumis une nouvelle réclamation.</p>
                        <p><strong>👤 UTILISATEUR :</strong> %s (%s)</p>
                        <p><strong>📅 Date :</strong> %s</p>
                        <p><strong>🆔 ID :</strong> %s</p>
                        <p><strong>📌 Sujet :</strong> %s</p>
                        <p><strong>📝 Contenu :</strong></p>
                        <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                            %s
                        </div>
                        <div style="text-align: center; margin: 25px 0;">
                            <a href="%s/admin/reclamations/%s" style="background-color: #e74c3c; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px;">🔗 Traiter la réclamation</a>
                        </div>
                        <hr>
                        <p style="color: #7f8c8d; font-size: 12px; text-align: center;">Cordialement,<br>%s</p>
                    </div>
                </body>
                </html>
                """, username, userEmail, dateTime, reclamationId, sujet, contenu, frontendUrl, reclamationId, APP_NAME);
            
            sendEmail(adminEmail, "📢 NOUVELLE RÉCLAMATION #" + reclamationId, htmlContent);
        } catch (Exception e) {
            log.error("❌ [RECLAMATION-ADMIN] Erreur: {}", e.getMessage(), e);
        }
    }

    // ==================== MÉTHODE UTILITAIRE ====================
    
    private boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty() && email.contains("@") && email.contains(".");
    }
}