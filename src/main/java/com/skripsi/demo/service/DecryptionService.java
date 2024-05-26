package com.skripsi.demo.service;

import com.skripsi.demo.model.DecryptionRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.NodeList;

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
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class DecryptionService {

    private static final String DECRYPTED_IMAGE_PATH = "src/main/resources/static/images/decrypted.jpg";

    public Map<String, Object> decrypt(DecryptionRequest request) throws Exception {
        byte[] keyBytes = hexStringToByteArray(request.getAesKey());
        SecretKeySpec aesKey = new SecretKeySpec(keyBytes, "AES");

        long startTime = System.currentTimeMillis();
        String encryptedData = extractEncryptedData(request.getDecryptImageFile());
        byte[] decryptedBytes = decryptData(encryptedData, aesKey);
        BufferedImage image = convertByteArrayToImage(decryptedBytes);

        saveImage(image, DECRYPTED_IMAGE_PATH);
        long endTime = System.currentTimeMillis();
        long decryptionTime = endTime - startTime;

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("imagePath", DECRYPTED_IMAGE_PATH);
        resultMap.put("decryptionTime", decryptionTime);

        return resultMap;
    }

    private void saveImage(BufferedImage image, String path) throws IOException {
        File outputFile = new File(path);
        outputFile.getParentFile().mkdirs(); // Ensure the directory exists
        ImageIO.write(image, "jpg", outputFile);
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

    private byte[] decryptData(String encryptedData, SecretKeySpec aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        return cipher.doFinal(decodedData);
    }

    private BufferedImage convertByteArrayToImage(byte[] imageBytes) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        return ImageIO.read(bis);
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
