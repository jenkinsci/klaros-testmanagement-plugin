/*
 * The MIT License
 *
 * Copyright (c) 2009, verit Informationssysteme GmbH, Caroline Albuquerque, Torsten Stolpmann
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.klaros;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Klaros test result publisher class. When a publish is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be
 * invoked.
 *
 * @author Caroline Albuquerque (albuquerque@verit.de)
 * @author Torsten Stolpmann (stolpmann@verit.de)
 */
public class KlarosTestResultPublisher extends Recorder {

    private String config;
    private String env;
    private String sut;
    private String type = "junit";
    private String pathTestResults;
    private String url;

    /**
     * Instantiates a new klaros test result publisher.
     *
     * @param config
     *            the Klaros project configuration to use
     * @param env
     *            the Klaros test environment to use
     * @param sut
     *            the Klaros system under test to use
     * @param type
     *            the type of test result to import
     * @param pathTestResults
     *            the path to the test results
     * @param url
     *            the Klaros application url
     */
    @DataBoundConstructor
    public KlarosTestResultPublisher(final String config, final String env,
            final String sut, final String type, final String pathTestResults,
            final String url) {

        this.config = config;
        this.env = env;
        this.sut = sut;
        this.pathTestResults = pathTestResults;
        this.url = url;
    }

    /**
     * Descriptor.
     *
     * @return the descriptor implementation
     */
    public static DescriptorImpl descriptor() {
        return Hudson.getInstance().getDescriptorByType(
                KlarosTestResultPublisher.DescriptorImpl.class);
    }

    public String getConfig() {

        return config;
    }

    public void setConfig(final String config) {

        this.config = config;
    }

    public String getEnv() {

        return env;
    }

    public void setEnv(final String env) {

        this.env = env;
    }

    public String getUrl() {

        return url;
    }

    public void setUrl(final String url) {

        this.url = url;
    }

    public String getSut() {

        return sut;
    }

    public void setSut(final String sut) {

        this.sut = sut;
    }

    public String getPathTestResults() {

        return pathTestResults;
    }

    public void setPathTestResults(final String pathTestResults) {

        this.pathTestResults = pathTestResults;
    }

    /**
     * Gets the urls.
     *
     * @return the urls
     */
    public List<String> getUrls() {

        return descriptor().getUrls();
    }

    /**
     * Runs the step over the given build and reports the progress to the
     * listener.
     *
     * @param build
     *            the current build
     * @param launcher
     *            the launcher
     * @param listener
     *            the listener
     *
     * @return null
     */
    @Override
    public boolean perform(final AbstractBuild<?, ?> build,
            final Launcher launcher, final BuildListener listener) {

        final boolean result;

        if (pathTestResults == null) {
            listener.getLogger().println("There are no test result to import!");
            result = false;
        } else {
            listener.getLogger().println(
                    "The test result(s) contained in target " + pathTestResults
                            + " will be exported to the "
                            + "Klaros-Testmanagement Server at " + getUrl(url)
                            + ".");
            listener.getLogger().println(
                    "With parameters Project[" + config + "], Environment["
                            + env + "], SUT[" + sut + "] and Type[" + type
                            + "].");

            FilePath ws = build.getWorkspace();
            if (ws == null) {
                listener.error("No workspace defined!");
                build.setResult(Result.FAILURE);
                result = false;
            } else {

                try {
                    FileCallableImplementation exporter = new FileCallableImplementation(
                            listener);
                    ws.act(exporter);

                } catch (IOException e) {
                    listener.getLogger().println(
                            "Failure to export test result(s).");
                    e.printStackTrace(listener.getLogger());
                } catch (InterruptedException e) {
                    listener.getLogger().println(
                            "Failure to export test result(s).");
                    e.printStackTrace(listener.getLogger());
                }

                listener.getLogger().println(
                        "Test result(s) successfully exported.");

                result = true;
            }
        }
        return result;
    }

