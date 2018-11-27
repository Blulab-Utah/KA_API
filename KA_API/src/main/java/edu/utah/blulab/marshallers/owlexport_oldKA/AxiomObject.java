package edu.utah.blulab.marshallers.owlexport_oldKA;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;

public class AxiomObject {
    private String property;
    private String operator;
    private String object;
    private String type, sign, data;
    private boolean isData, isObject;

    public AxiomObject() {
        property = "";
        operator = "";
        object = "";
        type = "";
        sign = "";
        data = "";
        isData = false;
        isObject = false;
    }

    public AxiomObject(final String property, final String operator, final String object, final boolean isData, final boolean isObject) {
        this.property = property;
        this.operator = operator;
        this.object = object;
        this.isData = isData;
        this.isObject = isObject;
    }

    public AxiomObject(final boolean isData, final boolean isObject) {

        this.isData = isData;
        this.isObject = isObject;
    }

    public void setData(final String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setSign(final String sign) {
        this.sign = sign;
    }

    public String getSign() {
        return sign;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setProperty(final String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    public void setOperator(final String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public void setObject(final String object) {
        this.object = object;
    }

    public String getObject() {
        return object;
    }

    public void setIsObject(final boolean isObject) {
        this.isObject = isObject;
    }

    public boolean getIsData() {
        return isData;
    }

    public void setIsData(final boolean isData) {
        this.isData = isData;
    }

    public boolean getIsObject() {
        return isObject;
    }

    public OWLAxiom getAnnotationPropAxiom(final OWLOntology ontology, final OWLClass cls) {
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OWLAnnotation label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(IRI.create(property)), factory.getOWLLiteral(object));
        OWLAxiom annoAx = factory.getOWLAnnotationAssertionAxiom(cls.getIRI(), label);

        return annoAx;
    }

    // create OWLClassExpressions for all object and data properties
    public OWLClassExpression getRestriction(final OWLOntology ontology) {
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        if (isObject) {
            OWLObjectProperty objProp = factory.getOWLObjectProperty(IRI.create(property));
            OWLClass cls = factory.getOWLClass(IRI.create(object));
            OWLClassExpression hasSome = factory.getOWLObjectSomeValuesFrom(objProp, cls);
            return hasSome;
        }

        if (isData) {
            OWLDataProperty dataProp = factory.getOWLDataProperty(IRI.create(property));
            if (type.equalsIgnoreCase("FloatRange")) {
                OWLDatatype floatDatatype = factory.getFloatOWLDatatype();
                Float floatValue = Float.valueOf(data);
                OWLLiteral floatLiteral = factory.getOWLLiteral((float) floatValue);
                OWLFacet facet = null;
                if (sign.equalsIgnoreCase(">")) {
                    facet = OWLFacet.MIN_EXCLUSIVE;
                }
                if (sign.equalsIgnoreCase("<")) {
                    facet = OWLFacet.MAX_EXCLUSIVE;
                }
                OWLDataRange range = factory.getOWLDatatypeRestriction(floatDatatype, facet, floatLiteral);
                OWLClassExpression gtDataExp = factory.getOWLDataSomeValuesFrom(dataProp, range);
                return gtDataExp;
            }
            if (type.equalsIgnoreCase("IntegerRange")) {
                OWLDatatype intDatatype = factory.getIntegerOWLDatatype();
                Integer intValue = Integer.valueOf(data);
                OWLLiteral intLiteral = factory.getOWLLiteral((int) intValue);
                OWLFacet facet = null;
                if (sign.equalsIgnoreCase(">")) {
                    facet = OWLFacet.MIN_EXCLUSIVE;
                }
                if (sign.equalsIgnoreCase("<")) {
                    facet = OWLFacet.MAX_EXCLUSIVE;
                }
                OWLDataRange range = factory.getOWLDatatypeRestriction(intDatatype, facet, intLiteral);
                OWLClassExpression gtDataExp = factory.getOWLDataSomeValuesFrom(dataProp, range);
                return gtDataExp;
            }
            if (type.equalsIgnoreCase("DateRange")) {
                OWLDatatype dateDatatype = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
                OWLLiteral dateLit = factory.getOWLLiteral(data, dateDatatype);
                OWLFacet facet = null;
                if (sign.equalsIgnoreCase(">")) {
                    facet = OWLFacet.MIN_EXCLUSIVE;
                }
                if (sign.equalsIgnoreCase("<")) {
                    facet = OWLFacet.MAX_EXCLUSIVE;
                }
                OWLDataRange range = factory.getOWLDatatypeRestriction(dateDatatype, facet, dateLit);
                OWLClassExpression exp = factory.getOWLDataSomeValuesFrom(dataProp, range);
                return exp;
            }
            if (type.equalsIgnoreCase("AnyInteger")) {
                OWLDataRange range = factory.getIntegerOWLDatatype();
                OWLClassExpression exp = factory.getOWLDataSomeValuesFrom(dataProp, range);
                return exp;
            }
            if (type.equalsIgnoreCase("AnyFloat")) {
                OWLDataRange range = factory.getFloatOWLDatatype();
                OWLClassExpression exp = factory.getOWLDataSomeValuesFrom(dataProp, range);
                return exp;
            }if (type.equalsIgnoreCase("AnyDate")) {
                OWLDataRange range = factory.getOWLDatatype(OWL2Datatype.XSD_DATE_TIME.getIRI());
                OWLClassExpression exp = factory.getOWLDataSomeValuesFrom(dataProp, range);
                return exp;
            }
            
        }

        return null;
    }

    public static Date getDate(final String str) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("mm/dd/yyyy");
        Date date = formatter.parse(str);
        return date;
    }

    @Override
    public String toString() {
        String type = "";
        if (isData) {
            type = "DataProperty";
        } else if (isObject) {
            type = "ObjectProperty";
        }

        return type + ": " + property + " " + operator + " " + object + "\n";

    }

}
