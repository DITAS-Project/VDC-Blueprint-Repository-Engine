package gr.ntua.ditas.repositoryengine.restheartcustomization;

import org.restheart.metadata.transformers.Transformer;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.File;
import java.util.List;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.restheart.handlers.RequestContext;

  
public class ResponseTransformer implements Transformer {
	private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart.metadata.transformers.Transformer");
    
	public static void unescape(BsonDocument schema) {
        BsonValue unescaped = JsonUtils.unescapeKeys(schema);

        if (unescaped != null && unescaped.isDocument()) {
            List<String> keys = Lists.newArrayList(schema.keySet().iterator());

            keys.stream().forEach(f -> schema.remove(f));

            schema.putAll(unescaped.asDocument());
        }
    }

	@Override
	public void transform(HttpServerExchange exchange, RequestContext context, BsonValue contentToTransform, BsonValue args) {
		
		if (context.isPost()) {
			LOGGER.debug("POST response");
			
			if (context.getResponseStatusCode() == HttpStatus.SC_CREATED) {
				LOGGER.debug("201 created, single insert");
				
				File location = new File(exchange.getResponseHeaders().get(Headers.LOCATION).getFirst());
				String loc_id = location.getName();
				BsonArray id = new BsonArray();
				id.add(new BsonString(loc_id));
				BsonArray id_arr = new BsonArray();
				id_arr.add(new BsonDocument()
						.append("blueprint_id", id));
				contentToTransform = new BsonDocument().append(
						"_embedded", new BsonDocument().append(
							"rh:result", id_arr));
				
				context.setResponseContent(contentToTransform);
				
			}else if  (context.getResponseStatusCode() == HttpStatus.SC_OK) {
				LOGGER.debug("200 OK , bulk insert");
				
				context.setResponseStatusCode(HttpStatus.SC_CREATED);
				
				BsonDocument data = contentToTransform.asDocument().getDocument("_embedded").getArray("rh:result").get(0).asDocument();
				BsonArray id_links = data.getDocument("_links").getArray("rh:newdoc");
				BsonArray ids = new BsonArray();
				for (BsonValue link : id_links) {
					String slink = link.asDocument().getString("href").getValue();
					String id = new File(slink).getName();
					ids.add(new BsonString(id));
				}
				data.append("blueprint_id", ids);
				data.remove("_links");
				data.remove("modified");
				data.remove("matched");
				data.remove("deleted");
				
				context.setResponseContent(contentToTransform);
				
			}
		}else if (context.isGet()) {
			LOGGER.debug("GET response");
			
			unescape(contentToTransform.asDocument());
		}	
	}
	
}   