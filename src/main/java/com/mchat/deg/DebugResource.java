
package com.mchat.deg;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.SseEventSink;

@Path("/debug")
@RequestScoped
public class DebugResource {

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
            Log.warn("Failed to parse debug log message as JSON, using raw string", e);
            message = body;
        }

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