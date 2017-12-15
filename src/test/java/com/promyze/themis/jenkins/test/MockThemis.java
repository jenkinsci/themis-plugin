package com.promyze.themis.jenkins.test;

import com.promyze.themis.jenkins.ThemisGlobalConfiguration;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MockThemis {

    public static Response response(int status, String message) {
        return new Response(status, message);
    }

    private HttpServer server;
    private HttpContext testContext;
    private HttpContext refreshContext;
    private HttpContext reportContext;

    public int start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        testContext = server.createContext("/api/testConnection");
        refreshContext = server.createContext("/api/refreshProject/");
        reportContext = server.createContext("/api/reportFiles/");
        server.start();
        return server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
    }

    public void setRefreshHandler(String apiKey, String path, Handler okHandler) {
        refreshContext.setHandler(new ThemisHandler(apiKey, path, okHandler));
    }

    public void setReporHandler(String apiKey, String path, Handler okHandler) {
        reportContext.setHandler(new ThemisHandler(apiKey, path, okHandler));
    }

    private static boolean checkApiKey(HttpExchange exchange, String apiKey) {
        return apiKey.equals(exchange.getRequestHeaders().getFirst(ThemisGlobalConfiguration.THEMIS_API_KEY));
    }

    @FunctionalInterface
    public interface Handler {

        Response getResponse(HttpExchange exchange);

        default void handle(HttpExchange exchange) throws IOException {
            Response response = getResponse(exchange);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = response.message.getBytes(Charset.forName("utf-8"));
            exchange.sendResponseHeaders(response.status, bytes.length);
            OutputStream body = exchange.getResponseBody();
            body.write(bytes);
            body.close();
        }

    }

    public static class Response {

        private final int status;
        private final String message;

        private Response(int status, String message) {
            this.status = status;
            this.message = message;
        }

    }

    private static class ThemisHandler implements HttpHandler {

        private final String apiKey;
        private final String path;
        private final Handler ok;
        private final Handler wrongApiKey = e -> response(403, "Wrong API key");
        private final Handler wrongPath = e -> response(400, "KO");

        private ThemisHandler(String apiKey, String path, Handler ok) {
            this.apiKey = apiKey;
            this.path = path;
            this.ok = ok;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String actualPath = exchange.getRequestURI().getPath();
            if (!checkApiKey(exchange, apiKey)) {
                wrongApiKey.handle(exchange);
            } else if (!actualPath.equals(path)) {
                wrongPath.handle(exchange);
            } else {
                ok.handle(exchange);
            }
        }

    }

    public static class RefreshHandler implements MockThemis.Handler {

        private boolean ok;
        private final String message;

        public RefreshHandler(String message) {
            this.message = message;
        }

        @Override
        public MockThemis.Response getResponse(HttpExchange exchange) {
            ok = true;
            return response(200, message);
        }

        public boolean isOk() {
            return ok;
        }

    }

    public static class ReportHandler implements Handler {

        private final List<List<FileItem>> fileItems = new ArrayList<>();

        @Override
        public Response getResponse(HttpExchange exchange) {
            FileItemFactory fileItemFactory = new TestFileItemFactory();
            FileUpload fileUpload = new FileUpload(fileItemFactory);
            try {
                fileItems.add(fileUpload.parseRequest(new TestRequestContext(exchange)));
            } catch (FileUploadException e) {
                return response(500, e.getMessage());
            }
            return response(200, "");
        }

        public List<List<FileItem>> getFileItems() {
            return fileItems;
        }

    }

    private static class TestRequestContext implements RequestContext {

        final String contentType;
        final InputStream inputStream;

        TestRequestContext(HttpExchange exchange) {
            this.contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            this.inputStream = exchange.getRequestBody();
        }

        @Override
        public String getCharacterEncoding() {
            return "UTF-8";
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public int getContentLength() {
            return 0;
        }

    }

    private static class TestFileItem implements FileItem {

        private String fieldName;
        private final String contenType;
        private final String filename;
        private boolean isFormField;
        private FileItemHeaders headers;
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        private TestFileItem(String fieldName, String contenType, boolean isFormField, String filename) {
            this.fieldName = fieldName;
            this.contenType = contenType;
            this.filename = filename;
            this.isFormField = isFormField;
        }

        @Override
        public String getFieldName() {
            return fieldName;
        }

        @Override
        public void setFieldName(String name) {
            this.fieldName = fieldName;
        }

        @Override
        public String getContentType() {
            return contenType;
        }

        @Override
        public boolean isFormField() {
            return isFormField;
        }

        @Override
        public void setFormField(boolean state) {
            this.isFormField = state;
        }

        @Override
        public String getName() {
            return filename;
        }

        @Override
        public FileItemHeaders getHeaders() {
            return headers;
        }

        @Override
        public void setHeaders(FileItemHeaders headers) {
            this.headers = headers;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(outputStream.toByteArray());
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return outputStream;
        }

        @Override
        public boolean isInMemory() {
            return true;
        }

        @Override
        public long getSize() {
            return outputStream.size();
        }

        @Override
        public byte[] get() {
            return outputStream.toByteArray();
        }

        @Override
        public String getString(String encoding) throws UnsupportedEncodingException {
            return new String(get(), encoding);
        }

        @Override
        public String getString() {
            return new String(get());
        }

        @Override
        public void write(File file) throws Exception {
        }

        @Override
        public void delete() {
        }

    }

    private static class TestFileItemFactory implements FileItemFactory {

        @Override
        public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
            return new TestFileItem(fieldName, contentType, isFormField, fileName);
        }

    }

}
