package gr.ntua.ditas.repositoryengine.restheartcustomization;

import org.restheart.metadata.transformers.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;

import io.undertow.server.HttpServerExchange;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

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
				Deque<String> d = new ArrayDeque<>();
				
				for(String qsection : qsections) {
					String section = parseSection(qsection , context);
					params.remove("section");
					if (!section.isEmpty()) {
						d.add("{'"+ section + "':1}");
						context.setKeys(d);
					}
				}
			}
			
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
			}	
		}else if (context.isPost()) {
			LOGGER.debug("POST request");
			
			if (contentToTransform.isArray()) 
				contentToTransform.asArray().forEach( 
						doc -> remove_id(doc , context)
				);
			if (contentToTransform.isDocument()) 
				remove_id(contentToTransform , context);
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
}   