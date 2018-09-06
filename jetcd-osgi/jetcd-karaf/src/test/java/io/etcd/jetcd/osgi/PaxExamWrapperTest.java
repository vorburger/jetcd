/**
 * Copyright 2017 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.etcd.jetcd.osgi;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;

/**
 * Pax Exam test wrapper which starts (and stops) an etcd test server via
 * launcher. We need to do this because jetcd-launcher uses Testcontainers,
 * which cannot run inside OSGi (due to the use of TCL loadClass(String) in
 * DockerClientProviderStrategy). And even if we were to use the launcher only
 * in the config() method (which "is executed before the OSGi container is
 * launched, so it does run in plain Java"), the Pax Exam probe still needs to
 * load the entire class and all of its references, thus launcher with
 * Testcontainers, into OSGi. It is therefore simplest to just launch the etcd
 * server before getting Pax Exam.
 *
 * @author Michael Vorburger.ch
 */
public class PaxExamWrapperTest {

    // TODO @Rule public final EtcdClusterResource etcd = new
    // EtcdClusterResource("karaf");
    // CANNOT use EtcdClusterResource, because Pax Exam would run that INSIDE the
    // OSGi container
    // where Testcontainers does not work (see TBD)
    // Instead, we do it in config(), because

    @Test
    public void testClientServiceChecks() throws Throwable {
        try (EtcdCluster etcd = EtcdClusterFactory.buildCluster("karaf", 1, false)) {
            etcd.start();
            String endpoint = etcd.getClientEndpoints().get(0);
            Files.write(endpoint, new File("target/endpoint"), Charsets.UTF_8);

            Optional<Failure> failure = JUnitCore.runClasses(ClientServiceChecks.class).getFailures().stream()
                    .findFirst();
            if (failure.isPresent()) {
                throw failure.get().getException();
            }
        }
    }

    static String getClientEndpoints() throws IOException {
        return Files.readFirstLine(new File("target/endpoint"), Charsets.UTF_8);
    }
}
