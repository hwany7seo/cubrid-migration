/*
 * Copyright (C) 2008 Search Solution Corporation.
 * Copyright (C) 2016 CUBRID Corporation.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors
 *   may be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */
package com.cubrid.cubridmigration.ui.preference;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.widgets.Table;

import com.cubrid.cubridmigration.core.dbobject.Column;

public class GraphDataTypeComboBoxCellEditor extends ComboBoxCellEditor {
	public GraphDataTypeComboBoxCellEditor(Table table, String[] tableData) {
		super(table, tableData);
	}
	
	@Override
	public void activate(ColumnViewerEditorActivationEvent activationEvent) {
		ViewerCell cell = (ViewerCell) activationEvent.getSource();
		
		Object element = cell.getElement();
		
		if (element instanceof Column){
			Column gdbCol = (Column) element;
			
			List<String> typeList = getTypeList(gdbCol.getDataType());
			
			setItems(typeList.toArray(new String[0]));
		}
	}
	
	public List<String> getTypeList(String type) {
		ArrayList<String> types = new ArrayList<String>();
		
		if (type.equals("not support")) {
			types.add(type);
			
			return types;
		} else if (type.equals("integer")) {
			types.add("integer");
			types.add("string");
			
			return types;
			
		} else if (type.equals("date")) {
			types.add("date");
			types.add("string");
			
			return types;
			
		} else if (type.equals("datetime")) {
			types.add("datetime");
			types.add("string");
			
			return types;
			
		} else if (type.equals("string")) {
			types.add("string");
			return types;
			
		} else {
			types.add(type);
			types.add("string");
			
			return types;
		}
	}
}
