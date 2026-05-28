/*
 * Copyright (C) 2009 Search Solution Corporation. All rights reserved by Search Solution. 
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
package com.cubrid.cubridmigration.ui.wizard.graph.page;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.cubrid.cubridmigration.core.common.CharsetUtils;
import com.cubrid.cubridmigration.core.common.CommonUtils;
import com.cubrid.cubridmigration.core.common.PathUtils;
import com.cubrid.cubridmigration.core.common.TimeZoneUtils;
import com.cubrid.cubridmigration.core.connection.ConnParameters;
import com.cubrid.cubridmigration.core.dbobject.Schema;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.common.Status;
import com.cubrid.cubridmigration.ui.database.IJDBCConnectionFilter;
import com.cubrid.cubridmigration.ui.database.JDBCConnectionMgrView;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.cubridmigration.ui.wizard.GraphMigrationWizard;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;
import com.cubrid.cubridmigration.ui.wizard.dialog.CSVSettingsDialog;
import com.cubrid.cubridmigration.ui.wizard.page.MigrationWizardPage;
import com.cubrid.cubridmigration.ui.wizard.page.view.AbstractDestinationView;

public class GraphSelectDestinationPage extends MigrationWizardPage {

	private OnlineTargetDBView onlineTargetDBView;
	// private OfflineTargetDBView offlineTargetDBView;
	private Composite container;

	// private Listener autoSelectAll = new Listener() {
	// public void handleEvent(Event event) {
	// if (event.item instanceof Text) {
	// ((Text) event.item).selectAll();
	// }
	// }
	// };

	/**
	 * Create the wizard
	 */
	public GraphSelectDestinationPage(String pageName) {
		super(pageName);
	}

	/**
	 * When migration wizard displayed current page.
	 * 
	 * @param event PageChangedEvent
	 */
	protected void afterShowCurrentPage(PageChangedEvent event) {
		if (isFirstVisible) {
			isFirstVisible = false;
		}
		final AbstractDestinationView crtDBView = getCrtDBView();
		crtDBView.createControls(container);
		onlineTargetDBView.hide();
		crtDBView.init();
		crtDBView.show();
		container.layout();
	}

	/**
	 * Create contents of the wizard
	 * 
	 * @param parent Composite
	 */
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		final GridLayout gridLayoutRoot = new GridLayout();
		container.setLayout(gridLayoutRoot);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setControl(container);

		onlineTargetDBView = new OnlineTargetDBView();
	}

	/**
	 * Retrieves current target DB view
	 * 
	 * @return TargetDBView
	 */
	private AbstractDestinationView getCrtDBView() {
		MigrationWizard wizard = getMigrationWizard();
		MigrationConfiguration config = wizard.getMigrationConfig();
		if (config.getDestType() == MigrationConfiguration.DEST_GRAPH) {
			return onlineTargetDBView;
		}

		throw new RuntimeException("Error destination configuration.");
	}

	/**
	 * When migration wizard will show next page or previous page.
	 * 
	 * @param event PageChangingEvent
	 */
	protected void handlePageLeaving(PageChangingEvent event) {
		// If page is not complete, it should be go to previous page.
		if (!isPageComplete()) {
			return;
		}
		if (!isGotoNextPage(event)) {
			return;
		}
		event.doit = updateMigrationConfig();
	}

	/**
	 * Update migration configuration.
	 * 
	 * @return true if all updated
	 */
	protected boolean updateMigrationConfig() {
		return getCrtDBView().save();
	}

	private class OnlineTargetDBView extends AbstractDestinationView {
		private final JDBCConnectionMgrView conMgrView;

		private OnlineTargetDBView() {
			conMgrView = new JDBCConnectionMgrView(GraphMigrationWizard.getSupportedTarDBTypes(),
					new IJDBCConnectionFilter() {
						public boolean doFilter(ConnParameters cp) {
							final MigrationConfiguration cfg = getMigrationWizard().getMigrationConfig();
							if (cp.getDatabaseType().getID() != MigrationConfiguration.SOURCE_TYPE_CORADB) {
								return true;
							}
							return false;
						}
					});
		}

		/**
		 * Create Controls
		 * 
		 * @param parent Composite
		 */
		public void createControls(Composite parent) {
			conMgrView.createControls(parent);

		}

		/**
		 * Hide view
		 */
		public void hide() {
			conMgrView.hide();
		}

		/**
		 * initial the page set which option is visiable and updateDialogStatus
		 */
		public void init() {
			setTitle(getMigrationWizard().getStepNoMsg(GraphSelectDestinationPage.this)
					+ Messages.msgDestSelectOnlineGRAPHDB);
			setDescription(Messages.msgDestSelectOnlineGRAPHDBDes);
			final MigrationConfiguration config = getMigrationWizard().getMigrationConfig();
			List<Integer> dts = new ArrayList<Integer>();
			if (config.getDestType() == MigrationConfiguration.DEST_GRAPH) {
				dts.add(MigrationConfiguration.SOURCE_TYPE_CORADB);
			} else {
				dts.add(config.getDestType());
			}
			conMgrView.setSupportedDBType(dts);
			if (config.getTargetConParams() == null) {
				conMgrView.init(config.getSourceConParams(), null);
			} else {
				conMgrView.init(config.getTargetConParams(), null);
			}
		}

		/**
		 * Save UI
		 * 
		 * @return true if saving successfully
		 */
		public boolean save() {
			if (conMgrView.getSelectedDCI() == null) {
				MessageDialog.openError(getShell(), Messages.msgError, Messages.sourceDBPageErrNoSelectedItem);
				return false;
			}
			final MigrationWizard wzd = getMigrationWizard();
			final MigrationConfiguration config = wzd.getMigrationConfig();
			ConnParameters connParameters = conMgrView.getSelectedDCI().getConnParameters();
			config.setTargetConParams(connParameters);

			//	//check connection
			//	ConnectionTestWithProgress connTest = new ConnectionTestWithProgress(config);
			//	
			//	if (connTest.launch()) {
			//		return true;
			//	} else {
			//		DetailMessageDialog.openInfo(getShell(), Messages.msgError, Messages.commonToolMysqlMsg5, "Cannot connect to database");
			//		return false;
			//	}

			return true;
		}

		/**
		 * displayOnlineContainer
		 */
		public void show() {
			conMgrView.show();
		}

	}
}
