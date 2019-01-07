package edu.utah.blulab.marshallers.graph;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Relationship;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentTarget;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.util.*;
import java.io.*;

public class KAtoOntology implements AutoCloseable
{
    private final Driver driverNeo4j;
    private final OWLOntologyManager managerOWL;
    //private final OWLDataFactory factoryOWL;
    OWLOntology ontology;
    String ontURI;
    IRI ontologyIRI;
    OWLDataFactory factoryOWL;

    List<Triple> tripleList;

    long startTime;
    long duration = 0;

    public KAtoOntology( String neo4jURL, String user, String password, String ontURI )
    {
        driverNeo4j = GraphDatabase.driver( neo4jURL, AuthTokens.basic( user, password ) );
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
        final long startTime2 = System.currentTimeMillis();
        String neo4jURL = "bolt://localhost:7687";
        String username =  "neo4j";
        String password = "chuck6";
        String ontURI = "http://blulab.chpc.utah.edu/ontologies/examples/smoking.owl";
        //String ontURI = OntologyConstants.SCHEMA_BASE_URI;
        //String ontURI = OntologyConstants.MODIFIER_BASE_URI;
        //String ontURI = "http://blulab.chpc.utah.edu/ontologies/examples/heartDiseaseInDiabetics.owl";


        String outputFileName = "C:\\Users\\Bill\\Desktop\\test.owl";
        try ( KAtoOntology kaDB = new KAtoOntology( neo4jURL, username, password, ontURI ) )
        {
//            List<Triple> tripleList = kaDB.getParentChildTripleAll();
//            String ontStr = kaDB.createOWLfromTriples(tripleList);
            String ontStr = kaDB.getOWLfromDB();

            // write ontology to file
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
            writer.write(ontStr);
            writer.close();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Execution time: " + (endTime - startTime2) / 1000 + " sec");
    }

    @Override
    public void close() throws Exception
    {
        driverNeo4j.close();
    }

    public String getOWLfromDB() throws Exception{
        List<Triple> tripleList = getParentChildTripleAll();
        String ontStr = createOWLfromTriples(tripleList);
        return ontStr;
    }

    @SuppressWarnings("Duplicates")
    public String createOWLfromTriples (List<Triple> tripleList) throws OWLOntologyStorageException {

        // add imports
        OWLImportsDeclaration importDeclaration = factoryOWL.getOWLImportsDeclaration(IRI.create(OntologyConstants.TERM_MAPPING_BASE_URI));
        OWLImportsDeclaration importDeclaration2 = factoryOWL.getOWLImportsDeclaration(IRI.create(OntologyConstants.SCHEMA_BASE_URI));
        OWLImportsDeclaration importDeclaration3 = factoryOWL.getOWLImportsDeclaration(IRI.create(OntologyConstants.SWIRL_BASE_URI));
        managerOWL.applyChange(new AddImport(ontology, importDeclaration));
        managerOWL.applyChange(new AddImport(ontology, importDeclaration2));
        managerOWL.applyChange(new AddImport(ontology, importDeclaration3));

        // create a list of all classes and create an owl node for each
        int count = 0;
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

            // skip nodes from one of the imported ontologies
            // todo: figure out how to write only the classes that are not included in the import declarations
//            if (node1.get("uri").asString().contains(OntologyConstants.SCHEMA_BASE_URI) & node2.get("uri").asString().contains(OntologyConstants.SCHEMA_BASE_URI)){ // if both nodes are from imported ontologies
//                continue;
//            } else if (node1.get("uri").asString().contains(OntologyConstants.SCHEMA_BASE_URI) | node2.get("uri").asString().contains(OntologyConstants.SCHEMA_BASE_URI)){ // if only one of the nodes is from imported ontologies
//                int xxx = 1;
//            }
//            if (isImportURI(node1.get("uri").asString()) & isImportURI(node2.get("uri").asString())){
//                continue;
//            }
            if (!node1.get("uri").asString().contains(ontURI) & !node2.get("uri").asString().contains(ontURI)){ // if both nodes are from imported ontologies
                continue;
            }

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
            for (String property : node2.keys()){
                if (property.equals("name") | property.equals("uri")){
                    continue;
                }
                String propertyValue = node2.get(property).asString();
                // parse multiple values from string ('|' delimited)
                for (String val : propertyValue.split("\\|")) {
                    addAnnotationToClass(property, val, nodeClass2);
                }
            }

            if (node2.get("uri").asString().contains("Kretek")){
                String xxx = "HERE";
            }

            // handle synonyms, misspellings, regex and abbreviations if they are separate nodes
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

            // add relationships between nodes
            if (rel.type().equals("IS_A")) {
                OWLAxiom axiom = factoryOWL.getOWLSubClassOfAxiom(nodeClass2, nodeClass1);
                managerOWL.applyChange(new AddAxiom(ontology, axiom));
            } else if (rel.type().equals("hasIndividual")){
                // todo: process object properties of individuals
                OWLIndividual indiv = factoryOWL.getOWLNamedIndividual(IRI.create(node2.get("uri").asString()));
                OWLClassAssertionAxiom classAssertion = factoryOWL.getOWLClassAssertionAxiom(nodeClass1, indiv);
                for (String property : node2.keys()){
                    if (property.equals("name") | property.equals("uri")){
                        continue;
                    }
                    String propertyValue = node2.get(property).asString();
                    // parse multiple values from string ('|' delimited)
                    for (String val : propertyValue.split("\\|")) {
                        if (property.equals("objectProperties")){ // if objectProperties, split key and value, then add to class
                            String[] keyVal = val.split("::");
                            String relIRI = keyVal[0];
                            String indiv2IRI = keyVal[1];
                            if (count >= 160) {
                                int ii = 1;
                            }
                                addObjectPropertyToIndividual(indiv2IRI, relIRI, indiv);
                                count++;
                        } else { // for all other properties, add to the original individual
                            addAnnotationToIndividual(property, val, indiv);
                        }
                    }
                }
                managerOWL.addAxiom(ontology, classAssertion);
            } else {
                if (node1.hasLabel("DATATYPE")){
                    OWLDatatype nodeData1 = factoryOWL.getOWLDatatype(IRI.create(node1.get("uri").asString()));
                    //OWLDataProperty dataProp = factoryOWL.getOWLDataProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
                    OWLDataProperty dataProp = factoryOWL.getOWLDataProperty(IRI.create(rel.get("uri").asString()));
                    OWLDataSomeValuesFrom hasSome = factoryOWL.getOWLDataSomeValuesFrom(dataProp, nodeData1);
                    OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass2, hasSome);
                    managerOWL.applyChange(new AddAxiom(ontology, subAx));
                } else if (node1.hasLabel("DATA_ONE_OF")){
                    OWLDatatype nodeData1 = factoryOWL.getOWLDatatype(IRI.create(node1.get("uri").asString()));
                    String[] dataOneOfStrArr = node1.get("name").asString().split("\\|");
                    Set<OWLLiteral> literalSet = new HashSet<>();
                    for (String litStr : dataOneOfStrArr){
                        OWLLiteral lit = new OWLLiteralImpl(litStr, "", nodeData1);
                        literalSet.add(lit);
                    }
                    OWLDataOneOf dataOneOf = factoryOWL.getOWLDataOneOf(literalSet);
                    //OWLDatatype nodeData1 = factoryOWL.getOWLDatatype(IRI.create(node1.get("uri").asString()));
                    //OWLDataProperty dataProp = factoryOWL.getOWLDataProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
                    OWLDataProperty dataProp = factoryOWL.getOWLDataProperty(IRI.create(rel.get("uri").asString()));
                    OWLDataSomeValuesFrom hasSome = factoryOWL.getOWLDataSomeValuesFrom(dataProp, dataOneOf);
                    OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass2, hasSome);
                    managerOWL.applyChange(new AddAxiom(ontology, subAx));
                } else if (node2.hasLabel("VARIABLE") & node2.get("uri").asString().contains(ontURI)) { // for the variable nodes the hasSome properties as Equivalent, not SubClass of
                    //OWLObjectProperty objProp = factoryOWL.getOWLObjectProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
                    OWLObjectProperty objProp = factoryOWL.getOWLObjectProperty(IRI.create(rel.get("uri").asString()));
                    OWLClassExpression hasSome = factoryOWL.getOWLObjectSomeValuesFrom(objProp, nodeClass1);
                    //OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass2, hasSome);
                    OWLEquivalentClassesAxiom eqAx = factoryOWL.getOWLEquivalentClassesAxiom(nodeClass2, hasSome);
                    managerOWL.applyChange(new AddAxiom(ontology, eqAx));
                } else {
                    //OWLObjectProperty objProp = factoryOWL.getOWLObjectProperty(IRI.create(ontologyIRI.toString() + "#" + rel.type()));
                    OWLObjectProperty objProp = factoryOWL.getOWLObjectProperty(IRI.create(rel.get("uri").asString()));
                    OWLClassExpression hasSome = factoryOWL.getOWLObjectSomeValuesFrom(objProp, nodeClass1);
                    OWLSubClassOfAxiom subAx = factoryOWL.getOWLSubClassOfAxiom(nodeClass2, hasSome);
                    managerOWL.applyChange(new AddAxiom(ontology, subAx));
                }
            }
        }

