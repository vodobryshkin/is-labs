package com.github.vodobryshkin.islab1.backend.resource;

import com.github.vodobryshkin.islab1.backend.dto.common.ListQuery;
import com.github.vodobryshkin.islab1.backend.dto.imports.ImportResponse;
import com.github.vodobryshkin.islab1.backend.service.imports.ImportReport;
import com.github.vodobryshkin.islab1.backend.service.imports.WorkerImportService;
import com.github.vodobryshkin.islab1.backend.service.ImportHistoryService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Set;
import java.io.InputStream;

/**
 * История импорта. Доступ без входа закрыт общим ограничением web.xml.
 */
@Path("/imports")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource {
    private final ImportHistoryService history;
    private final RequestReader reader;
    private final WorkerImportService imports;

    @Inject
    public ImportResource(ImportHistoryService history, RequestReader reader, WorkerImportService imports) {
        this.history = history;
        this.reader = reader;
        this.imports = imports;
    }

    public ImportResource() {
        this(null, null, null);
    }

    @GET
    public Response list(@Context HttpServletRequest request, @Context UriInfo uriInfo) {
        ListQuery query = reader.listQuery(uriInfo.getQueryParameters(), Set.of("id"));
        CacheControl cache = new CacheControl();
        cache.setNoStore(true);

        return Response.ok(history.list(request.getUserPrincipal().getName(), query))
                .cacheControl(cache).build();
    }

    /**
     * Файл передаётся непосредственно в теле запроса; формат выбирает реализацию парсера.
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
    public Response upload(InputStream input, @QueryParam("format") @DefaultValue("json") String format,
            @Context HttpServletRequest request) {
        ImportReport report = imports.importFile(input, format, request.getUserPrincipal().getName());
        int status = report.failure() == null ? 201 : switch (report.failure().getKind()) {
            case FORMAT -> 400;
            case VALIDATION -> 422;
            case CONFLICT -> 409;
            case STORAGE -> 500;
            case TOO_LARGE -> 413;
            case UNSUPPORTED_FORMAT -> 415;
        };
        CacheControl cache = new CacheControl();
        cache.setNoStore(true);
        return Response.status(status).entity(new ImportResponse(report)).cacheControl(cache).build();
    }
}
