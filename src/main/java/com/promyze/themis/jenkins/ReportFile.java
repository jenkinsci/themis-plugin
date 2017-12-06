package com.promyze.themis.jenkins;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.promyze.themis.jenkins.FormValidationUtils.checkNotNullOrEmpty;

public class ReportFile extends AbstractDescribableImpl<ReportFile> implements Serializable {

    private static final String[] SUPPORTED_TYPES = {
            "Cobertura",
            "ReSharper",
            "PMD",
            "Checkstyle"
    };

    private static final long serialVersionUID = 1L;

    private final String type;
    private final String path;

    @DataBoundConstructor
    public ReportFile(String type, String path) {
        this.type = type;
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    @Extension
    public static class ReportFileDesscriptor extends Descriptor<ReportFile> {

        @Override
        public String getDisplayName() {
            return "";
        }

        public ListBoxModel doFillTypeItems(@QueryParameter String type) {
            return new ListBoxModel(Arrays.stream(SUPPORTED_TYPES)
                                            .map(t -> new ListBoxModel.Option(t,
                                                                              t.toLowerCase(),
                                                                              t.equalsIgnoreCase(type)))
                                            .collect(Collectors.toList()));
        }

        public FormValidation doCheckPath(@QueryParameter String path) {
            try {
                checkNotNullOrEmpty(path, Messages.pathIsRequired());
                return FormValidation.ok();
            } catch (FormValidation formValidation) {
                return formValidation;
            }
        }

    }

}
