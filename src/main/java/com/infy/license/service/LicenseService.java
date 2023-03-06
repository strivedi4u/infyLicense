package com.infy.license.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.license.model.Constraint;
import com.infy.license.model.License;
import com.infy.license.model.SoftwareModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class LicenseService {
    @Value("${algorithm.name}")
    private String ALGORITHM;
    @Value("${algorithm.secret-key}")
    private String SECRET_KEY;

    @Value("${aws.bucket-name}")
    private String BUCKET_NAME;

    @Autowired
    private AmazonS3 amazonS3;

    public String generateLicense(License license) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(license);
        byte[] encrypted = encrypt(jsonString.getBytes(StandardCharsets.UTF_8));
        String base64Encoded = Base64.getEncoder().encodeToString(encrypted);
        String fileName = license.getSoftwareName() + ".txt";
        String content = writeToFile(fileName, base64Encoded);
        return content;
    }

    public License decryptLicense(MultipartFile file) throws Exception {
        String licenseContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        byte[] encrypted = Base64.getDecoder().decode(licenseContent);
        String jsonString = decrypt(encrypted);
        ObjectMapper objectMapper = new ObjectMapper();
        License license = objectMapper.readValue(jsonString, License.class);
        return license;
    }


    public List<License> getAllLicenses(String softwareName, String softwareVersion, String vendorName) throws Exception {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(BUCKET_NAME)
                .withPrefix("");
        List<S3ObjectSummary> objects = amazonS3.listObjects(listObjectsRequest).getObjectSummaries();
        List<License> licenses = new ArrayList<>();
        for (S3ObjectSummary object : objects) {
            S3Object s3Object = amazonS3.getObject(BUCKET_NAME, object.getKey());
            String licenseContent = readFromS3Object(s3Object);
            byte[] encrypted = Base64.getDecoder().decode(licenseContent);
            String jsonString = decrypt(encrypted);
            ObjectMapper objectMapper = new ObjectMapper();
            License license = objectMapper.readValue(jsonString, License.class);
            if (softwareName == null || softwareName.equals(license.getSoftwareName())) {
                if (softwareVersion == null || softwareVersion.equals(license.getSoftwareVersion())) {
                    if (vendorName == null || vendorName.equals(license.getVendorName())) {
                        licenses.add(license);
                    }
                }
            }
        }
        return licenses;
    }



    public License getLicenseByLicenseNo(String licenseNumber) throws Exception {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(BUCKET_NAME)
                .withPrefix("");
        List<S3ObjectSummary> objects = amazonS3.listObjects(listObjectsRequest).getObjectSummaries();
        for (S3ObjectSummary object : objects) {
            if (!object.getKey().endsWith(".txt")) {
                continue;
            }
            S3Object s3Object = amazonS3.getObject(BUCKET_NAME, object.getKey());
            String licenseContent = readFromS3Object(s3Object);
            byte[] encrypted = Base64.getDecoder().decode(licenseContent);
            String jsonString = decrypt(encrypted);
            ObjectMapper objectMapper = new ObjectMapper();
            License license = objectMapper.readValue(jsonString, License.class);
            if (licenseNumber == null || licenseNumber.equals(license.getLicenseNumber())) {
                return license;
            }
        }
        return null;
    }
    public boolean deleteLicense(String licenseNumber) throws Exception {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(BUCKET_NAME)
                .withPrefix("");
        List<S3ObjectSummary> objects = amazonS3.listObjects(listObjectsRequest).getObjectSummaries();
        for (S3ObjectSummary object : objects) {
            S3Object s3Object = amazonS3.getObject(BUCKET_NAME, object.getKey());
            String licenseContent = readFromS3Object(s3Object);
            byte[] encrypted = Base64.getDecoder().decode(licenseContent);
            String jsonString = decrypt(encrypted);
            ObjectMapper objectMapper = new ObjectMapper();
            License license = objectMapper.readValue(jsonString, License.class);
            if (licenseNumber == null || licenseNumber.equals(license.getLicenseNumber())) {
                amazonS3.deleteObject(BUCKET_NAME, object.getKey());
                return true;
            }
        }
        return false;
    }

    public boolean updateLicense(License updatedLicense) throws Exception {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(BUCKET_NAME)
                .withPrefix("");
        List<S3ObjectSummary> objects = amazonS3.listObjects(listObjectsRequest).getObjectSummaries();
        for (S3ObjectSummary object : objects) {
            S3Object s3Object = amazonS3.getObject(BUCKET_NAME, object.getKey());
            String licenseContent = readFromS3Object(s3Object);
            byte[] encrypted = Base64.getDecoder().decode(licenseContent);
            String jsonString = decrypt(encrypted);
            ObjectMapper objectMapper = new ObjectMapper();
            License existingLicense = objectMapper.readValue(jsonString, License.class);

            if (updatedLicense.getLicenseNumber() == null || existingLicense.getLicenseNumber().equals(updatedLicense.getLicenseNumber())) {
                existingLicense.setSoftwareName(updatedLicense.getSoftwareName());
                existingLicense.setSoftwareVersion(updatedLicense.getSoftwareVersion());
                existingLicense.setSoftwareDescription(updatedLicense.getSoftwareDescription());
                existingLicense.setVendorName(updatedLicense.getVendorName());
                existingLicense.setVendorUrl(updatedLicense.getVendorUrl());
                existingLicense.setGrantedTo(updatedLicense.getGrantedTo());
                existingLicense.setLicensedTo(updatedLicense.getLicensedTo());
                existingLicense.setPurpose(updatedLicense.getPurpose());
                existingLicense.setAdditionalInformation(updatedLicense.getAdditionalInformation());
                existingLicense.setSoftwareModules(updatedLicense.getSoftwareModules());

                String updatedJsonString = objectMapper.writeValueAsString(existingLicense);
                byte[] updatedEncrypted = encrypt(updatedJsonString.getBytes(StandardCharsets.UTF_8));
                String updatedBase64Encoded = Base64.getEncoder().encodeToString(updatedEncrypted);
                writeToFile(object.getKey(), updatedBase64Encoded);
                return true;
            }
        }
        return false;
    }

    public String validateLicense(String moduleName, Map<String, Object> constraintValues) throws Exception {
        List<S3ObjectSummary> licenseFiles = amazonS3.listObjects(BUCKET_NAME).getObjectSummaries();
        List<License> licenses = new ArrayList<>();
        StringBuilder message = new StringBuilder();
        boolean flag = false;
        for (S3ObjectSummary file : licenseFiles) {
            String licenseContent = readFromS3Object(amazonS3.getObject(BUCKET_NAME, file.getKey()));
            byte[] encrypted = Base64.getDecoder().decode(licenseContent);
            String jsonString = decrypt(encrypted);
            ObjectMapper objectMapper = new ObjectMapper();
            License license = objectMapper.readValue(jsonString, License.class);

            for (SoftwareModule module : license.getSoftwareModules()) {
                if (moduleName.equals(module.getModuleName())) {
                    flag = true;
                    for (Constraint constraint : module.getConstraints()) {
                        Object value = constraintValues.get(constraint.getName());
                        if (value == null) {
                            message.append("Constraint '").append(constraint.getName()).append("' is missing.");
                        } else if (!checkConstraint(value, constraint)) {
                            message.append("Constraint '").append(constraint.getName())
                                    .append("' failed validation. Reason: ").append(constraint.getErrorMessage()).append(".");
                        }
                    }
                }
            }
        }
        if(flag == false){
            message.append("moduleName: '").append(moduleName).append("' not found.");
        }
        return message.toString();
    }

    private boolean checkConstraint(Object value, Constraint constraint) throws ParseException {
        if (value instanceof Number && constraint.getValue() instanceof Number) {
            Number numberValue = (Number) value;
            Number constraintValue = (Number) constraint.getValue();
            switch (constraint.getOperator()) {
                case GREATER_THAN:
                    return numberValue.doubleValue() > constraintValue.doubleValue();
                case GREATER_THAN_OR_EQUALS:
                    return numberValue.doubleValue() >= constraintValue.doubleValue();
                case LESS_THAN:
                    return numberValue.doubleValue() < constraintValue.doubleValue();
                case LESS_THAN_OR_EQUALS:
                    return numberValue.doubleValue() <= constraintValue.doubleValue();
                case EQUALS:
                    return numberValue.doubleValue() == constraintValue.doubleValue();
                case NOT_EQUALS:
                    return numberValue.doubleValue() != constraintValue.doubleValue();
                default:
                    return false;
            }
        }  else if (value instanceof String && constraint.getValue() instanceof String) {
            String dateString = (String) constraint.getValue();
            String dateString2 = (String) value;

            SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd'T'HH:mm:ss");
            Date dateValue = format.parse(dateString);
            Date date = format.parse(dateString2);
            try {
                switch (constraint.getOperator()) {
                    case GREATER_THAN:
                        return date.getTime() > dateValue.getTime();
                    case GREATER_THAN_OR_EQUALS:
                        return date.getTime() >= dateValue.getTime();
                    case LESS_THAN:
                        return date.getTime() < dateValue.getTime();
                    case LESS_THAN_OR_EQUALS:
                        return date.getTime() <= dateValue.getTime();
                    case EQUALS:
                        return date.getTime() == dateValue.getTime();
                    case NOT_EQUALS:
                        return date.getTime() != dateValue.getTime();
                    default:
                        return false;
                }
            } catch (Exception e) {
                String stringValue = (String) value;
                String constraintValue = (String) constraint.getValue();
                switch (constraint.getOperator()) {
                    case EQUALS:
                        return stringValue.equals(constraintValue);
                    case NOT_EQUALS:
                        return !stringValue.equals(constraintValue);
                    default:
                        return false;
                }
            }

        }
        else {
            return false;
        }
    }


    private byte[] encrypt(byte[] input) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(input);
    }
    public String decrypt(byte[] input) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decrypted = cipher.doFinal(input);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private String writeToFile(String fileName, String content) throws IOException {
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentBytes.length);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(contentBytes);
            amazonS3.putObject(BUCKET_NAME, fileName, inputStream, metadata);
            inputStream.close();
            return content;
        }

    public String readFromS3Object(S3Object s3Object) throws IOException {
        InputStream inputStream = s3Object.getObjectContent();
        String content = new String(inputStream.readAllBytes());
        inputStream.close();
        return content;
    }
}
