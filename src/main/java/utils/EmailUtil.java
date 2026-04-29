package utils;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtil {
    
    // Remplacez ces valeurs par vos propres identifiants SMTP pour tester l'envoi réel
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static String SENDER_EMAIL = "myriam24bouziri@gmail.com"; // Email expéditeur par défaut
    private static String SENDER_PASSWORD = ""; // Sera chargé depuis config.properties
    
    public static String ADMIN_EMAIL = "myriam24bouziri@gmail.com"; // Email de l'admin par défaut
    
    static {
        try (java.io.FileInputStream fis = new java.io.FileInputStream("config.properties")) {
            Properties config = new Properties();
            config.load(fis);
            SENDER_EMAIL = config.getProperty("smtp.sender.email", SENDER_EMAIL);
            SENDER_PASSWORD = config.getProperty("smtp.sender.password", SENDER_PASSWORD);
            ADMIN_EMAIL = config.getProperty("smtp.admin.email", ADMIN_EMAIL);
        } catch (Exception e) {
            System.err.println("Avertissement: Impossible de charger config.properties. Assurez-vous que le fichier existe à la racine du projet.");
        }
    }
    
    public static void sendEmailToAdmin(String subject, String content) {
        // Envoi asynchrone pour ne pas bloquer l'interface utilisateur (UI)
        new Thread(() -> {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            // Utilisé pour certains serveurs exigeant SSL/TLS de manière plus stricte
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(ADMIN_EMAIL));
                message.setSubject(subject);
                message.setText(content);

                // Envoi réel de l'email
                Transport.send(message); 

                System.out.println("====== SIMULATION D'ENVOI D'EMAIL ======");
                System.out.println("À l'admin (" + ADMIN_EMAIL + "): " + subject);
                System.out.println("Contenu: \n" + content);
                System.out.println("========================================");
                
            } catch (Throwable e) {
                e.printStackTrace();
                System.err.println("Erreur critique lors de l'envoi de l'email: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Erreur d'envoi d'email");
                    alert.setHeaderText("L'email n'a pas pu être envoyé.");
                    alert.setContentText("Détail de l'erreur : " + e.toString() + "\n\nVérifiez votre connexion, vos identifiants ou si Maven a bien téléchargé javax.mail.");
                    alert.showAndWait();
                });
            }
        }).start();
    }
}
