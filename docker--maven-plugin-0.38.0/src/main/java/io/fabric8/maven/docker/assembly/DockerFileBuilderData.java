package io.fabric8.maven.docker.assembly;

import java.util.List;
import java.util.Map;

import io.fabric8.maven.docker.assembly.DockerFileBuilder.CopyEntry;
import io.fabric8.maven.docker.config.Arguments;
import io.fabric8.maven.docker.config.HealthCheckConfiguration;

public class DockerFileBuilderData {
	public String baseImage;
	public String maintainer;
	public String workdir;
	public String basedir;
	public Arguments entryPoint;
	public Arguments cmd;
	public Boolean exportTargetDir;
	public String assemblyUser;
	public String user;
	public HealthCheckConfiguration healthCheck;
	public List<CopyEntry> copyEntries;
	public List<String> ports;
	public Arguments shell;
	public List<String> runCmds;
	public Map<String, String> envEntries;
	public Map<String, String> labels;
	public List<String> volumes;
	public boolean shouldOptimise;

	public DockerFileBuilderData(String workdir, String basedir, Boolean exportTargetDir, List<CopyEntry> copyEntries,
			List<String> ports, List<String> runCmds, Map<String, String> envEntries, Map<String, String> labels,
			List<String> volumes, boolean shouldOptimise) {
		this.workdir = workdir;
		this.basedir = basedir;
		this.exportTargetDir = exportTargetDir;
		this.copyEntries = copyEntries;
		this.ports = ports;
		this.runCmds = runCmds;
		this.envEntries = envEntries;
		this.labels = labels;
		this.volumes = volumes;
		this.shouldOptimise = shouldOptimise;
	}
}