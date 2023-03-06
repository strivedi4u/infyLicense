package com.infy.license.controller;


import com.infy.license.model.ErrorResponse;
import com.infy.license.model.License;
import com.infy.license.model.LicenseValidationRequest;
import com.infy.license.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
public class LicenseController {
    @Autowired
    private LicenseService licenseService;

    @PostMapping("/")
    public String generateLicence(@RequestBody License license) throws Exception {
        return licenseService.generateLicense(license);
    }


    @PostMapping("/decrypt")
    public ResponseEntity<License> decryptLicense(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok().body(licenseService.decryptLicense(file));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/all")
    public List<License> getAllLicenses(String softwareName, String softwareVersion, String vendorName) throws Exception {
        return licenseService.getAllLicenses(softwareName, softwareVersion, vendorName);
    }

    @GetMapping("/{licenseNumber}")
    public Object getLicenseByLicenseNo(@PathVariable("licenseNumber") String licenseNumber) throws Exception {
        License license = licenseService.getLicenseByLicenseNo(licenseNumber);
        if (license == null){
            return "License not Found";
        }
        return license;
    }

    @DeleteMapping("/{licenseNumber}")
    public String deleteLicense(@PathVariable("licenseNumber") String licenseNumber) throws Exception {
        boolean license = licenseService.deleteLicense(licenseNumber);
        if (license == false){
            return "License not Found";
        }
        return "License have been deleted successfully";
    }

    @PutMapping("/")
    public String updateLicense(@RequestBody License updatedLicense) throws Exception {
        boolean license = licenseService.updateLicense(updatedLicense);
        if (license == false){
            return "License not Found";
        }
        return "License have been updated successfully";
    }


    @PostMapping("/validate")
    public ResponseEntity<?> validateLicense(@RequestBody LicenseValidationRequest request) {
        try {
            String message = licenseService.validateLicense(request.getModuleName(), request.getConstraintValues());
            if (message.isEmpty()) {
                return ResponseEntity.ok("License have been verified successfully");
            } else {
                ErrorResponse errorResponse = new ErrorResponse(message);
                return ResponseEntity.badRequest().body(errorResponse);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during license validation");
        }
    }

}