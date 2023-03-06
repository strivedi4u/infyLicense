package com.infy.license.model;


import java.util.List;

public class SoftwareModule {
    private String moduleName;
    private String moduleVersion;
    private List<Constraint> constraints;
    private SoftwareModuleInfo module;

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleVersion() {
        return moduleVersion;
    }

    public void setModuleVersion(String moduleVersion) {
        this.moduleVersion = moduleVersion;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<Constraint> constraints) {
        this.constraints = constraints;
    }

    public SoftwareModuleInfo getModule() {
        return module;
    }

    public void setModule(SoftwareModuleInfo module) {
        this.module = module;
    }
}
