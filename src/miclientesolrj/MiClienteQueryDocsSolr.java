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
public class MiClienteQueryDocsSolr {
    
    public static void main(String[] args) throws IOException,
			SolrServerException {
        
        HttpSolrClient solr = new HttpSolrClient.Builder("http://localhost:8983/solr/gettingstarted").build();
        QueryResponse rsp;
        
        // Ruta al fichero de docuementos del corpus
        String path = "./docs_to_import/CISI.QRY";
        
        ArrayList<SolrQuery> queries = SolrQueryParser.parseQueries(path);
        ArrayList<SolrDocumentList> results = new ArrayList<>();
        
        for (SolrQuery q : queries) {
            rsp = solr.query(q);
            results.add(rsp.getResults());
        }
        
        // DEBUG: si queremos visualizar cuantos documentos se han
        // recuperado con cada consulta
        // for (int i = 0; i < results.size(); i++)
        //     System.out.println("Num resultados para la consulta " 
        //             + i + ": " + results.get(i).getNumFound());
        
        System.out.println("Num de consultas procesadas: " + queries.size());
        
    }
    
}
