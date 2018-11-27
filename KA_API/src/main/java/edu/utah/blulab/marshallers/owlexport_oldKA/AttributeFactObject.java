package edu.utah.blulab.marshallers.owlexport_oldKA;

public class AttributeFactObject {
    private String attURI;
    private String catType;
    private String fieldURI;
    private String listURI;
    private String datatype;
    private int intField;
    private double floatField;
    private String stringField;
    private String booleanField;
    private String integerRangeField;
    private String floatRangeField;
    private String dateRangeField;
    private String textField;
    private String listField;

    public String getAttURI() {
        return attURI;
    }

    public void setAttURI(final String attURI) {
        this.attURI = attURI;
    }

    public String getCatType() {
        return catType;
    }

    public void setCatType(final String catType) {
        this.catType = catType;
    }

    public String getFieldURI() {
        return fieldURI;
    }

    public void setFieldURI(final String fieldURI) {
        this.fieldURI = fieldURI;
    }

    public String getListURI() {
        return listURI;
    }

    public void setListURI(final String listURI) {
        this.listURI = listURI;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(final String datatype) {
        this.datatype = datatype;
    }

    public int getIntField() {
        return intField;
    }

    public void setIntField(final int intField) {
        this.intField = intField;
    }

    public double getFloatField() {
        return floatField;
    }

    public void setFloatField(final double floatField) {
        this.floatField = floatField;
    }

    public String getStringField() {
        return stringField;
    }

    public void setStringField(final String stringField) {
        this.stringField = stringField;
    }

    public String getBooleanField() {
        return booleanField;
    }

    public void setBooleanField(final String booleanField) {
        this.booleanField = booleanField;
    }

    public String getIntegerRangeField() {
        return integerRangeField;
    }

    public void setIntegerRangeField(final String integerRangeField) {
        this.integerRangeField = integerRangeField;
    }

    public String getFloatRangeField() {
        return floatRangeField;
    }

    public void setFloatRangeField(final String floatRangeField) {
        this.floatRangeField = floatRangeField;
    }

    public String getDateRangeField() {
        return dateRangeField;
    }

    public void setDateRangeField(final String dateRangeField) {
        this.dateRangeField = dateRangeField;
    }

    public String getTextField() {
        return textField;
    }

    public void setTextField(final String textField) {
        this.textField = textField;
    }

    public String getListField() {
        return listField;
    }

    public void setListField(final String listField) {
        this.listField = listField;
    }

    @Override
    public String toString() {
        return "AttributeFactObject [fieldURI=" + fieldURI + ", datatype=" + datatype + "]";
    }

}
