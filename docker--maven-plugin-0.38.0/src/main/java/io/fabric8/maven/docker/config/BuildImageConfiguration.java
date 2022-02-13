package io.fabric8.maven.docker.config;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import io.fabric8.maven.docker.util.DeepCopy;
import io.fabric8.maven.docker.util.EnvUtil;
import io.fabric8.maven.docker.util.Logger;
import io.fabric8.maven.docker.util.MojoParameters;

/**
 * @author roland
 * @since 02.09.14
 */
public class BuildImageConfiguration implements Serializable {

    public static final String DEFAULT_FILTER = "${*}";
    public static final String DEFAULT_CLEANUP = "try";

    private BuildImageConfigurationData data = new BuildImageConfigurationData(ArchiveCompression.none);

	public BuildImageConfiguration() {}

    public boolean isDockerFileMode() {
        return data.dockerFileFile != null;
    }

    public String getLoadNamePattern() {
        return data.loadNamePattern;
    }

    public File getContextDir() {
        if (!isDockerFileMode()) {
            return null;
        }
        if (data.contextDir != null) {
            return new File(data.contextDir);
        }
        if (getDockerFile().getParentFile() == null) {
            return new File("");
        }
        return getDockerFile().getParentFile();
    }

    public String getContextDirRaw() {
        return data.contextDir;
    }

    public File getDockerFile() {
        return data.dockerFileFile;
    }

    public File getDockerArchive() {
        return data.dockerArchiveFile;
    }

    public String getDockerFileRaw() {
        return data.dockerFile;
    }

    public String getDockerArchiveRaw() {
        return data.dockerArchive;
    }

    public String getDockerFileDirRaw() {
        return data.dockerFileDir;
    }

    public String getFilter() {
        return data.filter != null ? data.filter : DEFAULT_FILTER;
    }

    public String getFilterRaw() {
        return data.filter;
    }

    public String getFrom() {
        if (data.from == null && getFromExt() != null) {
            return getFromExt().get("name");
        }
        return data.from;
    }

    public Map<String, String> getFromExt() {
        return data.fromExt;
    }

    public List<String> getCacheFrom() {
        return data.cacheFrom;
    }

    public String getNetwork() {
        return data.network;
    }

    public String getRegistry() {
        return data.registry;
    }

    public String getMaintainer() {
        return data.maintainer;
    }

    public String getWorkdir() {
        return data.workdir;
    }

    /**
     * @deprecated Use {@link #getAssemblyConfigurations()} instead.
     */
    @Deprecated
    public AssemblyConfiguration getAssemblyConfiguration() {
        return data.assembly;
    }

    @Nonnull
    public List<AssemblyConfiguration> getAssemblyConfigurations() {
        final List<AssemblyConfiguration> assemblyConfigurations = new ArrayList<>();
        if (data.assemblies != null) {
            for (AssemblyConfiguration config : data.assemblies) {
                if (config != null) {
                    assemblyConfigurations.add(config);
                }
            }
        }
        if (data.assembly != null) {
            assemblyConfigurations.add(data.assembly);
        }
        return assemblyConfigurations;
    }

    @Nonnull
    public List<String> getPorts() {
        return EnvUtil.removeEmptyEntries(data.ports);
    }

    public String getImagePullPolicy() {
        return data.imagePullPolicy;
    }

    @Nonnull
    public List<String> getVolumes() {
        return EnvUtil.removeEmptyEntries(data.volumes);
    }

    @Nonnull
    public List<String> getTags() {
        return EnvUtil.removeEmptyEntries(data.tags);
    }

    public Map<String, String> getEnv() {
        return data.env;
    }

    public Map<String, String> getLabels() {
        return data.labels;
    }

    public Arguments getCmd() {
        return data.cmd;
    }

    @Deprecated
    public String getCommand() {
        return data.command;
    }

    public String getCleanup() {
        return data.cleanup;
    }

    public CleanupMode cleanupMode() {
        return CleanupMode.parse(data.cleanup != null ? data.cleanup : DEFAULT_CLEANUP);
    }

