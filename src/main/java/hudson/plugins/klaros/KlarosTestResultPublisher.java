/*
 * The MIT License
 *
 * Copyright (c) 2009,2010,2015 verit Informationssysteme GmbH, Caroline Albuquerque, Torsten Stolpmann
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

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.Secret;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Klaros-Testmanagement test result publisher class. When a publish is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be invoked.
 *
 * @author Caroline Albuquerque (albuquerque@verit.de)
 * @author Torsten Stolpmann (stolpmann@verit.de)
 */
public class KlarosTestResultPublisher extends Recorder implements Serializable {

    private static final long serialVersionUID = -3220438013049857329L;

    private static final ArrayList<ResultFormat> DEFAULT_FORMATS;

    static {
        DEFAULT_FORMATS = new ArrayList<>();
        DEFAULT_FORMATS.add(new ResultFormat("aunit", "AUnit"));
        DEFAULT_FORMATS.add(new ResultFormat("boosttest", "Boost Test"));
        DEFAULT_FORMATS.add(new ResultFormat("check", "Check"));
        DEFAULT_FORMATS.add(new ResultFormat("cpptestunit", "UnitTest++"));
        DEFAULT_FORMATS.add(new ResultFormat("cppunit", "CppUnit"));
        DEFAULT_FORMATS.add(new ResultFormat("ctest", "ctest"));
        DEFAULT_FORMATS.add(new ResultFormat("cunit", "CUnit"));
        DEFAULT_FORMATS.add(new ResultFormat("embunit", "EmbUnit"));
        DEFAULT_FORMATS.add(new ResultFormat("fpcunit", "Free Pascal Unit"));
        DEFAULT_FORMATS.add(new ResultFormat("googletest", "GoogleTest"));
        DEFAULT_FORMATS.add(new ResultFormat("gtester", "GLib/gtester"));
        DEFAULT_FORMATS.add(new ResultFormat("jbehave", "JBehave"));
        DEFAULT_FORMATS.add(new ResultFormat("jsunit", "JsUnit"));
        DEFAULT_FORMATS.add(new ResultFormat("jubula", "Jubula/GUIDancer"));
        DEFAULT_FORMATS.add(new ResultFormat("junit", "JUnit"));
        DEFAULT_FORMATS.add(new ResultFormat("mbunit", "MbUnit"));
        DEFAULT_FORMATS.add(new ResultFormat("mstest", "MSTest"));
        DEFAULT_FORMATS.add(new ResultFormat("nunit", "NUnit"));
        DEFAULT_FORMATS.add(new ResultFormat("phpunit", "PHPUnit"));
        DEFAULT_FORMATS.add(new ResultFormat("qftest", "QFTest"));
        DEFAULT_FORMATS.add(new ResultFormat("qtestlib", "QTestLib"));
        DEFAULT_FORMATS.add(new ResultFormat("ranorex", "Ranorex"));
        DEFAULT_FORMATS.add(new ResultFormat("tessy", "TESSY"));
        DEFAULT_FORMATS.add(new ResultFormat("testcomplete", "Test Complete"));
        DEFAULT_FORMATS.add(new ResultFormat("tusar", "Tusar"));
        DEFAULT_FORMATS.add(new ResultFormat("unittest", "UnitTest"));
        DEFAULT_FORMATS.add(new ResultFormat("valgrind", "Valgrind"));
        DEFAULT_FORMATS.add(new ResultFormat("xunitdotnet", "xUnit.net"));
    }

    /** The Klaros project id. */
    private String config;

    /** The Klaros iteration id. */
    private String iteration;

    /** The Klaros test environment id. */
    private String env;

    /** The Klaros system under test id. */
    private String sut;

    /** The type. */
    private String type;

    /**
     * The path test results.
     *
     * @deprecated since 1.5
     */
    @Deprecated
    private String pathTestResults;

    /** The test result sets. */
    private ResultSet[] resultSets = new ResultSet[0];

