package org.openmicroscopy.shoola;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import ome.system.Login;
import omero.RLong;
import omero.RType;
import omero.api.IQueryPrx;
import omero.cmd.CmdCallbackI;
import omero.cmd.Delete2Response;
import omero.cmd.OK;
import omero.cmd.Request;
import omero.cmd.Response;
import omero.cmd.graphs.ChildOption;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.util.Requests;
import omero.log.SimpleLogger;
import omero.model.OriginalFile;
import omero.model.ParseJob;
import omero.model.ScriptJob;
import omero.sys.Parameters;
import omero.sys.ParametersI;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

public class DeleteScriptJobs {

    public void deleteScriptJobs() throws Throwable {
        final LoginCredentials credentials = new LoginCredentials();
        credentials.getServer().setHostname("localhost");
        credentials.getServer().setPort(4064);
        credentials.getUser().setUsername("user-6");
        credentials.getUser().setPassword("xxx");

        final Gateway gateway = new Gateway(new SimpleLogger());
        gateway.connect(credentials);
        final SecurityContext ctx = new SecurityContext(-1);
        final IQueryPrx iQuery = gateway.getQueryService(ctx);

        final String query = "SELECT id FROM ParseJob WHERE details.owner.omeName = :name";
        final Parameters params = new ParametersI().add("name", omero.rtypes.rstring("user-3"));
        final ImmutableMap<String, String> context = ImmutableMap.of(Login.OMERO_GROUP, "-1");

        System.out.println("Before:");
        final List<Long> ids = new ArrayList<>();
        for (final List<RType> job : iQuery.projection(query, params, context)) {
            final RLong id = (RLong) job.get(0);
            ids.add(id.getValue());
            System.out.println(id.getValue());
        }

        final ChildOption excludeFiles = Requests.option().excludeType(OriginalFile.class).build();
        final Request delete = Requests.delete().target(ParseJob.class).id(ids).option(excludeFiles).build();

        final CmdCallbackI callback = gateway.submit(ctx, delete);
        callback.loop(1000, 100);
        final Response response = callback.getResponse();
        if (response instanceof OK) {
            final Delete2Response deleted = (Delete2Response) response;
            for (final Entry<String, List<Long>> oneClass : deleted.deletedObjects.entrySet()) {
                System.out.println(oneClass.getKey() + ": " + Joiner.on(',').join(oneClass.getValue()));
            }
        } else {
            System.err.println("Failed! " + response);
        }

        System.out.println("After:");
        for (final List<RType> job : iQuery.projection(query, params, context)) {
            final RLong id = (RLong) job.get(0);
            System.out.println(id.getValue());
        }
        
        gateway.disconnect();
    } 
    public static void main(String[] args) throws Throwable {
        DeleteScriptJobs delJobs = new DeleteScriptJobs();
        delJobs.deleteScriptJobs();
    }
}