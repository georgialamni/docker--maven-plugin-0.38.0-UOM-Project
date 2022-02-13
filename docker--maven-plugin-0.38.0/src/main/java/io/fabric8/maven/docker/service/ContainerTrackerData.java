package io.fabric8.maven.docker.service;

import java.util.List;
import java.util.Map;

import io.fabric8.maven.docker.service.ContainerTracker.ContainerShutdownDescriptor;
import io.fabric8.maven.docker.util.GavLabel;

public class ContainerTrackerData {
	public Map<String, String> imageToContainerMap;
	public Map<String, String> aliasToContainerMap;
	public Map<String, ContainerShutdownDescriptor> shutdownDescriptorPerContainerMap;
	public Map<GavLabel, List<ContainerShutdownDescriptor>> shutdownDescriptorPerPomLabelMap;

	public ContainerTrackerData(Map<String, String> imageToContainerMap, Map<String, String> aliasToContainerMap,
			Map<String, ContainerShutdownDescriptor> shutdownDescriptorPerContainerMap,
			Map<GavLabel, List<ContainerShutdownDescriptor>> shutdownDescriptorPerPomLabelMap) {
		this.imageToContainerMap = imageToContainerMap;
		this.aliasToContainerMap = aliasToContainerMap;
		this.shutdownDescriptorPerContainerMap = shutdownDescriptorPerContainerMap;
		this.shutdownDescriptorPerPomLabelMap = shutdownDescriptorPerPomLabelMap;
	}
}