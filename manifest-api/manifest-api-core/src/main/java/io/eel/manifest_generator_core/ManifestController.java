package io.eel.manifest_generator_core;

import io.eel.common.WorkbookValidator;
import io.eel.common.http.HttpRequest;
import io.eel.common.http.HttpResponse;

public class ManifestController {

    private final static String GET = "GET";

    private final static String POST = "POST";

    public void handle(HttpRequest request, HttpResponse response) {
        // GET /manifest
        if (GET.equals(request.getHttpMethod()) && "/manifest".equals(request.getPath())) {
            WorkbookValidator.Manifest manifest = this.getManifest();
        // POST /manifest
        } else if (POST.equals(request.getHttpMethod()) && "/manifest".equals(request.getPath())) {
            WorkbookValidator.Manifest manifest = this.createManifest();
        } else {
            response.setStatus(404);
        }
    }

    private WorkbookValidator.Manifest getManifest() {
        return null;
    }

    private WorkbookValidator.Manifest createManifest() {
        return null;
    }

}
