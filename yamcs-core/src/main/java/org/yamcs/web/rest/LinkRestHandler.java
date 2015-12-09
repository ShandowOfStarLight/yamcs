package org.yamcs.web.rest;

import java.util.List;

import org.yamcs.YamcsException;
import org.yamcs.YamcsServer;
import org.yamcs.management.ManagementService;
import org.yamcs.protobuf.Rest.EditLinkRequest;
import org.yamcs.protobuf.Rest.ListLinkInfoResponse;
import org.yamcs.protobuf.SchemaRest;
import org.yamcs.protobuf.SchemaYamcsManagement;
import org.yamcs.protobuf.YamcsManagement.LinkInfo;
import org.yamcs.web.BadRequestException;
import org.yamcs.web.HttpException;
import org.yamcs.web.InternalServerErrorException;
import org.yamcs.web.MethodNotAllowedException;
import org.yamcs.web.NotFoundException;

import io.netty.channel.ChannelFuture;

/**
 * Gives information on data links
 */
public class LinkRestHandler extends RestHandler {
    
    @Override
    public ChannelFuture handleRequest(RestRequest req, int pathOffset) throws HttpException {
        if (req.hasPathSegment(pathOffset)) {
            String instance = req.getPathSegment(pathOffset);
            if (!YamcsServer.hasInstance(instance)) {
                throw new NotFoundException(req, "No instance named '" + instance + "'");
            }
            
            pathOffset++;
            if (req.hasPathSegment(pathOffset)) {
                String linkName = req.getPathSegment(pathOffset);
                LinkInfo linkInfo = ManagementService.getInstance().getLinkInfo(instance, linkName);
                if (linkInfo == null) {
                    throw new NotFoundException(req, "No link named '" + linkName + "' within instance '" + instance + "'");
                } else {
                    if (req.isGET()) {
                        return getLink(req, linkInfo);
                    } else if (req.isPATCH() || req.isPOST() || req.isPUT()) {
                        return editLink(req, linkInfo);
                    } else {
                        throw new MethodNotAllowedException(req);
                    }
                }
            } else {
                req.assertGET();
                return listLinks(req, instance);
            }
        } else {
            req.assertGET();
            return listLinks(req, null);
        }
    }
    
    private ChannelFuture getLink(RestRequest req, LinkInfo linkInfo) throws HttpException {
        return sendOK(req, linkInfo, SchemaYamcsManagement.LinkInfo.WRITE);
    }
    
    private ChannelFuture listLinks(RestRequest req, String instance) throws HttpException {
        List<LinkInfo> links = ManagementService.getInstance().getLinkInfo();
        ListLinkInfoResponse.Builder responseb = ListLinkInfoResponse.newBuilder();
        for (LinkInfo link : links) {
            if (instance == null || instance.equals(link.getInstance())) {
                responseb.addLink(link);
            }
        }
        return sendOK(req, responseb.build(), SchemaRest.ListLinkInfoResponse.WRITE);
    }
    
    private ChannelFuture editLink(RestRequest req, LinkInfo linkInfo) throws HttpException {
        EditLinkRequest request = req.bodyAsMessage(SchemaRest.EditLinkRequest.MERGE).build();
        String state = null;
        if (request.hasState()) state = request.getState();
        if (req.hasQueryParameter("state")) state = req.getQueryParameter("state");
        
        if (state != null) {
            ManagementService mservice = ManagementService.getInstance();
            switch (state.toLowerCase()) {
            case "enabled":
                try {
                    mservice.enableLink(linkInfo.getInstance(), linkInfo.getName());
                    return sendOK(req);
                } catch (YamcsException e) {
                    throw new InternalServerErrorException(e);
                }
            case "disabled":
                try {
                    mservice.disableLink(linkInfo.getInstance(), linkInfo.getName());
                    return sendOK(req);                    
                } catch (YamcsException e) {
                    throw new InternalServerErrorException(e);
                }
            default:
                throw new BadRequestException("Unsupported link state '" + state + "'");
            }
        } else {
            return sendOK(req);
        }
    }
}
