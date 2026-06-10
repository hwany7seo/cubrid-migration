package com.cubrid.cubridmigration.ui.wizard.graph.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IGraphEntityRelationshipContentProvider;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphItem;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.interfaces.LayoutContext;
import org.slf4j.Logger;

import com.cubrid.common.log.LogUtil;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.Column;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.graph.dbobj.Edge;
import com.cubrid.cubridmigration.graph.dbobj.GraphDictionary;
import com.cubrid.cubridmigration.graph.dbobj.Vertex;
import com.cubrid.cubridmigration.graph.dbobj.Work;
import com.cubrid.cubridmigration.graph.dbobj.WorkBuffer;
import com.cubrid.cubridmigration.graph.dbobj.WorkController;
import com.cubrid.cubridmigration.ui.MigrationUIPlugin;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.cubridmigration.ui.preference.GraphDataTypeComboBoxCellEditor;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;
import com.cubrid.cubridmigration.ui.wizard.dialog.GraphDateTimeFilterDialog;
import com.cubrid.cubridmigration.ui.wizard.dialog.GraphEdgeSettingDialog;
import com.cubrid.cubridmigration.ui.wizard.dialog.GraphRenamingDialog;
import com.cubrid.cubridmigration.ui.wizard.page.MigrationWizardPage;
import com.cubrid.cubridmigration.ui.wizard.page.ObjectMappingPage;

//GDB override ObjectMappingPage. GraphMappingPage seems to have a similar structure to ObjectMappingPage

enum workTypeEnum {
	WT_DELETE,
	WT_CREATE,
	WT_RENAME
}

public class GraphMappingPage extends MigrationWizardPage {
    private static final Logger LOG = LogUtil.getLogger(GraphMappingPage.class);
	/** ELK layered layout algorithm id (see org.eclipse.elk.alg.layered) */
	private static final String ELK_LAYERED_ALGORITHM = "org.eclipse.elk.layered";
	private static final RecursiveGraphLayoutEngine ELK_LAYOUT_ENGINE = new RecursiveGraphLayoutEngine();

	private boolean ctrlKeyMode = false;
	
    public static final int CTRL_KEYCODE = 0x40000;
    public static final int Z_KEYCODE = 0x7a;
    public static final int Y_KEYCODE = 0x79;

	private static final LayoutAlgorithm NO_OP_ZEST_LAYOUT = new LayoutAlgorithm() {
		@Override
		public void setLayoutContext(LayoutContext context) {
			// no-op
		}

		@Override
		public void applyLayout(boolean clean) {
			// no-op: positions applied after refresh via ELK
		}
	};

	public static final Image CHECK_IMAGE = MigrationUIPlugin.getImage("icon/checked.gif");
	public static final Image UNCHECK_IMAGE = MigrationUIPlugin.getImage("icon/unchecked.gif");
	
	private MigrationConfiguration mConfig;
	
	private GraphDictionary gdbDict;
	
	private GraphViewer graphViewer;
	private Graph graph;
	
	private String highlightNodeName = "";
	
	private Vertex startVertex;
	private Vertex endVertex;
	
	private List<Object> selectedObjectList = null;
	private Object selectedObject;
	
	private TableViewer gdbTable;
	private TableViewer rdbTable;
	
	private Menu popupMenu;
	
	interface PopupMenuType {
	    int START_VERTEX = 0;
	    int END_VERTEX = 1;
	    int CANCEL = 2;
	    int SEPARAYOR_1 = 3;
	    int CHANGE_NAME = 4;
	    int DELETE_EDGE = 5;
	    int SEPARATOR_2 = 6;
	    int UNDO = 7;
	    int REDO = 8;
	    int SEPARATOR_3 = 9;
	    int DATATIME_FILTER = 10;
	    int UNSET_FILTER = 11;
	}
	
	private Button twoWayBtn;
	
	Text dateTimeText;
	
	private GraphDataTypeComboBoxCellEditor comboEditor;
	
	private String[] columnProperties = {Messages.colPropertyName, Messages.msgGDBTypes};
	private String[] targetTypeList = {"integer", "string", "date", "datetime"};
	
	private WorkBuffer workBuffer = new WorkBuffer();
	private WorkController workCtrl = new WorkController();
	
	public GraphMappingPage(String pageName) {
		super(pageName);
		//GDB mapping page constructor
	}

	@Override
	public void createControl(Composite parent) {
		//GDB mapping page create control
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		SashForm sash = new SashForm(container, SWT.HORIZONTAL);
		sash.setLayout(new FillLayout());
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createGraphView(sash);
		createTableView(sash);
		
		sash.setWeights(new int[] {1, 1});
		sash.setSashWidth(10);
		
		setControl(container);
	}
	