    /** The Klaros URL to connect to. */
    private String url;

    /** The username used to authenticate with Klaros. */
    private String username;

    /** The password used to authenticate with Klaros. */
    private Secret password;

    /** The create test suite flag. */
    private boolean createTestSuite;

    private ResultFormat[] types;

    /**
     * Instantiates a new Klaros test result publisher.
     *
     * @param config the Klaros project configuration id to use
     * @param iteration the optional Klaros iteration id to use
     * @param env the Klaros test environment id to use
     * @param sut the Klaros system under test id to use
     * @param createTestSuite if true create a test suite automatically
     * @param type the type of test result to import
     * @param pathTestResults the path to the test results
     * @param resultSets the test result sets
     * @param url the Klaros application url
     * @param username the optional Klaros login user name
     * @param password the optional Klaros login password
     */
    @DataBoundConstructor
    public KlarosTestResultPublisher(final String config, final String iteration, final String env,
            final String sut, final boolean createTestSuite, final String type, final String pathTestResults,
            final ResultSet[] resultSets, final String url, final String username, final String password) {

        this.config = config;
        this.iteration = iteration;
        this.env = env;
        this.sut = sut;
        this.createTestSuite = createTestSuite;
        this.resultSets = resultSets != null ? resultSets.clone() : null;
        migratePathTestResults();
        this.url = url;
        this.username = username;
        this.password = Secret.fromString(password);
        this.type = type;
        this.types = null;
    }

    /**
     * Load formats from the remote application.
     *
     * @return the supported result formats
     */
    private ResultFormat[] loadFormats() {

        final String strURL = buildServletURL(url) + "/supportedFormats";

        final GetMethod get = new GetMethod(strURL);
        final StringBuilder query = new StringBuilder();
        if (StringUtils.isNotEmpty(username)) {
            query.append("username=").append(username).append("&password=").append(password);
        }
        get.setQueryString(query.toString());

        try {
            final HttpClient client = new HttpClient();
            final int result = client.executeMethod(get);
            if (result == HttpServletResponse.SC_OK) {
                final String response = get.getResponseBodyAsString();
                if (StringUtils.isNotBlank(response)) {
                    final List<ResultFormat> formats = extractSupportedFormats(response);
                    return formats.toArray(new ResultFormat[formats.size()]);
                }
            }
        } catch (RuntimeException | IOException e) {
            // ignore
        } finally {
            get.releaseConnection();
        }
        return DEFAULT_FORMATS.toArray(new ResultFormat[DEFAULT_FORMATS.size()]);
   }

    /**
     * Extract supported formats.
     *
     * @param content the content as a string
     * @return the list of supported formats
     */
    private List<ResultFormat> extractSupportedFormats(String content) {

        String[] lines = content.split("\n");
        List<ResultFormat> formats = new ArrayList<>(lines.length);
        for (String line : lines) {
            if (line.contains("=")) {
                String id = line.substring(0, line.lastIndexOf('='));
                String name = line.substring(line.lastIndexOf('=') + 1);
                formats.add(new ResultFormat(id, name));
            }
        }
        return formats;
    }

    /**
     * Descriptor.
     *
     * @return the descriptor implementation
     */
    public DescriptorImpl descriptor() {

        return getJenkinsInstance().getDescriptorByType(KlarosTestResultPublisher.DescriptorImpl.class);
    }

    /**
     * Gets the Jenkins instance or die trying.
     *
     * @return the Jenkins instance
     */
    private Jenkins getJenkinsInstance() {

        if (Jenkins.getInstance() != null) {
            return Jenkins.getInstance();
        } else {
            throw new RuntimeException("Jenkins instance not yet initialized");
        }
    }

    /**
     * Gets the Klaros project id.
     *
     * @return the project id
     */
    public String getConfig() {

        return config;
    }

    /**
     * Sets the Klaros project id.
     *
     * @param value the new project id
     */
    public void setConfig(final String value) {

        config = StringUtils.trim(value);
    }

