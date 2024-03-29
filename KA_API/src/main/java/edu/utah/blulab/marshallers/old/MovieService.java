package edu.utah.blulab.marshallers.old;

import org.neo4j.helpers.collection.Iterators;

import java.util.*;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 30.05.12
 */
public class MovieService {

    private final CypherExecutor cypher;
    private String ontURI;

    public MovieService(String uri, String username, String password, String ontURI) {
        cypher = new BoltCypherExecutor(uri, username, password);
        this.ontURI = ontURI;
        //createCypherExecutor(uri, username, password);
    }

    public static void main(String[] args){
        String neo4jURL = "bolt://localhost:7687";
        String username =  "neo4j";
        String password = "chuck6";
        String ontURI = "http://blulab.chpc.utah.edu/ontologies/examples/smoking.owl"; //http://blulab.chpc.utah.edu/ontologies/examples/heartDiseaseInDiabetics.owl" ) )
        final MovieService service = new MovieService(neo4jURL, username, password, ontURI);
        service.graph(10);
    }

//    private CypherExecutor createCypherExecutor(String uri) {
//        try {
//            String auth = new URL(uri.replace("bolt","http")).getUserInfo();
//            if (auth != null) {
//                String[] parts = auth.split(":");
//                return new BoltCypherExecutor(uri,parts[0],parts[1]);
//            }
//            return new BoltCypherExecutor(uri);
//        } catch (MalformedURLException e) {
//            throw new IllegalArgumentException("Invalid Neo4j-ServerURL " + uri);
//        }
//    }

    public Map findMovie(String title) {
        if (title==null) return Collections.emptyMap();
        return Iterators.singleOrNull(cypher.query(
                "MATCH (movie:Movie {title:{title}})" +
                        " OPTIONAL MATCH (movie)<-[r]-(person:Person)\n" +
                        " RETURN movie.title as title, collect({name:person.name, job:head(split(lower(type(r)),'_')), role:r.roles}) as cast LIMIT 1",
                map("title", title)));
    }

    @SuppressWarnings("unchecked")
    public Iterable<Map<String,Object>> search(String query) {
        if (query==null || query.trim().isEmpty()) return Collections.emptyList();
        return Iterators.asCollection(cypher.query(
                "MATCH (movie:Movie)\n" +
                        " WHERE lower(movie.title) CONTAINS {part}\n" +
                        " RETURN movie",
                map("part", query.toLowerCase())));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> graph(int limit) {
//        Iterator<Map<String,Object>> result = cypher.query(
//                "MATCH (m:Movie)<-[:ACTED_IN]-(a:Person) " +
//                        " RETURN m.title as movie, collect(a.name) as cast " +
//                        " LIMIT {limit}", map("limit",limit));
        Iterator<Map<String,Object>> result = cypher.query(
        "MATCH (parent {name:'" + ontURI  + "'})<-[rel:IS_A*..2]-(child) " +
                "RETURN parent.name as name, rel, collect(child.name) as childname " +
                "LIMIT {limit}", map("limit",limit));
        List nodes = new ArrayList();
        List rels= new ArrayList();
        int i=0;
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            nodes.add(map("name",row.get("name"),"label","parent"));
            int target=i;
            i++;
            for (Object name : (Collection) row.get("childname")) {
                Map<String, Object> actor = map("title", name,"label","child");
                int source = nodes.indexOf(actor);
                if (source == -1) {
                    nodes.add(actor);
                    source = i++;
                }
                rels.add(map("source",source,"target",target));
            }
        }
        return map("nodes", nodes, "links", rels);
    }
}