    public boolean noCache() {
        if (data.noCache != null) {
            return data.noCache;
        }
        if (data.nocache != null) {
            return data.nocache;
        }
        return false;
    }

    public boolean squash() {
        if (data.squash != null) {
            return data.squash;
        }
        return false;
    }

    public boolean optimise() {
        return data.optimise != null ? data.optimise : false;
    }

    public boolean skip() {
        return data.skip != null ? data.skip : false;
    }

    public boolean skipPush() {
        return data.skipPush != null ? data.skipPush : false;
    }

    public Boolean getNoCache() {
        return data.noCache != null ? data.noCache : data.nocache;
    }

    public Boolean getSquash() {
        return data.squash != null ? data.squash : false;
    }

    public Boolean getOptimise() {
        return data.optimise;
    }

    public Boolean getSkip() {
        return data.skip;
    }

    public Boolean getSkipPush() {
        return data.skipPush;
    }

    public ArchiveCompression getCompression() {
        return data.compression;
    }

    public Map<String, String> getBuildOptions() {
        return data.buildOptions;
    }

    public Arguments getEntryPoint() {
        return data.entryPoint;
    }

    public Arguments getShell() {
        return data.shell;
    }

    @Nonnull
    public List<String> getRunCmds() {
        return EnvUtil.removeEmptyEntries(data.runCmds);
    }

    public String getUser() {
      return data.user;
    }

    public HealthCheckConfiguration getHealthCheck() {
        return data.healthCheck;
    }

    public Map<String, String> getArgs() {
        return data.args;
    }

    public File getAbsoluteContextDirPath(MojoParameters mojoParams) {
        return EnvUtil.prepareAbsoluteSourceDirPath(mojoParams, getContextDir().getPath());
    }

    public File getAbsoluteDockerFilePath(MojoParameters mojoParams) {
        return EnvUtil.prepareAbsoluteSourceDirPath(mojoParams, getDockerFile().getPath());
    }

    public File getAbsoluteDockerTarPath(MojoParameters mojoParams) {
        return EnvUtil.prepareAbsoluteSourceDirPath(mojoParams, getDockerArchive().getPath());
    }

    public void initTags(ConfigHelper.NameFormatter nameFormatter) {
        if (data.tags != null) {
            data.tags = data.tags.stream().map(nameFormatter::format).collect(Collectors.toList());
        }
    }

    public static class Builder {
        private final BuildImageConfiguration config;

        public Builder() {
            this(null);
        }

        public Builder(BuildImageConfiguration that) {
            if (that == null) {
                this.config = new BuildImageConfiguration();
            } else {
                this.config = DeepCopy.copy(that);
            }
        }

        public Builder contextDir(String dir) {
            config.data.contextDir = dir;
            return this;
        }

        public Builder dockerFileDir(String dir) {
            config.data.dockerFileDir = dir;
            return this;
        }

        public Builder dockerFile(String file) {
            config.data.dockerFile = file;
            return this;
        }

        public Builder dockerArchive(String archive) {
            config.data.dockerArchive = archive;
            return this;
        }

        public Builder loadNamePattern(String archiveEntryRepoTagPattern) {
            config.data.loadNamePattern = archiveEntryRepoTagPattern;
            return this;
        }

        public Builder filter(String filter) {
            config.data.filter = filter;
            return this;
        }

        public Builder from(String from) {
            config.data.from = from;
            return this;
        }

        public Builder fromExt(Map<String, String> fromExt) {
            config.data.fromExt = fromExt;
            return this;
        }

        public Builder cacheFrom(String cacheFrom, String ...more) {
            if (more == null || more.length == 0) {
                return cacheFrom(Collections.singletonList(cacheFrom));
            }

            List<String> list = new ArrayList<>();
            list.add(cacheFrom);
            list.addAll(Arrays.asList(more));
            return cacheFrom(list);
        }

