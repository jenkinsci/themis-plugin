package com.promyze.themis.jenkins.test;

import com.promyze.themis.jenkins.test.MockThemis.RefreshHandler;
import com.promyze.themis.jenkins.test.MockThemis.ReportHandler;
import org.apache.commons.fileupload.FileItem;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

public class ThemisAssertions {

    public static RefreshHandlerAssert assertThat(RefreshHandler handler) {
        return new RefreshHandlerAssert(handler);
    }

    public static ReportHandlerAssert assertThat(ReportHandler handler) {
        return new ReportHandlerAssert(handler);
    }

    public static class RefreshHandlerAssert extends AbstractAssert<RefreshHandlerAssert, RefreshHandler> {

        RefreshHandlerAssert(RefreshHandler refreshHandler) {
            super(refreshHandler, RefreshHandlerAssert.class);
        }

        public void isOK() {
            isNotNull();
            if (!actual.isOk()) {
                failWithMessage("Refresh request failed");
            }
        }

        public void isKO() {
            isNotNull();
            if (actual.isOk()) {
                failWithMessage("Refresh request failed");
            }
        }

    }

    public static class ReportHandlerAssert extends AbstractAssert<ReportHandlerAssert, ReportHandler> {

        ReportHandlerAssert(ReportHandler reportHandler) {
            super(reportHandler, ReportHandlerAssert.class);
        }

        public void hasRequest(String type, JSONObject expectedMetadata) throws UnsupportedEncodingException {
            for (List<FileItem> fileItems : actual.getFileItems()) {
                Optional<FileItem> metadata = fileItems.stream()
                        .filter(f -> f.getFieldName().equals("metadata"))
                        .findAny();
                if (metadata.isPresent()) {
                    JSONObject actualMetadata = new JSONObject(metadata.get().getString("UTF-8"));
                    if (type.equals(actualMetadata.get("dataType"))) {
                        checkArchive(type, fileItems);
                        Assertions.assertThat(actualMetadata)
                                .as("Check metadata for type %s", type)
                                .isEqualToComparingFieldByFieldRecursively(expectedMetadata);
                    }
                    return;
                }
            }
            failWithMessage("No request for type %s", type);
        }

        private void checkArchive(String type, List<FileItem> fileItems) {
            Optional<FileItem> archive = fileItems.stream()
                    .filter(f -> f.getFieldName().equals("archive"))
                    .findAny();
            Assertions.assertThat(archive).as("Check that").isPresent();
            String contentType = archive.get().getContentType();
            Assertions.assertThat(contentType).as("Check archive content-type").isEqualTo("application/zip");
        }

        public void hasReports(int nbReports) {
            int actualNbReports = actual.getFileItems().size();
            if (actualNbReports != nbReports) {
                failWithMessage("Expected <%d> reports but actual was <%d>", nbReports, actualNbReports);
            }
        }

    }

}
