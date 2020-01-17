/*
 * This file is part of VDC-Blueprint-Repository-Engine.
 * 
 * VDC-Blueprint-Repository-Engine is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 * 
 * VDC-Blueprint-Repository-Engine is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with VDC-Blueprint-Repository-Engine.  
 * If not, see <https://www.gnu.org/licenses/>.
 * 
 * VDC-Blueprint-Repository-Engine is being developed for the
 * DITAS Project: https://www.ditas-project.eu/
 */
package gr.ntua.ditas.repositoryengine.restheartcustomization;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.metadata.checkers.Checker;
import org.restheart.metadata.checkers.CheckersUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.undertow.server.HttpServerExchange;

public class LogicalValidator implements Checker {
	private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart.metadata.checkers.Checker");
	
	@Override
	public boolean check(HttpServerExchange exchange, RequestContext context, BsonDocument contentToCheck, BsonValue args) {
		LOGGER.debug("Logical validator is started...");
		//LOGGER.debug(contentToCheck.toJson());
		String warning;
		boolean errorFound = false;
		
		LOGGER.debug("Check for duplicate data sources");
		BsonArray data_sources = contentToCheck.getDocument("INTERNAL_STRUCTURE").getArray("Data_Sources");
		Set<String> ds = new TreeSet<String>();
		for (BsonValue data_source : data_sources) {
			String id = data_source.asDocument().getString("id").getValue();
			if (!ds.add(id)) {
				warning = "Duplicate INTERNAL_STRUCTURE.Data_Sources with id " + id;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}
		
		LOGGER.debug("Check for duplicate api methods...");
		LOGGER.debug("...and for invalid data sources inside api methods");
		BsonDocument pathsdoc = contentToCheck.getDocument("EXPOSED_API").getDocument("paths");
		Iterator<String> paths = pathsdoc.keySet().iterator();
		Set<String> ms = new TreeSet<String>();
		
		while (paths.hasNext()) {
			String pathURI = paths.next();
			BsonDocument path = pathsdoc.getDocument(pathURI);
			Iterator<String> ops = path.keySet().iterator();
			while (ops.hasNext()) {
				String opname = ops.next();
				BsonDocument method = path.getDocument(opname);
				
				String id = method.getString("operationId").getValue();
				if (!ms.add(id)) {
					warning = "Duplicate EXPOSED_API.Paths."+pathURI+"."+opname+".operationId : " + id;
					LOGGER.debug(warning);
					context.addWarning(warning);
					errorFound = true;
				}
			}
		}
		
		LOGGER.debug("Check for invalid method_ids of tags");
		BsonArray tags = contentToCheck.getDocument("INTERNAL_STRUCTURE").getDocument("Overview").getArray("tags");
		Set<String> ts = new TreeSet<String>();
		for (BsonValue tag : tags ) {
			String name = tag.asDocument().getString("method_id").getValue();
			if (!ms.contains(name)) {
				warning = "Method " + name + " in INTERNAL_STRUCTURE.Overview.tags is not declared in EXPOSED_API" ;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
			if (!ts.add(name)) {
				warning = "Duplicate INTERNAL_STRUCTURE.Overview.tags with method_id " + name;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}
		
		LOGGER.debug("Check for invalid method_ids of Methods_Input...");
		LOGGER.debug("...and for invalid data sources inside Methods_Input");
		if (contentToCheck.getDocument("INTERNAL_STRUCTURE").containsKey("Methods_Input")) {
			BsonArray meths = contentToCheck.getDocument("INTERNAL_STRUCTURE").getDocument("Methods_Input").getArray("Methods");
			Set<String> methods = new TreeSet<String>();
			for (BsonValue meth : meths ) {
				String name = meth.asDocument().getString("method_id").getValue();
				if (!ms.contains(name)) {
					warning = "Method " + name + " in INTERNAL_STRUCTURE.Methods_Input.Methods is not declared in EXPOSED_API" ;
					LOGGER.debug(warning);
					context.addWarning(warning);
					errorFound = true;
				}
				if (!methods.add(name)) {
					warning = "Duplicate INTERNAL_STRUCTURE.Methods_Input.Methods with method_id " + name;
					LOGGER.debug(warning);
					context.addWarning(warning);
					errorFound = true;
				}
				if (meth.asDocument().containsKey("dataSources")) {
					BsonArray sources = meth.asDocument().getArray("dataSources");
                    for (BsonValue src : sources ) { 
                        String sourceid = src.asDocument().getString("dataSource_id").getValue();
                        if (!ds.contains(sourceid)) {
							warning = "Data source " + sourceid + " in INTERNAL_STRUCTURE.Methods_Input.Methods.dataSources is not declared in INTERNAL_STRUCTURE.Data_Sources";
							LOGGER.debug(warning);
							context.addWarning(warning);
							errorFound = true;
						}                
                    }
				}
			}
		}
		
		LOGGER.debug("Check for invalid method_ids of Testing_Output_Data");
                if (contentToCheck.getDocument("INTERNAL_STRUCTURE").containsKey("Testing_Output_Data")) {
		BsonArray output = contentToCheck.getDocument("INTERNAL_STRUCTURE").getArray("Testing_Output_Data");
		Set<String> tod = new TreeSet<String>();
		for (BsonValue o : output ) {
			String name = o.asDocument().getString("method_id").getValue();
			if (!ms.contains(name)) {
				warning = "Method " + name + " in INTERNAL_STRUCTURE.Testing_Output_Data is not declared in EXPOSED_API";
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
			if (!tod.add(name)) {
				warning = "Duplicate INTERNAL_STRUCTURE.Testing_Output_Data with method_id " + name;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}
                }
                
		LOGGER.debug("Check for invalid ids of DATA_MANAGEMENT.methods");
		BsonArray dm = contentToCheck.getArray("DATA_MANAGEMENT");
		Set<String> dm_ms = new TreeSet<String>();
		for (BsonValue dm_method : dm) {
			String name = dm_method.asDocument().getString("method_id").getValue();
			if (!ms.contains(name)) {
				warning = "Method  " + name + " in DATA_MANAGEMENT is not declared in EXPOSED_API" ;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
			if (!dm_ms.add(name)) {
				warning = "Duplicate DATA_MANAGEMENT.methods with method_id " + name;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}

		
		return !errorFound;
	}

    @Override
    public PHASE getPhase(RequestContext context) {
    	if (context.getMethod() == METHOD.PATCH
                || CheckersUtils
                        .doesRequestUsesDotNotation(context.getContent())
                || CheckersUtils
                        .doesRequestUsesUpdateOperators(context.getContent())) {
    		return PHASE.AFTER_WRITE;
        } else {
            return PHASE.BEFORE_WRITE;
        }
    }

    @Override
    public boolean doesSupportRequests(RequestContext context) {
    	return !(CheckersUtils.isBulkRequest(context)
                && getPhase(context) == PHASE.AFTER_WRITE);
    }
}