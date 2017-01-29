package com.github.rmannibucau.shiro.setup;

import com.github.rmannibucau.shiro.extension.ShiroExtension;
import com.github.rmannibucau.shiro.http.AsyncContextWrapper;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.event.EventBus;
import org.apache.shiro.mgt.RememberMeManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.SubjectDAO;
import org.apache.shiro.mgt.SubjectFactory;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.env.DefaultWebEnvironment;
import org.apache.shiro.web.env.EnvironmentLoader;
import org.apache.shiro.web.env.WebEnvironment;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.DefaultWebSubjectFactory;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.servlet.ShiroFilter;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public class ShiroSetup implements ServletContainerInitializer {
    @Override
    public void onStartup(final Set<Class<?>> set, final ServletContext servletContext) throws ServletException {
        final FilterRegistration.Dynamic filter = servletContext.addFilter("shiro", CdiShiroFilter.class);
        filter.setAsyncSupported(true);
        filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, ofNullable(servletContext.getInitParameter("shiro-cdi.mapping")).orElse("/*"));
    }

    public static class CdiShiroFilter extends EnvironmentLoader implements Filter {
        private ShiroFilter filter;
        private SecurityManager securityManager;
        private ServletContext servletContext;

        @Inject
        private Instance<WebSecurityManager> manager;

        @Inject
        private Instance<Realm> realm;

        @Inject
        private Instance<Authenticator> authenticator;

        @Inject
        private Instance<Authorizer> authorizer;

        @Inject
        private Instance<CacheManager> cacheManager;

        @Inject
        private Instance<EventBus> eventBus;

        @Inject
        private Instance<SubjectDAO> subjectDAO;

        @Inject
        private Instance<SubjectFactory> subjectFactory;

        @Inject
        private Instance<SessionManager> sessionManager;

        @Inject
        private Instance<FilterChainResolver> filterChainResolver;

        @Inject
        private Instance<RememberMeManager> rememberMeManager;

        @Inject
        private Event<WebEnvironment> environmentEvent;

        @Inject
        private ShiroExtension extension;

        @Override
        public void init(final FilterConfig filterConfig) throws ServletException {
            filter = new ShiroFilter();
            servletContext = filterConfig.getServletContext();
            initEnvironment(servletContext);
            filter.init(filterConfig);
        }

        @Override
        public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
            filter.doFilter(new HttpServletRequestWrapper(HttpServletRequest.class.cast(servletRequest)) {
                @Override
                public AsyncContext startAsync() throws IllegalStateException {
                    return propagate(super.startAsync());
                }

                @Override
                public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse) throws IllegalStateException {
                    return propagate(super.startAsync(servletRequest, servletResponse));
                }

                private AsyncContext propagate(final AsyncContext asyncContext) {
                    final Subject subject = ThreadContext.getSubject();
                    final SubjectThreadState state = new SubjectThreadState(subject);
                    asyncContext.addListener(new AsyncListener() {
                        @Override
                        public void onComplete(final AsyncEvent asyncEvent) throws IOException {
                            state.restore();
                        }

                        @Override
                        public void onTimeout(final AsyncEvent asyncEvent) throws IOException {
                            state.restore();
                        }

                        @Override
                        public void onError(final AsyncEvent asyncEvent) throws IOException {
                            state.restore();
                        }

                        @Override
                        public void onStartAsync(final AsyncEvent asyncEvent) throws IOException {
                            asyncEvent.getAsyncContext().addListener(this);
                            state.bind();
                        }
                    });
                    return new AsyncContextWrapper(asyncContext) {
                        @Override
                        public void start(final Runnable runnable) {
                            super.start(subject.associateWith(runnable));
                        }
                    };
                }
            }, servletResponse, filterChain);
        }

        @Override
        public void destroy() {
            filter.destroy();
            destroyEnvironment(servletContext);
        }

        @Override
        protected WebEnvironment createEnvironment(final ServletContext sc) {
            final DefaultWebEnvironment environment = new DefaultWebEnvironment();
            securityManager = configureManager(!extension.isSecurityManager() ? extension.newSecurityManager() : manager.get());
            environment.setSecurityManager(securityManager);
            if (environment.getFilterChainResolver() == null && !filterChainResolver.isUnsatisfied()) {
                environment.setFilterChainResolver(filterChainResolver.get());
            }
            environmentEvent.fire(environment); // to customize it
            return environment;
        }

        // here we use that philosophy: if set it was configured in the security manager producer otherwise use the produced value if there
        private SecurityManager configureManager(final SecurityManager manager) {
            if (!DefaultWebSecurityManager.class.isInstance(manager)) {
                return manager;
            }
            final DefaultWebSecurityManager mgr = DefaultWebSecurityManager.class.cast(manager);
            if ((mgr.getRealms() == null || mgr.getRealms().isEmpty()) && !realm.isUnsatisfied()) {
                mgr.setRealms(stream(realm.spliterator(), false).collect(toList()));
            }
            if (mgr.getAuthenticator() == null && !authenticator.isUnsatisfied()) {
                mgr.setAuthenticator(authenticator.get());
            }
            if (mgr.getAuthorizer() == null && !authorizer.isUnsatisfied()) {
                mgr.setAuthorizer(authorizer.get());
            }
            if (mgr.getCacheManager() == null && !cacheManager.isUnsatisfied()) {
                mgr.setCacheManager(cacheManager.get());
            }
            if (mgr.getEventBus() == null && !eventBus.isUnsatisfied()) {
                mgr.setEventBus(eventBus.get());
            }
            if (mgr.getSubjectDAO() == null && !subjectDAO.isUnsatisfied()) {
                mgr.setSubjectDAO(subjectDAO.get());
            }
            if (mgr.getSubjectFactory() == null && !subjectFactory.isUnsatisfied()) {
                mgr.setSubjectFactory(subjectFactory.get());
            } else if (mgr.getSubjectFactory() == null) {
                mgr.setSubjectFactory(new DefaultWebSubjectFactory());
            }
            if (mgr.getSessionManager() == null && !sessionManager.isUnsatisfied()) {
                mgr.setSessionManager(sessionManager.get());
            }
            if (mgr.getRememberMeManager() == null && !rememberMeManager.isUnsatisfied()) {
                mgr.setRememberMeManager(rememberMeManager.get());
            }
            return manager;
        }
    }
}
