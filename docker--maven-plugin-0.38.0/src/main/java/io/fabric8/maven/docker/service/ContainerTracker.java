package io.fabric8.maven.docker.service;

import java.util.*;

import io.fabric8.maven.docker.config.ImageConfiguration;
import io.fabric8.maven.docker.config.RunImageConfiguration;
import io.fabric8.maven.docker.config.StopMode;
import io.fabric8.maven.docker.config.WaitConfiguration;
import io.fabric8.maven.docker.service.ContainerTracker.ContainerShutdownDescriptor;
import io.fabric8.maven.docker.util.GavLabel;

/**
 * Tracker class for tracking started containers so that they can be shut down at the end when
 * <code>docker:start</code> and <code>docker:stop</code> are used in the same run
 */
public class ContainerTracker {

    private ContainerTrackerData data = new ContainerTrackerData(new HashMap<>(), new HashMap<>(), new LinkedHashMap<>(),
			new HashMap<>());

	/**
     * Register a started container to this tracker
     *
     * @param containerId container id to register
     * @param imageConfig configuration of associated image
     * @param gavLabel pom label to identifying the reactor project where the container was created
     */
    public synchronized void registerContainer(String containerId,
                                               ImageConfiguration imageConfig,
                                               GavLabel gavLabel) {
        ContainerShutdownDescriptor descriptor = new ContainerShutdownDescriptor(imageConfig, containerId);
        data.shutdownDescriptorPerContainerMap.put(containerId, descriptor);
        updatePomLabelMap(gavLabel, descriptor);
        updateImageToContainerMapping(imageConfig, containerId);
    }

    /**
     * Remove a container from this container (if stored) and return its descriptor
     *
     * @param containerId id to remove
     * @return descriptor of the container removed or <code>null</code>
     */
    public synchronized ContainerShutdownDescriptor removeContainer(String containerId) {
        ContainerShutdownDescriptor descriptor = data.shutdownDescriptorPerContainerMap.remove(containerId);
        if (descriptor != null) {
            removeContainerIdFromLookupMaps(containerId);
            removeDescriptorFromPomLabelMap(descriptor);
        }
        return descriptor;
    }

    /**
     * Lookup a container by name or alias from the tracked containers
     *
     * @param lookup name or alias of the container to lookup
     * @return container id found or <code>null</code>
     */
    public synchronized String lookupContainer(String lookup) {
        if (data.aliasToContainerMap.containsKey(lookup)) {
            return data.aliasToContainerMap.get(lookup);
        }
        return data.imageToContainerMap.get(lookup);
    }

    /**
     * Get all shutdown descriptors for a given pom label and remove it from the tracker. The descriptors
     * are returned in reverse order of their registration.
     *
     * If no pom label is given, then all descriptors are returned.
     *
     * @param gavLabel the label for which to get the descriptors or <code>null</code> for all descriptors
     * @return the descriptors for the given label or an empty collection
     */
    public synchronized Collection<ContainerShutdownDescriptor> removeShutdownDescriptors(GavLabel gavLabel) {
        List<ContainerShutdownDescriptor> descriptors;
        if (gavLabel != null) {
            descriptors = removeFromPomLabelMap(gavLabel);
            removeFromPerContainerMap(descriptors);
        } else {
            // All entries are requested
            descriptors = new ArrayList<>(data.shutdownDescriptorPerContainerMap.values());
            clearAllMaps();
        }

        Collections.reverse(descriptors);
        return descriptors;
    }

    /**
     * Get all shutdown descriptors for a given pom label.
     * The descriptors are returned in order of their registration.
     * If no pom label is given, then all descriptors are returned.
     *
     * @param gavLabel the label for which to get the descriptors or <code>null</code> for all descriptors
     * @return the descriptors for the given label or an empty collection
     */
    public synchronized List<ContainerShutdownDescriptor> getShutdownDescriptors(GavLabel gavLabel) {
        if (gavLabel == null) {
            // All entries are requested
            return new ArrayList<>(data.shutdownDescriptorPerContainerMap.values());
        }
        return getFromPomLabelMap(gavLabel);
    }

    // ========================================================

    private void updatePomLabelMap(GavLabel gavLabel, ContainerShutdownDescriptor descriptor) {
        if (gavLabel != null) {
            List<ContainerShutdownDescriptor> descList = data.shutdownDescriptorPerPomLabelMap.get(gavLabel);
            if (descList == null) {
                descList = new ArrayList<>();
                data.shutdownDescriptorPerPomLabelMap.put(gavLabel, descList);
            }
            descList.add(descriptor);
        }
    }

