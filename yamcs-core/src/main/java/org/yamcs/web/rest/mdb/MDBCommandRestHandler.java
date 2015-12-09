package org.yamcs.web.rest.mdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.protobuf.Mdb.CommandInfo;
import org.yamcs.protobuf.Rest.ListCommandInfoResponse;
import org.yamcs.protobuf.SchemaMdb;
import org.yamcs.protobuf.SchemaRest;
import org.yamcs.protobuf.Yamcs.NamedObjectId;
import org.yamcs.security.Privilege;
import org.yamcs.security.Privilege.Type;
import org.yamcs.web.BadRequestException;
import org.yamcs.web.HttpException;
import org.yamcs.web.NotFoundException;
import org.yamcs.web.rest.RestHandler;
import org.yamcs.web.rest.RestRequest;
import org.yamcs.web.rest.XtceToGpbAssembler;
import org.yamcs.web.rest.XtceToGpbAssembler.DetailLevel;
import org.yamcs.web.rest.mdb.MDBHelper.MatchResult;
import org.yamcs.xtce.MetaCommand;
import org.yamcs.xtce.XtceDb;

import io.netty.channel.ChannelFuture;

/**
 * Handles incoming requests related to command info from the MDB
 */
public class MDBCommandRestHandler extends RestHandler {
    final static Logger log = LoggerFactory.getLogger(MDBCommandRestHandler.class.getName());
    
    @Override
    public ChannelFuture handleRequest(RestRequest req, int pathOffset) throws HttpException {
        XtceDb mdb = req.getFromContext(MDBRestHandler.CTX_MDB);
        if (!req.hasPathSegment(pathOffset)) {
            return listCommands(req, null, mdb); // root namespace
        } else {
            MatchResult<MetaCommand> c = MDBHelper.matchCommandName(req, pathOffset);
            if (c.matches()) { // command
                return getSingleCommand(req, c.getRequestedId(), c.getMatch());
            } else { // namespace
                return listCommandsOrError(req, pathOffset);
            }
        }
    }
    
    private ChannelFuture listCommandsOrError(RestRequest req, int pathOffset) throws HttpException {
        XtceDb mdb = req.getFromContext(MDBRestHandler.CTX_MDB);
        MatchResult<String> nsm = MDBHelper.matchXtceDbNamespace(req, pathOffset, true);
        if (nsm.matches()) {
            return listCommands(req, nsm.getMatch(), mdb);
        } else {
            throw new NotFoundException(req);
        }
    }
    
    private ChannelFuture getSingleCommand(RestRequest req, NamedObjectId id, MetaCommand cmd) throws HttpException {
        if (!Privilege.getInstance().hasPrivilege(req.getAuthToken(), Privilege.Type.TC, cmd.getQualifiedName())) {
            log.warn("Command Info for {} not authorized for token {}, throwing BadRequestException", id, req.getAuthToken());
            throw new BadRequestException("Invalid command name specified "+id);
        }
        String instanceURL = req.getApiURL() + "/mdb/" + req.getFromContext(RestRequest.CTX_INSTANCE);
        CommandInfo cinfo = XtceToGpbAssembler.toCommandInfo(cmd, instanceURL, DetailLevel.FULL, req.getOptions());
        return sendOK(req, cinfo, SchemaMdb.CommandInfo.WRITE);
    }

    /**
     * Sends the commands for the requested yamcs instance. If no namespace
     * is specified, assumes root namespace.
     */
    private ChannelFuture listCommands(RestRequest req, String namespace, XtceDb mdb) throws HttpException {
        String instanceURL = req.getApiURL() + "/mdb/" + req.getFromContext(RestRequest.CTX_INSTANCE);
        boolean recurse = req.getQueryParameterAsBoolean("recurse", false);
        
        NameDescriptionSearchMatcher matcher = null;
        if (req.hasQueryParameter("q")) {
            matcher = new NameDescriptionSearchMatcher(req.getQueryParameter("q"));    
        }
        
        ListCommandInfoResponse.Builder responseb = ListCommandInfoResponse.newBuilder();
        if (namespace == null) {
            for (MetaCommand cmd : mdb.getMetaCommands()) {
                if (matcher != null && !matcher.matches(cmd)) continue;
                responseb.addCommand(XtceToGpbAssembler.toCommandInfo(cmd, instanceURL, DetailLevel.SUMMARY, req.getOptions()));
            }
        } else {
            Privilege privilege = Privilege.getInstance();
            for (MetaCommand cmd : mdb.getMetaCommands()) {
                if (!privilege.hasPrivilege(req.getAuthToken(), Type.TC, cmd.getQualifiedName()))
                    continue;
                if (matcher != null && !matcher.matches(cmd))
                    continue;
                
                String alias = cmd.getAlias(namespace);
                if (alias != null || (recurse && cmd.getQualifiedName().startsWith(namespace))) {
                    responseb.addCommand(XtceToGpbAssembler.toCommandInfo(cmd, instanceURL, DetailLevel.SUMMARY, req.getOptions()));
                }
            }
        }
        
        return sendOK(req, responseb.build(), SchemaRest.ListCommandInfoResponse.WRITE);
    }
}
