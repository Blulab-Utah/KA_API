package edu.utah.blulab.marshallers.graph;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.neo4j.driver.internal.value.NodeValue;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Relationship;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentTarget;
import uk.ac.manchester.cs.owl.owlapi.OWLDataOneOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplPlain;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplString;

import java.util.*;
import java.io.*;

import static org.neo4j.driver.v1.Values.parameters;
import static org.semanticweb.owlapi.vocab.OWLFacet.MAX_EXCLUSIVE;
import static org.semanticweb.owlapi.vocab.OWLFacet.MIN_INCLUSIVE;

public class KAtoOntology implements AutoCloseable
{
    private final Driver driverNeo4j;
    private final OWLOntologyManager managerOWL;
    //private final OWLDataFactory factoryOWL;
    OWLOntology ontology;
    String ontURI;
    IRI ontologyIRI;
    OWLDataFactory factoryOWL;

    public KAtoOntology( String uri, String user, String password, String ontURI )
    {
        driverNeo4j = GraphDatabase.driver( uri, AuthTokens.basic( user, password ) );
        managerOWL = OWLManager.createOWLOntologyManager();
        ontologyIRI = IRI.create(ontURI);
        try {
            ontology = managerOWL.createOntology(ontologyIRI);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        this.ontURI = ontURI;
        factoryOWL = managerOWL.getOWLDataFactory();
    }

    public static void main( String... args ) throws Exception
    {
        final long startTime = System.currentTimeMillis();
        String neo4jURL = "bolt://localhost:7687";
        String username =  "neo4j";
        String password = "chuck6";
        String ontURI = "http://blulab.chpc.utah.edu/ontologies/examples/smoking.owl";
        //String ontURI = "http://blulab.chpc.utah.edu/ontologies/examples/heartDiseaseInDiabetics.owl";
        try ( KAtoOntology kaDB = new KAtoOntology( neo4jURL, username, password, ontURI ) )
        {
            //kadb.printGreeting( "hello, world" );
            //int thingID = kaDB.getThingNode();
            //kaDB.getNodes();
            //kaDB.getRelationshipIDs();

            List<Triple> tripleList = kaDB.getParentChildTripleAll();
            kaDB.writeToOWL(tripleList);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Execution time: " + (endTime - startTime) / 1000 + " sec");
    }

    @Override
    public void close() throws Exception
    {
        driverNeo4j.close();
    }

    @SuppressWarnings("Duplicates")
    public void writeToOWL (List<Triple> tripleList) throws OWLOntologyCreationException,
            OWLOntologyStorageException {

        // create a list of all classes and create an owl node for each
        //HashMap<Long, Node> classMap = new HashMap<>();
        //HashMap<Long, OWLClass> OWLclassMap = new HashMap<>();
        for (Triple triple:tripleList){

            Relationship rel = triple.getRel();
            Triple.Direction dir = triple.getDirection();
            Node node1;
            Node node2;
            if (dir.equals(Triple.Direction.LEFT)) {
                node1 = triple.getNode1();
                node2 = triple.getNode2();
            } else if (dir.equals(Triple.Direction.RIGHT)){
                node2 = triple.getNode1();
                node1 = triple.getNode2();
            } else {
                node1 = triple.getNode1();
                node2 = triple.getNode2();
            }



            //classMap.put(node1.id(), node1);
            //classMap.put(node2.id(), node2);

            //skip the ontology node that KA creates
            if (node1.get("uri").asString().equals(ontologyIRI.toString()) | node2.get("uri").asString().equals(ontologyIRI.toString())){
                continue;
            }

            OWLClass nodeClass1 = factoryOWL.getOWLClass(IRI.create(node1.get("uri").asString()));
            OWLClass nodeClass2 = factoryOWL.getOWLClass(IRI.create(node2.get("uri").asString()));
            // write node properties to owl annotation properties of the class
            for (String property : node1.keys()){
                if (property.equals("name") | property.equals("uri")){
                    continue;
                }
                String propertyValue = node1.get(property).asString();
                // parse multiple values from string ('|' delimited)
                for (String val : propertyValue.split("\\|")) {
                    addAnnotationToClass(property, val, nodeClass1);
                }
            }

            // handle synonyms, misspellings, regex and abbreviations
//            if (dir.equals(Triple.Direction.RIGHT)) {
//                if (rel.hasType("hasSynonym")) {
//                    addAnnotationToClass("synonym", node1.get("name").asString(), nodeClass2);
//                    continue; // if it is a synonym, skip adding the relationship to the graph
//                } else if (rel.hasType("hasMisspelling")) {
//                    addAnnotationToClass("misspelling", node1.get("name").asString(), nodeClass2);
//                    continue; // if it is a synonym, skip adding the relationship to the graph
//                } else if (rel.hasType("hasAbbreviation")) {
//                    addAnnotationToClass("abbreviation", node1.get("name").asString(), nodeClass2);
//                    continue; // if it is a synonym, skip adding the relationship to the graph
//                } else if (rel.hasType("hasRegex")) {
//                    addAnnotationToClass("regex", node1.get("name").asString(), nodeClass2);
//                    continue; // if it is a synonym, skip adding the relationship to the graph
//                } else if (rel.hasType("hasSubjectiveExpression")) {
//                    addAnnotationToClass("subjectiveExpression", node1.get("name").asString(), nodeClass2);
//                    continue; // if it is a synonym, skip adding the relationship to the graph
//                }
//            } else if (dir.equals(Triple.Direction.LEFT)) {
//                if (rel.hasType("hasSynonym")) {
//                    addAnnotationToClass("synonym", node2.get("name").asString(), nodeClass1);
//                    continue; // if it is a synonym, skip adding the relationship to the graph
//                } else if (rel.hasType("hasMisspelling")) {
//                    addAnnotationToClass("misspelling", node2.get("name").asString(), nodeClass1);
//                    continue; // if it is a synonym, skip adding the relationship to the graph
//                } else if (rel.hasType("hasAbbreviation")) {
//                    addAnnotationToClass("abbreviation", node2.get("name").asString(), nodeClass1);
//                    continue; // if it is a synonym, skip adding the relationship to the graph
//                } else if (rel.hasType("hasRegex")) {
//                    addAnnotationToClass("regex", node2.get("name").asString(), nodeClass1);
//                    continue; // if it is a synonym, skip adding the relationship to the graph
//                } else if (rel.hasType("hasSubjectiveExpression")) {
//                    addAnnotationToClass("subjectiveExpression", node2.get("name").asString(), nodeClass1);
//                    continue; // if it is a synonym, skip adding the relationship to the graph
//                }
//            }

            // add relationships between nodes
            if (rel.type().equals("IS_A")){
                if (dir.equals(Triple.Direction.LEFT)){
                    OWLAxiom axiom = factoryOWL.getOWLSubClassOfAxiom(nodeClass2, nodeClass1);
                    managerOWL.applyChange(new AddAxiom(ontology, axiom));
                } else if (dir.equals(Triple.Direction.RIGHT)){
                    OWLAxiom axiom = factoryOWL.getOWLSubClassOfAxiom(nodeClass1, nodeClass2);
                    managerOWL.applyChange(new AddAxiom(ontology, axiom));
                }
            } else {






                    if (node1.hasLabel("DATATYPE")){
                        OWLDatatype nodeData1 = factoryOWL.getOWLDatatype(IRI.create(node1.get("uri").asString()));
                        OWLDataProperty dataProp = factoryOWL.getOWLDataProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
                        OWLDataSomeValuesFrom hasSome = factoryOWL.getOWLDataSomeValuesFrom(dataProp, nodeData1);
                        OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass2, hasSome);
                        managerOWL.applyChange(new AddAxiom(ontology, subAx));
                    } else if (node1.hasLabel("DATA_ONE_OF")){
                        OWLDatatype nodeData1 = factoryOWL.getOWLDatatype(IRI.create(node1.get("uri").asString()));
                        String[] dataOneOfStrArr = node1.get("name").asString().split("\\|");
                        Set<OWLLiteral> literalSet = new HashSet<>();
                        //OWLDataType litDataType = OWLDatatypeImpl(IRI iri)
                        for (String litStr : dataOneOfStrArr){
//                            OWLLiteral lit4 = new OWLLiteralImplString(litStr);
//                            OWLLiteral lit2 = factoryOWL.getOWLLiteral(litStr);
                            OWLLiteral lit3 = new OWLLiteralImplPlain(litStr, "en");
                            OWLLiteral lit = new OWLLiteralImpl(litStr, "en", nodeData1);
                            literalSet.add(lit);
                        }
                        OWLDataOneOf dataOneOf = factoryOWL.getOWLDataOneOf(literalSet);
                        //OWLDatatype nodeData1 = factoryOWL.getOWLDatatype(IRI.create(node1.get("uri").asString()));
                        OWLDataProperty dataProp = factoryOWL.getOWLDataProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
                        OWLDataSomeValuesFrom hasSome = factoryOWL.getOWLDataSomeValuesFrom(dataProp, dataOneOf);
                        OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass2, hasSome);
                        managerOWL.applyChange(new AddAxiom(ontology, subAx));
                    } else {
                        OWLObjectProperty objProp = factoryOWL.getOWLObjectProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
                        OWLClassExpression hasSome = factoryOWL.getOWLObjectSomeValuesFrom(objProp, nodeClass1);
                        OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass2, hasSome);
                        managerOWL.applyChange(new AddAxiom(ontology, subAx));
                    }

                    if (node2.hasLabel("DATATYPE")){
                        OWLDatatype nodeData2 = factoryOWL.getOWLDatatype(IRI.create(node2.get("uri").asString()));
                        OWLDataProperty dataProp = factoryOWL.getOWLDataProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
                        OWLDataSomeValuesFrom hasSome = factoryOWL.getOWLDataSomeValuesFrom(dataProp, nodeData2);
                        OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass1, hasSome);
                        managerOWL.applyChange(new AddAxiom(ontology, subAx));
                    } else {
                        OWLObjectProperty objProp = factoryOWL.getOWLObjectProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
                        OWLClassExpression hasSome = factoryOWL.getOWLObjectSomeValuesFrom(objProp, nodeClass2);
                        OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass1, hasSome);
                        managerOWL.applyChange(new AddAxiom(ontology, subAx));
                    }

//                if (dir.equals(Triple.Direction.LEFT)){
//                    if (node1.hasLabel("DATATYPE")){
//                        OWLDatatype nodeData1 = factoryOWL.getOWLDatatype(IRI.create(node1.get("uri").asString()));
//                        OWLDataProperty dataProp = factoryOWL.getOWLDataProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
//                        OWLDataSomeValuesFrom hasSome = factoryOWL.getOWLDataSomeValuesFrom(dataProp, nodeData1);
//                        OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass2, hasSome);
//                        managerOWL.applyChange(new AddAxiom(ontology, subAx));
//                    } else if (node1.hasLabel("DATA_ONE_OF")){
//                        OWLDatatype nodeData1 = factoryOWL.getOWLDatatype(IRI.create(node1.get("uri").asString()));
//                        String[] dataOneOfStrArr = node1.get("name").asString().split("\\|");
//                        Set<OWLLiteral> literalSet = new HashSet<>();
//                        //OWLDataType litDataType = OWLDatatypeImpl(IRI iri)
//                        for (String litStr : dataOneOfStrArr){
////                            OWLLiteral lit4 = new OWLLiteralImplString(litStr);
////                            OWLLiteral lit2 = factoryOWL.getOWLLiteral(litStr);
//                            OWLLiteral lit3 = new OWLLiteralImplPlain(litStr, "en");
//                            OWLLiteral lit = new OWLLiteralImpl(litStr, "en", nodeData1);
//                            literalSet.add(lit);
//                        }
//                        OWLDataOneOf dataOneOf = factoryOWL.getOWLDataOneOf(literalSet);
//                        //OWLDatatype nodeData1 = factoryOWL.getOWLDatatype(IRI.create(node1.get("uri").asString()));
//                        OWLDataProperty dataProp = factoryOWL.getOWLDataProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
//                        OWLDataSomeValuesFrom hasSome = factoryOWL.getOWLDataSomeValuesFrom(dataProp, dataOneOf);
//                        OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass2, hasSome);
//                        managerOWL.applyChange(new AddAxiom(ontology, subAx));
//                    } else {
//                        OWLObjectProperty objProp = factoryOWL.getOWLObjectProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
//                        OWLClassExpression hasSome = factoryOWL.getOWLObjectSomeValuesFrom(objProp, nodeClass1);
//                        OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass2, hasSome);
//                        managerOWL.applyChange(new AddAxiom(ontology, subAx));
//                    }
//                } else if (dir.equals(Triple.Direction.RIGHT)){
//                    if (node2.hasLabel("DATATYPE")){
//                        OWLDatatype nodeData2 = factoryOWL.getOWLDatatype(IRI.create(node2.get("uri").asString()));
//                        OWLDataProperty dataProp = factoryOWL.getOWLDataProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
//                        OWLDataSomeValuesFrom hasSome = factoryOWL.getOWLDataSomeValuesFrom(dataProp, nodeData2);
//                        OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass1, hasSome);
//                        managerOWL.applyChange(new AddAxiom(ontology, subAx));
//                    } else {
//                        OWLObjectProperty objProp = factoryOWL.getOWLObjectProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
//                        OWLClassExpression hasSome = factoryOWL.getOWLObjectSomeValuesFrom(objProp, nodeClass2);
//                        OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass1, hasSome);
//                        managerOWL.applyChange(new AddAxiom(ontology, subAx));
//                    }
//                }
            }
        }

