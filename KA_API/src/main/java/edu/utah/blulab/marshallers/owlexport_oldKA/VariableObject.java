package edu.utah.blulab.marshallers.owlexport_oldKA;

import java.util.ArrayList;

public class VariableObject {
    private String uri, varName, conName, prefCUI, definition;
    private ArrayList<String> category, altLabel, abbrLabel, hiddenLabel, subjExpLabel, altCUI;
    private ArrayList<AxiomObject> axioms, annotations;

    public VariableObject() {
        uri = "";
        varName = "";
        conName = "";
        prefCUI = "";
        definition = "";
        category = new ArrayList<String>();
        altLabel = new ArrayList<String>();
        abbrLabel = new ArrayList<String>();
        hiddenLabel = new ArrayList<String>();
        subjExpLabel = new ArrayList<String>();
        altCUI = new ArrayList<String>();
        axioms = new ArrayList<AxiomObject>();
        annotations = new ArrayList<AxiomObject>();
    }

    public void setType(final ArrayList<String> category) {
        this.category = category;
    }

    public ArrayList<String> getType() {
        return category;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public void setVarName(final String varName) {
        this.varName = varName;
    }

    public String getVarName() {
        return varName;
    }

    public void setConName(final String conName) {
        this.conName = conName;
    }

    public String getConName() {
        return conName;
    }

    public void setPrefCUI(final String prefCUI) {
        this.prefCUI = prefCUI;
    }

    public String getPrefCUI() {
        return prefCUI;
    }

    public void setDefinition(final String definition) {
        this.definition = definition;
    }

    public String getDefinition() {
        return definition;
    }

    public void setAltLabels(final ArrayList<String> altLabel) {
        this.altLabel = altLabel;
    }

    public ArrayList<String> getAltLabels() {
        return altLabel;
    }

    public void setAbbrLabels(final ArrayList<String> abbrLabel) {
        this.abbrLabel = abbrLabel;
    }

    public ArrayList<String> getAbbrLabels() {
        return abbrLabel;
    }

    public void setHiddenLabels(final ArrayList<String> hiddenLabel) {
        this.hiddenLabel = hiddenLabel;
    }

    public ArrayList<String> getHiddenLabels() {
        return hiddenLabel;
    }

    public void setSubjExpLabels(final ArrayList<String> subjExpLabel) {
        this.subjExpLabel = subjExpLabel;
    }

    public ArrayList<String> getSubjExpLabels() {
        return subjExpLabel;
    }

    public void setAltCUI(final ArrayList<String> altCUI) {
        this.altCUI = altCUI;
    }

    public ArrayList<String> getAltCUI() {
        return altCUI;
    }

    public void setAxioms(final ArrayList<AxiomObject> axioms) {
        this.axioms = axioms;
    }

    public ArrayList<AxiomObject> getAxioms() {
        return axioms;
    }

    public void setAnnotations(final ArrayList<AxiomObject> annotations) {
        this.annotations = annotations;
    }

    public ArrayList<AxiomObject> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "Variable: " + varName + "\nURI: " + uri + "\nCategories: " + category.toString() + "\nPreferred Concept Name: " + conName + "\nPref CUI: "
                        + prefCUI + "\nDefinition: " + definition + "\nAlt Labels: " + altLabel.toString() + "\nAxioms: " + axioms.toString()
                        + "\n******************************************************************************";
    }

}
