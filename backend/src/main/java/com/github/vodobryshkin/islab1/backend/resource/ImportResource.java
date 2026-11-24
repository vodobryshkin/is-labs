package com.github.vodobryshkin.islab1.backend.resource;

import com.github.vodobryshkin.islab1.backend.dto.common.ListQuery;
import com.github.vodobryshkin.islab1.backend.service.ImportHistoryService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Set;

/**
 * История импорта. Доступ без входа закрыт общим ограничением web.xml.
 */
@Path("/imports")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource {
    private final ImportHistoryService history;
    private final RequestReader reader;

    @Inject
    public ImportResource(ImportHistoryService history, RequestReader reader) {
        this.history = history;
        this.reader = reader;
    }

    public ImportResource() {
        this(null, null);
    }

    @GET
    public Response list(@Context HttpServletRequest request, @Context UriInfo uriInfo) {
        ListQuery query = reader.listQuery(uriInfo.getQueryParameters(), Set.of("id"));
        CacheControl cache = new CacheControl();
        cache.setNoStore(true);

        return Response.ok(history.list(request.getUserPrincipal().getName(), query))
                .cacheControl(cache).build();
    }
}
