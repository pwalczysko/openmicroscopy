/*
 * Copyright (C) 2016 University of Dundee & Open Microscopy Environment.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package integration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportContainer;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.ImportLibrary.ImportCallback;
import ome.formats.importer.util.ProportionalTimeEstimatorImpl;
import ome.formats.importer.util.TimeEstimator;
import ome.services.blitz.repo.path.FsFile;
import ome.util.checksum.ChecksumProviderFactory;
import ome.util.checksum.ChecksumProviderFactoryImpl;
import omero.RLong;
import omero.RString;
import omero.RType;
import omero.SecurityViolation;
import omero.ServerError;
import omero.api.IScriptPrx;
import omero.api.RawFileStorePrx;
import omero.api.ServiceFactoryPrx;
import omero.cmd.CmdCallbackI;
import omero.cmd.HandlePrx;
import omero.gateway.util.Requests;
import omero.gateway.util.Requests.Delete2Builder;
import omero.grid.ImportLocation;
import omero.grid.ImportProcessPrx;
import omero.grid.ImportRequest;
import omero.grid.ManagedRepositoryPrx;
import omero.grid.ManagedRepositoryPrxHelper;
import omero.grid.RepositoryMap;
import omero.grid.RepositoryPrx;
import omero.model.AdminPrivilege;
import omero.model.AdminPrivilegeI;
import omero.model.ChecksumAlgorithm;
import omero.model.ChecksumAlgorithmI;
import omero.model.Dataset;
import omero.model.DatasetI;
import omero.model.Experimenter;
import omero.model.ExperimenterGroup;
import omero.model.ExperimenterGroupI;
import omero.model.ExperimenterI;
import omero.model.Folder;
import omero.model.GroupExperimenterMapI;
import omero.model.IObject;
import omero.model.NamedValue;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;
import omero.model.Project;
import omero.model.ProjectI;
import omero.model.Session;
import omero.model.enums.AdminPrivilegeChgrp;
import omero.model.enums.AdminPrivilegeChown;
import omero.model.enums.AdminPrivilegeModifyUser;
import omero.model.enums.AdminPrivilegeReadSession;
import omero.model.enums.AdminPrivilegeSudo;
import omero.model.enums.AdminPrivilegeWriteFile;
import omero.model.enums.AdminPrivilegeWriteOwned;
import omero.model.enums.ChecksumAlgorithmMurmur3128;
import omero.model.enums.ChecksumAlgorithmSHA1160;
import omero.sys.EventContext;
import omero.sys.ParametersI;
import omero.sys.Principal;
import omero.util.TempFileManager;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import loci.formats.in.FakeReader;

/**
 * Tests the effectiveness of light administrator privileges.
 * @author m.t.b.carroll@dundee.ac.uk
 * @since 5.3.0
 */
public class LightAdminRolesTest extends AbstractServerImportTest {

    private static final TempFileManager TEMPORARY_FILE_MANAGER = new TempFileManager(
            "test-" + LightAdminRolesTest.class.getSimpleName());

    private ImmutableSet<AdminPrivilege> allPrivileges = null;

    private File fakeImageFile = null;

    /**
     * Populate the set of available light administrator privileges.
     * @throws ServerError unexpected
     */
    @BeforeClass
    public void populateAllPrivileges() throws ServerError {
        final ImmutableSet.Builder<AdminPrivilege> privileges = ImmutableSet.builder();
        for (final IObject privilege : factory.getTypesService().allEnumerations("AdminPrivilege")) {
            privileges.add((AdminPrivilege) privilege);
        }
        allPrivileges = privileges.build();
    }

    /**
     * Create a fake image file for use in import tests.
     * @throws IOException unexpected
     */
    @BeforeClass
    public void createFakeImageFile() throws IOException {
        final File temporaryDirectory = TEMPORARY_FILE_MANAGER.createPath("images", null, true);
        fakeImageFile = new File(temporaryDirectory, "image.fake");
        fakeImageFile.createNewFile();
    }