	//GDB this will show right side widget. show vertex list and edge list
	public void createGraphView(SashForm parent) {
		
		Group groupContainer1 = new Group(parent, SWT.NONE);
		groupContainer1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		groupContainer1.setLayout(new GridLayout());
		groupContainer1.setText(Messages.msgGraph);
		
		TabFolder tabFolder = new TabFolder(groupContainer1, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createGraph(tabFolder);
		
		TabItem folder1 = new TabItem(tabFolder, SWT.NONE);
		folder1.setText(Messages.msgVertex);
		folder1.setControl(graphViewer.getControl());
	}
	
	public void createGraph(Composite parent) {
		setPopupMenu(parent);
	
		graphViewer = new GraphViewer(parent, SWT.BORDER);	
		graphViewer.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);
		graphViewer.setLayoutAlgorithm(NO_OP_ZEST_LAYOUT, false);
		
		graphViewer.getGraphControl().setMenu(popupMenu);
		
		graphViewer.setContentProvider(new IGraphEntityRelationshipContentProvider() {
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
			
			@Override
			public void dispose() {}
			
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof List) {
					@SuppressWarnings("unchecked")
					List<Vertex> vertexList = (ArrayList<Vertex>) inputElement;
				
					return vertexList.toArray();
				} else {
					return new Object[0];
				}
			}
			
			@Override
			public Object[] getRelationships(Object source, Object dest) {
				Vertex startVertex = (Vertex) source;
				Vertex endVertex = (Vertex) dest;
				
				ArrayList<Edge> allEdgeList = (ArrayList<Edge>) gdbDict.getMigratedEdgeList();
				ArrayList<Edge> currentEdgeList = new ArrayList<Edge>();
				
				for (Edge edge : allEdgeList) {
					if (edge.getStartVertexName().equals(startVertex.getVertexLabel())
							&& edge.getEndVertexName().equals(endVertex.getVertexLabel())) {
						currentEdgeList.add(edge);
					}
				}
				
				return currentEdgeList.toArray();
			}
		});
				
		graphViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Vertex) {
					Vertex vertex = (Vertex) element;
					return vertex.getVertexLabel();
					
				} if (element instanceof Edge) {
					Edge edge = (Edge) element;
					
					return edge.getEdgeLabel();
				}
				
				return null;
			}
		});
		
		graphViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
			    System.out.println("graphViewer addSelectionListener e : " + event.getSelection().toString());
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (selection != null) {
				    selectedObjectList = selection.toList();
				} else {
				    selectedObjectList = null;
				}
//				changeColumnData(selection.getFirstElement());
				
//				if(selection.getFirstElement() instanceof Vertex) {
//					selectedObject = (Vertex) selection.getFirstElement();
//					
////					for (GraphNode gNode : (ArrayList<GraphNode>) graphViewer.getGraphControl().getNodes()) {
////						if (gNode.getText().equalsIgnoreCase(selectedVertex.getVertexLabel())) {
////							highlightNodeName = gNode.getText();
////						}
////					}
//					
//					menuHandler();
//					deleteMenuHandler(true);
//					redoUndoHandler();
////					System.out.println("select object: " + ((Vertex) selectedObject).getVertexLabel());
//				}
//				
//				if (selection.getFirstElement() instanceof Edge) {
//					selectedObject = (Edge) selection.getFirstElement();
//					
//					menuHandler();
//					deleteMenuHandler(false);
//					redoUndoHandler();
////					System.out.println("selected object: " + ((Edge) selectedObject).getEdgeLabel());
//				}
//				
//				dateTimeTextHandler();
				
