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
package com.cubrid.cubridmigration.ui.wizard.graph.page;

import com.cubrid.cubridmigration.core.connection.ConnParameters;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.database.IJDBCConnectionFilter;
import com.cubrid.cubridmigration.ui.database.JDBCConnectionMgrView;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;
import com.cubrid.cubridmigration.ui.wizard.page.MigrationWizardPage;
import com.cubrid.cubridmigration.ui.wizard.page.view.AbstractDestinationView;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class GraphSelectDestinationPage extends MigrationWizardPage {

    private OnlineTargetDBView onlineTargetDBView;

    private Composite container;

    /** Create the wizard */
    public GraphSelectDestinationPage(String pageName) {
        super(pageName);
    }

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

    public void createControl(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        final GridLayout gridLayoutRoot = new GridLayout();
        container.setLayout(gridLayoutRoot);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        setControl(container);

        onlineTargetDBView = new OnlineTargetDBView();
    }

    private AbstractDestinationView getCrtDBView() {
        MigrationWizard wizard = getMigrationWizard();
        MigrationConfiguration config = wizard.getMigrationConfig();
        if (config.targetIsOnline()) {
            return onlineTargetDBView;
        }
        throw new RuntimeException("Error destination configuration.");
    }

    protected void handlePageLeaving(PageChangingEvent event) {
        if (!isPageComplete()) {
            return;
        }
        if (isGotoNextPage(event)) {
            event.doit = updateMigrationConfig();
        }
    }

    protected boolean updateMigrationConfig() {
        return getCrtDBView().save();
    }

    private class OnlineTargetDBView extends AbstractDestinationView {
        private final int USERSCHEMA_VERSION = 112;
        private final JDBCConnectionMgrView conMgrView;

        private OnlineTargetDBView() {
            conMgrView =
                    new JDBCConnectionMgrView(
                            MigrationWizard.getSupportedTarDBTypes(),
                            new IJDBCConnectionFilter() {

                                public boolean doFilter(ConnParameters cp) {
                                    final MigrationConfiguration cfg =
                                            getMigrationWizard().getMigrationConfig();
                                    if (cfg.sourceIsOnline()) {
                                        return cfg.getSourceConParams().isSameDB(cp);
                                    }
                                    return false;
                                }
                            });
        }

        public void createControls(Composite parent) {
            conMgrView.createControls(parent);
        }

        public void hide() {
            conMgrView.hide();
        }

        public void init() {
            setTitle(
                    getMigrationWizard().getStepNoMsg(GraphSelectDestinationPage.this)
                            + Messages.msgDestSelectOnlineCUBRIDDB);
            setDescription(Messages.msgDestSelectOnlineCUBRIDDBDes);
            final MigrationConfiguration config = getMigrationWizard().getMigrationConfig();
            conMgrView.init(config.getTargetConParams(), null);
        }

        public boolean save() {
            if (conMgrView.getSelectedDCI() == null) {
                MessageDialog.openError(
                        getShell(), Messages.msgError, Messages.sourceDBPageErrNoSelectedItem);
                return false;
            }
            final MigrationWizard wzd = getMigrationWizard();
            final MigrationConfiguration config = wzd.getMigrationConfig();
            ConnParameters connParameters = conMgrView.getSelectedDCI().getConnParameters();
            config.setTargetConParams(connParameters);

            Catalog catalog = conMgrView.getCatalog();

            if (catalog == null) {
                return false;
            }

            int targetCubridVersion =
                    (catalog.getVersion().getDbMajorVersion() * 10)
                            + (catalog.getVersion().getDbMinorVersion());
            config.setTargetDBVersion(String.valueOf(targetCubridVersion));
            config.setAddUserSchema(targetCubridVersion >= USERSCHEMA_VERSION);

            if (null != catalog) {
                wzd.setTargetCatalog(catalog);
                config.setTarSchemaSize(catalog.getSchemas().size());
            }
            config.setTargetDBAGroup(catalog.isDBAGroup());
            return true;
        }

        public void show() {
            conMgrView.show();
        }
    }
}