    /**
     * Create a light administrator, with a specific privilege, and log in as them.
     * All the other privileges will be set to False.
     * @param isAdmin if the user should be a member of the <tt>system</tt> group
     * @param restriction the privilege that the user should not have, or {@code null} if they should have all privileges
     * @return the new user's context
     * @throws Exception if the light administrator could not be created
     */
    private EventContext loginNewAdmin(boolean isAdmin, String permission) throws Exception {
        final EventContext ctx = isAdmin ? newUserInGroup(iAdmin.lookupGroup(SYSTEM_GROUP), false) : newUserAndGroup("rwr-r-");
        final ServiceFactoryPrx rootSession = root.getSession();
        Experimenter user = new ExperimenterI(ctx.userId, false);
        user = (Experimenter) rootSession.getQueryService().get("Experimenter", ctx.userId);
        final AdminPrivilege privilege = new AdminPrivilegeI();
        privilege.setValue(omero.rtypes.rstring(permission));
        final List<AdminPrivilege> privileges = new ArrayList<>();
        rootSession.getAdminService().setAdminPrivileges(user, privileges);
        privileges.add(privilege);
        rootSession.getAdminService().setAdminPrivileges(user, privileges);
        /* avoid old session as privileges are briefly cached */
        loginUser(ctx);
        System.out.println(ctx.userId);
        return ctx;
    }

    /**
     * Sudo to the given user.
     * @param target a user
     * @return context for a session owned by the given user
     * @throws Exception if the sudo could not be performed
     */
    private EventContext sudo(Experimenter target) throws Exception {
        if (!target.isLoaded()) {
            target = iAdmin.getExperimenter(target.getId().getValue());
        }
        final Principal principal = new Principal();
        principal.name = target.getOmeName().getValue();
        final Session session = factory.getSessionService().createSessionWithTimeout(principal, 100 * 1000);
        final omero.client client = newOmeroClient();
        final String sessionUUID = session.getUuid().getValue();
        client.createSession(sessionUUID, sessionUUID);
        return init(client);
    }

    /**
     * Create a light administrator, possibly without a specific privilege, and log in as them, possibly sudo'ing afterward.
     * @param isAdmin if the user should be a member of the <tt>system</tt> group
     * @param isSudo if the user should then sudo to be a member of the <tt>system</tt> group with all privileges
     * @param restriction the privilege that the user should not have, or {@code null} if they should have all privileges
     * @return the new user's context (may be a sudo session)
     * @throws Exception if the light administrator actor could not be set up as specified
     */
    private EventContext loginNewActor(boolean isAdmin, boolean isSudo, String restriction) throws Exception {
        final EventContext adminContext = loginNewAdmin(isAdmin, restriction);
        if (isSudo) {
            final EventContext fullAdminContext = loginNewAdmin(true, null);
            loginUser(adminContext);
            try {
                final EventContext sudoContext = sudo(new ExperimenterI(fullAdminContext.userId, false));
                Assert.assertTrue(isAdmin, "normal users cannot sudo");
                return sudoContext;
            } catch (SecurityViolation sv) {
                Assert.assertFalse(isAdmin, "admins can sudo");
                throw sv;
            }
        } else {
            return adminContext;
        }
    }

    /**
     * Identifies expected repositories and provides their name for looking them up from the database.
     * @author m.t.b.carroll@dundee.ac.uk
     * @since 5.3.0
     */
    private static enum Repository {
        MANAGED("ManagedRepository"), SCRIPT("scripts");

        /* corresponds to OriginalFile.name */
        final String name;

        private Repository(String name) {
            this.name = name;
        }
    }

    /**
     * Get a proxy for the given repository. It is assumed that such a repository exists.
     * @param repository a repository
     * @return a proxy for the repository
     * @throws ServerError unexpected
     */
    private RepositoryPrx getRepository(Repository repository) throws ServerError {
        final RepositoryMap repositories = factory.sharedResources().repositories();
        int index;
        for (index = 0; !repository.name.equals(repositories.descriptions.get(index).getName().getValue()); index++);
        return repositories.proxies.get(index);
    }

