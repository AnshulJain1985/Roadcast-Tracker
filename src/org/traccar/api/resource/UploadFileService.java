package org.traccar.api.resource;

/**
 * Created by anshulj on 17/04/18.
 */
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("/upload")
public class UploadFileService {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
            @FormDataParam("file") InputStream in,
            @FormDataParam("file") FormDataContentDisposition info) throws IOException {

        File upload = new File(info.getFileName());

        if (upload.exists()){
            String message = "file: " + upload.getName() + " already exists";
            return Response.status(Status.CONFLICT).entity(message).build();
        } else {
            Files.copy(in, upload.toPath());
            return Response.status(Status.OK).build();
        }
    }

}