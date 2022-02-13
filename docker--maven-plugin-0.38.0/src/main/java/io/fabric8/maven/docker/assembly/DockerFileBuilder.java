package io.fabric8.maven.docker.assembly;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;

import io.fabric8.maven.docker.assembly.DockerFileBuilder.CopyEntry;
import io.fabric8.maven.docker.config.Arguments;
import io.fabric8.maven.docker.config.HealthCheckConfiguration;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Create a dockerfile
 *
 * @author roland
 * @since 17.04.14
 */
public class DockerFileBuilder {

    private static final Joiner JOIN_ON_COMMA = Joiner.on("\",\"");

    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("^\\$(\\{[a-zA-Z0-9_]+\\}|[a-zA-Z0-9_]+).*");

    private DockerFileBuilderData data = new DockerFileBuilderData(null, "/maven", null, new ArrayList<>(), new ArrayList<>(),
			new ArrayList<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>(), false);

	/**
     * Create a DockerFile in the given directory
     * @param  destDir directory where to store the dockerfile
     * @return the full path to the docker file
     * @throws IOException if writing fails
     */
    public File write(File destDir) throws IOException {
        File target = new File(destDir,"Dockerfile");
        FileUtils.fileWrite(target, content());
        return target;
    }

    /**
     * Create a Dockerfile following the format described in the
     * <a href="http://docs.docker.io/reference/builder/#usage">Docker reference manual</a>
     *
     * @return the dockerfile create
     * @throws IllegalArgumentException if no src/dest entries have been added
     */
    public String content() throws IllegalArgumentException {

        StringBuilder b = new StringBuilder();

        DockerFileKeyword.FROM.addTo(b, data.baseImage != null ? data.baseImage : DockerAssemblyManagerInterface.DEFAULT_DATA_BASE_IMAGE);
        if (data.maintainer != null) {
            DockerFileKeyword.MAINTAINER.addTo(b, data.maintainer);
        }

        addOptimisation();
        addEnv(b);
        addLabels(b);
        addPorts(b);

        addCopy(b);
        addWorkdir(b);
        addShell(b);
        addRun(b);
        addVolumes(b);

        addHealthCheck(b);

        addCmd(b);
        addEntryPoint(b);

        addUser(b);

        return b.toString();
    }

    private void addUser(StringBuilder b) {
        if (data.user != null) {
            DockerFileKeyword.USER.addTo(b, data.user);
        }
    }

    private void addHealthCheck(StringBuilder b) {
        if (data.healthCheck != null) {
            StringBuilder healthString = new StringBuilder();

            switch (data.healthCheck.getMode()) {
            case cmd:
                buildOption(healthString, DockerFileOption.HEALTHCHECK_INTERVAL, data.healthCheck.getInterval());
                buildOption(healthString, DockerFileOption.HEALTHCHECK_TIMEOUT, data.healthCheck.getTimeout());
                buildOption(healthString, DockerFileOption.HEALTHCHECK_START_PERIOD, data.healthCheck.getStartPeriod());
                buildOption(healthString, DockerFileOption.HEALTHCHECK_RETRIES, data.healthCheck.getRetries());
                buildArguments(healthString, DockerFileKeyword.CMD, false, data.healthCheck.getCmd());
                break;
            case none:
                DockerFileKeyword.NONE.addTo(healthString, false);
                break;
            default:
                throw new IllegalArgumentException("Unsupported health check mode: " + data.healthCheck.getMode());
            }

            DockerFileKeyword.HEALTHCHECK.addTo(b, healthString.toString());
        }
    }

    private void addWorkdir(StringBuilder b) {
        if (data.workdir != null) {
            DockerFileKeyword.WORKDIR.addTo(b, data.workdir);
        }
    }

    private void addEntryPoint(StringBuilder b){
        if (data.entryPoint != null) {
            buildArguments(b, DockerFileKeyword.ENTRYPOINT, true, data.entryPoint);
        }
    }

    private void addCmd(StringBuilder b){
        if (data.cmd != null) {
            buildArguments(b, DockerFileKeyword.CMD, true, data.cmd);
        }
    }

    private static void buildArguments(StringBuilder b, DockerFileKeyword key, boolean newline, Arguments arguments) {
        String arg;
        if (arguments.getShell() != null) {
            arg = arguments.getShell();
        } else {
            arg = "[\""  + JOIN_ON_COMMA.join(arguments.getExec()) + "\"]";
        }
        key.addTo(b, newline, arg);
    }

    private static void buildArgumentsAsJsonFormat(StringBuilder b, DockerFileKeyword key, boolean newline, Arguments arguments) {
        String arg = "[\""  + JOIN_ON_COMMA.join(arguments.asStrings()) + "\"]";
        key.addTo(b, newline, arg);
    }

    private static void buildOption(StringBuilder b, DockerFileOption option, Object value) {
        if (value != null) {
            option.addTo(b, value);
        }
    }

