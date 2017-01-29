package com.github.rmannibucau.shiro;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardService;
import org.apache.meecrowave.junit.MonoMeecrowave;
import org.apache.shiro.ShiroException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.subject.support.DefaultWebSubjectContext;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.webbeans.web.lifecycle.test.MockHttpSession;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class ShiroTest {
    @Parameterized.Parameters
    public static Case[] parameters() {
        return new Case[]{
                new Case("user", "pwd", test -> test.service.authenticated()),
                new Case("user", "pwd", test -> test.service.user()),
                new Case("user", "pwd", test -> test.service.permTest()),
                new Case("user", "pwd", test -> test.service.roleTest()),
                new Case(null, null, test -> test.service.guest()),
                new Case("user", "pwd", test -> {
                    try {
                        test.service.role2Test();
                        fail();
                    } catch (final ShiroException se) {
                        // ok
                    }
                }),
                new Case("user", "pwd", test -> {
                    try {
                        test.service.perm2Test();
                        fail();
                    } catch (final ShiroException se) {
                        // ok
                    }
                }),
                new Case("user", "pwd", test -> {
                    final Client client = ClientBuilder.newClient();
                    try {
                        assertEquals("ok", client.target("http://localhost:" + MEECROWAVE.getConfiguration().getHttpPort())
                                .path("service/user")
                                .request(TEXT_PLAIN_TYPE)
                                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("user:pwd".getBytes(StandardCharsets.UTF_8)))
                                .get(String.class));
                    } finally {
                        client.close();
                    }
                }),
        };
    }

    @ClassRule
    public static final MonoMeecrowave.Rule MEECROWAVE = new MonoMeecrowave.Rule();

    @Parameterized.Parameter
    public Case testCase;

    @Inject
    private SecurityManager manager;

    @Inject
    private Service service;

    @Before
    public void inject() {
        MEECROWAVE.inject(this);
    }

    @Test
    public void run() {
        // fake a http request since shiro-web needs it by default
        ThreadContext.bind(manager);
        final DefaultWebSubjectContext context = new DefaultWebSubjectContext();
        final Request request = new Request(new Connector()) {
            private final HttpSession session = new MockHttpSession();
            private final Context context = new StandardContext() {
                {
                    setParent(new StandardHost() {{
                        setParent(new StandardEngine() {{
                            setService(new StandardService());
                        }});
                    }});
                    setCookieProcessor(new Rfc6265CookieProcessor());
                }

                @Override
                public String getPath() {
                    return "/app";
                }
            };

            @Override
            public Context getContext() {
                return context;
            }

            @Override
            public HttpSession getSession() {
                return session;
            }
        };
        final org.apache.coyote.Request coyoteRequest = new org.apache.coyote.Request();
        coyoteRequest.requestURI().setString("/test");
        request.setCoyoteRequest(coyoteRequest);
        request.setRemoteHost("localhost");
        request.setRemoteAddr("127.0.0.1");
        context.setServletRequest(request);

        final Response response = new Response();
        response.setCoyoteResponse(new org.apache.coyote.Response());
        context.setServletResponse(response);
        final Subject subject = manager.createSubject(context);
        ThreadContext.bind(subject);
        if (testCase.user != null) {
            // finally login
            subject.login(new UsernamePasswordToken(testCase.user, testCase.password));
        }
        try {
            testCase.test.accept(this);
        } finally {
            if (testCase.user != null) {
                subject.logout();
            }
            ThreadContext.remove();
        }
    }

    private static class Case {
        private final String user;
        private final String password;
        private final Consumer<ShiroTest> test;

        private Case(final String user, final String password, final Consumer<ShiroTest> test) {
            this.user = user;
            this.password = password;
            this.test = test;
        }
    }

    @Path("service")
    @ApplicationScoped
    public static class Service {
        @RequiresRoles("ptest")
        public void role2Test() {
        }

        @RequiresRoles("rtest")
        public void roleTest() {
        }

        @RequiresPermissions("ptest")
        public void permTest() {
        }

        @RequiresPermissions("rtest")
        public void perm2Test() {
        }

        @RequiresGuest
        public void guest() {
        }

        @RequiresUser
        @GET
        @Path("user")
        @Produces(TEXT_PLAIN)
        public String user() {
            return "ok";
        }

        @RequiresAuthentication
        public void authenticated() {
        }
    }
}