//        // create a owl class for each node
//        HashMap<Long, OWLClass> OWLclassMap = new HashMap<>();
//        for (Node node:classMap.values()){
//            OWLClass nodeClass = factoryOWL.getOWLClass(IRI.create(node.get("uri").asString()));
//            OWLclassMap.put(node.id(), nodeClass);
//        }


//        OWLClassExpression hasSome = factoryOWL.getOWLObjectSomeValuesFrom(hasWife, person);
//        //OWLClass child = factoryOWL.getOWLClass(IRI.create(v.getUri()));
//        OWLClassExpression exp = ax.getRestriction(ontology);
//
//        OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(child, exp);
//        managerOWL.applyChange(new AddAxiom(ontology, subAx));


//        // save it (in a variety of formats). RDF/XML is the default format
//        System.out.println("RDF/XML: ");
//        managerOWL.saveOntology(ontology, new StreamDocumentTarget(System.out));
//        // OWL/XML
//        System.out.println("OWL/XML: ");
//        managerOWL.saveOntology(ontology, new OWLXMLOntologyFormat(),
//                new StreamDocumentTarget(System.out));
//        // Manchester Syntax
//        System.out.println("Manchester syntax: ");
//        managerOWL.saveOntology(ontology, new ManchesterOWLSyntaxOntologyFormat(),
//                new StreamDocumentTarget(System.out));
//        // Turtle
//        System.out.println("Turtle: ");
//        managerOWL.saveOntology(ontology, new TurtleOntologyFormat(),
//                new StreamDocumentTarget(System.out));

        FileDocumentTarget fileOut = new FileDocumentTarget(new File("C:\\Users\\Bill\\Desktop\\test.owl"));
        managerOWL.saveOntology(ontology, new OWLXMLOntologyFormat(), fileOut);

    }

    private void addAnnotationToClass(String property, String propertyValue, OWLClass nodeClass){
        OWLAnnotationProperty annoProp = getAnnotationProp(property);
        OWLAnnotation commentAnno = factoryOWL.getOWLAnnotation(annoProp, factoryOWL.getOWLLiteral(propertyValue));
        OWLAxiom ax = factoryOWL.getOWLAnnotationAssertionAxiom(nodeClass.getIRI(), commentAnno);
        managerOWL.applyChange(new AddAxiom(ontology, ax));
    }

    private OWLAnnotationProperty getAnnotationProp(String key) {
        if (key.equals("label")){
            return factoryOWL.getRDFSLabel();
        } else if (key.equals("comment")){
            return factoryOWL.getRDFSComment();
        } else if (key.equals("backwardCompatibleWith")){
            return factoryOWL.getOWLBackwardCompatibleWith();
        } else if (key.equals("depricated")){
            return factoryOWL.getOWLDeprecated();
        } else if (key.equals("incompatibleWith")){
            return factoryOWL.getOWLIncompatibleWith();
        } else if (key.equals("versionInfo")){
            return factoryOWL.getOWLVersionInfo();
        } else if (key.equals("isDefiniedBy")){
            return factoryOWL.getRDFSIsDefinedBy();
        } else if (key.equals("seeAlso")){
            return factoryOWL.getRDFSSeeAlso();
        } else if (key.equals("isRuleEnabled")){
            return factoryOWL.getOWLAnnotationProperty(IRI.create("http://swrl.stanford.edu/ontologies/3.3/swrla.owl#isRuleEnabled"));
        } else if (key.equals("license")){
            return factoryOWL.getOWLAnnotationProperty(IRI.create("http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl#license"));
        } else {
            return factoryOWL.getOWLAnnotationProperty(IRI.create("http://blulab.chpc.utah.edu/ontologies/TermMapping.owl#" + key));
        }
    }

    public void examples () throws OWLOntologyCreationException,
                OWLOntologyStorageException {
        int x = 1;
        // The OWLOntologyManager is at the heart of the OWL API, we can create
        // an instance of this using the OWLManager class, which will set up
        // commonly used options (such as which parsers are registered etc.
        // etc.)
        //OWLOntologyManager managerOWL = OWLManager.createOWLOntologyManager();
            // We want to create an ontology that corresponds to the ontology used
            // in the OWL Primer. Every ontology has a URI that uniquely identifies
            // the ontology. The URI is essentially a name for the ontology. Note
            // that the URI doesn't necessarily point to a location on the web - in
            // this example, we won't publish the ontology at the URL corresponding
            // to the ontology URI below.
        //IRI ontologyIRI = IRI.create(ontURI);
            // Now that we have a URI for out ontology, we can create the actual
            // ontology. Note that the create ontology method throws an
            // OWLOntologyCreationException if there was a problem creating the
            // ontology.
        //OWLOntology ontology = managerOWL.createOntology(ontologyIRI);
            // We can use the manager to get a reference to an OWLDataFactory. The
            // data factory provides a point for creating OWL API objects such as
            // classes, properties and individuals.
        //OWLDataFactory factoryOWL = managerOWL.getOWLDataFactory();
            // We first need to create some references to individuals. All of our
            // individual must have URIs. A common convention is to take the URI of
            // an ontology, append a # and then a local name. For example we can
            // create the individual 'John', using the ontology URI and appending
            // #John. Note however, that there is no reuqirement that a URI of a
            // class, property or individual that is used in an ontology have a
            // correspondance with the URI of the ontology.
            OWLIndividual john = factoryOWL.getOWLNamedIndividual(IRI
                    .create(ontologyIRI + "#John"));
            OWLIndividual mary = factoryOWL.getOWLNamedIndividual(IRI
                    .create(ontologyIRI + "#Mary"));
            OWLIndividual susan = factoryOWL.getOWLNamedIndividual(IRI
                    .create(ontologyIRI + "#Susan"));
            OWLIndividual bill = factoryOWL.getOWLNamedIndividual(IRI
                    .create(ontologyIRI + "#Bill"));
            // The ontologies that we created aren't contained in any ontology at
            // the moment. Individuals (or classes or properties) can't directly be
            // added to an ontology, they have to be used in axioms, and then the
            // axioms are added to an ontology. We now want to add some facts to the
            // ontology. These facts are otherwise known as property assertions. In
            // our case, we want to say that John has a wife Mary. To do this we
            // need to have a reference to the hasWife object property (object
            // properties link an individual to an individual, and data properties
            // link and individual to a constant - here, we need an object property
            // because John and Mary are individuals).
            OWLObjectProperty hasWife = factoryOWL.getOWLObjectProperty(IRI
                    .create(ontologyIRI + "#hasWife"));
            // Now we need to create the assertion that John hasWife Mary. To do
            // this we need an axiom, in this case an object property assertion
            // axiom. This can be thought of as a "triple" that has a subject, john,
            // a predicate, hasWife and an object Mary
            OWLObjectPropertyAssertionAxiom axiom1 = factoryOWL
                    .getOWLObjectPropertyAssertionAxiom(hasWife, john, mary);
            // We now need to add this assertion to our ontology. To do this, we
            // apply an ontology change to the ontology via the OWLOntologyManager.
            // First we create the change object that will tell the manager that we
            // want to add the axiom to the ontology
            AddAxiom addAxiom1 = new AddAxiom(ontology, axiom1);
            // Now we apply the change using the manager.
            managerOWL.applyChange(addAxiom1);
            // Now we want to add the other facts/assertions to the ontology John
            // hasSon Bill Get a refernece to the hasSon property
            OWLObjectProperty hasSon = factoryOWL.getOWLObjectProperty(IRI
                    .create(ontologyIRI + "#hasSon"));
            // Create the assertion, John hasSon Bill
            OWLAxiom axiom2 = factoryOWL.getOWLObjectPropertyAssertionAxiom(hasSon,
                    john, bill);
            // Apply the change
            managerOWL.applyChange(new AddAxiom(ontology, axiom2));
            // John hasDaughter Susan
            OWLObjectProperty hasDaughter = factoryOWL.getOWLObjectProperty(IRI
                    .create(ontologyIRI + "#hasDaughter"));
            OWLAxiom axiom3 = factoryOWL.getOWLObjectPropertyAssertionAxiom(
                    hasDaughter, john, susan);
            managerOWL.applyChange(new AddAxiom(ontology, axiom3));
            // John hasAge 33 In this case, hasAge is a data property, which we need
            // a reference to
            OWLDataProperty hasAge = factoryOWL.getOWLDataProperty(IRI
                    .create(ontologyIRI + "#hasAge"));
            // We create a data property assertion instead of an object property
            // assertion
            OWLAxiom axiom4 = factoryOWL.getOWLDataPropertyAssertionAxiom(hasAge,
                    john, 33);
            managerOWL.applyChange(new AddAxiom(ontology, axiom4));
            // In the above code, 33 is an integer, so we can just pass 33 into the
            // data factory method. Behind the scenes the OWL API will create a
            // typed constant that it will use as the value of the data property
            // assertion. We could have manually created the constant as follows:
            OWLDatatype intDatatype = factoryOWL.getIntegerOWLDatatype();
            OWLLiteral thirtyThree = factoryOWL.getOWLLiteral("33", intDatatype);
            // We would then create the axiom as follows:
            factoryOWL.getOWLDataPropertyAssertionAxiom(hasAge, john, thirtyThree);
            // However, the convenice method is much shorter! We can now create the
            // other facts/assertion for Mary. The OWL API uses a change object
            // model, which means we can stack up changes (or sets of axioms) and
            // apply the changes (or add the axioms) in one go. We will do this for
            // Mary
            Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
            axioms.add(factoryOWL.getOWLObjectPropertyAssertionAxiom(hasSon, mary,
                    bill));
            axioms.add(factoryOWL.getOWLObjectPropertyAssertionAxiom(hasDaughter,
                    mary, susan));
            axioms.add(factoryOWL.getOWLDataPropertyAssertionAxiom(hasAge, mary, 31));
            // Add facts/assertions for Bill and Susan
            axioms.add(factoryOWL.getOWLDataPropertyAssertionAxiom(hasAge, bill, 13));
            axioms.add(factoryOWL.getOWLDataPropertyAssertionAxiom(hasAge, mary, 8));
            // Now add all the axioms in one go - there is a convenience method on
            // OWLOntologyManager that will automatically generate the AddAxiom
            // change objects for us. We need to specify the ontology that the
            // axioms should be added to and the axioms to add.
            managerOWL.addAxioms(ontology, axioms);
            // Now specify the genders of John, Mary, Bill and Susan. To do this we
            // need the male and female individuals and the hasGender object
            // property.
            OWLIndividual male = factoryOWL.getOWLNamedIndividual(IRI
                    .create(ontologyIRI + "#male"));
            OWLIndividual female = factoryOWL.getOWLNamedIndividual(IRI
                    .create(ontologyIRI + "#female"));
            OWLObjectProperty hasGender = factoryOWL.getOWLObjectProperty(IRI
                    .create(ontologyIRI + "#hasGender"));
            Set<OWLAxiom> genders = new HashSet<OWLAxiom>();
            genders.add(factoryOWL.getOWLObjectPropertyAssertionAxiom(hasGender, john,
                    male));
            genders.add(factoryOWL.getOWLObjectPropertyAssertionAxiom(hasGender, mary,
                    female));
            genders.add(factoryOWL.getOWLObjectPropertyAssertionAxiom(hasGender, bill,
                    male));
            genders.add(factoryOWL.getOWLObjectPropertyAssertionAxiom(hasGender,
                    susan, female));
            // Add the facts about the genders
            managerOWL.addAxioms(ontology, genders);
            // Domain and Range Axioms //At this point, we have an ontology
            // containing facts about several individuals. We now want to specify
            // more information about the various properties that we have used. We
            // want to say that the domains and ranges of hasWife, hasSon and
            // hasDaughter are the class Person. To do this we need various domain
            // and range axioms, and we need a reference to the class Person First
            // get a reference to the person class
            OWLClass person = factoryOWL.getOWLClass(IRI.create(ontologyIRI
                    + "#Person"));
            // Now we add the domain and range axioms that specify the domains and
            // ranges of the various properties that we are interested in.
            Set<OWLAxiom> domainsAndRanges = new HashSet<OWLAxiom>();
            // Domain and then range of hasWife
            domainsAndRanges.add(factoryOWL.getOWLObjectPropertyDomainAxiom(hasWife,
                    person));
            domainsAndRanges.add(factoryOWL.getOWLObjectPropertyRangeAxiom(hasWife,
                    person));
            // Domain and range of hasSon and also hasDaugher
            domainsAndRanges.add(factoryOWL.getOWLObjectPropertyDomainAxiom(hasSon,
                    person));
            domainsAndRanges.add(factoryOWL.getOWLObjectPropertyRangeAxiom(hasSon,
                    person));
            domainsAndRanges.add(factoryOWL.getOWLObjectPropertyDomainAxiom(
                    hasDaughter, person));
            domainsAndRanges.add(factoryOWL.getOWLObjectPropertyRangeAxiom(
                    hasDaughter, person));
            // We also have the domain of the data property hasAge as Person, and
            // the range as integer. We need the integer datatype. The XML Schema
            // Datatype URIs are used for data types. The OWL API provide a built in
            // set via the XSDVocabulary enum.
            domainsAndRanges.add(factoryOWL.getOWLDataPropertyDomainAxiom(hasAge,
                    person));
            OWLDatatype integerDatatype = factoryOWL.getIntegerOWLDatatype();
            domainsAndRanges.add(factoryOWL.getOWLDataPropertyRangeAxiom(hasAge,
                    integerDatatype));
            // Now add all of our domain and range axioms
            managerOWL.addAxioms(ontology, domainsAndRanges);
            // Class assertion axioms //We can also explicitly say than an
            // individual is an instance of a given class. To do this we use a Class
            // assertion axiom.
            OWLClassAssertionAxiom classAssertionAx = factoryOWL
                    .getOWLClassAssertionAxiom(person, john);
            // Add the axiom directly using the addAxiom convenience method on
            // OWLOntologyManager
            managerOWL.addAxiom(ontology, classAssertionAx);
            // Inverse property axioms //We can specify the inverse property of
            // hasWife as hasHusband We first need a reference to the hasHusband
            // property.
            OWLObjectProperty hasHusband = factoryOWL.getOWLObjectProperty(IRI
                    .create(ontology.getOntologyID().getOntologyIRI() + "#hasHusband"));
            // The full URI of the hasHusband property will be
            // http://example.com/owlapi/families#hasHusband since the URI of our
            // ontology is http://example.com/owlapi/families Create the inverse
            // object properties axiom and add it
            managerOWL.addAxiom(ontology,
                    factoryOWL.getOWLInverseObjectPropertiesAxiom(hasWife, hasHusband));
            // Sub property axioms //OWL allows a property hierarchy to be
            // specified. Here, hasSon and hasDaughter will be specified as
            // hasChild.
            OWLObjectProperty hasChild = factoryOWL.getOWLObjectProperty(IRI
                    .create(ontology.getOntologyID().getOntologyIRI() + "#hasChild"));
            OWLSubObjectPropertyOfAxiom hasSonSubHasChildAx = factoryOWL
                    .getOWLSubObjectPropertyOfAxiom(hasSon, hasChild);
            // Add the axiom
            managerOWL.addAxiom(ontology, hasSonSubHasChildAx);
            // And hasDaughter, which is also a sub property of hasChild
            managerOWL.addAxiom(ontology,
                    factoryOWL.getOWLSubObjectPropertyOfAxiom(hasDaughter, hasChild));
            // Property characteristics //Next, we want to say that the hasAge
            // property is Functional. This means that something can have at most
            // one hasAge property. We can do this with a functional data property
            // axiom First create the axiom
            OWLFunctionalDataPropertyAxiom hasAgeFuncAx = factoryOWL
                    .getOWLFunctionalDataPropertyAxiom(hasAge);
            // Now add it to the ontology
            managerOWL.addAxiom(ontology, hasAgeFuncAx);
            // The hasWife property should be Functional, InverseFunctional,
            // Irreflexive and Asymmetric. Note that the asymmetric property axiom
            // used to be called antisymmetric - older versions of the OWL API may
            // refer to antisymmetric property axioms
            Set<OWLAxiom> hasWifeAxioms = new HashSet<OWLAxiom>();
            hasWifeAxioms.add(factoryOWL.getOWLFunctionalObjectPropertyAxiom(hasWife));
            hasWifeAxioms.add(factoryOWL
                    .getOWLInverseFunctionalObjectPropertyAxiom(hasWife));
            hasWifeAxioms
                    .add(factoryOWL.getOWLIrreflexiveObjectPropertyAxiom(hasWife));
            hasWifeAxioms.add(factoryOWL.getOWLAsymmetricObjectPropertyAxiom(hasWife));
            // Add all of the axioms that specify the characteristics of hasWife
            managerOWL.addAxioms(ontology, hasWifeAxioms);
            // SubClass axioms //Now we want to start specifying something about
            // classes in our ontology. To begin with we will simply say something
            // about the relationship between named classes Besides the Person class
            // that we already have, we want to say something about the classes Man,
            // Woman and Parent. To say something about these classes, as usual, we
            // need references to them:
            OWLClass man = factoryOWL.getOWLClass(IRI.create(ontologyIRI + "#Man"));
            OWLClass woman = factoryOWL
                    .getOWLClass(IRI.create(ontologyIRI + "#Woman"));
            OWLClass parent = factoryOWL.getOWLClass(IRI.create(ontologyIRI
                    + "#Parent"));
            // It is important to realise that simply getting references to a class
            // via the data factory does not add them to an ontology - only axioms
            // can be added to an ontology. Now say that Man, Woman and Parent are
            // subclasses of Person
            managerOWL.addAxiom(ontology, factoryOWL.getOWLSubClassOfAxiom(man, person));
            managerOWL.addAxiom(ontology, factoryOWL.getOWLSubClassOfAxiom(woman, person));
            managerOWL.addAxiom(ontology, factoryOWL.getOWLSubClassOfAxiom(parent, person));
            // Restrictions //Now we want to say that Person has exactly 1 Age,
            // exactly 1 Gender and, only has gender that is male or female. We will
            // deal with these restrictions one by one and then combine them as a
            // superclass (Necessary conditions) of Person. All anonymous class
            // expressions extend OWLClassExpression. First, hasAge exactly 1
            OWLDataExactCardinality hasAgeRestriction = factoryOWL
                    .getOWLDataExactCardinality(1, hasAge);
            // Now the hasGender exactly 1
            OWLObjectExactCardinality hasGenderRestriction = factoryOWL
                    .getOWLObjectExactCardinality(1, hasGender);
            // And finally, the hasGender only {male female} To create this
            // restriction, we need an OWLObjectOneOf class expression since male
            // and female are individuals We can just list as many individuals as we
            // need as the argument of the method.
            OWLObjectOneOf maleOrFemale = factoryOWL.getOWLObjectOneOf(male, female);
            // Now create the actual restriction
            OWLObjectAllValuesFrom hasGenderOnlyMaleFemale = factoryOWL
                    .getOWLObjectAllValuesFrom(hasGender, maleOrFemale);
            // Finally, we bundle these restrictions up into an intersection, since
            // we want person to be a subclass of the intersection of them
            OWLObjectIntersectionOf intersection = factoryOWL
                    .getOWLObjectIntersectionOf(hasAgeRestriction,
                            hasGenderRestriction, hasGenderOnlyMaleFemale);
            // And now we set this anonymous intersection class to be a superclass
            // of Person using a subclass axiom
            managerOWL.addAxiom(ontology,
                    factoryOWL.getOWLSubClassOfAxiom(person, intersection));
            // Restrictions and other anonymous classes can also be used anywhere a
            // named class can be used. Let's set the range of hasSon to be Person
            // and hasGender value male. This requires an anonymous class that is
            // the intersection of Person, and also, hasGender value male. We need
            // to create the hasGender value male restriction - this describes the
            // class of things that have a hasGender relationship to the individual
            // male.
            OWLObjectHasValue hasGenderValueMaleRestriction = factoryOWL
                    .getOWLObjectHasValue(hasGender, male);
            // Now combine this with Person in an intersection
            OWLClassExpression personAndHasGenderValueMale = factoryOWL
                    .getOWLObjectIntersectionOf(person,
                            hasGenderValueMaleRestriction);
            // Now specify this anonymous class as the range of hasSon using an
            // object property range axioms
            managerOWL.addAxiom(ontology, factoryOWL.getOWLObjectPropertyRangeAxiom(hasSon,
                    personAndHasGenderValueMale));
            // We can do a similar thing for hasDaughter, by specifying that
            // hasDaughter has a range of Person and hasGender value female. This
            // time, we will make things a little more compact by not using so many
            // variables
            OWLClassExpression rangeOfHasDaughter = factoryOWL
                    .getOWLObjectIntersectionOf(person,
                            factoryOWL.getOWLObjectHasValue(hasGender, female));
            managerOWL.addAxiom(ontology, factoryOWL.getOWLObjectPropertyRangeAxiom(
                    hasDaughter, rangeOfHasDaughter));
            // Data Ranges and Equivalent Classes axioms //In OWL 2, we can specify
            // expressive data ranges. Here, we will specify the classes Teenage,
            // Adult and Child by saying something about individuals ages. First we
            // take the class Teenager, all of whose instance have an age greater or
            // equal to 13 and less than 20. In Manchester Syntax this is written as
            // Person and hasAge some int[>=13, <20] We create a data range by
            // taking the integer datatype and applying facet restrictions to it.
            // Note that we have statically imported the data range facet vocabulary
            // OWLFacet
            OWLFacetRestriction geq13 = factoryOWL.getOWLFacetRestriction(
                    MIN_INCLUSIVE, factoryOWL.getOWLLiteral(13));
            // We don't have to explicitly create the typed constant, there are
            // convenience methods to do this
            OWLFacetRestriction lt20 = factoryOWL.getOWLFacetRestriction(
                    MAX_EXCLUSIVE, 20);
            // Restrict the base type, integer (which is just an XML Schema
            // Datatype) with the facet restrictions.
            OWLDataRange dataRng = factoryOWL.getOWLDatatypeRestriction(
                    integerDatatype, geq13, lt20);
            // Now we have the data range of greater than equal to 13 and less than
            // 20 we can use this in a restriction.
            OWLDataSomeValuesFrom teenagerAgeRestriction = factoryOWL
                    .getOWLDataSomeValuesFrom(hasAge, dataRng);
            // Now make Teenager equivalent to Person and hasAge some int[>=13, <20]
            // First create the class Person and hasAge some int[>=13, <20]
            OWLClassExpression teenagePerson = factoryOWL.getOWLObjectIntersectionOf(
                    person, teenagerAgeRestriction);
            OWLClass teenager = factoryOWL.getOWLClass(IRI.create(ontologyIRI
                    + "#Teenager"));
            OWLEquivalentClassesAxiom teenagerDefinition = factoryOWL
                    .getOWLEquivalentClassesAxiom(teenager, teenagePerson);
            managerOWL.addAxiom(ontology, teenagerDefinition);
            // Do the same for Adult that has an age greater than 21
            OWLDataRange geq21 = factoryOWL.getOWLDatatypeRestriction(integerDatatype,
                    factoryOWL.getOWLFacetRestriction(MIN_INCLUSIVE, 21));
            OWLClass adult = factoryOWL
                    .getOWLClass(IRI.create(ontologyIRI + "#Adult"));
            OWLClassExpression adultAgeRestriction = factoryOWL
                    .getOWLDataSomeValuesFrom(hasAge, geq21);
            OWLClassExpression adultPerson = factoryOWL.getOWLObjectIntersectionOf(
                    person, adultAgeRestriction);
            OWLAxiom adultDefinition = factoryOWL.getOWLEquivalentClassesAxiom(adult,
                    adultPerson);
            managerOWL.addAxiom(ontology, adultDefinition);
            // And finally Child
            OWLDataRange notGeq21 = factoryOWL.getOWLDataComplementOf(geq21);
            OWLClass child = factoryOWL
                    .getOWLClass(IRI.create(ontologyIRI + "#Child"));
            OWLClassExpression childAgeRestriction = factoryOWL
                    .getOWLDataSomeValuesFrom(hasAge, notGeq21);
            OWLClassExpression childPerson = factoryOWL.getOWLObjectIntersectionOf(
                    person, childAgeRestriction);
            OWLAxiom childDefinition = factoryOWL.getOWLEquivalentClassesAxiom(child,
                    childPerson);
            managerOWL.addAxiom(ontology, childDefinition);
            // Different individuals //In OWL, we can say that individuals are
            // different from each other. To do this we use a different individuals
            // axiom. Since John, Mary, Bill and Susan are all different
            // individuals, we can express this using a different individuals axiom.
            OWLDifferentIndividualsAxiom diffInds = factoryOWL
                    .getOWLDifferentIndividualsAxiom(john, mary, bill, susan);
            managerOWL.addAxiom(ontology, diffInds);
            // Male and Female are also different
            managerOWL.addAxiom(ontology,
                    factoryOWL.getOWLDifferentIndividualsAxiom(male, female));
            // Disjoint classes //Two say that two classes do not have any instances
            // in common we use a disjoint classes axiom:
            OWLDisjointClassesAxiom disjointClassesAxiom = factoryOWL
                    .getOWLDisjointClassesAxiom(man, woman);
            managerOWL.addAxiom(ontology, disjointClassesAxiom);
            // Ontology Management //Having added axioms to out ontology we can now
            // save it (in a variety of formats). RDF/XML is the default format
            System.out.println("RDF/XML: ");
            managerOWL.saveOntology(ontology, new StreamDocumentTarget(System.out));
            // OWL/XML
            System.out.println("OWL/XML: ");
            managerOWL.saveOntology(ontology, new OWLXMLOntologyFormat(),
                    new StreamDocumentTarget(System.out));
            // Manchester Syntax
            System.out.println("Manchester syntax: ");
            managerOWL.saveOntology(ontology, new ManchesterOWLSyntaxOntologyFormat(),
                    new StreamDocumentTarget(System.out));
            // Turtle
            System.out.println("Turtle: ");
            managerOWL.saveOntology(ontology, new TurtleOntologyFormat(),
                    new StreamDocumentTarget(System.out));

    }


    public List<Triple> getParentChildTripleAll() throws Exception{
        Session session = driverNeo4j.session();

        StatementResult result =
                session.run( "MATCH (n {name:'" + ontURI  + "'})<-[rel*1]-(child) RETURN n, rel, child");

        List<Triple> tripleList = new ArrayList<>();
        int count = 0;
        while (result.hasNext()) {
            Record record = result.next();
            //Value nv = record.get("child");
            Node childNode = record.get("child").asNode();
            Node parentNode = record.get("n").asNode();
            //System.out.println(record.get("child").get("name").asString() );

            String parentName = (String)parentNode.asMap().get("name"); //((InternalNode) parentNode
            String childName = (String)childNode.asMap().get("name");
            for (int i = 0; i < record.get("rel").size(); i++) {
                Relationship rel = record.get("rel").get(i).asRelationship();
                Triple trip = new Triple();
                trip.setNode1(parentNode);
                trip.setNode2(childNode);
                trip.setRel(rel);
                trip.setDirection(Triple.Direction.LEFT);
                tripleList.add(trip);
                System.out.println(parentName + " -- " + rel.type() + " -- " + childName);
                //System.out.println(rel.startNodeId() + " -- " + rel.type() + " -- " + rel.endNodeId());
                count += 1;
                //System.out.println(trip.toString());
            }

            getParentChildTriple(childNode, session, tripleList);
        }
        System.out.println("Total relationships found: " + tripleList.size());

        session.close();
        return tripleList;
        //driverNeo4j.close();
    }

    public void getParentChildTriple(Node parentNode, Session session, List<Triple> tripleList) throws Exception{
        //Session session = driverNeo4j.session();

        //List<Triple> tripListTemp = new ArrayList<>();
        //String parentID = (String)parentNode.asMap().get("id");
        System.out.println("parentNode id: " + parentNode.id());

        // for each relationship direction, run a separate query and add the direction info to the triple

        // RIGHT relationships
        StatementResult result =
                session.run( "MATCH (n)-[rel*1]->(child) WHERE ID(n)=" + parentNode.id() + " RETURN n, rel, child");

        int count = 0;
        while (result.hasNext()) {

            Record record = result.next();
            //Value nv = record.get("child");
            Node childNode = record.get("child").asNode();
            Node parentNode2 = record.get("n").asNode(); // redundant copy of parent node
            //System.out.println(record.get("child").get("name").asString() );

            String parentName = (String)parentNode2.asMap().get("name"); //((InternalNode) parentNode
            String childName = (String)childNode.asMap().get("name");
            for (int i = 0; i < record.get("rel").size(); i++) {
                Relationship rel = record.get("rel").get(i).asRelationship();
                Triple trip = new Triple();
                trip.setNode1(parentNode2);
                trip.setNode2(childNode);
                trip.setRel(rel);
                trip.setDirection(Triple.Direction.RIGHT);
                //tripleList.add(trip);
                System.out.println(parentName + " -- " + rel.type() + " --> " + childName);
                //System.out.println(rel.startNodeId() + " -- " + rel.type() + " -- " + rel.endNodeId());
                count += 1;
                //System.out.println(trip.toString());

                if(compareTriples(trip, tripleList)) { continue; } // if the triple is not unique, skip it

                tripleList.add(trip);
                //tripListTemp.add(trip);
                //getParentChildTriple(childNode, session, tripleList);
                getParentChildTriple(childNode, session, tripleList);
            }
        }

        // LEFT relationships
        result = session.run( "MATCH (n)<-[rel*1]-(child) WHERE ID(n)=" + parentNode.id() + " RETURN n, rel, child");
        while (result.hasNext()) {
            Record record = result.next();
            Node childNode = record.get("child").asNode();
            Node parentNode2 = record.get("n").asNode(); // redundant copy of parent node
            String parentName = (String)parentNode2.asMap().get("name"); //((InternalNode) parentNode
            String childName = (String)childNode.asMap().get("name");
            for (int i = 0; i < record.get("rel").size(); i++) {
                Relationship rel = record.get("rel").get(i).asRelationship();
                Triple trip = new Triple();
                trip.setNode1(parentNode2);
                trip.setNode2(childNode);
                trip.setRel(rel);
                trip.setDirection(Triple.Direction.LEFT);
                System.out.println(parentName + " <-- " + rel.type() + " -- " + childName);
                count += 1;
                //System.out.println(trip.toString());
                if(compareTriples(trip, tripleList)) { continue; } // if the triple is not unique, skip it
                //tripListTemp.add(trip);
                tripleList.add(trip);
                getParentChildTriple(childNode, session, tripleList);
            }
        }

//        // BOTH relationships
//        result = session.run( "MATCH (n)<-[rel*1]->(child) WHERE ID(n)=" + parentNode.id() + " RETURN n, rel, child");
//        while (result.hasNext()) {
//            Record record = result.next();
//            Node childNode = record.get("child").asNode();
//            Node parentNode2 = record.get("n").asNode(); // redundant copy of parent node
//            String parentName = (String)parentNode2.asMap().get("name"); //((InternalNode) parentNode
//            String childName = (String)childNode.asMap().get("name");
//            for (int i = 0; i < record.get("rel").size(); i++) {
//                Relationship rel = record.get("rel").get(i).asRelationship();
//                Triple trip = new Triple();
//                trip.setNode1(parentNode2);
//                trip.setNode2(childNode);
//                trip.setRel(rel);
//                trip.setDirection(Triple.Direction.BOTH);
//                System.out.println(parentName + " <-- " + rel.type() + " --> " + childName);
//                count += 1;
//                System.out.println(trip.toString());
//                if(compareTriples(trip, tripleList)) { continue; } // if the triple is not unique, skip it
//                tripleList.add(trip);
//                //tripListTemp.add(trip);
//                getParentChildTriple(childNode, session, tripleList);
//            }
//        }

        System.out.println("Total relationships found: " + count);

//        // iteration temp triple list and add to main list
//        for (Triple trip : tripListTemp){
//
//        }

        //session.close();
        //return tripleList;
        //driverNeo4j.close();
    }

    public void getParentChildTriple_old(Node parentNode, Session session, List<Triple> tripleList) throws Exception{
        int x = 11;
        //Session session = driverNeo4j.session();

        String parentID = (String)parentNode.asMap().get("id");
        System.out.println("parentNode id: " + parentNode.id());
        // TODO: for each relationship direction, run a separate query and add the direction info to the triple
        StatementResult result =
                session.run( "MATCH (n)-[rel*1]-(child) WHERE ID(n)=" + parentNode.id() + " RETURN n, rel, child");

        //List<Triple> tripleList = new ArrayList<>();
        int count = 0;
        while (result.hasNext()) {
            Record record = result.next();
            //Value nv = record.get("child");
            Node childNode = record.get("child").asNode();
            Node parentNode2 = record.get("n").asNode(); // redundant copy of parent node
            //System.out.println(record.get("child").get("name").asString() );

            String parentName = (String)parentNode2.asMap().get("name"); //((InternalNode) parentNode
            String childName = (String)childNode.asMap().get("name");
            for (int i = 0; i < record.get("rel").size(); i++) {
                Relationship rel = record.get("rel").get(i).asRelationship();
                Triple trip = new Triple();
                trip.setNode1(parentNode2);
                trip.setNode2(childNode);
                trip.setRel(rel);
                //tripleList.add(trip);
                System.out.println(parentName + " -- " + rel.type() + " -- " + childName);
                //System.out.println(rel.startNodeId() + " -- " + rel.type() + " -- " + rel.endNodeId());
                count += 1;
                System.out.println(trip.toString());

                if(compareTriples(trip, tripleList)) { continue; } // if the triple is not unique, skip it

                tripleList.add(trip);
                getParentChildTriple_old(childNode, session, tripleList);
            }
        }
        System.out.println("Total relationships found: " + count);

        //session.close();
        //return tripleList;
        //driverNeo4j.close();
    }

    private boolean compareTriples(Triple trip1, List<Triple> tripList){
        boolean match = false;
        for (Triple trip2 : tripList) {
            if (trip1.toString().equals(trip2.toString()) | trip1.toString().equals(trip2.toStringReverse())) {
                match = true;
                break;
            }
        }
        return match;
    }

    public void getAllNodes() throws Exception{
        Session session = driverNeo4j.session();

        StatementResult result =
                session.run( "MATCH (n {name:'" + ontURI  + "'})-[rel*3]-(child) RETURN n, rel, child");


        List<Relationship> relList = new ArrayList<>();
        int count = 0;
        while (result.hasNext()) {
            int xx =1;
            Record record = result.next();
            //Value nv = record.get("child");
            Node childNode = record.get("child").asNode();
            Node parentNode = record.get("n").asNode();
            //System.out.println(record.get("child").get("name").asString() );

            String parentName = (String)parentNode.asMap().get("name"); //((InternalNode) parentNode
            String childName = (String)childNode.asMap().get("name");
            for (int i = 0; i < record.get("rel").size(); i++) {
                Relationship rel = record.get("rel").get(i).asRelationship();
                relList.add(rel);
                System.out.println(parentName + " -- " + rel.type() + " -- " + childName);
                System.out.println(rel.startNodeId() + " -- " + rel.type() + " -- " + rel.endNodeId());
                count += 1;
            }
        }
        System.out.println("Total relationships found: " + count);

        session.close();
        //driverNeo4j.close();
    }

    public void getRelationshipIDs() throws Exception{
        Session session = driverNeo4j.session();

        String queryStr  = "MATCH (n {name:'http://blulab.chpc.utah.edu/ontologies/examples/smoking.owl'})-[rel*1..8]-(child) " +
                "WITH n, rel, child " +
                "MATCH (child)-[rel2:hasSection]-(child2) RETURN child, rel2, child2";
        System.out.println(queryStr);
        StatementResult result =
                ////session.run( "MATCH (n {name:'" + ontURI  + "'})<-[rel:IS_A*]-(child) RETURN n, rel, child");
                //session.run( "MATCH (n {name:'" + ontURI  + "'})-[rel*1..2]-(child) RETURN n, rel, child");
//                session.run( "MATCH (n {name:'http://blulab.chpc.utah.edu/ontologies/examples/smoking.owl'})-[rel*1..8]-(child) " +
//                        "WITH n, rel, child " +
//                        "MATCH (child)-[rel2:hasSection]-(child2) RETURN child, rel2, child2");
                //"MATCH (n {name:'http://blulab.chpc.utah.edu/ontologies/examples/smoking.owl'})-[rel*1..3]-(child) WITH n, rel, child WHERE (child)-[:hasSection]-() RETURN child"
                session.run( queryStr);

        List<Relationship> relList = new ArrayList<>();
        Set<String> relSet = new HashSet<>();
        int count = 0;
        while (result.hasNext()) {
            Record record = result.next();
            //Value nv = record.get("child");
            Node childNode = record.get("child").asNode();
            Node parentNode = record.get("child2").asNode();
            //Node parentNode = record.get("n").asNode();
            //System.out.println(record.get("child").get("name").asString() );

            String parentName = (String)parentNode.asMap().get("name"); //((InternalNode) parentNode
            String childName = (String)childNode.asMap().get("name");
            for (int i = 0; i < record.get("rel2").size(); i++) {
                //Relationship rel = record.get("rel").get(i).asRelationship();
                Relationship rel = record.get("rel2").asRelationship();
                relSet.add(rel.type());
                relList.add(rel);
                System.out.println(parentName + " -- " + rel.type() + " -- " + childName);
                System.out.println(rel.startNodeId() + " -- " + rel.type() + " -- " + rel.endNodeId());
                count += 1;
            }
        }
        System.out.println("Total relationships found: " + count);

        session.close();

        // interate relationships to get
        //driverNeo4j.close();
    }

    public void getNodes() throws Exception
    {
        Session session = driverNeo4j.session();

        StatementResult result =
                session.run( "MATCH (n {name:'" + ontURI  + "'})<-[rel:IS_A*..2]-(child) RETURN n, rel, child");

        while (result.hasNext()) {
            Record record = result.next();
            //Value nv = record.get("child");
            System.out.println(record.get("child").get("name").asString() );
            System.out.println(record.get("rel").get("name").asString() );
        }

        session.close();
        //driverNeo4j.close();
    }

