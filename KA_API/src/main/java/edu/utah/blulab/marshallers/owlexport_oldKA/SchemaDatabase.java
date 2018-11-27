package edu.utah.blulab.marshallers.owlexport_oldKA;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

public class SchemaDatabase {
    private static SchemaDatabase instance;
    private Properties properties;
    private Connection connection;
    private String owner;
    private OWLOntology ontology;
    private OWLReasoner reasoner;
    private Map<String, Integer> attributeCategoryMap;
    private String schemaName;

    private SchemaDatabase() {}

    public static SchemaDatabase getInstance() {
        if (SchemaDatabase.instance == null) {
            SchemaDatabase.instance = new SchemaDatabase();
        }
        return SchemaDatabase.instance;
    }

    public OWLOntology getOntology() {
        return ontology;
    }

    private OWLReasoner getReasoner() {
        if (reasoner == null) {
            reasoner = new StructuralReasonerFactory().createReasoner(ontology);
        }
        return reasoner;
    }

    /**
     * database.user,database.url,database.pass,database.driver
     * 
     * @param p
     */
    public void setDatabaseProperties(final Properties p) {
        properties = p;
    }

    /**
     * get generated key
     * 
     * @param st
     * @return
     * @throws SQLException
     */
    public static int getGeneratedKey(final Statement st) throws SQLException {
        int id = -1;
        ResultSet result = st.getGeneratedKeys();
        if (result.next()) {
            id = result.getInt(1);
        }
        result.close();
        return id;
    }

