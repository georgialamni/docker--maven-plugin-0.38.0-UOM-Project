package io.fabric8.maven.docker.service;

import io.fabric8.maven.docker.access.DockerAccess;
import io.fabric8.maven.docker.log.LogOutputSpecFactory;
import io.fabric8.maven.docker.util.Logger;

public class RunServiceData {
	public Logger log;
	public ContainerTracker tracker;
	public DockerAccess docker;
	public QueryService queryService;
	public LogOutputSpecFactory logConfig;

	public RunServiceData() {
	}
}