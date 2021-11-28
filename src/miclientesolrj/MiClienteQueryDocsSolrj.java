package miclientesolrj;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import solrparser.SolrQueryParser;

/**
 * @author Valeo
 */
public class MiClienteQueryDocsSolrj {
    
    public static void main(String[] args) throws IOException,
			SolrServerException {
        
        HttpSolrClient solr = new HttpSolrClient.Builder("http://localhost:8983/solr/gettingstarted").build();
        QueryResponse rsp;
        
        // Ruta al fichero de documentos del corpus
        String path = "./docs_to_import/CISI.QRY";
        
        ArrayList<SolrQuery> queries = SolrQueryParser.parseQueries(path);
        ArrayList<SolrDocumentList> results = new ArrayList<>();
        
        for (SolrQuery q : queries) {
            rsp = solr.query(q);
            results.add(rsp.getResults());
        }
        
        // Visualizar cuantos documentos se han recuperado con cada
        // consulta y los titulos y scores de cada uno
        for (int i = 0; i < results.size(); i++) {
            System.out.println("Num resultados para la consulta " 
                     + (i+1) + ": " + results.get(i).getNumFound());
            
            for (int j = 0; j < results.get(i).getNumFound() && j < 10; j++) {
                System.out.println(results.get(i).get(j).toString());
            }
            
            System.out.println("----------------------------------"
                    + "-----------------------------------------");
        }
             
        
        System.out.println("Num de consultas procesadas: " + queries.size());
        
    }
    
}
