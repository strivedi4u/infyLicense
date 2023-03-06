package com.infy.license.model;


public class Constraint {
    private String name;
    private Object value;
    private ConstraintOperator operator;
    private String errorMessage;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public ConstraintOperator getOperator() {
        return operator;
    }

    public void setOperator(ConstraintOperator operator) {
        this.operator = operator;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}