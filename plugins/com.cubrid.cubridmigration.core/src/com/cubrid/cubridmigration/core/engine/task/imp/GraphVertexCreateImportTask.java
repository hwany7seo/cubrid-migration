package com.cubrid.cubridmigration.core.engine.task.imp;

import com.cubrid.cubridmigration.core.engine.task.ImportTask;
import com.cubrid.cubridmigration.graph.dbobj.Vertex;

public class GraphVertexCreateImportTask extends ImportTask {

	private final Vertex vertex;
	
	public GraphVertexCreateImportTask(Vertex v) {
		this.vertex = v;
	}
	
	@Override
	protected void executeImport() {
		importer.createVertex(vertex);
	}
}
