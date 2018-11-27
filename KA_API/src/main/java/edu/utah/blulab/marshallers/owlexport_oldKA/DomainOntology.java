package edu.utah.blulab.marshallers.owlexport_oldKA;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.*;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.PrefixManager;

public class DomainOntology {
    private OWLOntologyManager manager;
    private OWLOntology ontology;
    private OWLDataFactory factory;
    private PrefixManager pm;
    private File ontFile;
    private String ontURI;

    public String getOntologyFromDB(final int ontologyID, final SchemaDatabase db) throws Exception {

        ArrayList<VariableObject> variables = new ArrayList<VariableObject>();
        ArrayList<VariableObject> numberVariables = new ArrayList<VariableObject>();

        db.exportVariables(ontologyID, variables, numberVariables);

        
       /**for(VariableObject v : numberVariables){ 
        	System.out.println(v.toString()); 
        } 
        
        for(VariableObject v : variables){ 
        	System.out.println(v.toString()); 
        }**/
         

        URI domainURI = URI.create(db.getDomainUri(ontologyID));

        this.create(domainURI);

        this.importVariables(numberVariables, variables);

        // convert ontology to a string
        StringDocumentTarget stringOntology = new StringDocumentTarget();
        manager.saveOntology(ontology, stringOntology);
        // System.out.println(stringOntology.toString());
        this.load(stringOntology.toString());

        return stringOntology.toString();
    }

    public void create(final URI domainURI) throws Exception {
        // create domain ontology
        manager = OWLManager.createOWLOntologyManager();
        IRI domainIRI = IRI.create(domainURI);
        ontology = manager.createOntology(domainIRI);
        factory = manager.getOWLDataFactory();

        // import schema ontology into domain ontology
        IRI toImport = IRI.create(OntologyConstants.SCHEMA_BASE_URI);
        OWLImportsDeclaration importDeclaration = factory.getOWLImportsDeclaration(toImport);
        manager.applyChange(new AddImport(ontology, importDeclaration));

        // save to string
        StringDocumentTarget stringOntology = new StringDocumentTarget();
        manager.saveOntology(ontology, stringOntology);
        this.load(stringOntology.toString());
    }

    public void load(final String ontologyStr) throws OWLException {
        manager = OWLManager.createOWLOntologyManager();
        StringDocumentSource stringSource = new StringDocumentSource(ontologyStr.toString());
        ontology = manager.loadOntologyFromOntologyDocument(stringSource);
        factory = manager.getOWLDataFactory();
        ontURI = ontology.getOntologyID().getOntologyIRI().toString();
        pm = manager.getOntologyFormat(ontology).asPrefixOWLOntologyFormat();
    }

    public void printInfo(final PrintStream out) {
        out.println("Ontology Loaded...");
        out.println("Document IRI: " + manager.getOntologyDocumentIRI(ontology));
        out.println("Ontology Imports: " + ontology.getImports());
        out.println("Ontology : " + ontology.getOntologyID());
        out.println("Format      : " + manager.getOntologyFormat(ontology));
        out.println(ontology.getDirectImportsDocuments());
        out.println("\n\n");
    }

    private void importVariables(final ArrayList<VariableObject> numVariables, final ArrayList<VariableObject> variables) throws OWLException {
        // create classes of number variables first
        for (VariableObject v : numVariables) {
            OWLClass child = factory.getOWLClass(IRI.create(v.getUri()));
            if(v.getType().size() < 1){
            	OWLClass parent = factory.getOWLClass(IRI.create(OntologyConstants.CLASS_SEMANTIC_ATTRIBUTE));
            	OWLAxiom axiom = factory.getOWLSubClassOfAxiom(child, parent);
                manager.applyChange(new AddAxiom(ontology, axiom));
            }else{
            	for (String s : v.getType()) {
                    OWLClass parent = factory.getOWLClass(IRI.create(s));
                    OWLAxiom axiom = factory.getOWLSubClassOfAxiom(child, parent);
                    manager.applyChange(new AddAxiom(ontology, axiom));
                }
            }

        }

        // add restrictions to number variable classes
        for (VariableObject v : numVariables) {
            for (AxiomObject ax : v.getAxioms()) {
                OWLClass child = factory.getOWLClass(IRI.create(v.getUri()));
                OWLClassExpression exp = ax.getRestriction(ontology);
                OWLSubClassOfAxiom subAx = factory.getOWLSubClassOfAxiom(child, exp);
                manager.applyChange(new AddAxiom(ontology, subAx));

            }
        }

        // create classes of variables
        for (VariableObject v : variables) {
            OWLClass child = factory.getOWLClass(IRI.create(v.getUri()));
            if(v.getType().size() < 1){
            	OWLClass parent = factory.getOWLClass(IRI.create(OntologyConstants.CLASS_ELEMENT));
            	OWLAxiom axiom = factory.getOWLSubClassOfAxiom(child, parent);
                manager.applyChange(new AddAxiom(ontology, axiom));
            }else{
            	for (String s : v.getType()) {
                    OWLClass parent = factory.getOWLClass(IRI.create(s));
                    OWLAxiom axiom = factory.getOWLSubClassOfAxiom(child, parent);
                    manager.applyChange(new AddAxiom(ontology, axiom));
                }
            }
            
            

            // create annotation properties
            for (AxiomObject ax : v.getAnnotations()) {
                if (ax.getObject() != null) {
                    manager.applyChange(new AddAxiom(ontology, ax.getAnnotationPropAxiom(ontology, child)));
                }

            }
        }
        // add restrictions to variables
        for (VariableObject v : variables) {
            for (AxiomObject ax : v.getAxioms()) {
                OWLClass child = factory.getOWLClass(IRI.create(v.getUri()));
                OWLClassExpression exp = ax.getRestriction(ontology);
                OWLSubClassOfAxiom subAx = factory.getOWLSubClassOfAxiom(child, exp);
                manager.applyChange(new AddAxiom(ontology, subAx));

            }
        }
    }

    
     /**public static void main(final String[] args) throws Exception, IllegalAccessException, ClassNotFoundException, SQLException {
    	 // TODO Auto-generated method stub 
    	 // int ontologyID = 38; 
    	 int ontologyID = 38; 
    	 // int ontologyID = 24; 
    	 String fileName = "/Users/mtharp/testOntologies/testExport.owl";
    	 
    	 // set db properties 
    	 Properties props = new Properties(); 
    	 props.setProperty("driver", "com.mysql.jdbc.Driver"); 
    	 props.setProperty("url","jdbc:mysql://localhost/ka"); 
    	 props.setProperty("username", "root"); 
    	 props.setProperty("password", "");
    	 
    	 // create & set properties for db 
    	 SchemaDatabase db = SchemaDatabase.getInstance(); 
    	 db.setDatabaseProperties(props);
    	 
    	 DomainOntology domain = new DomainOntology(); String ontologyStr = domain.getOntologyFromDB(ontologyID, db); System.out.println(ontologyStr);
    	 PrintWriter writer = new PrintWriter(fileName, "UTF-8"); 
    	 writer.println(ontologyStr); 
    	 writer.close();
    	 domain.printInfo(System.out);
    	 
    	 System.out.println("Done!");
      
     }**/
     

}
