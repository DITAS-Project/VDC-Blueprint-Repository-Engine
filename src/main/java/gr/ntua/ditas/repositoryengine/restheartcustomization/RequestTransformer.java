package gr.ntua.ditas.repositoryengine.restheartcustomization;

import org.restheart.metadata.transformers.Transformer;
import org.restheart.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;

import io.undertow.server.HttpServerExchange;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.restheart.handlers.RequestContext;
import org.restheart.db.MongoDBClientSingleton;
  
public class RequestTransformer implements Transformer {
	private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart.metadata.transformers.Transformer");
	private static final MongoClient client = MongoDBClientSingleton.getInstance().getClient();

	private static final List<String> SECTIONS = Arrays.asList(
			"INTERNAL_STRUCTURE",
			"DATA_MANAGEMENT",
			"ABSTRACT_PROPERTIES",
			"COOKBOOK_APPENDIX",
			"EXPOSED_API"
	);
	
    public static void escape(BsonDocument schema, boolean escapeDots) {
        BsonValue escaped = JsonUtils.escapeKeys(schema, escapeDots);

        if (escaped.isDocument()) {
            List<String> keys = Lists.newArrayList(schema.keySet().iterator());

            keys.stream().forEach(f -> schema.remove(f));

            schema.putAll(escaped.asDocument());
        }
    }
    
	@Override
	public void transform(HttpServerExchange exchange, RequestContext context, BsonValue contentToTransform,
			BsonValue args) {
		
		if (context.isGet()) {
			
			LOGGER.debug("GET request");
			//LOGGER.debug("Entry : "+ contentToTransform.asDocument().toJson());
			
			Map<String, Deque<String>> params = exchange.getQueryParameters();
			
			if (params.containsKey("section")) {
				LOGGER.debug("Section parameter found");
				Deque<String> qsections = params.get("section");
				Deque<String> d = new ArrayDeque<String>();
				
				for(String qsection : qsections) {
					String section = parseSection(qsection , context);
					params.remove("section");
					if (!section.isEmpty()) {
						d.add("{'"+ section + "':1}");
						context.setKeys(d);
					}
				}
			}
			
			Deque<String> sortBy = params.containsKey("sort_by") ? context.getSortBy() : new ArrayDeque<String>();
			sortBy.add("_id");
			context.setSortBy(sortBy);
			
			
			Deque<String> filter = context.getFilter();
			if (filter!=null) 
				filter.stream().forEach(
						(String f) -> {LOGGER.debug(f);}
				);
			else filter = new ArrayDeque<>();
			
			context.setFilter(filter);
		}else if (context.isPatch() && context.isDocument()) {
			
			LOGGER.debug("PATCH request");
			BsonValue id = context.getDocumentId();
			String db = context.getDBName();
			String collname = context.getCollectionName();
			
			MongoCollection<Document> coll = client.getDatabase(db).getCollection(collname);
			long count = coll.count(new Document("_id",  id));
			
			if (count == 0) {
				exchange.setStatusCode(404);
				context.setContent(new BsonDocument());
				exchange.endExchange();
			}
			
			if (contentToTransform.isDocument()) {
				BsonDocument data = contentToTransform.asDocument();
				if (data.containsKey("_etag")) data.remove("_etag");
				escape(contentToTransform.asDocument(), false);
			}	
			
			
			
		}else if (context.isPost()) {
			LOGGER.debug("POST request");
			
			if (contentToTransform.isArray()) 
				contentToTransform.asArray().forEach( 
						doc -> {
							remove_id(doc , context);
							escape(contentToTransform.asDocument(), true);
						}
				);
			if (contentToTransform.isDocument()) {
				remove_id(contentToTransform , context);
				escape(contentToTransform.asDocument(), true);
			}
			
		}else if (context.isDelete()) {
			LOGGER.debug("DELETE request");
			
			String id = context.getDocumentId().asObjectId().getValue().toHexString();
			HttpResponse response = elasticDeletion(id);
			int statusCode = response.getStatusLine().getStatusCode();
			if (!(statusCode == HttpStatus.SC_NOT_FOUND || statusCode == HttpStatus.SC_OK)) {
				exchange.setStatusCode(statusCode);
				context.setInError(true);
				context.setDocumentId(null);
				context.setContent(new BsonDocument());
				context.addWarning("Elastic Search Service is not available");
				exchange.endExchange();
			};
		}
	}
	
	
	private void remove_id(BsonValue doc, RequestContext context) {
		
		BsonDocument docd = doc.asDocument();
		if (docd.containsKey("_id")) {
			docd.remove("_id");
			context.addWarning("Field _id was filtered out from the request");
		}
		return;
	}



	private String parseSection(String qsection , RequestContext context) {
		
		if (SECTIONS.contains(qsection)) return qsection;
		
		try {
			
		      int index = Integer.parseInt(qsection);
		      return SECTIONS.get(index-1);
		      
		} catch (IndexOutOfBoundsException | NumberFormatException e) {
			
			String warning = "invalid section parameter";
			LOGGER.debug(warning);
			context.addWarning(warning);
			return "";
			
		}
	}
	
	
	private HttpResponse elasticDeletion(String id) {
		
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("publicUser", "Resolution");
		provider.setCredentials(AuthScope.ANY, credentials);
		
		HttpClient httpClient = HttpClientBuilder.create()
				.setDefaultCredentialsProvider(provider)
				.build();
		HttpResponse response;
		try {
			HttpDelete request = new HttpDelete("http://31.171.247.162:50014/ditas/blueprints/"+id);
			response = httpClient.execute(request);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return response;
	}
}   