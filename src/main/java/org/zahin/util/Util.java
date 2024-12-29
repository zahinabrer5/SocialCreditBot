package org.zahin.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;

public class Util {
    public static NumberFormat plusMinusNumFmt = new DecimalFormat("+#;-#");
    public static DecimalFormat twoDecFmt = new DecimalFormat("#.##");
    public static DecimalFormat oneDecFmt = new DecimalFormat("#.#");

    public static String thousandsSep(long n) {
        return thousandsSep(n, ' ');
    }

    public static String thousandsSep(long n, char sep) {
        return String.format(Locale.US, "%,d", n).replace(',', sep);
    }

    public static String pluralizer(String singular, String plural, long amount) {
        return amount == 1 ? singular : plural;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list)
            result.put(entry.getKey(), entry.getValue());

        return result;
    }

    public static Image urlToImage(String url) {
        try {
            return ImageIO.read(new URI(url).toURL());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // https://stackoverflow.com/a/13605411/21405641

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage)
            return (BufferedImage) img;

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    public static int mostCommonColour(Image img) {
        // my bad algo:
        /*
        Map<Color, Integer> colourFreq = new LinkedHashMap<>();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color colour = new Color(img.getRGB(x, y));
                colourFreq.put(colour, colourFreq.getOrDefault(colour, 0)+1);
            }
        }

        double totalOccurences = colourFreq.values().stream()
                .mapToInt(i -> i) // automatically unboxes Integer type to primitive int
                .sum();

        List<Color> dominantColours = colourFreq.keySet().stream()
                .filter(i -> colourFreq.get(i) / totalOccurences >= 0.1)
                .toList();

        if (dominantColours.isEmpty())
            return Color.GRAY;

        return dominantColours.stream().reduce(dominantColours.getFirst(), this::averageColour);
        */

        Image scaled = img.getScaledInstance(1, 1, Image.SCALE_REPLICATE);
        return Util.toBufferedImage(scaled).getRGB(0, 0);
    }

    // https://stackoverflow.com/a/29576746/21405641
    /*
    private static Color averageColour(Color a, Color b) {
        double r1 = a.getRed(), r2 = b.getRed();
        double g1 = a.getGreen(), g2 = b.getGreen();
        double b1 = a.getBlue(), b2 = b.getBlue();
        return new Color(roundSqrt((r1*r1+r2*r2)/2), roundSqrt((g1*g1+g2*g2)/2), roundSqrt((b1*b1+b2*b2)/2));
    }

    private int roundSqrt(double x) {
        return (int)Math.round(Math.sqrt(x));
    }
    */

    // https://stackoverflow.com/a/13632114/21405641
    public static String urlContentToString(String requestURL) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URI(requestURL).toURL().openStream()))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null; )
                sb.append(line);
            return sb.toString();
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static long millisToHours(long millis) {
        return Math.round(millis / 3600.0 / 1000);
    }

    public static BigInteger randomBigInteger(BigInteger min, BigInteger max, Random rand) {
        BigInteger randomNumber;
        do {
            randomNumber = min.add(new BigInteger(max.bitLength(), rand));
        } while (randomNumber.compareTo(max) >= 0 || randomNumber.compareTo(min) <= 0);
        return randomNumber;
    }
}
