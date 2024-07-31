package com.skripsi.demo.service;

import com.skripsi.demo.model.DecryptionRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

@Service
public class DecryptionService {

    private static final String DECRYPTED_IMAGE_PATH = "src/main/resources/static/images/decrypted.jpg";


    public Map<String, Object> decrypt(DecryptionRequest request) throws Exception {
        System.out.println("Starting decryption process...");

        byte[] keyBytes = hexStringToByteArray(request.getAesKey());
        SecretKeySpec aesKey = new SecretKeySpec(keyBytes, "AES");

        long startTime = System.currentTimeMillis();
        String hexEncryptedData = extractEncryptedData(request.getDecryptImageFile());
        System.out.println("Encrypted data extracted: " + hexEncryptedData.substring(0, Math.min(hexEncryptedData.length(), 100)) + "...");

        byte[] decryptedPixels = decryptData(hexEncryptedData, aesKey);
        System.out.println("Data decrypted. Length of decrypted data: " + decryptedPixels.length);

        BufferedImage image = convertByteArrayToImage(decryptedPixels);
        File outputFile = new File("src/main/resources/static/images/decrypted.jpg");
        ImageIO.write(image, "PNG", outputFile);


//        saveImage(image, DECRYPTED_IMAGE_PATH);
        long endTime = System.currentTimeMillis();
        long decryptionTime = endTime - startTime;

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("imagePath", DECRYPTED_IMAGE_PATH);
        resultMap.put("decryptionTime", decryptionTime);

        System.out.println("Decryption completed. Image saved at " + DECRYPTED_IMAGE_PATH);

        return resultMap;
    }

    private String extractEncryptedData(MultipartFile file) throws Exception {
        InputStream inputStream = file.getInputStream();
        ImageInputStream iis = ImageIO.createImageInputStream(inputStream);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

        if (!readers.hasNext()) {
            throw new RuntimeException("No readers found for given image");
        }

        ImageReader reader = readers.next();
        reader.setInput(iis, true);
        IIOMetadata metadata = reader.getImageMetadata(0);

        // Extract metadata in PNG format
        String nativeFormatName = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(nativeFormatName);

        IIOMetadataNode text = (IIOMetadataNode) root.getElementsByTagName("tEXt").item(0);
        if (text == null) {
            throw new RuntimeException("No tEXt metadata found in the image");
        }

        IIOMetadataNode textEntry = (IIOMetadataNode) text.getElementsByTagName("tEXtEntry").item(0);
        if (textEntry == null) {
            throw new RuntimeException("No tEXtEntry found in the tEXt metadata");
        }

        String encryptedData = textEntry.getAttribute("value");
        if (encryptedData == null || encryptedData.isEmpty()) {
            throw new RuntimeException("Encrypted data not found in the image metadata");
        }

        return encryptedData;
    }


    private BufferedImage convertByteArrayToImage(byte[] imageBytes) throws Exception {
        System.out.println("Converting byte array back to image...");
        ByteBuffer buffer = ByteBuffer.wrap(imageBytes);
//        System.out.println("Raw byte buffer content: " + Arrays.toString(imageBytes));
        int width = buffer.getInt();
        int height = buffer.getInt();

        System.out.println("Image dimensions extracted: Width = " + width + ", Height = " + height);

        int[] pixels = new int[(imageBytes.length - 8) / 4];
        buffer.asIntBuffer().get(pixels);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    private void saveImage(BufferedImage image, String path) throws IOException {
        File outputFile = new File(path);
        outputFile.getParentFile().mkdirs(); // Ensure the directory exists
        ImageIO.write(image, "jpg", outputFile);
    }

    private byte[] decryptData(String hexEncryptedData, SecretKeySpec aesKey) throws Exception {
        System.out.println("Decrypting data...");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] encryptedBytes = hexStringToByteArray(hexEncryptedData);
        return cipher.doFinal(encryptedBytes);
    }

    private byte[] hexStringToByteArray(String s) {
        System.out.println("Converting hex string to byte array...");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