    /**
     * Gets the URL of the given name, or returns null.
     *
     * @param sourceURL
     *            the URL of the klaros server import servlet
     * @return the klaros server import servlet URL
     */
    public String getUrl(final String sourceURL) {

        String result = null;

        if (sourceURL == null) {
            // if only one URL is configured, "default URL" should mean that
            // URL.
            List<String> urls = descriptor().getUrls();
            if (urls.size() >= 1) {
                result = urls.get(0);
            }
            return result;
        }
        for (String j : descriptor().getUrls()) {
            if (j.equals(sourceURL)) {
                result = j;
                break;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public BuildStepMonitor getRequiredMonitorService() {

        return BuildStepMonitor.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {

        return descriptor();
    }

    /**
     * The Class FileCallableImplementation.
     */
    private final class FileCallableImplementation implements
            FileCallable<List<Integer>> {

        /** The serial version UID. */
        private static final long serialVersionUID = 1560913900801548965L;

        private final BuildListener listener;

        private FileCallableImplementation(final BuildListener listener) {
            this.listener = listener;
        }

        /**
         * Invoke.
         *
         * @param baseDir
         *            the base directory
         * @param channel
         *            the channel
         *
         * @return the list of http return codes
         *
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         *
         * @see hudson.FilePath.FileCallable#invoke(File,
         *      hudson.remoting.VirtualChannel)
         */
        public List<Integer> invoke(final File baseDir,
                final VirtualChannel channel) throws IOException {

            List<Integer> results = new ArrayList<Integer>();

            FileSet src = Util.createFileSet(baseDir, pathTestResults);
            DirectoryScanner ds = src.getDirectoryScanner();
            ds.scan();
            if (ds.getIncludedFilesCount() == 0) {
                listener.getLogger().println("No exportable files found");
                return results;
            }

            // Get target URL
            String targetUrl = getUrl(url);
            if (targetUrl != null) {
                String strURL = new StringBuffer(targetUrl).append(
                        "/seam/resource/rest/importer").toString();

                // Prepare HTTP PUT
                for (String f : ds.getIncludedFiles()) {
                    PutMethod put = new PutMethod(strURL);
                    String query = "config=" + config + "&env=" + env + "&sut="
                            + sut + "&type=" + type;
                    put.setQueryString(query);

                    File file = new File(baseDir, f);
                    int result;
                    try {
                        result = executePut(file, put);
                        if (result != HttpServletResponse.SC_OK) {
                            StringBuffer msg = new StringBuffer().append(
                                    "Export of ").append(file.getName())
                                    .append(" failed - Response status code: ")
                                    .append(result)
                                    .append(" for request URL: ")
                                    .append(strURL).append("?").append(query);
                            String response = put.getResponseBodyAsString();
                            if (response != null && response.length() > 0) {
                                msg.append("Reason: ").append(response);
                            }
                            listener.getLogger().println(msg.toString());
                        } else {
                            results.add(result);
                            listener
                                    .getLogger()
                                    .println(
                                            "Test result file "
                                                    + file.getName()
                                                    + " has been successfully exported.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace(listener.getLogger());
                    }
                }
            } else {
                listener.getLogger().println(
                        url + ": unable to locate this Klaros URL");
            }
            return results;
        }

        /**
         * Execute the method post.
         *
         * @param file
         *            the file to put
         * @param put
         *            the put method to execute
         *
         * @throws IOException
         *             Signals that an I/O exception of some sort has occurred.
         * @throws HttpException
         *             Signals that an HTTP exception of some sort has occurred.
         *
         * @return The return code of the put method execution.
         */
        private int executePut(final File file, final PutMethod put)
                throws HttpException, IOException {

            // Request content will be retrieved directly
            // from the input stream
            int result = 0;
            RequestEntity entity = new FileRequestEntity(file,
                    "text/xml; charset=ISO-8859-1");
            put.setRequestEntity(entity);
            // Get HTTP client
            HttpClient httpclient = new HttpClient();

            // Execute request
            try {
                result = httpclient.executeMethod(put);

            } finally {
                // Release current connection to the connection pool once you
                // are
                // done
                put.releaseConnection();
            }
            return result;

        }

    }

    /**
     * Descriptor for KlarosImportPublisher class. Used as a singleton. The
     * class is marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Publisher> {

        private static final String PROJECT_CONFIG_HTML = //
        "/plugin/klaros-testmanagement/help-projectConfig.html";
        private static final String URL_NAME = "url.name";
        /**
         * Global configuration information.
         */

        private List<String> urls = new ArrayList<String>();

        public DescriptorImpl() {

            load();
        }

        /**
         * This human readable name is used in the configuration screen.
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {

            return Messages.DisplayName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean configure(final StaplerRequest req, final JSONObject json)
                throws hudson.model.Descriptor.FormException {

            urls.clear();
            if (req.getParameterValues(URL_NAME) != null) {
                for (int i = 0; i < req.getParameterValues(URL_NAME).length; i++) {
                    urls.add(req.getParameterValues(URL_NAME)[i]);
                    save();
                }
            }

            return super.configure(req, json);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isApplicable(
                final Class<? extends AbstractProject> jobType) {
            return true; // for all types
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getHelpFile() {

            return PROJECT_CONFIG_HTML;
        }

        public List<String> getUrls() {

            return urls;
        }

        /**
         * Sets the URLs.
         *
         * @param setUrls
         *            the new URLs
         */
        public void setUrls(final List<String> setUrls) {

            urls.clear();
            for (String url : setUrls) {
                urls.add(url);
            }
        }

        /**
         * Performs on-the-fly validation on a Klaros application URL.
         *
         * @param value
         *            the url value to check
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         * @throws ServletException
         *             the servlet exception
         *
         * @return the form validation result
         */
        public FormValidation doCheckUrl(final String value) throws IOException,
                ServletException {

            return new FormValidation.URLCheck() {

                @Override
                protected FormValidation check() throws IOException,
                        ServletException {

                    String cooked = Util.fixEmpty(value);
                    if (cooked == null) { // nothing entered yet
                        return FormValidation.ok();
                    }

                    if (!value.endsWith("/")) {
                        cooked += '/';
                    }

                    FormValidation result = FormValidation.ok();
                    try {
                        if (findText(open(new URL(cooked)), "Klaros")) {
                            result = FormValidation.ok();
                        } else {
                            result = FormValidation.error( //
                                    "This is a valid URL but it doesn't look like Klaros-Testmanagement");
                        }
                    } catch (IOException e) {
                        result = handleIOException(value, e);
                    }
                    return result;
                }
            }.check();
        }

        /**
         * Performs on-the-fly validation on the file mask wildcard.
         *
         * @param project
         *            the current project
         * @param value
         *            the mask value to check
         *
         * @return the form validation result
         *
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         * @throws ServletException
         *             the servlet exception
         */
        public FormValidation doCheck(
                @AncestorInPath final AbstractProject<?, ?> project,
                @QueryParameter final String value) throws IOException,
                ServletException {

            FilePath ws = project.getSomeWorkspace();
            return ws != null ? ws.validateFileMask(value, false)
                    : FormValidation.ok();
        }

        /**
         * Performs on-the-fly validation on the server installation.
         *
         * @param value
         *            the mask value to check
         *
         * @return the form validation result
         *
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         * @throws ServletException
         *             the servlet exception
         */
        public FormValidation doCheckInstallation(
                @QueryParameter final String value) throws IOException,
                ServletException {

            if (Util.fixEmpty(value) != null) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.ErrorMissingInstallation());
            }
        }

    }

}