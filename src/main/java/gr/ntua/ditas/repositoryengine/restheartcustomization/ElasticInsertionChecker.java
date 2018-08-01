package gr.ntua.ditas.repositoryengine.restheartcustomization;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.metadata.checkers.Checker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.HttpServerExchange;

public class ElasticInsertionChecker implements Checker {
	private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart.metadata.checkers.Checker");
	
	@Override
	public boolean check(HttpServerExchange exchange, RequestContext context, BsonDocument contentToCheck, BsonValue args) {
		LOGGER.debug("ElasticInsertionChecker is started...");
		
		String id = contentToCheck.getObjectId("_id").getValue().toHexString();
		BsonDocument elastic_doc = contentToCheck.getDocument("INTERNAL_STRUCTURE").getDocument("Overview");
		elastic_doc.remove("name");
		LOGGER.debug("blueprint for elastic : "+elastic_doc.toJson());
		
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("publicUser", "Resolution");
		provider.setCredentials(AuthScope.ANY, credentials);
		
		
		HttpClient httpClient = HttpClientBuilder.create()
				.setDefaultCredentialsProvider(provider)
				.build();
		
	   
		try {
			HttpPut request = new HttpPut("http://31.171.247.162:50014/ditas/blueprints/"+id);
			StringEntity params =new StringEntity(elastic_doc.toJson());
			request.addHeader("content-type", "application/json");
			request.setEntity(params);
			HttpResponse response = httpClient.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_OK) return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return false;
	}

    @Override
    public PHASE getPhase(RequestContext context) {
    	return PHASE.AFTER_WRITE;
    }

    @Override
    public boolean doesSupportRequests(RequestContext context) {
    	BsonDocument update = context.getContent().asDocument();
    	if (context.isPatch()) {
    		if (!(update.containsKey("INTERNAL_STRUCTURE")
    			|| update.containsKey("INTERNAL_STRUCTURE.Overview")
    			|| update.containsKey("INTERNAL_STRUCTURE.Overview.description")
    			||update.containsKey("INTERNAL_STRUCTURE.Overview.tags")))
    					return false;
    	}
    	return true;
    }

}
