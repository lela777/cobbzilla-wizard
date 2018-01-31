package org.cobbzilla.wizard.resources;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.cobbzilla.wizard.asset.AssetStorageService;
import org.cobbzilla.wizard.asset.AssetStream;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.model.BasicAccount;
import org.cobbzilla.wizard.model.AssetStorageInfo;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Optional;
import java.util.regex.Pattern;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.http.HttpContentTypes.contentType;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

public abstract class FileUploadResource<A extends BasicAccount,
                                         FI extends AssetStorageInfo,
                                         FID extends DAO<FI>,
                                         RE extends Identifiable,
                                         RED extends DAO<RE>> {

    protected abstract FID getFileInfoDAO();
    protected abstract RED getRelatedEntityDAO();

    @Autowired private RestServerConfiguration configuration;

    private static final Pattern ALLOWED_EXTENSIONS = Pattern.compile(".*\\.pdf");

    @POST @Path("/{uuid}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    public Response sendDocuments(@Context HttpContext context,
                                  @PathParam("uuid") String uuid,
                                  @FormDataParam("file") InputStream fileStream,
                                  @FormDataParam("file") FormDataContentDisposition fileDetail) {

        final FileUploadContext ctx = new FileUploadContext(context, uuid);
        final Optional<String> error = validateFile(fileStream, fileDetail);
        if (error.isPresent()) return invalid(error.get());

        final String asset = sha256_hex(fileDetail.getFileName() + ctx.relatedEntity.getUuid());
        ctx.storage.store(fileStream, fileDetail.getFileName(), asset);
        return ok(getFileInfoDAO().create(createFileInfo(ctx.relatedEntity, asset, fileDetail.getFileName())));
    }

    @GET @Path("/{uuid}/download/{fileInfoUuid}")
    @Consumes(APPLICATION_JSON)
    @Produces("*/*")
    public Response downloadDocuments(@Context HttpContext context,
                                      @PathParam("uuid") String uuid,
                                      @PathParam("fileInfoUuid") String fileInfoUuid) {
        FileUploadContext ctx = new FileUploadContext(context, uuid, fileInfoUuid);
        AssetStream assetStream = ctx.storage.load(ctx.fileInfo.getAsset());
        if (empty(assetStream)) return notFound();
        return stream(contentType(assetStream.getFormatName()), assetStream.getStream());
    }

    protected abstract FI createFileInfo(RE relatedEntity, String asset, String fileName);

    protected abstract boolean validateCaller(A account, RE relatedEntity);

    /**
     * Subclasses should override this method if the allowed file extensions differs from the default.
     * @return Regular expression representing the allowed file extensions
     */
    protected Pattern getAllowedFileExtensions() { return ALLOWED_EXTENSIONS; }

    private class FileUploadContext {
        public A caller;
        public RE relatedEntity;
        public AssetStorageService storage;
        public FI fileInfo;
        FileUploadContext(HttpContext context, String uuid) { this(context, uuid, null); }
        FileUploadContext(HttpContext context, String uuid, String fileInfoUuid) {
            caller = userPrincipal(context);
            relatedEntity = getRelatedEntityDAO().findByUuid(uuid);
            if (empty(relatedEntity)) throw notFoundEx();

            if (!validateCaller(caller, relatedEntity)) throw forbiddenEx();

            if (!empty(fileInfoUuid)) {
                fileInfo = getFileInfoDAO().findByUuid(fileInfoUuid);
                if (empty(fileInfo)) throw notFoundEx();
            }
            storage = configuration.getAssetStorageService();
        }
    }

    private Optional<String> validateFile(InputStream fileStream, FormDataContentDisposition fileDetail) {
        if (fileStream == null) return Optional.of("err.fileStream.empty");
        if (fileDetail == null) return Optional.of("err.fileDetail.empty");
        if (empty(fileDetail.getFileName())) return Optional.of("err.fileName.empty");
        if (!getAllowedFileExtensions().matcher(fileDetail.getFileName()).matches()) {
            return Optional.of("err.fileFormat.invalid");
        }

        return Optional.empty();
    }
}
