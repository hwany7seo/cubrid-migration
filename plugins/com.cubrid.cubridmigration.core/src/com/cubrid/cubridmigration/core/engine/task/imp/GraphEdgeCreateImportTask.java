package com.cubrid.cubridmigration.core.engine.task.imp;

import com.cubrid.cubridmigration.core.engine.task.ImportTask;
import com.cubrid.cubridmigration.graph.dbobj.Edge;

public class GraphEdgeCreateImportTask extends ImportTask{

	private final Edge edge;
	
	public GraphEdgeCreateImportTask(Edge e) {
		this.edge = e;
	}
	
	@Override
	protected void executeImport() {
		importer.createEdge(edge);
	}

}