    /**
     * Test that an ImporterAs can create new Project and Dataset
     * and import data on behalf of another user solely with <tt>Sudo</tt> privilege
     * into this Dataset. Further link the Dataset to the Project, check
     * that the link belongs to the user (not to the ImporterAs) and finally
     * delete the Project, Dataset and Image.
     * @throws Exception unexpected
     */
    @Test(dataProvider = "roles test cases")
    public void testImporterAsSudoPrivileges(boolean isAdmin) throws Exception {
        final EventContext normalUser = newUserAndGroup("rwr-r-");
        System.out.println("normalUser");
        System.out.println(normalUser.userId);
        final EventContext ctx = loginNewAdmin(isAdmin, AdminPrivilegeSudo.value);
        
        try {
            sudo(new ExperimenterI(normalUser.userId, false));
            if (!isAdmin) {
                Assert.fail("Sudo-permitted non-administrators cannot sudo.");
            } else {
                
            }
        } catch (SecurityViolation sv) {
            /* sudo expected to fail if the user is not in system group */
        }
        /* First, check that the light admin/importer As 
         * can create Project and Dataset on behalf of the normalUser
         * in the group of the normalUser in anticipation of importing
         * data for the normalUser in the next step into these containers */
        client.getImplicitContext().put("omero.group", Long.toString(normalUser.groupId));
        Project proj = mmFactory.simpleProject();
        Dataset dat = mmFactory.simpleDataset();
        Project sentProj = new ProjectI();
        sentProj = null;
        Dataset sentDat = new DatasetI();
        sentDat = null;
        try {
            sentProj = (Project) iUpdate.saveAndReturnObject(proj);
            sentDat = (Dataset) iUpdate.saveAndReturnObject(dat);
            Assert.assertTrue(isAdmin);
        } catch (ServerError se) {
            Assert.assertFalse(isAdmin);
        }
        /* Check the owner of the project and dataset is the normalUser in case
         * these were created */
        if (isAdmin) {
            long projId = sentProj.getId().getValue();
            final Project retrievedProject = (Project) iQuery.get("Project", projId);
            Assert.assertEquals(retrievedProject.getDetails().getOwner().getId().getValue(), normalUser.userId);
            long datId = sentDat.getId().getValue();
            System.out.println("dataset Id");
            System.out.println(datId);
            final Dataset retrievedDataset = (Dataset) iQuery.get("Dataset", datId);
            Assert.assertEquals(retrievedDataset.getDetails().getOwner().getId().getValue(), normalUser.userId);
        } else {
            Assert.assertNull(sentProj);
            Assert.assertNull(sentDat);
        }

        /* check that after sudo, the light admin is able to ImportAs and target
         * the import into the just created Dataset.
         * Check thus that the light admin can import and write the original file
         * on behalf of the normalUser and into the group of normalUser */
        client.getImplicitContext().put("omero.group", Long.toString(normalUser.groupId));
        final RString imageName = omero.rtypes.rstring(fakeImageFile.getName());
        final List<List<RType>> result = iQuery.projection(
                "SELECT id FROM OriginalFile WHERE name = :name ORDER BY id DESC LIMIT 1",
                new ParametersI().add("name", imageName));
        final long previousId = result.isEmpty() ? -1 : ((RLong) result.get(0).get(0)).getValue();
        try {
            List<String> path = Collections.singletonList(fakeImageFile.getPath());
            importFileset(path, path.size(), sentDat);
            Assert.assertTrue(isAdmin);
        } catch (ServerError se) {
            Assert.assertFalse(isAdmin);
        }
        final OriginalFile remoteFile = (OriginalFile) iQuery.findByQuery(
                "FROM OriginalFile o WHERE o.id > :id AND o.name = :name",
                new ParametersI().addId(previousId).add("name", imageName));
        if (isAdmin) {
            Assert.assertEquals(remoteFile.getDetails().getOwner().getId().getValue(), normalUser.userId);
            Assert.assertEquals(remoteFile.getDetails().getGroup().getId().getValue(), normalUser.groupId);
        } else {
            Assert.assertNull(remoteFile);
        }
        if (isAdmin) {
            final IObject image = iQuery.findByQuery(
                    "FROM Image WHERE fileset IN "
                    + "(SELECT fileset FROM FilesetEntry WHERE originalFile.id = :id)",
                    new ParametersI().addId(remoteFile.getId()));
            System.out.println(image.getId().getValue());
        }
    }

    /**
     * @return a variety of test cases for light administrator privileges
     */
    @DataProvider(name = "roles test cases")
    public Object[][] provideAdminPrivilegeCases() {
        int index = 0;
        final int IS_ADMIN = index++;

        final boolean[] booleanCases = new boolean[]{false, true};

        final List<Object[]> testCases = new ArrayList<Object[]>();

        for (final boolean isAdmin : booleanCases) {
                    final Object[] testCase = new Object[index];
                    testCase[IS_ADMIN] = isAdmin;
                    // DEBUG  if (isAdmin == false && isRestricted == true && isSudo == false)
                    testCases.add(testCase);
                }

        return testCases.toArray(new Object[testCases.size()][]);
    }
}