//				refreshGraph();
			}
		});
		
		graph = graphViewer.getGraphControl();
		
		graph.addSelectionListener(new SelectionListener() {
			
			@Override
			@SuppressWarnings("unchecked")
			public void widgetSelected(SelectionEvent e) {
			    if (e.item instanceof GraphNode) {
			        selectedObject = ((GraphNode)e.item).getData();
			    } else if (e.item instanceof GraphConnection) {
			        selectedObject = ((GraphConnection)e.item).getData();
			    } else {
			        selectedObject = null;
			        return;
			    }
			    
			    changeColumnData(selectedObject);
			    
				ArrayList<GraphItem> selectList = (ArrayList<GraphItem>) graph.getSelection();
				for (GraphItem selection : selectList) {
					if (selection instanceof GraphConnection) {
						GraphConnection conntion = (GraphConnection) selection;
						//conntion.unhighlight();
					} else if (selection instanceof GraphNode) {
						GraphNode gNode = (GraphNode) selection;
						if (gNode.getText().equals(highlightNodeName)) {
							setHighlight(gNode);
						}
					}
				}
				
				if (selectedObjectList != null) {
    				for (Object obj : selectedObjectList) {
    				    if (obj instanceof Edge) {
    				        System.out.println("edge list in e : " + ((Edge)obj).getName());
    				    }
    				}
				}
			    
			    if(selectedObject instanceof Vertex) {
//                    for (GraphNode gNode : (ArrayList<GraphNode>) graphViewer.getGraphControl().getNodes()) {
//                        if (gNode.getText().equalsIgnoreCase(selectedVertex.getVertexLabel())) {
//                            highlightNodeName = gNode.getText();
//                        }
//                    }
                  
			        menuHandler();
			        deleteMenuHandler(true);
			        redoUndoHandler();
//                    System.out.println("select object: " + ((Vertex) selectedObject).getVertexLabel());
			    }
              
			    if (selectedObject instanceof Edge) {
			        menuHandler();
			        deleteMenuHandler(false);
			        redoUndoHandler();
//                    System.out.println("selected object: " + ((Edge) selectedObject).getEdgeLabel());
			    }
				
				dateTimeTextHandler();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		graph.addKeyListener(new KeyListener() {
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.keyCode == CTRL_KEYCODE) {
                    ctrlKeyMode = false;
                }
            }
            
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == CTRL_KEYCODE) {
                    ctrlKeyMode = true;
                }
                
                if (ctrlKeyMode) {
                    if (e.keyCode == Z_KEYCODE) {
                        executeUndo(workBuffer.undo());
                        redoUndoHandler();
                    } else if (e.keyCode == Y_KEYCODE) {
                        executeRedo(workBuffer.redo());
                        redoUndoHandler();
                    }
                }

                if ((selectedObjectList != null && selectedObjectList.size() > 0) && e.keyCode == SWT.DEL) {
                    for (Object obj : selectedObjectList) {
                        if (obj instanceof Edge) {
                           deleteEdgeInGraph((Edge)obj);
                           redoUndoHandler();
                        }
                    }
                }
            }
        });
	}
	
	@SuppressWarnings("unused")
	public void setPopupMenu(Composite parent) {
		popupMenu = new Menu(parent);
		
		//TODO setting message
		MenuItem item1 = new MenuItem(popupMenu, SWT.POP_UP);
		item1.setText(Messages.msgMenuStartVertex);
		
		item1.addSelectionListener(new SelectionListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (startVertex != null) {
					startVertex = null;
				}
				
				startVertex = (Vertex)selectedObject;
				
				for (GraphNode gNode : (ArrayList<GraphNode>) graphViewer.getGraphControl().getNodes()) {
					if (gNode.getText().equalsIgnoreCase(startVertex.getVertexLabel())) {
						highlightNodeName = gNode.getText();
						
						setHighlight(gNode);
						
						break;
					}
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		MenuItem item2 = new MenuItem(popupMenu, SWT.POP_UP);
		item2.setText(Messages.msgMenuEndVertex);
		
		item2.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				if (endVertex != null) {
					endVertex = null;
				}
				
				endVertex = (Vertex)selectedObject;
				
				GraphEdgeSettingDialog edgeSettingDialog = new GraphEdgeSettingDialog(getShell(), 
						mConfig, gdbDict, startVertex, endVertex, workBuffer, workCtrl);
				edgeSettingDialog.open();
				
				redoUndoHandler();
			}
		});
		
		MenuItem item3 = new MenuItem(popupMenu, SWT.POP_UP);
		item3.setText(Messages.btnCancel);
		
		item3.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				if (startVertex != null) {
					startVertex = null;
				}
				
				if (endVertex != null) {
					endVertex = null;
				}
			}
		});
		
		MenuItem separator = new MenuItem(popupMenu, SWT.SEPARATOR);
		
		MenuItem changeName = new MenuItem(popupMenu, SWT.POP_UP);
		changeName.setText(Messages.msgMenuChangeName);
		
		changeName.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				String originalName;
				 
				if (selectedObject instanceof Vertex) {
					originalName = ((Vertex)selectedObject).getName();
				} else {
					originalName = ((Edge)selectedObject).getName();
				}
				
				GraphRenamingDialog renameDialog = new GraphRenamingDialog(getShell(), gdbDict, selectedObject);
				renameDialog.open();
				
				GraphDictionary gdbDict = mConfig.getGraphDictionary();
				List<Edge> migratedEdgeList = gdbDict.getMigratedEdgeList();
				List<Vertex> migratedVertexList = gdbDict.getMigratedVertexList();
				
				if (selectedObject instanceof Vertex) {
				    Vertex SelectedVertex = (Vertex)selectedObject;
					for (Vertex vertex : migratedVertexList) {
						if (vertex.getName().equals(SelectedVertex.getName())) {
							workBuffer.addWork(workCtrl.createWork(workTypeEnum.WT_RENAME.ordinal(), vertex, originalName));
							renameVertexFromGraph(vertex, vertex.getName());
							break;
						}
					}
				} else {
				    Edge SelectedEdge = (Edge)selectedObject;
					for (Edge edge : migratedEdgeList) {
						if (edge.getName().equals(SelectedEdge.getName())) {
							workBuffer.addWork(workCtrl.createWork(workTypeEnum.WT_RENAME.ordinal(), edge, originalName));
							renameEdgeFromGraph(edge, SelectedEdge.getName());
							break;
						}
					}
				}
				
				redoUndoHandler();
			}
		});
		
		MenuItem deleteEdge = new MenuItem(popupMenu, SWT.POP_UP);
		deleteEdge.setText(Messages.msgMenuDeleteEdge);
		
		deleteEdge.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteEdgeInGraph(((Edge)selectedObject));
			}

		});
		
		MenuItem separator2 = new MenuItem(popupMenu, SWT.SEPARATOR);
		
		MenuItem undo = new MenuItem(popupMenu, SWT.POP_UP);
		undo.setText(Messages.msgMenuUndo);
		
		undo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				executeUndo(workBuffer.undo());
				redoUndoHandler();
			}
		});
		
		MenuItem redo = new MenuItem(popupMenu, SWT.POP_UP);
		redo.setText(Messages.msgMenuRedo);
		
		redo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				executeRedo(workBuffer.redo());
				redoUndoHandler();
			}
		});
		
		MenuItem separator3 = new MenuItem(popupMenu, SWT.SEPARATOR);
		
		MenuItem dateTimeFilter = new MenuItem(popupMenu, SWT.POP_UP);
		dateTimeFilter.setText("set datetime filter");
		
		dateTimeFilter.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				GraphDateTimeFilterDialog dateTimeFilter = new GraphDateTimeFilterDialog(getShell(), selectedObject);
				dateTimeFilter.open();
				dateTimeTextHandler();
				filterHandler();
			}
			
		});
		
		MenuItem unsetFilter = new MenuItem(popupMenu, SWT.POP_UP);
		unsetFilter.setText("unset filter");
		
		unsetFilter.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (selectedObject instanceof Vertex) {
					((Vertex) selectedObject).setHasDateTimeFilter(false);
				} else if (selectedObject instanceof Edge) {
					((Edge) selectedObject).setHasDateTimeFilter(false);
				}
				
				dateTimeTextHandler();
				filterHandler();
			}
		});
		
		item1.setEnabled(true);
		item2.setEnabled(false);
		item3.setEnabled(false);

		changeName.setEnabled(true);
		deleteEdge.setEnabled(true);
		redo.setEnabled(false);
		undo.setEnabled(false);
		
		unsetFilter.setEnabled(false);
	}
	
	public void dateTimeTextHandler() {
		List<Column> columnList;
		
		String fromDateString = null;
		String toDateString = null;
		
		StringBuffer dateTimeTextBuffer = new StringBuffer();
		
		if (selectedObject instanceof Vertex) {
			columnList = ((Vertex) selectedObject).getColumnList();
		} else {
			columnList = ((Edge) selectedObject).getColumnList();
		}
		
		for (Column col : columnList) {
			if (col.isConditionColumn()) {
				fromDateString = col.getFromDate();
				toDateString = col.getToDate();
				break;
			}
		}
		
		if (!(fromDateString == null || fromDateString.isEmpty()) && !(toDateString == null || toDateString.isEmpty())) {
			dateTimeTextBuffer.append("from: " + fromDateString + "\n");
			dateTimeTextBuffer.append("to: " + toDateString);
		} else {
			dateTimeTextBuffer.append("no filter applied");
		}
		
		dateTimeText.setText(dateTimeTextBuffer.toString());
	}
	
	public void setHighlight(GraphNode node) {
		Display dis = graphViewer.getGraphControl().getDisplay();
		
		node.unhighlight();
		node.setHighlightColor(new Color(dis, 153, 204, 102));
		node.setNodeStyle(GraphNode.HIGHLIGHT_ON);
		node.highlight();
	}
	
	public void deleteMenuHandler(boolean isVertex) {
		MenuItem[] items = popupMenu.getItems();
		
		if (isVertex) {
			items[PopupMenuType.DELETE_EDGE].setEnabled(false);
		} else {
			items[PopupMenuType.DELETE_EDGE].setEnabled(true);
		}
	}
	
	public void menuHandler() {
		MenuItem[] items = popupMenu.getItems();
		MenuItem start_vertex = items[PopupMenuType.START_VERTEX];
		MenuItem end_vertex = items[PopupMenuType.END_VERTEX];
		MenuItem cancle = items[PopupMenuType.CANCEL];
		
		if (selectedObject != null) {
		    start_vertex.setEnabled(true);
		    end_vertex.setEnabled(false);
		    cancle.setEnabled(false);
			
		} else {
		    start_vertex.setEnabled(false);
		    end_vertex.setEnabled(false);
		    cancle.setEnabled(false);
		}
		
		if (startVertex != null) {
		    start_vertex.setEnabled(true);
		    end_vertex.setEnabled(true);
		    cancle.setEnabled(true);
		}
		
		if (endVertex != null) {
			//do nothing?
		}
	}
	
	public void redoUndoHandler() {
	   MenuItem undo = popupMenu.getItem(PopupMenuType.UNDO);
	   MenuItem redo = popupMenu.getItem(PopupMenuType.REDO);
	    
		if (workBuffer.isUndoListEmpty()) {
		    undo.setEnabled(false);
		} else {
		    undo.setEnabled(true);
		}
		
		if (workBuffer.isRedoListEmpty()) {
			redo.setEnabled(false);
		} else {
			redo.setEnabled(true);
		}
	}
	
	public void filterHandler() {
		boolean hasDateTimeFilter = false;
		MenuItem unsetFilter = popupMenu.getItem(PopupMenuType.UNSET_FILTER);
		
		if (selectedObject instanceof Vertex) {
			hasDateTimeFilter = true;
		} else if (selectedObject instanceof Edge) {
			hasDateTimeFilter = ((Edge) selectedObject).hasDateTimeFilter();
		}
		
		if (hasDateTimeFilter) {
		    unsetFilter.setEnabled(true);
		} else {
		    unsetFilter.setEnabled(false);
		}
	}
	
	public void changeColumnData(Object data) {
		if (data == null) {
			return;
		}
		
		List<Column> columnList = null;
		List<Column> gdbColumnList = null;
		
		if (data instanceof Vertex) {
			Vertex vertex = (Vertex) data;
			columnList = vertex.getColumnList();
			gdbColumnList = vertex.getGraphColumnList();
			
		} else if (data instanceof EntityConnectionData) {
			EntityConnectionData connData = (EntityConnectionData) data;
			
		} else if (data instanceof Edge) {
			Edge edge = (Edge) data;
			columnList = edge.getColumnList();
			gdbColumnList = edge.getGraphColumnList();
		}
		
		gdbTable.setInput(gdbColumnList);
		rdbTable.setInput(columnList);
		
		gdbTable.refresh();
		rdbTable.refresh();
		
	}

	/** Refreshes graph figures and reapplies ELK layered positions on Zest nodes. */
	private void refreshGraph() {
		graphViewer.refresh();
		applyElkLayout();
		clearData();
	}
	
	private void clearData() {
	    workBuffer.clearAll();
	    redoUndoHandler();
	    startVertex = null;
	    endVertex = null;
	    menuHandler();
	}
	
	/**
	 * Builds an ELK graph from current vertices/edges, runs layered layout, and copies x/y to {@link GraphNode}.
	 */
	private void applyElkLayout() {
		if (graphViewer == null || graph == null || graphViewer.getControl().isDisposed()) {
			return;
		}
		Object input = graphViewer.getInput();
		if (!(input instanceof List)) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<Vertex> vertexList = (List<Vertex>) input;
		if (vertexList.isEmpty()) {
			return;
		}

		ElkNode root = ElkGraphUtil.createGraph();
//		root.setIdentifier("graphMappingRoot");

		Map<String, ElkNode> labelToElk = new HashMap<>();

		GC gc = new GC(graphViewer.getControl());
		try {
			gc.setFont(graphViewer.getControl().getFont());
			for (Vertex v : vertexList) {
				ElkNode n = ElkGraphUtil.createNode(root);
				String label = v.getVertexLabel();
				if (label == null) {
					label = "";
				}
				n.setIdentifier(label);
				org.eclipse.swt.graphics.Point extent = gc.textExtent(label);
				double w = Math.max(80, extent.x + 24);
				double h = Math.max(40, extent.y + 16);
				n.setDimensions(w, h);
				labelToElk.put(label, n);
			}
		} finally {
			gc.dispose();
		}

		if (gdbDict != null) {
			List<Edge> edges = gdbDict.getMigratedEdgeList();
			if (edges != null) {
				for (Edge edge : edges) {
					ElkNode src = labelToElk.get(edge.getStartVertexName());
					ElkNode tgt = labelToElk.get(edge.getEndVertexName());
					if (src != null && tgt != null) {
						ElkGraphUtil.createSimpleEdge(src, tgt);
					}
				}
			}
		}

		root.setProperty(CoreOptions.ALGORITHM, ELK_LAYERED_ALGORITHM);
		root.setProperty(CoreOptions.DIRECTION, Direction.DOWN);
		root.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, 64.0);
		root.setProperty(CoreOptions.PADDING, new ElkPadding(4));

		ELK_LAYOUT_ENGINE.layout(root, new BasicProgressMonitor());

		Map<String, double[]> layoutXY = new HashMap<>();
		for (Vertex v : vertexList) {
			String lbl = v.getVertexLabel();
			if (lbl == null) {
				lbl = "";
			}
			ElkNode en = labelToElk.get(lbl);
			if (en != null) {
				layoutXY.put(lbl, new double[] { en.getX(), en.getY() });
			}
		}

		List<Edge> edgeListForIsolate = gdbDict != null ? gdbDict.getMigratedEdgeList() : null;
		adjustPositionsForIsolatedVertices(layoutXY, labelToElk, vertexList, edgeListForIsolate);
		normalizeLayoutToTopLeft(layoutXY);

		for (Object item : graph.getNodes()) {
			if (!(item instanceof GraphNode)) {
				continue;
			}
			GraphNode gNode = (GraphNode) item;
			Object data = gNode.getData();
			if (!(data instanceof Vertex)) {
				continue;
			}
			Vertex v = (Vertex) data;
			String vl = v.getVertexLabel();
			if (vl == null) {
				vl = "";
			}
			double[] xy = layoutXY.get(vl);
			if (xy != null) {
				gNode.setLocation(xy[0], xy[1]);
			}
		}
	}

	private static void adjustPositionsForIsolatedVertices(Map<String, double[]> layoutXY, Map<String, ElkNode> labelToElk,
			List<Vertex> vertexList, List<Edge> edges) {
		Set<String> endpointLabels = new HashSet<>();
		if (edges != null) {
			for (Edge edge : edges) {
				String s = edge.getStartVertexName();
				String t = edge.getEndVertexName();
				if (s != null) {
					endpointLabels.add(s);
				}
				if (t != null) {
					endpointLabels.add(t);
				}
			}
		}

		boolean hasConnectedLayout = false;
		double connMinY = Double.POSITIVE_INFINITY;
		double connMaxX = Double.NEGATIVE_INFINITY;

		for (Vertex v : vertexList) {
			String lbl = v.getVertexLabel();
			if (lbl == null) {
				lbl = "";
			}
			if (!endpointLabels.contains(lbl)) {
				continue;
			}
			ElkNode en = labelToElk.get(lbl);
			if (en == null) {
				continue;
			}
			hasConnectedLayout = true;
			double x = en.getX();
			double y = en.getY();
			double w = en.getWidth();
			connMinY = Math.min(connMinY, y);
			connMaxX = Math.max(connMaxX, x + w);
		}

		final double isoGapX = 48;
		final double isoGapY = 16;
		final double isoColW = 140;
		final double isoRowH = 56;
		final int isoCols = 3;

		int isoIdx = 0;
		for (Vertex v : vertexList) {
			String lbl = v.getVertexLabel();
			if (lbl == null) {
				lbl = "";
			}
			if (endpointLabels.contains(lbl)) {
				continue;
			}
			if (!layoutXY.containsKey(lbl)) {
				continue;
			}
			double px;
			double py;
			if (hasConnectedLayout) {
				int col = isoIdx % isoCols;
				int row = isoIdx / isoCols;
				px = connMaxX + isoGapX + col * isoColW;
				py = connMinY + row * (isoRowH + isoGapY);
			} else {
				int col = isoIdx % isoCols;
				int row = isoIdx / isoCols;
				px = col * isoColW;
				py = row * (isoRowH + isoGapY);
			}
			layoutXY.put(lbl, new double[] { px, py });
			isoIdx++;
		}
	}

	private static void normalizeLayoutToTopLeft(Map<String, double[]> layoutXY) {
		if (layoutXY.isEmpty()) {
			return;
		}
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		for (double[] xy : layoutXY.values()) {
			minX = Math.min(minX, xy[0]);
			minY = Math.min(minY, xy[1]);
		}
		if (minX == Double.POSITIVE_INFINITY) {
			return;
		}
		final double margin = 8;
		double dx = margin - minX;
		double dy = margin - minY;
		for (Map.Entry<String, double[]> e : layoutXY.entrySet()) {
			double[] xy = e.getValue();
			e.setValue(new double[] { xy[0] + dx, xy[1] + dy });
		}
	}
	
	public void createTableView(Composite parent) {
		SashForm verticalSash = new SashForm(parent, SWT.VERTICAL);
		verticalSash.setLayout(new GridLayout(4, false));
		verticalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		SashForm sashContainer = new SashForm(verticalSash, SWT.HORIZONTAL);
		sashContainer.setLayout(new FillLayout());
		sashContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SashForm textContainer = new SashForm(verticalSash, SWT.BORDER);
		dateTimeText = new Text(textContainer, SWT.MULTI | SWT.READ_ONLY);
		
		SashForm btnContiainer = new SashForm(verticalSash, SWT.VERTICAL);
		btnContiainer.setLayout(new FillLayout());
		btnContiainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		twoWayBtn = new Button(btnContiainer, SWT.CHECK);
		twoWayBtn.setText(Messages.twowayEdge);
		
		verticalSash.setWeights(new int[] {15, 5, 1});
		verticalSash.setSashWidth(1);
		
		Group leftSash = new Group(sashContainer, SWT.NONE);
		leftSash.setLayout(new FillLayout());
		leftSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		leftSash.setText(Messages.msgRDBColumn);
		
		Group rightSash = new Group(sashContainer, SWT.NONE);
		rightSash.setLayout(new FillLayout());
		rightSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		rightSash.setText(Messages.msgGDBColumn);
		
		rdbTable = new TableViewer(leftSash, SWT.FULL_SELECTION);
		rdbTable.setContentProvider(new IStructuredContentProvider() {
			@Override
			@SuppressWarnings("unchecked")
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof ArrayList) {
					List<Column> columnList = (ArrayList<Column>) inputElement;
					
					return columnList.toArray();
				} else {
					return new Object[0];
				}
			}
						
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}		
			@Override
			public void dispose() {}
		});
		
		rdbTable.setLabelProvider(new ITableLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				Column column = (Column) element;
				
				switch (columnIndex) {
				case 0:
					return null;
				case 1:
					return column.getName();
				case 2:
					return column.getDataType();
					
				default :
					return null;
				}
			}
			
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				Column column = (Column) element;
				
				if (columnIndex == 0) {
					if (column.isSelected()) {
						return CHECK_IMAGE;
					} else {
						return UNCHECK_IMAGE;
					}
				}
				return null;
			}
			
			@Override
			public void removeListener(ILabelProviderListener listener) {}
			
			@Override
			public boolean isLabelProperty(Object element, String property) {return false;}
			
			@Override
			public void dispose() {}
			
			@Override
			public void addListener(ILabelProviderListener listener) {}
			
		});
		
		TableLayout tableLayout1 = new TableLayout();
		
		tableLayout1.addColumnData(new ColumnWeightData(10, true));
		tableLayout1.addColumnData(new ColumnWeightData(45, true));
		tableLayout1.addColumnData(new ColumnWeightData(45, true));
		
		rdbTable.getTable().setLayout(tableLayout1);
		rdbTable.getTable().setLinesVisible(true);
		rdbTable.getTable().setHeaderVisible(true);
		
		TableColumn rdbColumn1 = new TableColumn(rdbTable.getTable(), SWT.LEFT);
		TableColumn rdbColumn2 = new TableColumn(rdbTable.getTable(), SWT.LEFT);
		TableColumn rdbColumn3 = new TableColumn(rdbTable.getTable(), SWT.LEFT);
		
		rdbColumn2.setText(Messages.msgColumnName);
		rdbColumn3.setText(Messages.tabTitleDataType);
		
		rdbTable.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (selection.isEmpty()) {
					return;
				}
				changeColumnSelect(selection.getFirstElement());
			}
		});
		
		gdbTable = new TableViewer(rightSash, SWT.FULL_SELECTION);
		
		comboEditor = new GraphDataTypeComboBoxCellEditor(gdbTable.getTable(), targetTypeList);
		
		CellEditor[] editors = new CellEditor[] {
				null,
				comboEditor,
		};
		
		gdbTable.setCellEditors(editors);
		
		gdbTable.setCellModifier(new ICellModifier() {

			@Override
			public void modify(Object element, String property, Object value) {
				// TODO Auto-generated method stub
				TableItem tabItem = (TableItem) element;
				Column col = (Column) tabItem.getData();

				if (value instanceof Integer) {
					int intVal = (Integer) value;

					if (intVal == 1) {
						col.setDataType(targetTypeList[1]);
					}

					gdbTable.refresh();
				}
			}

			@Override
			public Object getValue(Object element, String property) {
				// TODO Auto-generated method stub
				if (property.equals(columnProperties[1])){
					return returnIndex(element);
				} else {
					return null;
				}
			}

			@Override
			public boolean canModify(Object element, String property) {
				// TODO Auto-generated method stub
				if (property.equals(Messages.msgGDBTypes)){
					return true;
				} else {
					return false;
				}
			}

			public int returnIndex(Object element) {
				if (element instanceof Column) {
					Column column = (Column) element;

					for (int i = 0; i < targetTypeList.length; i++) {
						if (column.getDataType().equals(targetTypeList[i])) {
							return i;
						}
					}
				}

				return 0;
			}
		});
		
		gdbTable.setContentProvider(new IStructuredContentProvider() {
			
			@Override
			@SuppressWarnings("unchecked")
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof ArrayList) {
					List<Column> columnList = (ArrayList<Column>) inputElement;
					
					return columnList.toArray();
				} else {
					return new Object[0];
				}
			}
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
			
			@Override
			public void dispose() {}
			
		});
		
		gdbTable.setLabelProvider(new ITableLabelProvider() {
			
			@Override
			public String getColumnText(Object element, int columnIndex) {
				// TODO Auto-generated method stub
				
				Column column = (Column) element;
				
				switch (columnIndex) {
				case 0:
					return column.getName();
				case 1:
				    return column.getDataType();
				default:
					return null;
				}
			}
			
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}
			
			@Override
			public void removeListener(ILabelProviderListener listener) {}
			
			@Override
			public boolean isLabelProperty(Object element, String property) {return false;}
			
			@Override
			public void dispose() {}
			
			@Override
			public void addListener(ILabelProviderListener listener) {}
			
		});
		
		TableLayout tableLayout2 = new TableLayout();
		
		tableLayout2.addColumnData(new ColumnWeightData(50, true));
		tableLayout2.addColumnData(new ColumnWeightData(50, true));
		
		gdbTable.getTable().setLayout(tableLayout2);
		gdbTable.getTable().setLinesVisible(true);
		gdbTable.getTable().setHeaderVisible(true);
		
		TableColumn gdbColumn1 = new TableColumn(gdbTable.getTable(), SWT.LEFT);
		TableColumn gdbColumn2 = new TableColumn(gdbTable.getTable(), SWT.LEFT);
		
		gdbTable.setColumnProperties(columnProperties);
		
		gdbColumn1.setText(columnProperties[0]);
		gdbColumn2.setText(columnProperties[1]);
	}

	public void changeColumnSelect(Object selectedColumn) {
		Column column = (Column) selectedColumn;
		if (column.isSelected()) {
			column.setSelected(false);
		} else {
			column.setSelected(true);
		}
		
		rdbTable.refresh();
	}
	
	public void showGraphData(List<Vertex> vertexList) {
		graphViewer.setInput(vertexList);
		refreshGraph();
	}
	
	//GDB GraphMappingPage -> afterShowCurrentPage
	protected void afterShowCurrentPage(PageChangedEvent event) {
		final MigrationWizard mw = getMigrationWizard();
		mConfig = mw.getMigrationConfig();
		setTitle(mw.getStepNoMsg(GraphMappingPage.this) + Messages.objectMapPageTitle);
		setDescription(Messages.objectMapPageDescription);
		
		setErrorMessage(null);
		
		try {
            gdbDict = mConfig.getGraphDictionary();
            gdbDict.printVertexAndEdge();
            showGraphData(gdbDict.getMigratedVertexList());
            
		} catch (Exception e) {
		    LOG.error(LogUtil.getExceptionString(e));
            throw e;
        } finally {
            isFirstVisible = false;
        }
	}
	
	protected void handlePageLeaving(PageChangingEvent event) {
		if (!isPageComplete()) {
			return;
		}
		
		if (twoWayBtn.getSelection()) {
			event.doit = setTwoWayEdge();
		}
		
		gdbDict.setVertexAndEdge();
	}
	
	private boolean setTwoWayEdge() {
		List<Edge> edgeList = gdbDict.getMigratedEdgeList();
		List<Edge> twoWayEdgeList = new ArrayList<Edge>();

		for (Edge edge : edgeList) {
			int edgeType = Edge.TWO_WAY_TYPE;

			if (edge.getEdgeType() == Edge.JOINTABLE_TYPE)
				edgeType = Edge.JOIN_TWO_WAY_TYPE;

			Edge copiedEdge = new Edge(edge);
			copiedEdge.setEdgeType(edgeType);
			copiedEdge.removeIDCol();

			copiedEdge.setStartVertexName(edge.getEndVertexName());
			copiedEdge.setEndVertexName(edge.getStartVertexName());
			copiedEdge.setStartVertex(edge.getEndVertex());
			copiedEdge.setEndVertex(edge.getStartVertex());

			// For regular two-way edges, reverse fkCol2RefMapping so the SQL direction flips.
			// For join two-way edges the FK columns belong to the join table (not vertices),
			// so keep the mapping as-is; getTargetInsertJoinEdge and setEdgeRecord2Statement
			// handle the FROM/TO and binding index swap internally.
			if (edgeType == Edge.TWO_WAY_TYPE) {
				copiedEdge.clearFKCol2Ref();
				for (String fkCol : edge.getFKColumnNames()) {
					String refCol = edge.getREFColumnNames(fkCol);
					copiedEdge.addFKCol2Ref(refCol, fkCol);
				}
			}

			if (mConfig.targetIsCSV()) {
				Column twoWayStartCol = new Column(":END_ID(" + copiedEdge.getEndVertexName() + ")");
				twoWayStartCol.setDataType("ID");

				Column twoWayEndCol = new Column(":START_ID(" + copiedEdge.getStartVertexName() + ")");
				twoWayEndCol.setDataType("ID");

				copiedEdge.addColumnAtFirst(twoWayEndCol);
				copiedEdge.addColumnAtFirst(twoWayStartCol);
			}

			twoWayEdgeList.add(copiedEdge);
		}

		gdbDict.addMigratedEdgeList(twoWayEdgeList);

		return true;
	}

	private void executeUndo(Work work) {
	    if (work == null) {
	        return;
	    }
	    
		workCtrl.setWork(work);
		
		if (work.getObject() instanceof Vertex) {
			if (work.getWorkType() == workTypeEnum.WT_DELETE.ordinal()) {
				gdbDict.addMigratedVertexList((Vertex) work.getObject());
				
			} else if (work.getWorkType() == workTypeEnum.WT_CREATE.ordinal()) {
				gdbDict.removeVertex(((Vertex) workCtrl.getObject()).getVertexLabel());
				
			} else if (work.getWorkType() == workTypeEnum.WT_RENAME.ordinal()) {
				String tempStr = work.getObject().getName();
				
				renameFromGraph(work.getObject(), work.getOriginalName());
				changeVertexName(work.getObject().getName(), work.getOriginalName());
				work.setOriginalName(tempStr);
			}
		} else {
		    Edge edge = (Edge)work.getObject();
			if (work.getWorkType() == workTypeEnum.WT_DELETE.ordinal()) {
				gdbDict.addMigratedEdgeList(edge);
				SetEdgeVisibleFromGraph(edge, true);
			} else if (work.getWorkType() == workTypeEnum.WT_CREATE.ordinal()) {
				gdbDict.removeEdge(edge.getEdgeLabel());
				SetEdgeVisibleFromGraph(edge, false);
			} else if (work.getWorkType() == workTypeEnum.WT_RENAME.ordinal()) {
				String tempStr = work.getObject().getName();
				
				renameFromGraph(work.getObject(), work.getOriginalName());
				changeEdgeName(work.getObject().getName(), work.getOriginalName());
				work.setOriginalName(tempStr);
			}
		}
	}
	
	private void executeRedo(Work work) {
	    if (work == null) {
            return;
        }
	    
		workCtrl.setWork(work);
		
		if (work.getObject() instanceof Vertex) {
			if (work.getWorkType() == workTypeEnum.WT_DELETE.ordinal()) {
				gdbDict.removeVertex(((Vertex) workCtrl.getObject()).getVertexLabel());
				
			} else if (work.getWorkType() == workTypeEnum.WT_CREATE.ordinal()) {
				gdbDict.addMigratedVertexList((Vertex) work.getObject());
				
			} else if (work.getWorkType() == workTypeEnum.WT_RENAME.ordinal()) {
				String tempStr = work.getObject().getName();
				
				renameFromGraph(work.getObject(), work.getOriginalName());
				changeVertexName(work.getObject().getName(), work.getOriginalName());
				work.setOriginalName(tempStr);
				
			}
		} else {
		    Edge edge = (Edge)work.getObject();
			if (work.getWorkType() == workTypeEnum.WT_DELETE.ordinal()) {
				gdbDict.removeEdge(edge.getEdgeLabel());
				SetEdgeVisibleFromGraph(edge, false);
			} else if (work.getWorkType() == workTypeEnum.WT_CREATE.ordinal()) {
				gdbDict.addMigratedEdgeList(edge);
				SetEdgeVisibleFromGraph(edge, true);
			} else if (work.getWorkType() == workTypeEnum.WT_RENAME.ordinal()) {
				String tempStr = work.getObject().getName();
				
				renameFromGraph(work.getObject(), work.getOriginalName());
				changeEdgeName(work.getObject().getName(), work.getOriginalName());
				work.setOriginalName(tempStr);
				
			}
		}
	}
	
	private void changeVertexName(String nowName, String originalName) {
		ArrayList<Edge> edgeList = (ArrayList<Edge>) gdbDict.getMigratedEdgeList();
		
		for (Edge edge : edgeList) {
			if (edge.getStartVertexName().equals(nowName)) {
				edge.setStartVertexName(originalName);
			}
			
			if (edge.getEndVertexName().equals(nowName)) {
				edge.setEndVertexName(originalName);
			}
		}
		
		ArrayList<Vertex> vertexList = (ArrayList<Vertex>) gdbDict.getMigratedVertexList();
		
		for (Vertex vertex : vertexList) {
			if (vertex.getVertexLabel().equals(nowName)) {
				vertex.setVertexLabel(originalName);
			}
			
			for (Vertex endVertex : vertex.getEndVertexes()) {
				if (endVertex.getVertexLabel().equals(nowName)) {
					endVertex.setVertexLabel(originalName);
				}
			}
		}
	}
	
	private void changeEdgeName(String nowName, String originalName) {
		ArrayList<Edge> edgeList = (ArrayList<Edge>) gdbDict.getMigratedEdgeList();
		
		for (Edge edge : edgeList) {
			if (edge.getName().equals(nowName)) {
				edge.setEdgeLabel(originalName);
			}
		}
	}
	
	private void deleteEdgeInGraph(Edge e) {
	    GraphDictionary gdbDict = mConfig.getGraphDictionary();
        workBuffer.addWork(workCtrl.createWork(workTypeEnum.WT_DELETE.ordinal(), e));
        gdbDict.removeEdge(e.getEdgeLabel());
        SetEdgeVisibleFromGraph(e, false);
	}

	private void SetEdgeVisibleFromGraph(Edge e, boolean visible) {
		if (graph == null || graph.isDisposed() || e == null) {
			return;
		}
		List<GraphConnection> connections = new ArrayList<>(graph.getConnections());
		for (GraphConnection connection : connections) {
			if (connection.isDisposed() || connection.getData() != e) {
				continue;
			}
			if (e.equals(selectedObject)) {
				selectedObject = null;
			}
			connection.setVisible(visible);
		}
		graph.redraw();
	}
	
	private void renameFromGraph(Object o, String name) {
	    if (o instanceof Vertex) {
	        renameVertexFromGraph((Vertex)o, name);
	    } else if ( o instanceof Edge) {
            renameEdgeFromGraph((Edge)o, name);
	    } 
	}
	
	private void renameEdgeFromGraph(Edge e, String name) {
        if (graph == null || graph.isDisposed() || e == null) {
            return;
        }
        List<GraphConnection> connections = new ArrayList<>(graph.getConnections());
        for (GraphConnection connection : connections) {
            if (connection.isDisposed() || connection.getData() != e) {
                continue;
            }
            connection.setText(name);
            
            if (e.equals(selectedObject)) {
                selectedObject = e;
            }
        }
        graph.redraw();
    }
	
	private void renameVertexFromGraph(Vertex v, String name) {
        if (graph == null || graph.isDisposed() || v == null) {
            return;
        }
        List<GraphNode> nodes = new ArrayList<>(graph.getNodes());
        for (GraphNode node : nodes) {
            if (node.isDisposed() || node.getData() != v) {
                continue;
            }
            node.setText(name);
            
            if (v.equals(selectedObject)) {
                selectedObject = v;
            }
        }
        graph.redraw();
    }
	
}
