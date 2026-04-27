package utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

/**
 * Dispatches desktop notifications via SystemTray with a JavaFX fallback.
 */
public final class DesktopNotificationDispatcher {

    private static final Object LOCK = new Object();
    private static TrayIcon trayIcon;
    private static boolean trayInitAttempted;

    private DesktopNotificationDispatcher() {
    }

    public static void notify(String title, String message) {
        String safeTitle = sanitize(title, "Notification");
        String safeMessage = sanitize(message, "Vous avez une nouvelle notification.");

        if (showUsingSystemTray(safeTitle, safeMessage)) {
            return;
        }

        showFallbackAlert(safeTitle, safeMessage);
    }

    private static boolean showUsingSystemTray(String title, String message) {
        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
            return false;
        }

        ensureTrayIcon();
        if (trayIcon == null) {
            return false;
        }

        try {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void ensureTrayIcon() {
        synchronized (LOCK) {
            if (trayInitAttempted) {
                return;
            }
            trayInitAttempted = true;

            try {
                SystemTray tray = SystemTray.getSystemTray();
                TrayIcon icon = new TrayIcon(createDefaultIcon(), "Artium Notifications");
                icon.setImageAutoSize(true);
                tray.add(icon);
                trayIcon = icon;
            } catch (AWTException | SecurityException ignored) {
                trayIcon = null;
            }
        }
    }

    private static Image createDefaultIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(31, 63, 114));
        g.fillRoundRect(0, 0, 16, 16, 6, 6);
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(11f));
        g.drawString("A", 4, 12);
        g.dispose();
        return image;
    }

    private static void showFallbackAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.show();
        });
    }

    private static String sanitize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}

