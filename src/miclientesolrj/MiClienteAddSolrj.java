package miclientesolrj;

import java.io.IOException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

/**
 * @author Valeo
 */
public class MiClienteAddSolrj {
    
    public static void main(String[] args) throws IOException,
			SolrServerException {

            HttpSolrClient solr = new HttpSolrClient.Builder("http://localhost:8983/solr/gettingstarted").build();

            //Create solr document
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("name", "Jesús Valeo Fernández");
            doc.addField("age", 20);
            doc.addField("email", "jesus.valeo679@alu.uhu.es");
            solr.add(doc);
            solr.commit();
    }
    
}
