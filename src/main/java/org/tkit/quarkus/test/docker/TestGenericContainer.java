package org.tkit.quarkus.test.docker;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.FixedHostPortGenericContainer;

public class TestGenericContainer extends FixedHostPortGenericContainer<TestGenericContainer> {

    private StartingListener starting;

    public TestGenericContainer(final String dockerImageName) {
        super(dockerImageName);
    }

    public void setStartingListener(StartingListener starting) {
        this.starting = starting;
    }

    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        if (starting != null) {
            starting.containerIsStarting(containerInfo);
        }
    }

    public interface StartingListener {
        void containerIsStarting(InspectContainerResponse containerInfo);
    }

}