        public Builder cacheFrom(Collection<String> cacheFrom) {
            config.data.cacheFrom = cacheFrom != null ? new ArrayList<>(cacheFrom) : null;
            return this;
        }

        public Builder registry(String registry) {
            config.data.registry = registry;
            return this;
        }

        public Builder maintainer(String maintainer) {
            config.data.maintainer = maintainer;
            return this;
        }

        public Builder network(String network) {
            config.data.network = network;
            return this;
        }

        public Builder workdir(String workdir) {
            config.data.workdir = workdir;
            return this;
        }

        public Builder assembly(AssemblyConfiguration assembly) {
            config.data.assembly = assembly;
            return this;
        }

        public Builder assemblies(List<AssemblyConfiguration> assemblies) {
            config.data.assemblies = assemblies;
            return this;
        }

        public Builder ports(List<String> ports) {
            config.data.ports = ports;
            return this;
        }

        public Builder imagePullPolicy(String imagePullPolicy) {
            config.data.imagePullPolicy = imagePullPolicy;
            return this;
        }

        public Builder shell(Arguments shell) {
            if(shell != null) {
                config.data.shell = shell;
            }

            return this;
        }

        public Builder runCmds(List<String> theCmds) {
            if (theCmds == null) {
                config.data.runCmds = new ArrayList<>();
            } else {
                config.data.runCmds = theCmds;
            }
            return this;
        }

        public Builder volumes(List<String> volumes) {
            config.data.volumes = volumes;
            return this;
        }

        public Builder tags(List<String> tags) {
            config.data.tags = tags;
            return this;
        }

        public Builder env(Map<String, String> env) {
            config.data.env = env;
            return this;
        }

        public Builder args(Map<String, String> args) {
            config.data.args = args;
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            config.data.labels = labels;
            return this;
        }

        public Builder cmd(Arguments cmd) {
            if (cmd != null) {
                config.data.cmd = cmd;
            }
            return this;
        }

        public Builder cleanup(String cleanup) {
            config.data.cleanup = cleanup;
            return this;
        }

        public Builder compression(String compression) {
            if (compression == null) {
                config.data.compression = ArchiveCompression.none;
            } else {
                config.data.compression = ArchiveCompression.valueOf(compression);
            }
            return this;
        }

        public Builder noCache(Boolean noCache) {
            config.data.noCache = noCache;
            return this;
        }

        public Builder squash(Boolean squash) {
            config.data.squash = squash;
            return this;
        }

        public Builder optimise(Boolean optimise) {
            config.data.optimise = optimise;
            return this;
        }

        public Builder entryPoint(Arguments entryPoint) {
            if (entryPoint != null) {
                config.data.entryPoint = entryPoint;
            }
            return this;
        }

        public Builder user(String user) {
            config.data.user = user;
            return this;
        }

        public Builder healthCheck(HealthCheckConfiguration healthCheck) {
            config.data.healthCheck = healthCheck;
            return this;
        }

        public Builder skip(Boolean skip) {
            config.data.skip = skip;
            return this;
        }

        public Builder skipPush(Boolean skipPush) {
            config.data.skipPush = skipPush;
            return this;
        }

        public Builder buildOptions(Map<String,String> buildOptions) {
            config.data.buildOptions = buildOptions;
            return this;
        }

        public BuildImageConfiguration build() {
            return config;
        }
    }

