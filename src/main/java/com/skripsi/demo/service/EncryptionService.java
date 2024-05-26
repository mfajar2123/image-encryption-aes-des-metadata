package com.skripsi.demo.service;

import com.skripsi.demo.model.EncryptionRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.NodeList;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/**
 * Service untuk melakukan enkripsi gambar dan menulis metadata.
 */
@Service
public class EncryptionService {

    // Permutation and shift tables for DES key scheduling
    private static final int[] PC1 = {
            57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18,
            10, 2, 59, 51, 43, 35, 27, 19, 11, 3, 60, 52, 44, 36,
            63, 55, 47, 39, 31, 23, 15, 7, 62, 54, 46, 38, 30, 22,
            14, 6, 61, 53, 45, 37, 29, 21, 13, 5, 28, 20, 12, 4
    };
    private static final int[] PC2 = {
            14, 17, 11, 24, 1, 5, 3, 28, 15, 6, 21, 10, 23, 19, 12, 4,
            26, 8, 16, 7, 27, 20, 13, 2, 41, 52, 31, 37, 47, 55, 30, 40,
            51, 45, 33, 48, 44, 49, 39, 56, 34, 53, 46, 42, 50, 36, 29, 32
    };
    private static final int[] SHIFTS = {1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1};

    /**
     * Enkripsi gambar dan tulis metadata.
     */
    public Map<String, Object> encryptAndWriteMetadata(EncryptionRequest request) throws Exception {
        List<String> desKeys = generateDESKeys(request.getUserKey());
        SecretKeySpec aesKey = convertDESKeyToAES(desKeys);

        long startTime = System.currentTimeMillis();
        ByteArrayOutputStream encryptedStream = aesEncryptFile(request.getEncryptImageFile(), aesKey);
        String base64Encrypted = Base64.getEncoder().encodeToString(encryptedStream.toByteArray());

        BufferedImage image = ImageIO.read(request.getMetadataImageFile().getInputStream());
        String outputPath = "src/main/resources/static/images/encrypted.png";
        writeMetadata(image, base64Encrypted, outputPath);
        long endTime = System.currentTimeMillis(); // Selesai hitung waktu
        long duration = endTime - startTime; // Hitung durasi dalam milidetik
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("outputPath", outputPath);
        resultMap.put("duration", duration);

        return resultMap;


    }

    /**
     * Generate DES keys from the user's key.
     */
    public List<String> generateDESKeys(String key) {
        String binaryKey = convertToBinary(key);
        String key56 = permuteKey(binaryKey, PC1);
        String left = key56.substring(0, 28);
        String right = key56.substring(28);
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < SHIFTS.length; i++) {
            left = rotateLeft(left, SHIFTS[i]);
            right = rotateLeft(right, SHIFTS[i]);
            String combinedKey = left + right;
            keys.add(permuteKey(combinedKey, PC2));
        }
        return keys;
    }

    /**
     * Convert the last three DES keys to a single AES key.
     */
    public SecretKeySpec convertDESKeyToAES(List<String> keys) {
        String concatenatedKey = keys.get(13) + keys.get(14) + keys.get(15);
        String aesKeyBinary = concatenatedKey.substring(0, 128);
        byte[] aesKeyBytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            int byteValue = Integer.parseInt(aesKeyBinary.substring(i * 8, (i + 1) * 8), 2);
            aesKeyBytes[i] = (byte) byteValue;
        }
        return new SecretKeySpec(aesKeyBytes, "AES");
    }

    /**
     * Encrypt the file using AES encryption.
     */
    private ByteArrayOutputStream aesEncryptFile(MultipartFile inputFile, SecretKeySpec aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InputStream inputStream = null;
        CipherOutputStream cipherOutputStream = null;
        try {
            inputStream = inputFile.getInputStream();
            cipherOutputStream = new CipherOutputStream(outputStream, cipher);
            byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = inputStream.read(buffer)) >= 0) {
                cipherOutputStream.write(buffer, 0, numRead);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (cipherOutputStream != null) {
                cipherOutputStream.close();
            }
        }
        System.out.println("Size of encrypted data: " + outputStream.size());
        return outputStream;
    }

    /**
     * Write encrypted data as metadata to an image file.
     */
    private static void writeMetadata(BufferedImage image, String encryptedData, String outputPath) throws Exception {
        // Use PNG format to avoid JPEG compression issues with metadata
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No writers found for format png");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        IIOMetadata metadata = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), param);

        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
        IIOMetadataNode text = new IIOMetadataNode("tEXt");
        IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
        textEntry.setAttribute("keyword", "comment");
        textEntry.setAttribute("value", encryptedData);
        text.appendChild(textEntry);
        root.appendChild(text);
        metadata.mergeTree("javax_imageio_png_1.0", root);

        File outputFile = new File(outputPath);
        try (FileImageOutputStream output = new FileImageOutputStream(outputFile)) {
            writer.setOutput(output);
            writer.write(metadata, new IIOImage(image, null, metadata), param);
        } finally {
            writer.dispose();
        }
        System.out.println("Image successfully encrypted with AES-128 and saved with metadata.");
        System.out.println("Size of output image file: " + outputFile.length());
    }
    /**
     * Convert a string to its binary representation.
     */
    private String convertToBinary(String key) {
        StringBuilder binary = new StringBuilder();
        for (char c : key.toCharArray()) {
            String binString = Integer.toBinaryString(c);
            while (binString.length() < 8) {
                binString = "0" + binString;
            }
            binary.append(binString);
        }
        return binary.toString();
    }

    /**
     * Permute key according to a given table.
     */
    private String permuteKey(String key, int[] table) {
        StringBuilder permutedKey = new StringBuilder();
        for (int index : table) {
            permutedKey.append(key.charAt(index - 1));
        }
        return permutedKey.toString();
    }

    /**
     * Rotate a string left by a given number of positions.
     */
    private String rotateLeft(String input, int count) {
        return input.substring(count) + input.substring(0, count);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static void clearMetadata(IIOMetadata metadata) throws Exception {
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
        IIOMetadataNode markerSequence = (IIOMetadataNode) root.getElementsByTagName("markerSequence").item(0);

        NodeList comments = markerSequence.getElementsByTagName("com");
        while (comments.getLength() > 0) {
            markerSequence.removeChild(comments.item(0));
        }

        metadata.mergeTree("javax_imageio_jpeg_image_1.0", root);
    }

}
