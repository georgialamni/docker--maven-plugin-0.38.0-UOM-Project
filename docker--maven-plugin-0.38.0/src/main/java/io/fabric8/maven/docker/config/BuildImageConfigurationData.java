package io.fabric8.maven.docker.config;

import java.io.File;
import java.util.List;
import java.util.Map;

public class BuildImageConfigurationData {
	/**
	 * Directory is used as build context.
	 * If not specified, dockerfile's parent directory is used as build context.
	 */
	@Parameter
	public String contextDir;
	/**
	 * Directory holding an external Dockerfile which is used to build the
	 * image. This Dockerfile will be enriched by the addition build configuration
	 */
	@Deprecated
	@Parameter
	public String dockerFileDir;
	/**
	 * Path to a dockerfile to use.
	 * Multiple different Dockerfiles can be specified that way. If set overwrites a possibly givem
	 * <code>dockerFileDir</code>
	 */
	@Parameter
	public String dockerFile;
	/**
	 * Path to a docker archive to load an image instead of building from scratch.
	 * Note only either dockerFile/dockerFileDir or
	 * dockerArchive can be used.
	 */
	@Parameter
	public String dockerArchive;
	/**
	 * Pattern for the image name we expect to find in the dockerArchive.
	 *
	 * If set, the archive is scanned prior to sending to Docker and checked to
	 * ensure a matching name is found linked to one of the images in the archive.
	 * After loading, the image with the matching name will be tagged with the
	 * image name configured in this project.
	 */
	@Parameter
	public String loadNamePattern;
	/**
	 * How interpolation of a dockerfile should be performed
	 */
	@Parameter
	public String filter;
	/**
	 * Base Image
	 */
	@Parameter
	public String from;
	/**
	 * Extended version for <from>
	 */
	@Parameter
	public Map<String, String> fromExt;
	@Parameter
	public List<String> cacheFrom;
	@Parameter
	public String registry;
	@Parameter
	public String maintainer;
	@Parameter
	public String network;
	@Parameter
	public List<String> ports;
	/**
	 * Policy for pulling the base images
	 */
	@Parameter
	public String imagePullPolicy;
	/**
	 * SHELL excutable with params
	 */
	@Parameter
	public Arguments shell;
	/**
	 * RUN Commands within Build/Image
	 */
	@Parameter
	public List<String> runCmds;
	@Parameter
	public String cleanup;
	@Parameter
	@Deprecated
	public Boolean nocache;
	@Parameter
	public Boolean noCache;
	@Parameter
	public Boolean squash;
	@Parameter
	public Boolean optimise;
	@Parameter
	public List<String> volumes;
	@Parameter
	public List<String> tags;
	@Parameter
	public Map<String, String> env;
	@Parameter
	public Map<String, String> labels;
	@Parameter
	public Map<String, String> args;
	@Parameter
	public Arguments entryPoint;
	@Deprecated
	@Parameter
	public String command;
	@Parameter
	public String workdir;
	@Parameter
	public Arguments cmd;
	@Parameter
	public String user;
	@Parameter
	public HealthCheckConfiguration healthCheck;
	@Parameter
	public AssemblyConfiguration assembly;
	@Parameter
	public List<AssemblyConfiguration> assemblies;
	@Parameter
	public Boolean skip;
	@Parameter
	public Boolean skipPush;
	@Parameter
	public ArchiveCompression compression;
	@Parameter
	public Map<String, String> buildOptions;
	public File dockerFileFile;
	public File dockerArchiveFile;

	public BuildImageConfigurationData(ArchiveCompression compression) {
		this.compression = compression;
	}
}