package edu.utah.blulab.marshallers.graph;

import edu.utah.blulab.marshallers.old.OwlToGraph_bill;

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
        OwlToGraph_bill mainOnt1 = new OwlToGraph_bill();
        mainOnt1.createNewDB(graphFile);
        mainOnt1.createDBfromOnt(ontFile, OwlToGraph_bill.NodeTypes.ONTOLOGY, true);
        mainOnt1.labelDomainOntology();

        // create domain ontology 2 DB
        File ontFile2 = new File(ontFileStr2);
        String name2 = ontFile2.getName();
        name2 = name2.substring(0, name2.lastIndexOf("."));
        graphFile = new File(ontFile2.getParent() + "/db/" + name2);
        OwlToGraph_bill mainOnt2 = new OwlToGraph_bill();
        mainOnt2.createNewDB(graphFile);
        mainOnt2.createDBfromOnt(ontFile2, OwlToGraph_bill.NodeTypes.ONTOLOGY, true);
        mainOnt2.labelDomainOntology();

        // create Schema ontology DB
        File schemaFile = new File(schemaFileStr);
        name = schemaFile.getName();
        name = name.substring(0, name.lastIndexOf("."));
        graphFile = new File(schemaFile.getParent() + "/db/" + name);
        OwlToGraph_bill mainSchema = new OwlToGraph_bill();
        mainSchema.createNewDB(graphFile);
        mainSchema.createDBfromOnt(schemaFile, OwlToGraph_bill.NodeTypes.SCHEMA_ONTOLOGY, false);
        mainSchema.labelSchemaOnt();

        // create Modifier ontology DB
        File modifierFile = new File(modifierFileStr);
        name = modifierFile.getName();
        name = name.substring(0, name.lastIndexOf("."));
        graphFile = new File(modifierFile.getParent() + "/db/" + name);
        OwlToGraph_bill mainModifier = new OwlToGraph_bill();
        mainModifier.createNewDB(graphFile);
        mainModifier.createDBfromOnt(modifierFile, OwlToGraph_bill.NodeTypes.MODIFIER_ONTOLOGY, true);
        mainModifier.labelModifierOnt();

        // create root db
        File graphFileRoot = new File(rootDBName);
        OwlToGraph_bill mainRoot = new OwlToGraph_bill();
        mainRoot.createNewDB(graphFileRoot);

        // add ontology databases to root DB
        mainRoot.makeCopy(mainOnt1.getGraphDB());
        mainRoot.makeCopy(mainOnt2.getGraphDB());
        mainRoot.makeCopy(mainSchema.getGraphDB());
        mainRoot.makeCopy(mainModifier.getGraphDB());

        // delete ontology databases (?)

    }
}
