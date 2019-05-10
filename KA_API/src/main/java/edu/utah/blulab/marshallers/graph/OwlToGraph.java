package edu.utah.blulab.marshallers.graph;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.io.fs.FileUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Bill on 6/28/2017.
 */
public class OwlToGraph {

    private GraphDatabaseService graphDB = null;
    private String ontologyIRI;
    //private final String schemaURI = "http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl";
    Map<NodeTypes, Integer> labelCounter = new LinkedHashMap<>();

    public static void main(String[] args) throws Exception {


        // todo:comment this back out
        //File ontFile = new File(args[0]);
        File ontFile = new File("C:\\Users\\Bill\\Documents\\2018\\Chapman Lab\\Data\\ontology\\smoking.owl");
        //File ontFile = new File("C:\\Users\\Bill\\Desktop\\smoking_sub.owl");

        String name = ontFile.getName();
        name = name.substring(0, name.lastIndexOf("."));

        File graphFile = new File(ontFile.getParent() + "/db/" + name);

        OwlToGraph main = new OwlToGraph();
        main.createNewDB(graphFile);
        main.createDBfromOnt(ontFile, NodeTypes.ONTOLOGY, true);
        main.labelDomainOntology();
        //main.makeCopy(main.getGraphDB());
    }

    public void createNewDB(File graphFile) {
        if (graphFile.exists()) {
            try {
                FileUtils.deleteRecursively(graphFile);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
        graphDB = dbFactory.newEmbeddedDatabase(graphFile);
    }

    public void createDBfromOnt(File ontFile, NodeTypes ontType, boolean includeInstances) throws Exception {

//        File ontFile = new File(args[0]);
//        //File f = new File("/Users/melissa/db/domain");
//        String name = ontFile.getName();
//        name = name.substring(0, name.lastIndexOf("."));
//
//        File graphFile = new File(ontFile.getParent() + "/db/" + name);

//        if(graphFile.exists()){
//            try {
//                FileUtils.deleteRecursively(graphFile);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
//        graphDB = dbFactory.newEmbeddedDatabase(graphFile);

        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        final OWLDataFactory factory = manager.getOWLDataFactory();
        final OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontFile);

        ontologyIRI = ontology.getOntologyID().getOntologyIRI().toString();
        System.out.println("OntologyIRI set to " + ontologyIRI);

        Transaction tx = null;
        try {
            tx = graphDB.beginTx();
        } catch (NullPointerException e) {
            System.out.println("**** You must create the database with createNewDB first! ****");
            e.printStackTrace();
        }
        try{
            //create Thing node in db
            final Node thingNode = getOrCreateNodeWithUniqueFactory("Thing", NodeTypes.THING, graphDB);
            thingNode.setProperty("uri", OntologyConstants.THING_URI);

            // ontology node in db and connect to thing node
            //final Node ontologyNode = getOrCreateNodeWithUniqueFactory(ontologyIRI, NodeTypes.ONTOLOGY, graphDB);
            final Node ontologyNode = getOrCreateNodeWithUniqueFactory(ontologyIRI, ontType == null ? NodeTypes.ONTOLOGY : ontType, graphDB);
            ontologyNode.setProperty("uri", ontologyIRI);
            thingNode.createRelationshipTo(ontologyNode, RelationshipType.withName("IS_A"));

            //get all classes in domain and imported ontologies
            Set<OWLClass> classes = ontology.getClassesInSignature(true);

            //Set<OWLDatatype> datatypes = ontology.getDatatypesInSignature(true);

            System.out.println(ontologyIRI);
            //create node and attributes of each node
            for(OWLClass cls : classes) {
                System.out.println(cls.getIRI());
                Node newNode = getOrCreateNodeWithUniqueFactory(cls.getIRI().getShortForm(), graphDB);
                newNode.setProperty("uri", cls.getIRI().toString());
                //newNode.addLabel(OwlToGraph_old.NodeTypes.ANCHOR);


                if (cls.getIRI().toString().equals("http://blulab.chpc.utah.edu/ontologies/examples/smoking.owl#Duration_gte_12_months")){
                    int xxx = 1;
                }

                //get superclasses to link to or else link to owl:Thing
                Set<OWLClassExpression> superClasses = cls.getSuperClasses(manager.getOntologies());

                int classCount = 0;
                if (superClasses.isEmpty()) {
                    newNode.createRelationshipTo(thingNode, RelationshipType.withName("IS_A"));
                } else {
                    for (OWLClassExpression exp : superClasses) {
                        if (exp.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {
                            //System.out.println(exp.getClassExpressionType());
                            Node parentNode = getOrCreateNodeWithUniqueFactory(exp.asOWLClass().getIRI().getShortForm(),
                                    graphDB);
                            newNode.createRelationshipTo(parentNode, RelationshipType.withName("IS_A"));
                            //newNode.addLabel(NodeTypes.CLASS);
                            classCount++;
                        } else {
                            //System.out.println(exp.getClassExpressionType());
                        }
                    }
                }
                if (!superClasses.isEmpty() & classCount == 0) { // bug fix to fix case where there are superclasses but they are not of type Class
                    newNode.createRelationshipTo(thingNode, RelationshipType.withName("IS_A"));
                    //System.out.println("HERE");
                }

                //OWLAnnotationProperty label = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.get);
                OWLAnnotationProperty label = factory.getRDFSLabel();
                Set<OWLAnnotation> annotation = cls.getAnnotations(ontology, label);
                //get all annotation properties and create properties for each
                Set<OWLAnnotationAssertionAxiom> annProps = cls.getAnnotationAssertionAxioms(ontology);
                for (OWLAnnotationAssertionAxiom axiom : annProps) {
                    //System.out.println(axiom.toString());
                    String relType = axiom.getProperty().getIRI().getShortForm();
                    OWLLiteral value = (OWLLiteral) axiom.getValue();
                    String valueStr = value.getLiteral();

//                    if (relType.equals("synonym") | relType.equals("misspelling") | relType.equals("regex")
//                            | relType.equals("abbreviation") | relType.equals("subjectiveExpression")) { // for a synonym, create a new node with the hasSynonym relationship
//                        Node addNode = createNode(valueStr, graphDB);
//                        String relationshipName = "has" + relType.substring(0, 1).toUpperCase() + relType.substring(1);
//                        addNode.createRelationshipTo(newNode, RelationshipType.withName(relationshipName));
//                    } else {
                    if (newNode.hasProperty(relType)) { // if the property already exists, append data
                        String newValue = newNode.getProperty(relType).toString() + "|" + valueStr;
                        newNode.setProperty(relType, newValue);
                    } else { // if the property does not exist, add it
                        newNode.setProperty(relType, valueStr);
                    }
//                    }
                }

//                //get all datatype expressions
//                Set<OWLDatatype> datatypes = cls.getDatatypesInSignature();

                //get all axioms on each class and create relationships for each

                Set<OWLClassExpression> classExpressionsSubCls = cls.getSubClasses(manager.getOntologies());

                Set<OWLClassExpression> classExpressions = cls.getEquivalentClasses(manager.getOntologies());

                if (classExpressions.size() > 0){
                    int xxxx = 1;
                }

                classExpressions.addAll(cls.getSuperClasses(manager.getOntologies()));
                for (OWLClassExpression exp : classExpressions) {
                    if (exp.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
                        OWLObjectSomeValuesFrom axiom = (OWLObjectSomeValuesFrom) exp;
                        OWLObjectPropertyExpression objPropertyExpression = axiom.getProperty();
                        String relation = objPropertyExpression.asOWLObjectProperty().getIRI().getShortForm();
                        OWLClassExpression fillerClass = axiom.getFiller();

                        if (fillerClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {
                            String fillerName = fillerClass.asOWLClass().getIRI().getShortForm();
                            if (fillerName.equals("Annotation")) {
                                int ss = 1;
                            }
                            Node fillerNode = getOrCreateNodeWithUniqueFactory(fillerName, graphDB);
                            // newNode.createRelationshipTo(fillerNode, RelationshipType.withName(relation));
                            Relationship propRelation = newNode.createRelationshipTo(fillerNode,
                                    RelationshipType.withName(relation));
                            propRelation.setProperty("uri",
                                    objPropertyExpression.asOWLObjectProperty().getIRI().toString());
                            //propRelation.setProperty("owlRelationshipType", "equivalent");
                        } else if (fillerClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_UNION_OF)){ // if there are a list of classes for a hasSome expression
                            for (OWLClass clsFromList : fillerClass.getClassesInSignature()){
                                String fillerName = clsFromList.asOWLClass().getIRI().getShortForm();
                                Node fillerNode = getOrCreateNodeWithUniqueFactory(fillerName, graphDB);
                                Relationship propRelation = newNode.createRelationshipTo(fillerNode,
                                        RelationshipType.withName(relation));
                                propRelation.setProperty("uri",
                                        objPropertyExpression.asOWLObjectProperty().getIRI().toString());
                                //propRelation.setProperty("owlRelationshipType", "equivalent");
                            }
                        }
                    } else if (exp.getClassExpressionType().equals(ClassExpressionType.DATA_SOME_VALUES_FROM)) {
                        OWLDataSomeValuesFrom axiom = (OWLDataSomeValuesFrom) exp;
                        OWLDataPropertyExpression dataPropertyExpression = axiom.getProperty();
                        String relation = dataPropertyExpression.asOWLDataProperty().getIRI().getShortForm();
                        OWLDataRange fillerClass = axiom.getFiller();

                        if (fillerClass.getDataRangeType().equals(DataRangeType.DATATYPE)) {
                            String fillerName = fillerClass.asOWLDatatype().getIRI().getShortForm();
                            Node fillerNode = getOrCreateNodeWithUniqueFactory(fillerName, graphDB);
                            fillerNode.addLabel(NodeTypes.DATATYPE);
                            fillerNode.setProperty("uri", fillerClass.asOWLDatatype().getIRI().toString());
                            // newNode.createRelationshipTo(fillerNode, RelationshipType.withName(relation));
                            Relationship propRelation = newNode.createRelationshipTo(fillerNode,
                                    RelationshipType.withName(relation));
                            propRelation.setProperty("uri",
                                    dataPropertyExpression.asOWLDataProperty().getIRI().toString());
                            ///propRelation.setProperty("owlRelationshipType", "equivalent");
                            //newNode.setProperty("DataSomeValuesFrom", "test"); // add the data type axiom as a string (below, add it as a node)
                        } else if (fillerClass.getDataRangeType().equals(DataRangeType.DATA_ONE_OF)) { // get the data_one_of values such as "true"
                            StringBuilder sb = new StringBuilder();
                            String prefix = "";
                            for (OWLLiteral dataoneof : ((OWLDataOneOfImpl) fillerClass).getValues()) {
                                sb.append(prefix + dataoneof.getLiteral());
                                prefix = "|";
                            }
                            String fillerName = sb.toString();
                            Node fillerNode = getOrCreateNodeWithUniqueFactory(fillerName, graphDB);
                            fillerNode.addLabel(NodeTypes.DATA_ONE_OF);
                            fillerNode.setProperty("uri", OntologyConstants.LITERAL_URI);
                            // newNode.createRelationshipTo(fillerNode, RelationshipType.withName(relation));
                            Relationship propRelation = newNode.createRelationshipTo(fillerNode,
                                    RelationshipType.withName(relation));
                            propRelation.setProperty("uri",
                                    dataPropertyExpression.asOWLDataProperty().getIRI().toString());
                            //propRelation.setProperty("owlRelationshipType", "equivalent");
                            //newNode.setProperty("DataSomeValuesFrom", "test"); // add the data type axiom as a string (below, add it as a node)

                        } else if (fillerClass.getDataRangeType().equals(DataRangeType.DATATYPE_RESTRICTION)) { // add datatype restriction as properties of the new node
                            StringBuilder sb = new StringBuilder();
                            String prefix = "";
                            for (OWLFacetRestriction facet : ((OWLDatatypeRestrictionImpl) fillerClass).getFacetRestrictions()) {
                                String symbol = ((OWLFacetRestrictionImpl) facet).toString();
                                sb.append(prefix + ((OWLFacetRestrictionImpl) facet).toString());
                                prefix = "|";
                            }
                            newNode.setProperty(relation, sb.toString());

                        } else {
                            System.out.println ("Could not handle datatype: " + fillerClass.getDataRangeType() + "  for node: " + cls.getIRI());
                        }
                    }
                }


                // get all instances and create a node for each
                if (includeInstances){
                    if (cls.getIRI().toString().contains("Uncertain_Certainty")) {
                        int xx = 1;
                    }
                    Set<OWLIndividual> individuals = cls.getIndividuals(ontology);
                    for (OWLIndividual individual : individuals) {
                        System.out.println(individual.toString());
                        OWLNamedIndividualImpl indiv = (OWLNamedIndividualImpl) individual;
                        Node indNode = getOrCreateNodeWithUniqueFactory(indiv.getIRI().getShortForm(), graphDB);
                        indNode.setProperty("uri", indiv.getIRI().toString());
                        //Node indNode = getOrCreateNodeWithUniqueFactory(((((OWLNamedIndividualImpl) indiv).getIRI()).getIRI().getShortForm(), graphDB);
                        //                    for (OWLAnnotationAssertionAxiom annot : indiv.getAnnotationAssertionAxioms(ontology)){
                        //                        annot.getProperty()
                        //                    }


                        for (OWLAnnotationAssertionAxiom axiom : indiv.getAnnotationAssertionAxioms(ontology)) {
                            //System.out.println(axiom.toString());
                            String relType = axiom.getProperty().getIRI().getShortForm();
                            OWLLiteral value = (OWLLiteral) axiom.getValue();
                            String valueStr = value.getLiteral();
                            indNode.setProperty(relType, valueStr);
                        }

                        Map<OWLObjectPropertyExpression, Set<OWLIndividual>> objProps = indiv.getObjectPropertyValues(ontology);
                        List<String> valList = new ArrayList<>();
                        for (OWLObjectPropertyExpression key : objProps.keySet()) {
                            Set<OWLIndividual> values = objProps.get(key);
                            for (OWLIndividual val : values) {
                                valList.add(((OWLObjectPropertyImpl) key).getIRI() + "::" + ((OWLNamedIndividualImpl) val).getIRI());
                            }
                        }
                        indNode.setProperty("objectProperties", String.join("|", valList));
                        indNode.createRelationshipTo(newNode, RelationshipType.withName("hasIndividual"));
                    }
                }
            }

            tx.success();
        }finally {
            tx.close();
        }

    }

    static void printLabels(Map<NodeTypes, Integer> labelCtr) {
        for (NodeTypes type : labelCtr.keySet()) {
            System.out.println(type + " : " + labelCtr.get(type));
        }
    }

    public void labelDomainOntology() {
        Transaction tx = graphDB.beginTx();

        try {
            addAnchorLabels();
            addLinguisticModifierLabels();
            addVariableLabels();
            addNumericModifierLabels();
            addLexicalItemLabels();
            addClosureLabels();
            addPseudoModifierLabels();
            addPseudoAnchorLabels();
            addQualifierLabels();
            addSemanticModifierLabels();
            //todo This causes double labeling.
                        //addSemanticCategoryLabels();
            addCategoryLables();
            addAnnotationTypeLabels();
            addDocumentsSectionLabels();
            labelUnlabeled();
            //fixRelations();
            makeAllOwlEntry();
            removeSelfReferences();
            tx.success();
            System.out.println("\n\n** Labels in ontology " + ontologyIRI);
            printLabels(this.labelCounter);
        }
        finally {
            tx.close();
        }
    }

    private void removeSelfReferences() {
        graphDB.execute("match(n)-[r]->(n) delete r");
    }

    private void makeAllOwlEntry() {
        graphDB.execute("MATCH (p) set p:OWL_ENTRY return null");

    }

    private void fixOne(String from, String to) {
        Transaction tx = graphDB.beginTx();
        try {
            Result result = graphDB.execute("MATCH (p)-[:" + from + "]->(q) RETURN p");
            ResourceIterator<Node> iter = result.columnAs("p");
            while (iter.hasNext()) {
                Node fr = iter.next();
                Iterator<Relationship> riter = fr.getRelationships(RelationshipType.withName(from), Direction.OUTGOING).iterator();
                Iterator<Relationship> reverse = fr.getRelationships(RelationshipType.withName(to), Direction.INCOMING).iterator();
                Set<Node> right = new HashSet<>();
                while (reverse.hasNext()) {
                    right.add(reverse.next().getStartNode());
                }
                while (riter.hasNext()) {
                    Relationship r = riter.next();
                    Node end = r.getEndNode();
                    if (!right.contains(end)) {
                        end.createRelationshipTo(fr, RelationshipType.withName(to));
                        //create right relationship
                    }
                    r.delete();

                }
            }
            tx.success();
        }
        catch (QueryExecutionException e) {
            tx.failure();
            throw new RuntimeException(e);
        }
        finally {
            tx.close();
        }

    }

    private void fixRelations() {
        fixOne("isAnchorOf", "hasAnchor");
        fixOne("isTerminationOf", "hasTermination");
        fixOne("isPseudoOf", "hasPseudo");
    }

    public void makeCopy(GraphDatabaseService copyDB) {
        System.out.println("Making copy");
        Transaction tx_copy = copyDB.beginTx();
        Transaction tx = graphDB.beginTx();
        try {
            /*MATCH (n:Node {name: 'abc'})
            WITH n as map
            create (copy:Node)
            set copy=map return copy*/
            Result result = copyDB.execute("MATCH (n) RETURN n");

            List<Node> nodeListOrig = new ArrayList<Node>();
            Map<Long, Long> nodeIDMap = new HashMap<Long, Long>();
            Map<Node, Node> nodeMap = new HashMap<Node, Node>();
            ResourceIterator<Node> iter = result.columnAs("n");
            while (iter.hasNext()) {
                Node foundNode = iter.next();
                nodeListOrig.add(foundNode);
                Node newNode = graphDB.createNode();
                nodeIDMap.put(foundNode.getId(), newNode.getId()); // key: original node ID, value: new node ID
                nodeMap.put(foundNode, newNode);
                // add labels
                Iterator<Label> labelIter = foundNode.getLabels().iterator();
                while (labelIter.hasNext()) {
                    Label lab = labelIter.next();
                    //System.out.println(lab.toString());
                    newNode.addLabel(lab);
                }
                //newNode.addLabel(NodeTypes.COPY);
                // add properties to the new node
                Map<String, Object> props = foundNode.getAllProperties();
                for (String key : props.keySet()) {
                    newNode.setProperty(key, props.get(key));
                    //System.out.println(key+" "+ props.get(key));
                }
                //                System.out.println("Copied node " + foundNode.getId());
            }

            // add the relationships
            for (Node origNode : nodeMap.keySet()) {
                Node newNode = nodeMap.get(origNode);
                Iterator<Relationship> relIter = origNode.getRelationships().iterator();

                //            Iterator<Relationship> relIter2 = newNode.getRelationships().iterator();
                //            while (relIter2.hasNext()) {
                //                Relationship relation2 = relIter2.next();
                //                System.out.println("New Node: " + relation2.toString());
                //            }

                while (relIter.hasNext()) {
                    Relationship relation = relIter.next();
                    //System.out.println("Orig Node: " + relation.toString());
                    // figure out if the original node is the 1st or 2nd node in the relationsihp
                    Node otherNode;
                    if (relation.getStartNodeId() == origNode.getId()) { // if the original node is the 1st node
                        otherNode = nodeMap.get(relation.getEndNode());
                    }
                    else { // if the original node is the 2nd node
                        continue; // skip it. It will be added by the other node
                        //otherNode = nodeMap.get(relation.getStartNode());
                    }
                    Relationship propRelation = newNode.createRelationshipTo(otherNode, relation.getType());

                    if (newNode.getId() == otherNode.getId()) {
                        int xxx = 1;
                    }

                    if (isRedundant(propRelation, newNode)) {
                        propRelation.delete();
                    }
                    else {
                        //System.out.println("Not redundant");
                    }

                    // add relationship properties
                    Map<String, Object> relProperties = relation.getAllProperties();
                    for (String key : relProperties.keySet()) {
                        try {
                            propRelation.setProperty(key, relProperties.get(key));
                        }
                        catch (Exception e) {
                            System.out.println("Can't find property for key " + key);
                        }
                    }
                    //                String uri;
                    //                try {
                    //                    uri = uriObj.toString();
                    //                    propRelation.setProperty("uri", relation.getAllProperties().get("uri"));
                    //                } catch(Exception ex) {
                    //
                    //                }

                }
            }

            tx_copy.success();
            tx.success();
        }
        finally {
            tx_copy.close();
            tx.close();
        }
        //        for (Node origNode : nodeListOrig){
        //            Iterator<Relationship> relIter = origNode.getRelationships().iterator();
        //            while (relIter.hasNext()){
        //                Relationship relation = relIter.next();
        //                System.out.println(relation.toString());
        //                //
        ////                Relationship propRelation = foundNode.createRelationshipTo(fillerNode,
        ////                        relation.getType());
        ////                propRelation.setProperty("uri",
        ////                        objPropertyExpression.asOWLObjectProperty().getIRI().toString());
        //            }
        //       }
    }

    public void labelSchemaOnt() {
        this.labelDomainOntology();
        //        Transaction tx = graphDB.beginTx();
        //        try {
        //            Result result = graphDB.execute("MATCH (n) RETURN n");
        //
        //            ResourceIterator<Node> iter = result.columnAs("n");
        //            while (iter.hasNext()) {
        //                Node foundNode = iter.next();
        //                foundNode.addLabel(NodeTypes.SCHEMA_ONTOLOGY);
        //            }
        //            tx.success();
        //        }
        //        finally {
        //            tx.close();
        //        }
    }

    public void labelModifierOnt() {
        this.labelDomainOntology();
        //        Transaction tx = graphDB.beginTx();
        //        try {
        //            Result result = graphDB.execute("MATCH (n) RETURN n");
        //
        //            ResourceIterator<Node> iter = result.columnAs("n");
        //            while (iter.hasNext()) {
        //                Node foundNode = iter.next();
        //                foundNode.addLabel(NodeTypes.MODIFIER_ONTOLOGY);
        //            }
        //            tx.success();
        //        }
        //        finally {
        //            tx.close();
        //        }
    }

    private boolean isRedundant(Relationship rel, Node nd) {
        Iterator<Relationship> relIter = nd.getRelationships().iterator();
        boolean answer = false;
        int matchCount = 0;
        while (relIter.hasNext()) {
            Relationship nodeRel = relIter.next();
            String relToString = rel.getStartNodeId() + rel.getType().toString() + rel.getEndNodeId();
            String nodeRelToString = nodeRel.getStartNodeId() + nodeRel.getType().toString() + nodeRel.getEndNodeId();
            if (relToString.equals(nodeRelToString)) {
                matchCount++;
                //System.out.println("Redundant: " + relToString + "   " + nodeRelToString);
            }
            if (matchCount > 1) {
                answer = true;
                break;
            }
            //System.out.println(relToString);
        }

        return answer;
    }

    public enum NodeTypes implements Label {
        THING,
        INDIVIDUAL,
        COPY,
        OWL_GROUP,
        ONTOLOGY,
        NOS,
        SCHEMA_ONTOLOGY,
        MODIFIER_ONTOLOGY,
        VARIABLE,
        NUMERIC_MODIFIER,
        ANCHOR,
        LEXICAL_ITEM,
        CLOSURE,
        PSEUDO_MODIFIER,
        QUALIFIER,
        LINGUISTIC_MODIFIER,
        SEMANTIC_MODIFIER,
        MODIFIER,
        COMPOUND_ANCHOR,
        SEMANTIC_CATEGORY,
        DOCUMENT_SECTION,
        ANNOTATION_TYPE,
        PSEUDO_ANCHOR,
        DATATYPE,
        DATA_ONE_OF,
        CATEGORY;
    }

    private void setLabels(ResourceIterator<Node> iter, NodeTypes type) {
        int cnt = 0;
        while (iter.hasNext()) {
            Node foundNode = iter.next();
            if (foundNode.getLabels() != null && foundNode.getLabels().iterator().hasNext()) {
                List<NodeTypes> existing = new ArrayList<>();
                Iterator<Label> lblIterator = foundNode.getLabels().iterator();
                while (lblIterator.hasNext()) {
                    existing.add(NodeTypes.valueOf(lblIterator.next().name()));
                }
                if (existing.contains(type)) {
                    continue;
                }
                System.err.println("Double labels on node " + foundNode.getProperty("name") + ": " + existing + " while assigning label " + type);
            }
            foundNode.addLabel(type);
            cnt++;
        }
        Integer total = this.labelCounter.get(type);
        if (total == null) {
            total = 0;
        }
        total += cnt;
        this.labelCounter.put(type, total);
    }

    private void owlHierarchyLabel(NodeTypes type, String... owlTopNodes) {
        for (String owlTopNode : owlTopNodes) {
            Result result = graphDB.execute("MATCH (nd {name:'" + owlTopNode + "'})<-[:IS_A*]-(child)" + " RETURN child");
            ResourceIterator<Node> iter = result.columnAs("child");
            this.setLabels(iter, type);

            Result prt = graphDB.execute("MATCH (nd {name:'" + owlTopNode + "'})" + " RETURN nd");
            ResourceIterator<Node> iter2 = prt.columnAs("nd");
            this.setLabels(iter2, NodeTypes.OWL_GROUP);

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

    private static Node getOrCreateNodeWithUniqueFactory(String nodeName, GraphDatabaseService graphDb) { // without adding a label
        UniqueFactory<Node> factory = new UniqueFactory.UniqueNodeFactory(graphDb, "index") {
            @Override
            protected void initialize(Node created, Map<String, Object> properties) {
                created.setProperty("name", properties.get("name"));
            }
        };

        return factory.getOrCreate("name", nodeName);
    }

    private void addLinguisticModifierLabels() {
        this.owlHierarchyLabel(NodeTypes.LINGUISTIC_MODIFIER, "LinguisticModifier");
    }

    private void addAnchorLabels() {
        this.owlHierarchyLabel(NodeTypes.ANCHOR, "Anchor", "SemanticType", "CompoundAnchor");
    }

    private void addVariableLabels() {
        this.owlHierarchyLabel(NodeTypes.VARIABLE, "Annotation");
    }

    private void addDocumentsSectionLabels() {
        this.owlHierarchyLabel(NodeTypes.DOCUMENT_SECTION, "DocumentSection");
    }

    private void addNumericModifierLabels() {
        this.owlHierarchyLabel(NodeTypes.NUMERIC_MODIFIER, "NumericModifier");
    }

    private void addLexicalItemLabels() {
        this.owlHierarchyLabel(NodeTypes.LEXICAL_ITEM, "LexicalItem");

    }

    private void addClosureLabels() {
        this.owlHierarchyLabel(NodeTypes.CLOSURE, "Closure");
    }

    private void addPseudoModifierLabels() {
        this.owlHierarchyLabel(NodeTypes.PSEUDO_MODIFIER, "PseudoModifier");
    }

    private void addPseudoAnchorLabels() {
        this.owlHierarchyLabel(NodeTypes.PSEUDO_ANCHOR, "PseudoAnchor");
    }

    private void addQualifierLabels() {
        this.owlHierarchyLabel(NodeTypes.QUALIFIER, "Qualifier");
    }

    private void addAnnotationTypeLabels() {
        this.owlHierarchyLabel(NodeTypes.ANNOTATION_TYPE, "AnnotationType");
    }

    private void addSemanticModifierLabels() {
        this.owlHierarchyLabel(NodeTypes.SEMANTIC_MODIFIER, "SemanticModifier");
    }

    private void addSemanticCategoryLabels() {
        // todo: make more robust to SemanticCategory named "SemanticCategory"
        Result result = graphDB.execute("MATCH (nd {name:'Event'})<-[:IS_A*]-(child)" + "WHERE child.uri =~ '.*" + OntologyConstants.SCHEMA_BASE_URI + ".*' " + "MATCH (child) "
                                        + "WHERE NOT (()-[:IS_A]->(child)) " + // match node that does not have any children
                                        "RETURN child");

        ResourceIterator<Node> iter = result.columnAs("child");
        this.setLabels(iter, NodeTypes.SEMANTIC_CATEGORY);

        // match notes that have children that are not from the Schema ontology
        result = graphDB.execute(
                "MATCH (nd {name:'Event'})<-[:IS_A*]-(child) " + "WHERE child.uri =~ '.*" + OntologyConstants.SCHEMA_BASE_URI + ".*' " + "MATCH ((newchild)-[:IS_A]->(child)) "
                + "WHERE NOT newchild.uri =~ '.*" + OntologyConstants.SCHEMA_BASE_URI + ".*' " + "RETURN child");

        ResourceIterator<Node> iter2 = result.columnAs("child");
        this.setLabels(iter2, NodeTypes.SEMANTIC_CATEGORY);

    }

    private void addCategoryLables() {
        Set<String> categoryURIs = new HashSet<>();
        categoryURIs.add("http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl#Encounter");
        categoryURIs.add("http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl#Procedure");
        categoryURIs.add("http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl#Observation");
        categoryURIs.add("http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl#MedicationStatement");
        categoryURIs.add("http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl#Condition");
        categoryURIs.add("http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl#AllergyIntolerance");
        categoryURIs.add("http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl#Patient");

        for (String catURI : categoryURIs){
            Result result = graphDB.execute("MATCH (n) WHERE n.uri=\"" + catURI + "\" RETURN n");
            ResourceIterator<Node> iter = result.columnAs("n");
            this.setLabels(iter, NodeTypes.CATEGORY);
        }
    }

    private void labelUnlabeled() {
        Result result = graphDB.execute("MATCH (n) " + "WHERE size(labels(n)) = 0\n" + "RETURN n");
        ResourceIterator<Node> iter = result.columnAs("n");
        this.setLabels(iter, NodeTypes.NOS);
    }

    public GraphDatabaseService getGraphDB() {
        return graphDB;
    }
}
