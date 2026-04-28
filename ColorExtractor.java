import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ColorExtractor {
    public static void main(String[] args) throws Exception {
        File file = new File("src/main/resources/views/assets/PNG Icon.png");
        BufferedImage image = ImageIO.read(file);
        
        Map<Integer, Integer> colorCounts = new HashMap<>();
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xff;
                if (alpha < 250) continue; // ignore transparent
                
                int rgb = argb & 0x00FFFFFF;
                colorCounts.put(rgb, colorCounts.getOrDefault(rgb, 0) + 1);
            }
        }
        
        colorCounts.entrySet().stream()
            .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
            .limit(10)
            .forEach(e -> {
                int c = e.getKey();
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                System.out.printf("Count: %d, Color: #%02X%02X%02X%n", e.getValue(), r, g, b);
            });
    }
}
