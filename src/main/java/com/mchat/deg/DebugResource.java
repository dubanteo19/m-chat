
package com.mchat.deg;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.SseEventSink;

@Path("/debug")
@Consumes(MediaType.APPLICATION_JSON)
public class DebugResource {

    private static final Logger LOG = Logger.getLogger(DebugResource.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    DebugLogHub logHub;

    @POST
    public Response log(String body) {
        String message;

        try {
            Object json = objectMapper.readValue(body, Object.class);
            message = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(json);
        } catch (Exception e) {
            message = body;
        }

        LOG.infof("[REMOTE DEBUG]\n%s", message);
        logHub.publish(message);

        return Response.ok().build();
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void stream(
            @Context SseEventSink sink) {
        logHub.register(sink);
    }

    @GET
    @Path("/history")
    @Produces(MediaType.APPLICATION_JSON)
    public java.util.List<DebugLogHub.LogEntry> history() {
        return logHub.history();
    }
}