//    Session session=context.connection.session();
//    StatementResult rs=session.run("match (a)-[r]-(b) where id(a)="+node+" and id(b)="+luceneSearchResults.get(i).id+" return id(r)");
//            while (rs.hasNext()){
//    Record item=rs.next();
//    r.getEdges().add(item.get("id(r)").asLong());
//}
//            session.close();
//}
//    }

    public int getThingNode() throws Exception
    {
        Session session = driverNeo4j.session();

        StatementResult result =
                session.run( "MATCH (n:ONTOLOGY) WHERE n.name=\"" + ontURI + "\" RETURN n");

        if (!result.hasNext()){
            System.out.println("Domain Ontology Not Found (" + ontURI + ")");
        }

        while (result.hasNext()) {
            Record record = result.next();
            Value node = record.get(0);

            System.out.println(record.get(0).get("name").asString() );
        }

        session.close();
        //driverNeo4j.close();

        return 1;
    }

    public void getRootNode_old()
    {
        try ( Session session = driverNeo4j.session() )
        {
            String out = session.writeTransaction( new TransactionWork<String>()
            {
                @Override
                public String execute( Transaction tx )
                {
                    StatementResult result = tx.run( "MATCH (n:ONTOLOGY) WHERE n.name=\"http://blulab.chpc.utah.edu/ontologies/examples/heartDiseaseInDiabetics.owl\" RETURN n");
                    //System.out.println("Nodes Found: " + result.list().size());
                    //Value val = result.single().get(0);
                    return result.single().get( 0 ).asString();
                }
            } );
            System.out.println( out );
        }
    }

    public void readVariables( final String message )
    {
        try ( Session session = driverNeo4j.session() )
        {
            String out = session.writeTransaction( new TransactionWork<String>()
            {
                @Override
                public String execute( Transaction tx )
                {
                    StatementResult result = tx.run( "CREATE (a:Greeting) " +
                                    "SET a.message = $message " +
                                    "RETURN a.message + ', from node ' + id(a)",
                            parameters( "message", message ) );
                    return result.single().get( 0 ).asString();
                }
            } );
            System.out.println( out );
        }
    }

    public void printGreeting( final String message )
    {
        try ( Session session = driverNeo4j.session() )
        {
            String greeting = session.writeTransaction( new TransactionWork<String>()
            {
                @Override
                public String execute( Transaction tx )
                {
                    StatementResult result = tx.run( "CREATE (a:Greeting) " +
                                    "SET a.message = $message " +
                                    "RETURN a.message + ', from node ' + id(a)",
                            parameters( "message", message ) );
                    return result.single().get( 0 ).asString();
                }
            } );
            System.out.println( greeting );
        }
    }


}