    private String userForEntry(CopyEntry copyEntry) {
        if (copyEntry.user != null) {
            return copyEntry.user;
        }
        return data.assemblyUser;
    }

    private String targetDirForEntry(CopyEntry copyEntry) {
        if (copyEntry.target != null) {
            return copyEntry.target.equals("/") ? "" : copyEntry.target;
        }
        return data.basedir.equals("/") ? "" : data.basedir;
    }

    private void addCopy(StringBuilder b) {
        for (CopyEntry entry : data.copyEntries) {
            String entryUser = userForEntry(entry);
            if (entryUser != null) {
                String[] userParts = entryUser.split(":");
                if (userParts.length > 2) {
                    DockerFileKeyword.USER.addTo(b, "root");
                }
                addCopyEntries(b, entry, "", (userParts.length > 1 ?
                        userParts[0] + ":" + userParts[1] :
                        userParts[0]));
                if (userParts.length > 2) {
                    DockerFileKeyword.USER.addTo(b, userParts[2]);
                }
            } else {
                addCopyEntries(b, entry, "", null);
            }
        }
    }

    private void addCopyEntries(StringBuilder b, CopyEntry entry, String topLevelDir, String ownerAndGroup) {
        String dest = topLevelDir + targetDirForEntry(entry) + "/" + entry.destination;
        if (ownerAndGroup == null) {
            DockerFileKeyword.COPY.addTo(b, entry.source, dest);
        } else {
            DockerFileKeyword.COPY.addTo(b, "--chown=" + ownerAndGroup, entry.source, dest);
        }
    }

    private void addEnv(StringBuilder b) {
        addMap(b, DockerFileKeyword.ENV, data.envEntries);
    }

    private void addLabels(StringBuilder b) {
        addMap(b, DockerFileKeyword.LABEL, data.labels);
    }

    private void addMap(StringBuilder b,DockerFileKeyword keyword, Map<String,String> map) {
        if (map != null && map.size() > 0) {
            String entries[] = new String[map.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                entries[i++] = createKeyValue(entry.getKey(), entry.getValue());
            }
            keyword.addTo(b, entries);
        }
    }

