package services;

import entities.Evenement;
import entities.Ticket;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class EmailService {

    // IMPORTANT: Remplacez ces valeurs par vos vrais identifiants
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    // Mettez votre email expéditeur ici:
    private static final String SENDER_EMAIL = "khalil.elmnari@gmail.com";
    // Mettez votre "Mot de passe d'application" Google ici:
    private static final String SENDER_PASSWORD = "ohqrgcygvzbporza";

    private final TicketPdfService ticketPdfService = new TicketPdfService();

    public void sendTicketEmailAsync(String recipientEmail, Evenement event, Ticket ticket) {
        CompletableFuture.runAsync(() -> {
            try {
                sendTicketEmail(recipientEmail, event, ticket);
            } catch (Exception e) {
                System.err.println("Échec de l'envoi de l'e-mail: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void sendTicketEmail(String recipientEmail, Evenement event, Ticket ticket) throws Exception {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            System.err.println("Cannot send email: recipient is empty.");
            return;
        }
        
        if (SENDER_EMAIL.equals("votre.email@gmail.com")) {
            System.err.println("L'e-mail n'a pas été envoyé: Veuillez configurer vos identifiants dans EmailService.java");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL, "ARTIUM Ticketing"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("Votre Ticket VIP pour l'évènement : " + event.getTitre());

        // Corps du message
        String bodyText = "Bonjour,\n\n"
                + "Félicitations pour votre achat ! Veuillez trouver ci-joint votre ticket d'accès VIP pour l'évènement '" + event.getTitre() + "'.\n"
                + "Le code QR présent sur ce ticket devra être scanné à l'entrée.\n\n"
                + "Nous vous souhaitons une excellente expérience.\n\n"
                + "Cordialement,\n"
                + "L'équipe ARTIUM";

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(bodyText);

        // Pièce jointe (Le ticket PDF)
        File pdfFile = ticketPdfService.createPreviewPdf(event, ticket);
        MimeBodyPart attachmentPart = new MimeBodyPart();
        DataSource source = new FileDataSource(pdfFile);
        attachmentPart.setDataHandler(new DataHandler(source));
        attachmentPart.setFileName("Ticket_" + event.getTitre().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(attachmentPart);

        message.setContent(multipart);

        Transport.send(message);
        System.out.println("L'e-mail avec le ticket PDF a bien été envoyé à " + recipientEmail);
        
        // Nettoyage du fichier temporaire
        if (pdfFile.exists()) {
            pdfFile.delete();
        }
    }
}
