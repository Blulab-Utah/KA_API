package edu.utah.blulab.marshallers.old;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.io.fs.FileUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
//import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class OwlToGrpah_newApproach {

    private GraphDatabaseService db = null;
    private String ontologyIRI;

    public static void main(String[] args) throws Exception {
        File ontFile = new File("C:\\Users\\Bill\\Documents\\2018\\Chapman Lab\\Data\\ontology\\smoking.owl");

        String name = ontFile.getName();
        name = name.substring(0, name.lastIndexOf("."));

        File graphFile = new File(ontFile.getParent() + "/db/" + name);

        OwlToGrpah_newApproach main = new OwlToGrpah_newApproach();
        main.createNewDB(graphFile);
        main.importOntology(ontFile);
    }

    public void createNewDB(File graphFile) {
        if (graphFile.exists()) {
            try {
                FileUtils.deleteRecursively(graphFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
        db = dbFactory.newEmbeddedDatabase(graphFile);
    }

    private void importOntology(File ontFile) throws Exception {
        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        final OWLDataFactory factory = manager.getOWLDataFactory();
        final OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontFile);

        ontologyIRI = ontology.getOntologyID().getOntologyIRI().toString();
        System.out.println("OntologyIRI set to " + ontologyIRI);


        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
        OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, config);

        //OWLReasoner reasoner = new OWLReasonerBase(ontology);
        if (!reasoner.isConsistent()) {
            //logger.error("Ontology is inconsistent");
            //throw your exception of choice here
            throw new Exception("Ontology is inconsistent");
        }
        Transaction tx = db.beginTx();
        try {
            Node thingNode = getOrCreateNodeWithUniqueFactory("owl:Thing", db);
            for (OWLClass c : ontology.getClassesInSignature(true)) {
                String classString = c.toString();
                if (classString.contains("#")) {
                    classString = classString.substring(
                            classString.indexOf("#") + 1, classString.lastIndexOf(">"));
                }
                Node classNode = getOrCreateNodeWithUniqueFactory(classString, db);
                NodeSet<OWLClass> superclasses = reasoner.getSuperClasses(c, true);
                if (superclasses.isEmpty()) {
                    classNode.createRelationshipTo(thingNode,
                            DynamicRelationshipType.withName("isA"));
                } else {
                    for (org.semanticweb.owlapi.reasoner.Node<OWLClass>
                            parentOWLNode : superclasses) {
                        OWLClassExpression parent =
                                parentOWLNode.getRepresentativeElement();
                        String parentString = parent.toString();
                        if (parentString.contains("#")) {
                            parentString = parentString.substring(
                                    parentString.indexOf("#") + 1,
                                    parentString.lastIndexOf(">"));
                        }
                        Node parentNode =
                                getOrCreateNodeWithUniqueFactory(parentString, db);
                        classNode.createRelationshipTo(parentNode,
                                DynamicRelationshipType.withName("isA"));
                    }
                }
                for (org.semanticweb.owlapi.reasoner.Node<OWLNamedIndividual> in
                        : reasoner.getInstances(c, true)) {
                    OWLNamedIndividual i = in.getRepresentativeElement();
                    String indString = i.toString();
                    if (indString.contains("#")) {
                        indString = indString.substring(
                                indString.indexOf("#") + 1, indString.lastIndexOf(">"));
                    }
                    Node individualNode =
                            getOrCreateNodeWithUniqueFactory(indString, db);
                    individualNode.createRelationshipTo(classNode,
                            DynamicRelationshipType.withName("isA"));

                    for (OWLObjectPropertyExpression objectProperty :
                            ontology.getObjectPropertiesInSignature()) {
                        for
                        (org.semanticweb.owlapi.reasoner.Node<OWLNamedIndividual>
                                object : reasoner.getObjectPropertyValues(i,
                                objectProperty)) {
                            String reltype = objectProperty.toString();
                            reltype = reltype.substring(reltype.indexOf("#") + 1,
                                    reltype.lastIndexOf(">"));
                            String s =
                                    object.getRepresentativeElement().toString();
                            s = s.substring(s.indexOf("#") + 1,
                                    s.lastIndexOf(">"));
                            Node objectNode =
                                    getOrCreateNodeWithUniqueFactory(s, db);
                            individualNode.createRelationshipTo(objectNode,
                                    DynamicRelationshipType.withName(reltype));
                        }
                    }
                    for (OWLDataPropertyExpression dataProperty :
                            ontology.getDataPropertiesInSignature()) {
                        for (OWLLiteral object : reasoner.getDataPropertyValues(
                                i, dataProperty.asOWLDataProperty())) {
                            String reltype =
                                    dataProperty.asOWLDataProperty().toString();
                            reltype = reltype.substring(reltype.indexOf("#") + 1,
                                    reltype.lastIndexOf(">"));
                            String s = object.toString();
                            individualNode.setProperty(reltype, s);
                        }
                    }
                }
            }
        tx.success();
    } finally

    {
        tx.close();
    }

}

    private Node getOrCreateNodeWithUniqueFactory(String nodeName, final Label label, GraphDatabaseService graphDb) {
        UniqueFactory<Node> factory = new UniqueFactory.UniqueNodeFactory(graphDb, "index") {
            @Override
            protected void initialize(Node created, Map<String, Object> properties) {
                created.addLabel(label);
                created.setProperty("name", properties.get("name"));
            }
        };

        return factory.getOrCreate("name", nodeName);
    }

    private static Node getOrCreateNodeWithUniqueFactory(String nodeName, GraphDatabaseService db) { // without adding a label
        UniqueFactory<Node> factory = new UniqueFactory.UniqueNodeFactory(db, "index") {
            @Override
            protected void initialize(Node created, Map<String, Object> properties) {
                created.setProperty("name", properties.get("name"));
            }
        };

        return factory.getOrCreate("name", nodeName);
    }
}
