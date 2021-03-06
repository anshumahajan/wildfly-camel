/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wildfly.camel.test.rest.dsl;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.rest.dsl.subA.ContextPathPrintingServlet;

@RunWith(Arquillian.class)
public class UndertowRestDslIntegrationTest extends AbstractRestDslIntegrationTest {

    private static final String SIMPLE_JAR = "simple.jar";
    private static final String SIMPLE_WAR = "simple.war";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-undertow-rest-dsl-tests.jar")
            .addClasses(HttpRequest.class, AbstractRestDslIntegrationTest.class);
    }

    @Deployment(testable = false, managed = false, name = SIMPLE_WAR)
    public static WebArchive createSimpleDeployment() {
        return ShrinkWrap.create(WebArchive.class, SIMPLE_WAR).addClass(ContextPathPrintingServlet.class);
    }

    @Deployment(testable = false, managed = false, name = SIMPLE_JAR)
    public static JavaArchive cameSimpleSpringDeployment() {
        return ShrinkWrap.create(JavaArchive.class, SIMPLE_JAR)
            .addAsResource("rest/rest-camel-context.xml", "camel-context.xml");
    }

    @Override
    protected String getComponentName() {
        return "undertow";
    }

    @Test
    public void testRestDslRootContext() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component(getComponentName())
                    .contextPath("/")
                    .host("localhost")
                    .port(getPort());

                rest()
                    .get()
                        .route()
                            .setBody(constant("GET: /"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            deployer.deploy(SIMPLE_WAR);

            // Verify root context path
            HttpRequest.HttpResponse response = HttpRequest.get("http://localhost:8080/").getResponse();
            Assert.assertEquals(200, response.getStatusCode());
            Assert.assertTrue(response.getBody().contains("GET: /"));

            // Verify other deployed context paths
            response = HttpRequest.get("http://localhost:8080/simple").getResponse();
            Assert.assertEquals(200, response.getStatusCode());
            Assert.assertTrue(response.getBody().contains("GET: /simple"));

        } finally {
            deployer.undeploy(SIMPLE_WAR);
            camelctx.stop();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testRestDslOverlappingPaths() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("undertow")
                    .host("localhost")
                    .port(8080);

                rest()
                    .get("/say/hello")
                        .route()
                          .setBody(constant("GET: /say/hello"))
                        .endRest();
            }
        });

        deployer.deploy(SIMPLE_JAR);
        try {
            camelctx.start();
        } finally {
            camelctx.stop();
            deployer.undeploy(SIMPLE_JAR);
        }
    }

    @Test
    public void testRestDslHandlerUnregistered() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("undertow")
                    .host("localhost")
                    .port(8080);

                rest()
                    .get("/test")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            int statusCode = HttpRequest.get("http://localhost:8080/test").getResponse().getStatusCode();
            Assert.assertEquals(200, statusCode);
        } finally {
            camelctx.stop();
        }

        int statusCode = HttpRequest.get("http://localhost:8080/test").throwExceptionOnFailure(false).getResponse().getStatusCode();
        Assert.assertEquals(404, statusCode);
    }
}