    /**
     * Gets the Klaros iteration id.
     *
     * @return the iteration id
     */
    public String getIteration() {

        return iteration;
    }

    /**
     * Sets the Klaros iteration id.
     *
     * @param iteration the new iteration id
     */
    public void setIteration(final String iteration) {

        this.iteration = iteration;
    }

    /**
     * Gets the Klaros test environment id.
     *
     * @return the test environment id
     */
    public String getEnv() {

        return env;
    }

    /**
     * Sets the Klaros test environment id.
     *
     * @param value the new test environment id
     */
    public void setEnv(final String value) {

        env = StringUtils.trim(value);
    }

    /**
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {

        return url;
    }

    /**
     * Sets the url.
     *
     * @param value the new url
     */
    public void setUrl(final String value) {

        url = StringUtils.trim(value);
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {

        return username;
    }

    /**
     * Sets the username.
     *
     * @param value the new username
     */
    public void setUsername(final String value) {

        username = StringUtils.trim(value);
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {

        return password.getEncryptedValue();
    }

    /**
     * Sets the password.
     *
     * @param value the new password
     */
    public void setPassword(final String value) {

        password = Secret.fromString(value);
    }

    /**
     * Gets the Klaros system under test id.
     *
     * @return the system under test id
     */
    public String getSut() {

        return sut;
    }

    /**
     * Sets the Klaros system under test id.
     *
     * @param value the new system under test id
     */
    public void setSut(final String value) {

        sut = StringUtils.trim(value);
    }

    /**
     * Checks if the create test suite flag is set.
     *
     * @return true, if set
     */
    public boolean isCreateTestSuite() {

        return createTestSuite;
    }

    /**
     * Sets the create test suite flag.
     *
     * @param createTestSuite the new create test suite flag
     */
    public void setCreateTestSuite(boolean createTestSuite) {

        this.createTestSuite = createTestSuite;
    }

    /**
     * Gets the valid result types.
     *
     * @return the valid result types
     */
    public ResultFormat[] getTypes() {

        if (types == null) {
            types = loadFormats();
        }
        return types.clone();
    }

    /**
     * Sets the valid result types.
     *
     * @param types the new result types
     */
    public void setTypes(ResultFormat[] types) {

        this.types = types != null ? types.clone() : null;;
    }

    /**
     * Gets the path test results.
     *
     * @return the path test results
     * @deprecated use getResultSets() instead
     */
    @Deprecated
    public String getPathTestResults() {

        return pathTestResults;
    }

    /**
     * Sets the path test results.
     *
     * @param value the new path test results
     * @deprecated use setResultSets() instead
     */
    @Deprecated
    public void setPathTestResults(final String value) {

        migratePathTestResults();
    }

    /**
     * Migrate deprecated path test results setting.
     */
    private void migratePathTestResults() {

        if (StringUtils.isNotEmpty(pathTestResults)) {
            resultSets = new ResultSet[]{new ResultSet(StringUtils.trim(pathTestResults), "") };
            pathTestResults = null;
        }
    }

    /**
     * Gets the result sets.
     *
     * @return the result sets
     */
    public ResultSet[] getResultSets() {

        migratePathTestResults();
        return resultSets != null ? resultSets.clone() : null;
    }

    /**
     * Sets the result sets.
     *
     * @param values the new result sets
     */
    public void setResultSets(ResultSet[] values) {

        resultSets = values != null ? values.clone() : null;
    }

    /**
     * Gets the urls.
     *
     * @return the urls
     */
    public List<String> getUrls() {

        return descriptor().getUrls();
    }

    public String getKlarosUrl(final String sourceURL) {

        String result = null;

        if (sourceURL == null) {
            // if only one URL is configured, "default URL" should mean that URL.
            List<String> urls = descriptor().getUrls();
            if (!urls.isEmpty()) {
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
     * Runs the step over the given build and reports the progress to the listener.
     *
     * @param build the current build
     * @param launcher the launcher
     * @param listener the listener
     * @return null
     */
    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher,
        final BuildListener listener) {

        boolean result = false;
        if (Result.SUCCESS.equals(build.getResult()) || Result.UNSTABLE.equals(build.getResult())) {

            FilePath ws = build.getWorkspace();
            if (ws == null) {
                listener.error("No workspace defined!");
                build.setResult(Result.FAILURE);
                result = false;
            } else {
                for (ResultSet resultSet : getResultSets()) {
                    if (StringUtils.isEmpty(resultSet.getSpec())) {
                        listener.getLogger().println("Empty result spec implementation detected");
                    } else {
                        listener.getLogger().println(
                            "The test result(s) contained in target " + resultSet.getSpec()
                                + " will be exported to the " + "Klaros-Testmanagement Server at "
                                + getUrl(url) + " using the " + resultSet.getFormat() + " format.");
                        listener.getLogger().print("With parameters Project[" + config + "]");
                        if (StringUtils.isNotBlank(iteration)) {
                            listener.getLogger().print(" Iteration[" + iteration + "]");
                        }
                        listener.getLogger().println(
                            " Environment[" + env + "], SUT[" + sut + "] and Type[" + type + "].");

                        try {
                            FileCallableImplementation exporter =
                                new FileCallableImplementation(getJenkinsInstance().getRootUrl(), build
                                    .getProject().getName(), build.getNumber(), build
                                    .getEnvironment(listener), build.getBuildVariables(), listener);
                            exporter.setKlarosUrl(getKlarosUrl(url));
                            exporter.setResultSet(resultSet);
                            exporter.setConfig(config);
                            exporter.setIteration(iteration);
                            exporter.setSut(sut);
                            exporter.setEnv(env);
                            exporter.setUsername(username);
                            exporter.setPassword(password.getPlainText());
                            exporter.setCreateTestSuite(createTestSuite);
                            ws.act(exporter);

                        } catch (IOException e) {
                            listener.getLogger().println("Failure to export test result(s).");
                            e.printStackTrace(listener.getLogger());
                        } catch (InterruptedException e) {
                            listener.getLogger().println("Failure to export test result(s).");
                            e.printStackTrace(listener.getLogger());
                        } catch (RuntimeException e) {
                            listener.getLogger().println("Failure to export test result(s).");
                            e.printStackTrace(listener.getLogger());
                        }

                        listener.getLogger().println("Test result(s) successfully exported.");

                        result = true;
                    }
                }
            }
        } else {
            listener.getLogger().println(
                "Skipping export of test results to Klaros-Testmangement due to build status");
            result = true;
        }
        return result;
    }

    /**
     * Gets the URL of the given name, or returns null.
     *
     * @param sourceURL the URL of the klaros server import servlet
     * @return the klaros server import servlet URL
     */
    public String getUrl(final String sourceURL) {

        String result = null;

        if (sourceURL == null) {
            // if only one URL is configured, "default URL" should mean that
            // URL.
            List<String> urls = descriptor().getUrls();
            if (!urls.isEmpty()) {
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

    @Override
    public BuildStepMonitor getRequiredMonitorService() {

        return BuildStepMonitor.NONE;
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {

        return descriptor();
    }

    /**
     * Builds the servlet url. Try to honor URL's with trailing slashes.
     *
     * @param applicationURL the application url
     * @return the servlet url
     */
    private static String buildServletURL(final String applicationURL) {

        final String result;
        if (applicationURL.endsWith("/")) {
            result =
                new StringBuffer(applicationURL.substring(0, applicationURL.length() - 1)).append(
                    "/seam/resource/rest/importer").toString();
        } else {
            result = new StringBuffer(applicationURL).append("/seam/resource/rest/importer").toString();
        }
        return result;
    }

    /**
     * The Class FileCallableImplementation.
     */
    private static class FileCallableImplementation implements FileCallable<List<Integer>>, Serializable {

        private static final long serialVersionUID = 1560913900801548965L;

        private final BuildListener listener;
        final EnvVars environment;
        final Map<String, String> buildVariables;

        private String buildServerUrl;
        private String buildJobId;
        private String buildId;

        private String klarosUrl;
        private ResultSet resultSet;
        private String config;
        private String iteration;
        private String env;
        private String sut;
        private String username;
        private Secret password;
        private boolean createTestSuite;

        /**
         * Instantiates a new file callable implementation.
         *
         * @param listener the build listener
         */
        private FileCallableImplementation(final String buildServerUrl, final String buildJobId,
                final int buildNumber, final EnvVars environment, final Map<String, String> buildVariables,
                final BuildListener listener) {

            this.buildServerUrl = buildServerUrl;
            this.buildJobId = buildJobId;
            this.buildId = Integer.toString(buildNumber);
            this.environment = environment;
            this.buildVariables = buildVariables;
            this.listener = listener;
        }

        /**
         * Invoke the build publisher.
         *
         * @param baseDir the base directory
         * @param channel the channel
         * @return the list of http return codes
         * @throws IOException Signals that an I/O exception has occurred.
         * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise paused for a
         *             long time and another thread interrupts it using the interrupt method in class Thread.
         * @see hudson.FilePath.FileCallable#invoke(File, hudson.remoting.VirtualChannel)
         */
        @Override
        public List<Integer> invoke(final File baseDir, final VirtualChannel channel) throws IOException,
            InterruptedException {

            List<Integer> results = new ArrayList<Integer>();

            FileSet src = Util.createFileSet(baseDir, resultSet.getSpec());
            DirectoryScanner ds = src.getDirectoryScanner();
            ds.scan();
            if (ds.getIncludedFilesCount() == 0) {
                listener.getLogger().println("No exportable files found");
                return results;
            }

            // Get target URL
            String targetUrl = klarosUrl;

            if (targetUrl != null) {
                String strURL = buildServletURL(targetUrl);

                // Get HTTP client
                HttpClient httpclient = new HttpClient();

                // Prepare HTTP PUT
                for (String f : ds.getIncludedFiles()) {
                    final PutMethod put = new PutMethod(strURL);
                    final StringBuilder query =
                        new StringBuilder("config=").append(expandVariables(config, environment,
                            buildVariables));
                    if (StringUtils.isNotBlank(iteration)) {
                        query.append("&iteration=").append(
                            expandVariables(iteration, environment, buildVariables));
                    }
                    query.append("&env=").append(expandVariables(env, environment, buildVariables)).append(
                        "&sut=").append(expandVariables(sut, environment, buildVariables)).append("&type=")
                        .append(expandVariables(resultSet.getFormat(), environment, buildVariables));
                    if (createTestSuite) {
                        query.append("&createTestSuiteResults=true");
                    }

                    query.append("&buildServerUrl=").append(buildServerUrl);
                    query.append("&buildJobId=").append(buildJobId);
                    query.append("&buildId=").append(buildId);

                    if (StringUtils.isNotBlank(username)) {
                        query.append("&username=").append(
                            expandVariables(username, environment, buildVariables)).append("&password=")
                            .append(expandVariables(password.getPlainText(), environment, buildVariables));
                    }
                    put.setQueryString(query.toString());

                    File file = new File(baseDir, f);
                    int result;

                    RequestEntity entity = new FileRequestEntity(file, "text/xml; charset=ISO-8859-1");
                    put.setRequestEntity(entity);

                    // Execute request
                    try {
                        result = httpclient.executeMethod(put);

                        if (result != HttpServletResponse.SC_OK) {
                            StringBuilder msg =
                                new StringBuilder().append("Export of ").append(file.getName()).append(
                                    " failed - Response status code: ").append(result).append(
                                    " for request URL: ").append(strURL).append("?").append(query);
                            String response = put.getResponseBodyAsString();
                            if (response != null && response.length() > 0) {
                                msg.append("\nReason: ").append(response);
                            }
                            listener.getLogger().println(msg.toString());
                        } else {
                            results.add(result);
                            listener.getLogger().println(
                                "Test result file " + file.getName() + " has been successfully exported.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace(listener.getLogger());
                    } finally {
                        // Release current connection to the connection pool once you are done
                        put.releaseConnection();
                    }
                }
            } else {
                listener.getLogger().println(klarosUrl + ": unable to locate this Klaros URL");
            }
            return results;
        }

        /**
         * Expand build environment variables.
         *
         * @param value the value
         * @param environment the environment variables
         * @param buildVariables the build variables
         * @return the expanded string, if applicable
         * @throws IOException Signals that an I/O exception has occurred.
         * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise paused for a
         *             long time and another thread interrupts it using the interrupt method in class Thread.
         */
        private String expandVariables(final String value, final EnvVars environment,
            final Map<String, String> buildVariables) throws IOException, InterruptedException {

            String result = value;
            if (result != null) {
                for (Entry<String, String> entry : environment.entrySet()) {
                    result = result.replaceAll("\\$\\{" + entry.getKey() + "\\}", entry.getValue());
                }
                for (Entry<String, String> entry : buildVariables.entrySet()) {
                    result = result.replaceAll("\\$\\{" + entry.getKey() + "\\}", entry.getValue());
                }
            }
            return result;
        }

        /**
         * Sets the result set to deliver the results from.
         *
         * @param value the new result set
         */
        private void setResultSet(final ResultSet value) {

            resultSet = value;
        }

        /**
         * Sets the Klaros url to deliver the results to.
         *
         * @param value the new klaros url
         */
        private void setKlarosUrl(final String value) {

            klarosUrl = value;
        }

        /**
         * Sets the Klaros project id.
         *
         * @param value the new project id
         */
        private void setConfig(final String value) {

            config = StringUtils.trim(value);
        }

        /**
         * Sets the Klaros iteration id.
         *
         * @param iteration the new iteration id
         */
        private void setIteration(final String iteration) {

            this.iteration = iteration;
        }

        /**
         * Sets the Klaros test environment id.
         *
         * @param value the new test environment id
         */
        private void setEnv(final String value) {

            env = StringUtils.trim(value);
        }

        /**
         * Sets the username.
         *
         * @param value the new username
         */
        private void setUsername(final String value) {

            username = StringUtils.trim(value);
        }

        /**
         * Sets the password.
         *
         * @param value the new password
         */
        private void setPassword(final String value) {

            password = Secret.fromString(value);
        }

        /**
         * Sets the Klaros system under test id.
         *
         * @param value the new system under test id
         */
        private void setSut(final String value) {

            sut = StringUtils.trim(value);
        }

        /**
         * Sets the create test suite flag.
         *
         * @param createTestSuite the new create test suite flag
         */
        private void setCreateTestSuite(boolean createTestSuite) {

            this.createTestSuite = createTestSuite;
        }
    }

    /**
     * Descriptor for KlarosImportPublisher class. Used as a singleton. The class is
     * marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements Serializable {

        private static final long serialVersionUID = 1L;

        private static final String PROJECT_CONFIG_HTML =
            "/plugin/klaros-testmanagement/help-projectConfig.html";
        private static final String URL_NAME = "url.name";

        /** Global configuration information. */

        private List<String> urls = new ArrayList<String>();

        /**
         * Instantiates a new descriptor implementation.
         */
        public DescriptorImpl() {

            load();
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {

            return Messages.displayName();
        }

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

        @Override
        public boolean isApplicable(
            @SuppressWarnings("rawtypes") final Class<? extends AbstractProject> jobType) {

            return true; // for all types
        }

        @Override
        public String getHelpFile() {

            return PROJECT_CONFIG_HTML;
        }

        /**
         * Gets the urls.
         *
         * @return the urls
         */
        public List<String> getUrls() {

            return urls;
        }

        /**
         * Sets the URLs.
         *
         * @param setUrls the new URLs
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
         * @param value the url value to check
         * @return the form validation result
         * @throws IOException Signals that an I/O exception has occurred.
         * @throws ServletException the servlet exception
         */
        public FormValidation doCheckUrl(final String value) throws IOException, ServletException {

            return new FormValidation.URLCheck() {

                @Override
                protected FormValidation check() throws IOException, ServletException {

                    String cooked = Util.fixEmpty(value);
                    if (cooked == null) { // nothing entered yet
                        return FormValidation.ok();
                    }

                    if (!value.endsWith("/")) {
                        cooked += Character.toString('/');
                    }

                    FormValidation result = FormValidation.ok();
                    try {
                        if (findText(open(new URL(cooked)), "Klaros")) {
                            result = FormValidation.ok();
                        } else {
                            result =
                                FormValidation
                                    .error("This URL does not point to a running Klaros-Testmanagement installation");
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
         * @param project the current project
         * @param value the mask value to check
         * @return the form validation result
         * @throws IOException Signals that an I/O exception has occurred.
         * @throws ServletException the servlet exception
         */
        public FormValidation doCheck(@AncestorInPath final AbstractProject<?, ?> project,
            @QueryParameter final String value) throws IOException, ServletException {

            FilePath ws = project.getSomeWorkspace();
            return ws != null ? ws.validateFileMask(value, false) : FormValidation.ok();
        }

        /**
         * Performs on-the-fly validation on the server installation.
         *
         * @param value the mask value to check
         * @return the form validation result
         * @throws IOException Signals that an I/O exception has occurred.
         * @throws ServletException the servlet exception
         */
        public FormValidation doCheckInstallation(@QueryParameter final String value) throws IOException,
            ServletException {

            if (Util.fixEmpty(value) != null) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.errorMissingInstallation());
            }
        }

        /**
         * Test the connection with the given parameters.
         *
         * @param url the url
         * @param username the username
         * @param password the password
         * @return the form validation
         * @throws IOException Signals that an I/O exception has occurred.
         * @throws ServletException the servlet exception
         */
        public FormValidation doTestConnection(@QueryParameter final String url,
            @QueryParameter final String username, @QueryParameter final String password) throws IOException,
            ServletException {

            final String strURL = buildServletURL(url);

            PutMethod put = new PutMethod(strURL);
            StringBuilder query = new StringBuilder();
            if (username != null) {
                query.append("username=").append(username).append("&password=").append(password).append(
                    "&type=").append("check");
            }
            put.setQueryString(query.toString());
            try {
                RequestEntity entity = new StringRequestEntity("", "text/xml; charset=UTF-8", "UTF-8");
                put.setRequestEntity(entity);
                return putResultFile(put);
            } catch (RuntimeException e) {
                return FormValidation.error(e.getMessage());
            }
        }

        /**
         * HTTP Put the result file.
         *
         * @param put the put request
         * @return the form validation
         * @throws IOException Signals that an I/O exception has occurred.
         * @throws HttpException the http exception
         */
        private FormValidation putResultFile(PutMethod put) throws IOException, HttpException {

            try {
                HttpClient client = new HttpClient();
                int result = client.executeMethod(put);
                String response = "";
                if (result != HttpServletResponse.SC_OK) {
                    StringBuilder msg = new StringBuilder();
                    response = put.getResponseBodyAsString();
                    if (response.length() > 0) {
                        msg.append("Connection failed: ").append(response);
                        System.out.println(msg.toString());
                    }
                    return FormValidation.error(msg.toString());
                } else {
                    if (response.length() > 0) {
                        return FormValidation.ok(Messages.connectionEstablished() + ": " + response);
                    } else {
                        return FormValidation.ok(Messages.connectionEstablished());
                    }
                }
            } finally {
                put.releaseConnection();
            }
        }
    }
}