//        String outputFileName = "C:\\Users\\Bill\\Desktop\\test.owl";
//        FileDocumentTarget fileOut = new FileDocumentTarget(new File(outputFileName));
//        managerOWL.saveOntology(ontology, new OWLXMLOntologyFormat(), fileOut);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        managerOWL.saveOntology(ontology, new OWLXMLOntologyFormat(), stream);
        //System.out.println(stream.toString());
        return stream.toString();
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
        //System.out.println("Total relationships found: " + tripleList.size());

        session.close();
        return tripleList;
        //driverNeo4j.close();
    }

    @SuppressWarnings("Duplicates")
    public void getParentChildTriple(Node parentNode, Session session, List<Triple> tripleList) {

        //System.out.println("parentNode id: " + parentNode.id());

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
                //EndTimeMS();
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

        //System.out.println("Total relationships found: " + count);

        //session.close();
        //return tripleList;
        //driverNeo4j.close();
    }

    private boolean isImportURI(String uri){
        if ( uri.contains(OntologyConstants.SCHEMA_BASE_URI) |
                uri.contains(OntologyConstants.CONTEXT_BASE_URI) |
                uri.contains(OntologyConstants.SEMANTIC_TYPE_BASE_URI) |
                uri.contains(OntologyConstants.THING_URI)) {
            return true;
        } else {
            return false;
        }
    }

    private void addAnnotationToClass(String property, String propertyValue, OWLClass nodeClass){
        OWLAnnotationProperty annoProp = getAnnotationProp(property);
        OWLAnnotation commentAnno = factoryOWL.getOWLAnnotation(annoProp, factoryOWL.getOWLLiteral(propertyValue));
        OWLAxiom ax = factoryOWL.getOWLAnnotationAssertionAxiom(nodeClass.getIRI(), commentAnno);
        managerOWL.applyChange(new AddAxiom(ontology, ax));
    }

    private void addAnnotationToIndividual(String property, String propertyValue, OWLIndividual individual){
        OWLNamedIndividualImpl indiv = (OWLNamedIndividualImpl) individual;
        OWLAnnotationProperty annoProp = getAnnotationProp(property);
        OWLAnnotation commentAnno = factoryOWL.getOWLAnnotation(annoProp, factoryOWL.getOWLLiteral(propertyValue));
        OWLAxiom ax = factoryOWL.getOWLAnnotationAssertionAxiom(indiv.getIRI(), commentAnno);
        managerOWL.applyChange(new AddAxiom(ontology, ax));
    }

    private void addObjectPropertyToIndividual(String indiv2IRI, String relationshipIRI, OWLIndividual individual){
        OWLNamedIndividualImpl indiv = (OWLNamedIndividualImpl) individual;
//        OWLNamedIndividual indiv2 = factoryOWL.getOWLNamedIndividual(IRI.create(indiv2IRI));
        OWLIndividual indiv2 = factoryOWL.getOWLNamedIndividual(IRI.create(indiv2IRI));
        OWLObjectProperty relationship = factoryOWL.getOWLObjectProperty(IRI.create(relationshipIRI));
//        OWLObjectPropertyAssertionAxiom assertion = factoryOWL.getOWLObjectPropertyAssertionAxiom(relationship, indiv, indiv2);
//        managerOWL.applyChange(new AddAxiom(ontology, assertion));


        String base = "http://www.semanticweb.org/ontologies/individualsexample";
        OWLIndividual matthew = factoryOWL.getOWLNamedIndividual(IRI.create(base + "#matthew"));
        OWLIndividual peter = factoryOWL.getOWLNamedIndividual(IRI.create(base
                + "#peter"));
        // We want to link the subject and object with the hasFather property,
        // so use the data factory to obtain a reference to this object
        // property.
        OWLObjectProperty hasFather = factoryOWL.getOWLObjectProperty(IRI
                .create(base + "#hasFather"));
        // Now create the actual assertion (triple), as an object property
        // assertion axiom matthew --> hasFather --> peter
        OWLObjectPropertyAssertionAxiom assertion = factoryOWL
                .getOWLObjectPropertyAssertionAxiom(relationship, indiv, indiv2);
        // Finally, add the axiom to our ontology and save
        AddAxiom addAxiomChange = new AddAxiom(ontology, assertion);
        managerOWL.applyChange(addAxiomChange);

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

        boolean matchTemp = false;
        for (Triple trip2 : tripList) {
//            if (trip1.toString().equals(trip2.toString()) | trip1.toString().equals(trip2.toStringReverse())) {
            if (trip1.isEqual(trip2) | trip1.isEqualReverse(trip2)) {
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

    private void StartTime(){
        startTime = System.nanoTime();
    }

    private long EndTimeMS(){
        long endTime = System.nanoTime();
        duration = (endTime - startTime) + duration;
        return duration / 1000000; //to get milliseconds;
    }

    public String getOntURI() {
        return ontURI;
    }
}