    /**
     * get connection
     * 
     * @return
     * @throws SQLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    private Connection getConnection() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (connection == null) {
            if (properties == null)
                throw new SQLException("Database properties not set");
            String driver = properties.getProperty("driver");
            String url = properties.getProperty("url");
            String user = properties.getProperty("username");
            String pass = properties.getProperty("password");
            if (driver == null || user == null || pass == null || url == null)
                throw new SQLException("Database properties don't specify username,password,driver or url fields");
            Class.forName(driver).newInstance();
            connection = DriverManager.getConnection(url, user, pass);
        }
        return connection;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(final String schemaName) {
        this.schemaName = schemaName;
    }

    public static String fixTableName(final String tableName, final String schemaName) {
        String nm = tableName;
        if (schemaName != null) {
            if (tableName.indexOf(".") > 0) {
                nm = tableName.substring(tableName.indexOf("."));
            }
            nm = schemaName + "." + nm;
        }
        return nm;
    }

    protected String correctTableName(final String tbName) {
        return SchemaDatabase.fixTableName(tbName, schemaName);

    }

    public void setConnection(final Connection con) {
        connection = con;
    }

    public String getDomainUri(final int ontologyId) throws Exception {
        String domainUri = "";
        String sql = "SELECT uri from ka.ontology" + " where id = ?";
        PreparedStatement st = this.getConnection().prepareStatement(sql);
        st.setInt(1, ontologyId);
        ResultSet rs = st.executeQuery();
        while (rs.next()) {
            domainUri = rs.getString(1);
        }
        rs.close();
        st.close();

        return domainUri;
    }

    public void exportVariables(final int ontId, final ArrayList<VariableObject> list, final ArrayList<VariableObject> numberVariables) throws Exception {
        // ArrayList<VariableObject> numberVariables = new ArrayList<VariableObject>();

        // Get list of variable ids associated with domain
        List<Integer> var_ids = new ArrayList<Integer>();
        String sql = "SELECT id from " + this.correctTableName("concept") + " where ontologyId = ?";
        PreparedStatement st = this.getConnection().prepareStatement(sql);
        st.setInt(1, ontId);
        ResultSet rs = st.executeQuery();
        while (rs.next()) {
            var_ids.add(rs.getInt(1));
        }
        rs.close();
        st.close();

        // Get domain uri
        String domainUri = this.getDomainUri(ontId);

        // System.out.println("domainUri: " + domainUri);
        for (int id : var_ids) {
            list.add(SchemaDatabase.getVarObj(id, this.getConnection(), numberVariables, domainUri));

        }

        // return numberVariables;
    }

    public static VariableObject getVarObj(final int varId, final Connection con, final ArrayList<VariableObject> numValues, final String domainUri) throws SQLException {
        VariableObject variable = new VariableObject();
        ArrayList<AxiomObject> axioms = new ArrayList<AxiomObject>();
        ArrayList<AxiomObject> annotations = new ArrayList<AxiomObject>();

        // get category (if person), uri, varName, conName, definition
        String sql = "SELECT * from " + "ka.concept" + " where id = ?";
        PreparedStatement st = con.prepareStatement(sql);
        st.setInt(1, varId);
        ResultSet rs = st.executeQuery();
        while (rs.next()) {
            // System.out.println("uri: " + rs.getString("uri"));
            variable.setUri(rs.getString("uri"));
            variable.setVarName(rs.getString("name"));
            // System.out.println("name: " + rs.getString("name"));
            variable.setConName(rs.getString("prefLabel"));
            
            //Adding hack to make prefLabel == variable name if prefLabel isn't provided in db
            if(variable.getConName() == null){
            	variable.setConName(variable.getVarName());
            }
            
            // System.out.println("definition: " + rs.getString("definition"));
            variable.setDefinition(rs.getString("definition"));
            if (rs.getBoolean("person")) {
                ArrayList<String> categories = new ArrayList<String>();
                categories.add(OntologyConstants.CLASS_PATIENT_DEMOGRAPHICS);
                variable.setType(categories);
            }
            annotations.add(new AxiomObject(OntologyConstants.PROP_LABEL, null, variable.getVarName(), false, false));
            annotations.add(new AxiomObject(OntologyConstants.PREF_LABEL, null, variable.getConName(), false, false));
            annotations.add(new AxiomObject(OntologyConstants.PROP_DEFINITION, null, variable.getDefinition(), false, false));

        }
        rs.close();
        st.close();

        // get categories if not person variable
        ArrayList<String> categories = new ArrayList<String>();
        sql =
                        "SELECT * FROM ka.cat_relation\n" + " RIGHT OUTER JOIN ka.category\n" + " ON cat_relation.categoryId = category.id\n"
                                        + " WHERE cat_relation.conceptId = ?";
        st = con.prepareStatement(sql);
        st.setInt(1, varId);
        rs = st.executeQuery();
        while (rs.next()) {
            String cat = rs.getString("uri");
            if (!cat.equalsIgnoreCase("http://blulab.chpc.utah.edu/ontologies/SchemaOntology.owl#Linguistic")) {
                categories.add(rs.getString("uri"));
            }

        }
        rs.close();
        st.close();
        variable.setType(categories);

        // get altLabels, hiddenLabels, prefCUI, altCUIs, subjExpLabels, abbrLabels
        ArrayList<String> altLabels = new ArrayList<String>();
        ArrayList<String> hiddenLabels = new ArrayList<String>();
        ArrayList<String> subjExpLabels = new ArrayList<String>();
        ArrayList<String> abbrLabels = new ArrayList<String>();
        ArrayList<String> altCUIs = new ArrayList<String>();

        sql = "SELECT * FROM ka.concept_info\n" + " WHERE concept_info.conceptId = ?";
        st = con.prepareStatement(sql);
        st.setInt(1, varId);
        rs = st.executeQuery();
        while (rs.next()) {
            String type = rs.getString("type");
            if (type.equalsIgnoreCase("Abbreviation")) {
                abbrLabels.add(rs.getString("value"));
                annotations.add(new AxiomObject(OntologyConstants.ABBR_LABEL, null, rs.getString("value"), false, false));
            } else if (type.equalsIgnoreCase("Misspelling")) {
                hiddenLabels.add(rs.getString("value"));
                annotations.add(new AxiomObject(OntologyConstants.HID_LABEL, null, rs.getString("value"), false, false));
            } else if (type.equalsIgnoreCase("SubjectiveExpression")) {
                subjExpLabels.add(rs.getString("value"));
                annotations.add(new AxiomObject(OntologyConstants.SUBJ_LABEL, null, rs.getString("value"), false, false));
            } else if (type.equalsIgnoreCase("PreferredCUI")) {
                // System.out.println("prefCUI: " + rs3.getString("value"));
                variable.setPrefCUI(rs.getString("value"));
                annotations.add(new AxiomObject(OntologyConstants.PREF_CUI, null, rs.getString("value"), false, false));
            } else if (type.equalsIgnoreCase("AlternateCUI")) {
                altCUIs.add(rs.getString("value"));
                annotations.add(new AxiomObject(OntologyConstants.ALT_CUI, null, rs.getString("value"), false, false));
            } else if (type.equalsIgnoreCase("Synonym")) {
                altLabels.add(rs.getString("value"));
                annotations.add(new AxiomObject(OntologyConstants.ALT_LABEL, null, rs.getString("value"), false, false));
            }

        }
        rs.close();
        st.close();
        variable.setAbbrLabels(abbrLabels);
        variable.setAltLabels(altLabels);
        variable.setHiddenLabels(hiddenLabels);
        variable.setSubjExpLabels(subjExpLabels);
        //variable.setAnnotations(annotations);
        
        sql = "SELECT * FROM ka.cui_info\n" + " WHERE cui_info.conceptId = ?";
        st = con.prepareStatement(sql);
        st.setInt(1, varId);
        rs = st.executeQuery();
        while (rs.next()) {
            String type = rs.getString("type");
            if (type.equalsIgnoreCase("PreferredCUI")) {
                // System.out.println("prefCUI: " + rs3.getString("value"));
                variable.setPrefCUI(rs.getString("value"));
                annotations.add(new AxiomObject(OntologyConstants.PREF_CUI, null, rs.getString("value"), false, false));
            } else if (type.equalsIgnoreCase("AlternateCUI")) {
                altCUIs.add(rs.getString("value"));
                annotations.add(new AxiomObject(OntologyConstants.ALT_CUI, null, rs.getString("value"), false, false));
            }

        }
        rs.close();
        st.close();

        variable.setAltCUI(altCUIs);
        variable.setAnnotations(annotations);

        // Get list of attribute facts for each variable and create proper axiom objects for each type
        HashMap<Integer, ArrayList<AttributeFactObject>> af = new HashMap<Integer, ArrayList<AttributeFactObject>>();
        sql =
                        "SELECT attribute.id as attId, \n" + "attribute.uri as attURI, \n" + "attribute.catType as catType, \n"
                                        + "attribute_field.uri as fieldURI, \n" + "field_list.uri as listURI, \n" + "attribute_field.dataType as dataType, \n"
                                        + "attribute_fact.intField as intField, \n" + "attribute_fact.floatField as floatField, \n"
                                        + "attribute_fact.stringField as stringField, \n" + "attribute_fact.booleanField as booleanField, \n"
                                        + "attribute_fact.integerRangeField as integerRangeField, \n" + "attribute_fact.floatRangeField as floatRangeField, \n"
                                        + "attribute_fact.dateRangeField as dateRangeField, \n" + "attribute_fact.listField as listFiedl, \n"
                                        + "attribute_fact.textField as textField FROM attribute_fact \n"
                                        + "LEFT OUTER JOIN attribute_field ON attribute_fact.fieldId=attribute_field.id \n"
                                        + "LEFT OUTER JOIN field_list ON attribute_fact.listField=field_list.id \n"
                                        + "LEFT OUTER JOIN attribute ON attribute_field.attributeID = attribute.id \n" + "WHERE attribute_fact.conceptId = ?";
        st = con.prepareStatement(sql);
        st.setInt(1, varId);
        rs = st.executeQuery();
        while (rs.next()) {
            AttributeFactObject fact = new AttributeFactObject();
            fact.setAttURI(rs.getString("attURI"));
            fact.setBooleanField(rs.getString("booleanField"));
            fact.setCatType(rs.getString("catType"));
            fact.setDatatype(rs.getString("dataType"));
            fact.setDateRangeField(rs.getString("dateRangeField"));
            fact.setFieldURI(rs.getString("fieldURI"));
            fact.setFloatRangeField(rs.getString("floatRangeField"));
            fact.setIntegerRangeField(rs.getString("integerRangeField"));
            fact.setListURI(rs.getString("listURI"));
            fact.setIntField(rs.getInt("intField"));
            fact.setFloatField(rs.getDouble("floatField"));
            fact.setStringField(rs.getString("stringField"));
            fact.setBooleanField(rs.getString("booleanField"));

            if (af.containsKey(rs.getInt("attId"))) {
                ArrayList<AttributeFactObject> temp = af.get(rs.getInt("attId"));
                temp.add(fact);
                af.put(rs.getInt("attId"), temp);
            } else {
                ArrayList<AttributeFactObject> facts = new ArrayList<AttributeFactObject>();
                facts.add(fact);
                af.put(rs.getInt("attId"), facts);
            }

        }
        rs.close();
        st.close();

        int i = 100;
        Iterator it = af.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ArrayList<AttributeFactObject>> entry = (Entry<Integer, ArrayList<AttributeFactObject>>)it.next();
            // System.out.println("Att ID = " + entry.getKey().toString() + "\tFacts: " + entry.getValue().toString());
            ArrayList<AttributeFactObject> factList = entry.getValue();
            // Creating number variables that contain 2 facts- a num value and unit value
            if (factList.size() > 1) {
                // System.out.println("Num variable needs to be created!!");
                VariableObject numVar = new VariableObject();
                ArrayList<AxiomObject> numAxioms = new ArrayList<AxiomObject>();
                String valName = "";
                String unitName = "";
                for (AttributeFactObject fact : factList) {

                    ArrayList<String> str = new ArrayList<String>();
                    str.add(fact.getAttURI());
                    numVar.setType(str);
                    if (fact.getDatatype().equalsIgnoreCase("FloatRange")) {
                    	// Create class name for numeric value
                        String name = fact.getFloatRangeField();
                        if (name != null){
                        	name = name.replaceAll(",", "");
                        	name = name.replaceAll(" ", "_");
                            name = name.replaceAll(">", "gt");
                            name = name.replaceAll("<", "lt");
                            valName = name;
                        }else{
                        	valName = "some_float";
                        }
                        
                        if(fact.getFloatRangeField() != null){
                        	String[] temp = fact.getFloatRangeField().split(" ");

                            // If length of temp is <3 then just one threshold value needs to be encoded
                            if (temp.length < 3) {
                                // System.out.println("This is simple numeric value");
                                // String obj = "float[" + temp[0] + temp[1] + "]";

                                AxiomObject ax = new AxiomObject(true, false);
                                ax.setProperty(fact.getFieldURI());
                                ax.setOperator("some");
                                ax.setType("FloatRange");
                                ax.setSign(temp[0]);
                                ax.setData(temp[1].replaceAll(",", ""));
                                numAxioms.add(ax);
                            } else {
                                // System.out.println("this is a numeric range and needs to be split into 2 axioms");
                                // String[] obj = {};
                                // obj[0] = "float[" + temp[0] + temp[1] + "]";
                                // obj[1] = "float[" + temp[3] + temp[4] + "]";
                                AxiomObject ax1 = new AxiomObject(true, false);
                                ax1.setProperty(fact.getFieldURI());
                                ax1.setOperator("some");
                                ax1.setType("FloatRange");
                                ax1.setSign(temp[0]);
                                ax1.setData(temp[1].replaceAll(",", ""));
                                numAxioms.add(ax1);
                                AxiomObject ax2 = new AxiomObject(true, false);
                                ax2.setProperty(fact.getFieldURI());
                                ax2.setOperator("some");
                                ax2.setType("FloatRange");
                                ax2.setSign(temp[3]);
                                ax2.setData(temp[4].replaceAll(",", ""));
                                numAxioms.add(ax2);
                            }
                        	
                        }else{
                        	AxiomObject ax = new AxiomObject(true, false);
                            ax.setProperty(fact.getFieldURI());
                            ax.setOperator("some");
                            ax.setType("AnyFloat");
                            numAxioms.add(ax);
                        }
                    }
                    if (fact.getDatatype().equalsIgnoreCase("List")) {
                        String name = fact.getListURI();
                        unitName = name.substring(name.indexOf("#") + 1, name.indexOf("_Unit"));
                        AxiomObject ax = new AxiomObject(OntologyConstants.PROP_HAS_SEMANTIC_ATTRIBUTE, "some", fact.getListURI(), false, true);
                        numAxioms.add(ax);
                    }
                    if (fact.getDatatype().equalsIgnoreCase("IntegerRange")) {
                        // Create class name for numeric value
                        String name = fact.getIntegerRangeField();
                        if (name != null){
                        	name = name.replaceAll(",", "");
                        	name = name.replaceAll(" ", "_");
                            name = name.replaceAll(">", "gt");
                            name = name.replaceAll("<", "lt");
                            valName = name;
                        }else{
                        	valName = "some_integer";
                        }
                        
                        if(fact.getIntegerRangeField() != null){
                        	String[] temp = fact.getIntegerRangeField().split(" ");

                            // If length of temp is <3 then just one threshold value needs to be encoded
                            if (temp.length < 3) {
                                // System.out.println("This is simple numeric value");
                                // String obj = "float[" + temp[0] + temp[1] + "]";

                                AxiomObject ax = new AxiomObject(true, false);
                                ax.setProperty(fact.getFieldURI());
                                ax.setOperator("some");
                                ax.setType("IntegerRange");
                                ax.setSign(temp[0]);
                                ax.setData(temp[1].replaceAll(",", ""));
                                numAxioms.add(ax);
                            } else {
                                // System.out.println("this is a numeric range and needs to be split into 2 axioms");
                                // String[] obj = {};
                                // obj[0] = "float[" + temp[0] + temp[1] + "]";
                                // obj[1] = "float[" + temp[3] + temp[4] + "]";
                                AxiomObject ax1 = new AxiomObject(true, false);
                                ax1.setProperty(fact.getFieldURI());
                                ax1.setOperator("some");
                                ax1.setType("IntegerRange");
                                ax1.setSign(temp[0]);
                                ax1.setData(temp[1].replaceAll(",", ""));
                                numAxioms.add(ax1);
                                AxiomObject ax2 = new AxiomObject(true, false);
                                ax2.setProperty(fact.getFieldURI());
                                ax2.setOperator("some");
                                ax2.setType("IntegerRange");
                                ax2.setSign(temp[3]);
                                ax2.setData(temp[4].replaceAll(",", ""));
                                numAxioms.add(ax2);
                            }
                        	
                        }else{
                        	AxiomObject ax = new AxiomObject(true, false);
                            ax.setProperty(fact.getFieldURI());
                            ax.setOperator("some");
                            ax.setType("AnyInteger");
                            numAxioms.add(ax);
                        }
                        
                    }

                }
                numVar.setVarName(valName + "_" + unitName);
                numVar.setUri(domainUri + "#" + numVar.getVarName());
                numVar.setAxioms(numAxioms);
                numValues.add(numVar);

                // have to add numVar as axiom to original variable class
                AxiomObject numAx = new AxiomObject(OntologyConstants.PROP_HAS_SEMANTIC_ATTRIBUTE, "some", numVar.getUri(), false, true);
                axioms.add(numAx);

            }
            // creating non-number variables
            if (factList.size() < 2) {
                for (AttributeFactObject fact : factList) {
                    if (fact.getDatatype().equalsIgnoreCase("List")) {
                        AxiomObject ax = new AxiomObject(OntologyConstants.PROP_HAS_SEMANTIC_ATTRIBUTE, "some", fact.getListURI(), false, true);
                        axioms.add(ax);
                    }
                    if (fact.getDatatype().equalsIgnoreCase("StringField")) {

                        AxiomObject ax = new AxiomObject(true, false);
                        ax.setProperty(fact.getAttURI());
                        ax.setOperator("some");
                        ax.setType("String");
                        ax.setData(fact.getStringField());
                        axioms.add(ax);
                    }
                    if (fact.getDatatype().equalsIgnoreCase("DateRange")) {
                    	if (fact.getDateRangeField() != null){
                    		String[] temp = fact.getDateRangeField().split(" ");
                            
                            // If length of temp is <3 then just one threshold value needs to be encoded
                            if (temp.length < 3) {
                               if (temp.length >1){
                            	   AxiomObject ax = new AxiomObject(true, false);
                                   ax.setProperty(fact.getFieldURI());
                                   ax.setOperator("some");
                                   ax.setType("DateRange");
                                   ax.setSign(temp[0]);
                                   ax.setData(temp[1]);
                                   axioms.add(ax);
                               }else{
                            	   AxiomObject ax = new AxiomObject(true, false);
                                   ax.setProperty(fact.getFieldURI());
                                   ax.setOperator("some");
                                   ax.setType("AnyDate");;
                                   axioms.add(ax);
                               }
                            } else {
                                // System.out.println("this is a date range and needs to be split into 2 axioms");
                                AxiomObject ax = new AxiomObject(true, false);
                                ax.setProperty(fact.getFieldURI());
                                ax.setOperator("some");
                                ax.setType("DateRange");
                                ax.setSign(temp[0]);
                                ax.setData(temp[1]);
                                axioms.add(ax);
                                ax = new AxiomObject(true, false);
                                ax.setProperty(fact.getFieldURI());
                                ax.setOperator("some");
                                ax.setType("DateRange");
                                ax.setSign(temp[3]);
                                ax.setData(temp[4]);
                                axioms.add(ax);
                            }
                    	}
                    	
                    }

                }

            }
        }

        variable.setAxioms(axioms);
        return variable;
    }

    public List<Integer> getAllOntolgoyIds() throws Exception {
        // get the id for all of the ontologies in the database

        ArrayList<Integer> idList = new ArrayList<Integer>();
        String sql = "SELECT id FROM ka.ontology";
        PreparedStatement st = this.getConnection().prepareStatement(sql);
        ResultSet rs = st.executeQuery();
        while (rs.next()) {
            int id = rs.getInt("id");
            idList.add(id);
        }

        return idList;
    }

}
