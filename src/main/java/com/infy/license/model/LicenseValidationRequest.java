package com.infy.license.model;

import java.util.Map;

public class LicenseValidationRequest {
    private String moduleName;
    private Map<String, Object> constraintValues;

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public Map<String, Object> getConstraintValues() {
        return constraintValues;
    }

    public void setConstraintValues(Map<String, Object> constraintValues) {
        this.constraintValues = constraintValues;
    }

}