    private void removeDescriptorFromPomLabelMap(ContainerShutdownDescriptor descriptor) {
        Iterator<Map.Entry<GavLabel, List<ContainerShutdownDescriptor>>> mapIt = data.shutdownDescriptorPerPomLabelMap.entrySet().iterator();
        while(mapIt.hasNext()) {
            Map.Entry<GavLabel,List<ContainerShutdownDescriptor>> mapEntry = mapIt.next();
            List<ContainerShutdownDescriptor> descs = mapEntry.getValue();
            Iterator<ContainerShutdownDescriptor> it = descs.iterator();
            while (it.hasNext()) {
                ContainerShutdownDescriptor desc = it.next();
                if (descriptor.equals(desc)) {
                    it.remove();
                }
            }
            if (descs.size() == 0) {
                mapIt.remove();
            }
        }
    }

    private void removeContainerIdFromLookupMaps(String containerId) {
        removeValueFromMap(data.imageToContainerMap,containerId);
        removeValueFromMap(data.aliasToContainerMap,containerId);
    }

    private void removeValueFromMap(Map<String, String> map, String value) {
        Iterator<Map.Entry<String,String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String,String> entry = it.next();
            if (entry.getValue().equals(value)) {
                it.remove();
            }
        }
    }

    private void updateImageToContainerMapping(ImageConfiguration imageConfig, String id) {
        // Register name -> containerId and alias -> name
        data.imageToContainerMap.put(imageConfig.getName(), id);
        if (imageConfig.getAlias() != null) {
            data.aliasToContainerMap.put(imageConfig.getAlias(), id);
        }
    }

    private void removeFromPerContainerMap(List<ContainerShutdownDescriptor> descriptors) {
        Iterator<Map.Entry<String, ContainerShutdownDescriptor>> it = data.shutdownDescriptorPerContainerMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ContainerShutdownDescriptor> entry = it.next();
            if (descriptors.contains(entry.getValue())) {
                removeContainerIdFromLookupMaps(entry.getKey());
                it.remove();
            }
        }
    }

    private List<ContainerShutdownDescriptor> removeFromPomLabelMap(GavLabel gavLabel) {
        List<ContainerShutdownDescriptor> descriptors;
        descriptors = data.shutdownDescriptorPerPomLabelMap.remove(gavLabel);
        if (descriptors == null) {
            descriptors = new ArrayList<>();
        } return descriptors;
    }

    private List<ContainerShutdownDescriptor> getFromPomLabelMap(GavLabel gavLabel) {
        List<ContainerShutdownDescriptor> descriptors = data.shutdownDescriptorPerPomLabelMap.get(gavLabel);
        if (descriptors == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(descriptors);
    }

    private void clearAllMaps() {
        data.shutdownDescriptorPerContainerMap.clear();
        data.shutdownDescriptorPerPomLabelMap.clear();
        data.imageToContainerMap.clear();
        data.aliasToContainerMap.clear();
    }

    // =======================================================

    static class ContainerShutdownDescriptor {

        // The image's configuration
        private final ImageConfiguration imageConfig;

        // Alias of the image
        private final String containerId;

        // How long to wait after shutdown (in milliseconds)
        private final int shutdownGracePeriod;

        // How long to wait after stop to kill container (in seconds)
        private final int killGracePeriod;

        // Whether to kill or stop gracefully
        private final StopMode stopMode;


        // Command to call before stopping container and whether to stop the build
        private String preStop;
        private boolean breakOnError = false;

        ContainerShutdownDescriptor(ImageConfiguration imageConfig, String containerId) {
            this.imageConfig = imageConfig;
            this.containerId = containerId;

            RunImageConfiguration runConfig = imageConfig.getRunConfiguration();
            WaitConfiguration waitConfig = runConfig != null ? runConfig.getWaitConfiguration() : null;
            this.shutdownGracePeriod = waitConfig != null && waitConfig.getShutdown() != null ? waitConfig.getShutdown() : 0;
            this.stopMode = runConfig != null ? runConfig.getStopMode()  : StopMode.graceful;
            this.killGracePeriod = waitConfig != null && waitConfig.getKill() != null ? waitConfig.getKill() : 0;
            if (waitConfig != null && waitConfig.getExec() != null) {
                this.preStop = waitConfig.getExec().getPreStop();
                this.breakOnError = waitConfig.getExec().isBreakOnError();
            }
        }

        public ImageConfiguration getImageConfiguration() {
            return imageConfig;
        }

        public String getImage() {
            return imageConfig.getName();
        }

        public String getContainerId() {
            return containerId;
        }

        public String getDescription() {
            return imageConfig.getDescription();
        }

        public int getShutdownGracePeriod() {
            return shutdownGracePeriod;
        }

        public int getKillGracePeriod() {
            return killGracePeriod;
        }

        public String getPreStop() {
            return preStop;
        }

        public boolean isBreakOnError() {
            return breakOnError;
        }

        public StopMode getStopMode() {
            return stopMode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ContainerShutdownDescriptor that = (ContainerShutdownDescriptor) o;

            return containerId.equals(that.containerId);

        }

        @Override
        public int hashCode() {
            return containerId.hashCode();
        }
    }
}
