package io.fabric8.maven.docker.assembly;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;

import io.fabric8.maven.docker.assembly.AssemblyFiles.Entry;
import io.fabric8.maven.docker.config.AssemblyConfiguration;
import io.fabric8.maven.docker.config.BuildImageConfiguration;
import io.fabric8.maven.docker.util.Logger;
import io.fabric8.maven.docker.util.MojoParameters;

public interface DockerAssemblyManagerInterface {

	String DEFAULT_DATA_BASE_IMAGE = "busybox:latest";
	String SCRATCH_IMAGE = "scratch";
	// Assembly name used also as build directory within outputBuildDir
	String DOCKER_IGNORE = ".maven-dockerignore";
	String DOCKER_EXCLUDE = ".maven-dockerexclude";
	String DOCKER_INCLUDE = ".maven-dockerinclude";
	String DOCKERFILE_NAME = "Dockerfile";

	/**
	 * Extract a docker tar archive into the given directory.
	 *
	 * @param archiveFile a tar archive to extract
	 * @param destinationDirectory directory where to place extracted content
	 * @throws MojoExecutionException if an error occurs during extracting.
	 */
	void extractDockerTarArchive(File archiveFile, File destinationDirectory) throws MojoExecutionException;

	/**
	 * Create an docker tar archive from the given configuration which can be send to the Docker host for
	 * creating the image.
	 *
	 * @param imageName Name of the image to create (used for creating build directories)
	 * @param params Mojos parameters (used for finding the directories)
	 * @param buildConfig configuration for how to build the image
	 * @param log Logger used to display warning if permissions are to be normalized
	 * @return file holding the path to the created assembly tar file
	 * @throws MojoExecutionException
	 */
	File createDockerTarArchive(String imageName, MojoParameters params, BuildImageConfiguration buildConfig,
			Logger log) throws MojoExecutionException;

	/**
	 * Create an docker tar archive from the given configuration which can be send to the Docker host for
	 * creating the image.
	 *
	 * @param imageName Name of the image to create (used for creating build directories)
	 * @param params Mojos parameters (used for finding the directories)
	 * @param buildConfig configuration for how to build the image
	 * @param log Logger used to display warning if permissions are to be normalized
	 * @param finalCustomizer finalCustomizer to be applied to the tar archive
	 * @return file holding the path to the created assembly tar file
	 * @throws MojoExecutionException
	 */
	File createDockerTarArchive(String imageName, MojoParameters params, BuildImageConfiguration buildConfig,
			Logger log, ArchiverCustomizer finalCustomizer) throws MojoExecutionException;

	/**
	 * Extract all files with a tracking archiver. These can be used to track changes in the filesystem and triggering
	 * a rebuild of the image if needed ('docker:watch')
	 */
	AssemblyFiles getAssemblyFiles(String name, AssemblyConfiguration assemblyConfig, MojoParameters mojoParams,
			Logger log) throws InvalidAssemblerConfigurationException, ArchiveCreationException,
			AssemblyFormattingException, MojoExecutionException;

	File createChangedFilesArchive(List<AssemblyFiles.Entry> entries, File assemblyDirectory, String imageName,
			MojoParameters mojoParameters) throws MojoExecutionException;

}