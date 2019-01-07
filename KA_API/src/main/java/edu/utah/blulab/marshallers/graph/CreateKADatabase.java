package edu.utah.blulab.marshallers.graph;

import java.io.File;

/**
 * Created by Bill on 7/27/2017.
 */
public class CreateKADatabase {

    public static void main(String[] args) throws Exception {

        String ontFileStr1 = "C:\\Users\\Bill\\Documents\\2018\\Chapman Lab\\Data\\ontology\\smoking.owl";
        String ontFileStr2 = "C:\\Users\\Bill\\Documents\\2018\\Chapman Lab\\Data\\ontology\\heartDiseaseInDiabetics.owl";
        String schemaFileStr = "C:\\Users\\Bill\\Documents\\2018\\Chapman Lab\\Data\\ontology\\Schema.owl";
        String modifierFileStr = "C:\\Users\\Bill\\Documents\\2018\\Chapman Lab\\Data\\ontology\\Modifier.owl";
        String rootDBName = "C:\\Users\\Bill\\Documents\\2018\\Chapman Lab\\Data\\ontology\\db\\KnowledgeAuthor";

        // create domain ontology DB
        File ontFile = new File(ontFileStr1);
        String name = ontFile.getName();
        name = name.substring(0, name.lastIndexOf("."));
        File graphFile = new File(ontFile.getParent() + "/db/" + name);
        OwlToGraph mainOnt1 = new OwlToGraph();
        mainOnt1.createNewDB(graphFile);
        mainOnt1.createDBfromOnt(ontFile, OwlToGraph.NodeTypes.ONTOLOGY, true);
        mainOnt1.labelDomainOntology();

        // create domain ontology 2 DB
        File ontFile2 = new File(ontFileStr2);
        String name2 = ontFile2.getName();
        name2 = name2.substring(0, name2.lastIndexOf("."));
        graphFile = new File(ontFile2.getParent() + "/db/" + name2);
        OwlToGraph mainOnt2 = new OwlToGraph();
        mainOnt2.createNewDB(graphFile);
        mainOnt2.createDBfromOnt(ontFile2, OwlToGraph.NodeTypes.ONTOLOGY, true);
        mainOnt2.labelDomainOntology();

        // create Schema ontology DB
        File schemaFile = new File(schemaFileStr);
        name = schemaFile.getName();
        name = name.substring(0, name.lastIndexOf("."));
        graphFile = new File(schemaFile.getParent() + "/db/" + name);
        OwlToGraph mainSchema = new OwlToGraph();
        mainSchema.createNewDB(graphFile);
        mainSchema.createDBfromOnt(schemaFile, OwlToGraph.NodeTypes.SCHEMA_ONTOLOGY, false);
        mainSchema.labelSchemaOnt();

        // create Modifier ontology DB
        File modifierFile = new File(modifierFileStr);
        name = modifierFile.getName();
        name = name.substring(0, name.lastIndexOf("."));
        graphFile = new File(modifierFile.getParent() + "/db/" + name);
        OwlToGraph mainModifier = new OwlToGraph();
        mainModifier.createNewDB(graphFile);
        mainModifier.createDBfromOnt(modifierFile, OwlToGraph.NodeTypes.MODIFIER_ONTOLOGY, true);
        mainModifier.labelModifierOnt();

        // create root db
        File graphFileRoot = new File(rootDBName);
        OwlToGraph mainRoot = new OwlToGraph();
        mainRoot.createNewDB(graphFileRoot);

        // add ontology databases to root DB
        mainRoot.makeCopy(mainOnt1.getGraphDB());
        mainRoot.makeCopy(mainOnt2.getGraphDB());
        mainRoot.makeCopy(mainSchema.getGraphDB());
        mainRoot.makeCopy(mainModifier.getGraphDB());

        // delete ontology databases (?)

    }
}