    public String initAndValidate(Logger log) throws IllegalArgumentException {
        if (data.entryPoint != null) {
            data.entryPoint.validate();
        }
        if (data.cmd != null) {
            data.cmd.validate();
        }
        if (data.healthCheck != null) {
            data.healthCheck.validate();
        }

        ensureUniqueAssemblyNames(log);

        if (data.command != null) {
            log.warn("<command> in the <build> configuration is deprecated and will be be removed soon");
            log.warn("Please use <cmd> with nested <shell> or <exec> sections instead.");
            log.warn("");
            log.warn("More on this is explained in the user manual: ");
            log.warn("https://github.com/fabric8io/docker-maven-plugin/blob/master/doc/manual.md#start-up-arguments");
            log.warn("");
            log.warn("Migration is trivial, see changelog to version 0.12.0 -->");
            log.warn("https://github.com/fabric8io/docker-maven-plugin/blob/master/doc/changelog.md");
            log.warn("");
            log.warn("For now, the command is automatically translated for you to the shell form:");
            log.warn("   <cmd>%s</cmd>", data.command);
        }

        initDockerFileFile(log);

        if (data.cacheFrom != null && !data.cacheFrom.isEmpty()) {
            // cachefrom query param was introduced in v1.25
            return "1.25";
        } else if (data.healthCheck != null) {
            // HEALTHCHECK support added later
            return "1.24";
        } else if (data.args != null) {
            // ARG support came in later
            return "1.21";
        } else {
            return null;
        }
    }

    private void ensureUniqueAssemblyNames(Logger log) {
        List<AssemblyConfiguration> assemblyConfigurations = getAssemblyConfigurations();
        Set<String> assemblyNames = new HashSet<>();
        for (AssemblyConfiguration config : assemblyConfigurations) {
            String assemblyName = config.getName();
            boolean wasElementAbsent = assemblyNames.add(assemblyName);
            if (!wasElementAbsent) {
                log.error("Multiple assemblies use the name \"%s\". Please assign each assembly a unique name.", assemblyName);
                throw new IllegalArgumentException("Assembly names must be unique");
            }
        }
    }

    // Initialize the dockerfile location and the build mode
    private void initDockerFileFile(Logger log) {
        // can't have dockerFile/dockerFileDir and dockerArchive
        if ((data.dockerFile != null || data.dockerFileDir != null) && data.dockerArchive != null) {
            throw new IllegalArgumentException("Both <dockerFile> (<dockerFileDir>) and <dockerArchive> are set. " +
                                               "Only one of them can be specified.");
        }
        data.dockerFileFile = findDockerFileFile(log);

        if (data.dockerArchive != null) {
            data.dockerArchiveFile = new File(data.dockerArchive);
        }
    }

    private File findDockerFileFile(Logger log) {
        if(data.dockerFileDir != null && data.contextDir != null) {
            log.warn("Both contextDir (%s) and deprecated dockerFileDir (%s) are configured. Using contextDir.", data.contextDir, data.dockerFileDir);
        }

        if (data.dockerFile != null) {
            File dFile = new File(data.dockerFile);
            if (data.dockerFileDir == null && data.contextDir == null) {
                return dFile;
            } else {
                if(data.contextDir != null) {
                    if (dFile.isAbsolute()) {
                        return dFile;
                    }
                    return new File(data.contextDir, data.dockerFile);
                }

                if (data.dockerFileDir != null) {
                    if (dFile.isAbsolute()) {
                        throw new IllegalArgumentException("<dockerFile> can not be absolute path if <dockerFileDir> also set.");
                    }
                    log.warn("dockerFileDir parameter is deprecated, please migrate to contextDir");
                    return new File(data.dockerFileDir, data.dockerFile);
                }
            }
        }


        if (data.contextDir != null) {
            return new File(data.contextDir, "Dockerfile");
        }

        if (data.dockerFileDir != null) {
            return new File(data.dockerFileDir, "Dockerfile");
        }

        // TODO: Remove the following deprecated handling section
        if (data.dockerArchive == null) {
            Optional<String> deprecatedDockerFileDir =
                    getAssemblyConfigurations().stream()
                            .map(AssemblyConfiguration::getDockerFileDir)
                            .filter(Objects::nonNull)
                            .findFirst();
            if (deprecatedDockerFileDir.isPresent()) {
                log.warn("<dockerFileDir> in the <assembly> section of a <build> configuration is deprecated");
                log.warn("Please use <dockerFileDir> or <dockerFile> directly within the <build> configuration instead");
                return new File(deprecatedDockerFileDir.get(),"Dockerfile");
            }
        }

        // No dockerfile mode
        return null;
    }
}