    /**
     * Escape any slashes, quotes, and newlines int the value.  If any escaping occurred, quote the value.
     * @param key The key
     * @param value The value
     * @return Escaped and quoted key="value"
     */
    private String createKeyValue(String key, String value) {
        StringBuilder sb = new StringBuilder();
        // no quoting the key; "Keys are alphanumeric strings which may contain periods (.) and hyphens (-)"
        sb.append(key).append('=');
        if (value == null || value.isEmpty()) {
            return sb.append("\"\"").toString();
        }
	StringBuilder valBuf = new StringBuilder();
	boolean toBeQuoted = false;
        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                case '\n':
                case '\\':
                    // escape the character
                    valBuf.append('\\');
                case ' ':
                    // space needs quotes, too
                    toBeQuoted = true;
                default:
                    // always append
                    valBuf.append(c);
            }
        }
        if (toBeQuoted) {
            // need to keep quotes
            sb.append('"').append(valBuf.toString()).append('"');
        } else {
            sb.append(value);
        }
        return sb.toString();
    }

    private void addPorts(StringBuilder b) {
        if (data.ports.size() > 0) {
            String[] portsS = new String[data.ports.size()];
            int i = 0;
            for(String port : data.ports) {
            	portsS[i++] = validatePortExposure(port);
            }
            DockerFileKeyword.EXPOSE.addTo(b, portsS);
        }
    }

    private String validatePortExposure(String input) throws IllegalArgumentException {
        try {
            Matcher matcher = Pattern.compile("(.*?)(?:/(tcp|udp))?$", Pattern.CASE_INSENSITIVE).matcher(input);
            // Matches always.  If there is a tcp/udp protocol, should end up in the second group
            // and get factored out.  If it's something invalid, it should get stuck to the first group.
            matcher.matches();
            Integer.valueOf(matcher.group(1));
            return input.toLowerCase();
        } catch (NumberFormatException exp) {
            throw new IllegalArgumentException("\nInvalid port mapping '" + input + "'\n" +
                    "Required format: '<hostIP>(/tcp|udp)'\n" +
                    "See the reference manual for more details");
        }
    }

    private void addOptimisation() {
        if (data.runCmds != null && !data.runCmds.isEmpty() && data.shouldOptimise) {
            String optimisedRunCmd = StringUtils.join(data.runCmds.iterator(), " && ");
            data.runCmds.clear();
            data.runCmds.add(optimisedRunCmd);
        }
    }

    private void addShell(StringBuilder b) {
        if (data.shell != null) {
            buildArgumentsAsJsonFormat(b, DockerFileKeyword.SHELL, true, data.shell);
        }
    }

	private void addRun(StringBuilder b) {
		for (String run : data.runCmds) {
            DockerFileKeyword.RUN.addTo(b, run);
		}
	}

    private void addVolumes(StringBuilder b) {
        for (CopyEntry e : data.copyEntries) {
            if (e.export != null ? e.export : data.baseImage == null) {
                addVolume(b, targetDirForEntry(e));
            }
        }

        if (data.exportTargetDir != null ? data.exportTargetDir : data.baseImage == null) {
            addVolume(b, data.basedir);
        }

        for (String volume : data.volumes) {
            addVolume(b, volume);
        }
    }

    private void addVolume(StringBuilder buffer, String volume) {
        while (volume.endsWith("/")) {
            volume = volume.substring(0, volume.length() - 1);
        }
        // don't export '/'
        if (volume.length() > 0) {
            DockerFileKeyword.VOLUME.addTo(buffer, "[\"" + volume + "\"]");
        }
    }

    // ==========================================================================
    // Builder stuff ....
    public DockerFileBuilder() {}

    public DockerFileBuilder baseImage(String baseImage) {
        if (baseImage != null) {
            this.data.baseImage = baseImage;
        }
        return this;
    }

    public DockerFileBuilder maintainer(String maintainer) {
        this.data.maintainer = maintainer;
        return this;
    }

    public DockerFileBuilder workdir(String workdir) {
        this.data.workdir = workdir;
        return this;
    }

    public DockerFileBuilder basedir(String dir) {
        if (dir != null) {
            if (!dir.startsWith("/") && !ENV_VAR_PATTERN.matcher(dir).matches()) {
                throw new IllegalArgumentException("'basedir' must be an absolute path starting with / (and not " +
                                                   "'" + data.basedir + "') or start with an environment variable");
            }
            data.basedir = dir;
        }
        return this;
    }

    public DockerFileBuilder cmd(Arguments cmd) {
        this.data.cmd = cmd;
        return this;
    }

    public DockerFileBuilder entryPoint(Arguments entryPoint) {
        this.data.entryPoint = entryPoint;
        return this;
    }

    public DockerFileBuilder assemblyUser(String assemblyUser) {
        this.data.assemblyUser = assemblyUser;
        return this;
    }

    public DockerFileBuilder user(String user) {
        this.data.user = user;
        return this;
    }

    public DockerFileBuilder healthCheck(HealthCheckConfiguration healthCheck) {
        this.data.healthCheck = healthCheck;
        return this;
    }

    public DockerFileBuilder add(String source, String destination) {
        this.data.copyEntries.add(new CopyEntry(source, destination));
        return this;
    }

    public DockerFileBuilder add(String source, String destination, String target, String user, Boolean exportTarget) {
        this.data.copyEntries.add(new CopyEntry(source, destination, target, user, exportTarget));
        return this;
    }

    public DockerFileBuilder expose(List<String> ports) {
        if (ports != null) {
            this.data.ports.addAll(ports);
        }
        return this;
    }

    /**
     * Adds the SHELL Command plus params within the build image section
     * @param shell
     * @return
     */
    public DockerFileBuilder shell(Arguments shell) {
        this.data.shell = shell;
        return this;
    }

    /**
     * Adds the RUN Commands within the build image section
     * @param runCmds
     * @return
     */
    public DockerFileBuilder run(List<String> runCmds) {
        if (runCmds != null) {
            for (String cmd : runCmds) {
                if (!StringUtils.isEmpty(cmd)) {
                    this.data.runCmds.add(cmd);
                }
            }
        }
        return this;
    }

    public DockerFileBuilder exportTargetDir(Boolean exportTargetDir) {
        this.data.exportTargetDir = exportTargetDir;
        return this;
    }

    public DockerFileBuilder env(Map<String, String> values) {
        if (values != null) {
            this.data.envEntries.putAll(values);
            validateMap(data.envEntries);
        }
        return this;
    }

    public DockerFileBuilder labels(Map<String,String> values) {
        if (values != null) {
            this.data.labels.putAll(values);
        }
        return this;
    }

    public DockerFileBuilder volumes(List<String> volumes) {
        if (volumes != null) {
           this.data.volumes.addAll(volumes);
        }
        return this;
    }

    public DockerFileBuilder optimise() {
        this.data.shouldOptimise = true;
        return this;
    }

    private void validateMap(Map<String, String> env) {
        for (Map.Entry<String,String> entry : env.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length() == 0) {
                throw new IllegalArgumentException("Environment variable '" +
                                                   entry.getKey() + "' must not be null or empty if building an image");
            }
        }
    }

    // All entries required, destination is relative to exportDir
    private static final class CopyEntry {
        private String source;
        private String destination;
        private String target;
        private String user;
        private Boolean export;

        private CopyEntry(String src, String dest) {
            source = src;

            // Strip leading slashes
            destination = dest;

            // squeeze slashes
            while (destination.startsWith("/")) {
                destination = destination.substring(1);
            }
        }

        public CopyEntry(String src, String dest, String target, String user, Boolean exportTarget) {
            this(src, dest);
            this.target = target;
            this.user = user;
            this.export = exportTarget;
        }
    }

}
