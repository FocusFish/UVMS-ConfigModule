package fish.focus.uvms.config.service;

import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@ArquillianSuiteDeployment
public abstract class BuildConfigServiceTestDeployment {

    final static Logger LOG = LoggerFactory.getLogger(BuildConfigServiceTestDeployment.class);

    @Deployment(name = "configservice", order = 1)
    public static Archive<?> createDeployment() {
        WebArchive testWar = ShrinkWrap.create(WebArchive.class, "test.war");

        File[] files = Maven.resolver().loadPomFromFile("pom.xml")
                .importRuntimeAndTestDependencies().resolve().withTransitivity().asFile();
        testWar.addAsLibraries(files);

        testWar.addPackages(true, "fish.focus.uvms.config.service");
        testWar.addPackages(true, "fish.focus.uvms.config.rest");

        testWar.addAsWebInfResource("ejb-jar.xml");
        testWar.addAsResource("beans.xml", "META-INF/beans.xml");
        testWar.addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml");
        
        testWar.delete("/WEB-INF/web.xml");
        testWar.addAsWebInfResource("mock-web.xml", "web.xml");

		return testWar;
	}
}
