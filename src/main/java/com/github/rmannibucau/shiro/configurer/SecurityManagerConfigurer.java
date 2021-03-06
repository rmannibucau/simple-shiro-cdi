package com.github.rmannibucau.shiro.configurer;

import com.github.rmannibucau.shiro.loader.Load;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.event.EventBus;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSubjectFactory;
import org.apache.shiro.mgt.RememberMeManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.SubjectDAO;
import org.apache.shiro.mgt.SubjectFactory;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SessionManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

@ApplicationScoped
public class SecurityManagerConfigurer {
    @Inject
    private Instance<SecurityManager> manager;

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
    private Instance<RememberMeManager> rememberMeManager;

    @Inject
    private Event<SecurityManager> securityManagerEvent;

    // here we use that philosophy: if set it was configured in the security manager producer otherwise use the produced value if there
    public SecurityManager configureManager(final SecurityManager manager) {
        if (!DefaultSecurityManager.class.isInstance(manager)) {
            securityManagerEvent.fire(manager); // to customize it through an observer
            return manager;
        }
        final DefaultSecurityManager mgr = DefaultSecurityManager.class.cast(manager);
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
            try {
                mgr.setSubjectFactory(SubjectFactory.class.cast(Load.load("org.apache.shiro.web.mgt.DefaultWebSubjectFactory", DefaultSubjectFactory.class).newInstance()));
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        if (mgr.getSessionManager() == null && !sessionManager.isUnsatisfied()) {
            mgr.setSessionManager(sessionManager.get());
        }
        if (mgr.getRememberMeManager() == null && !rememberMeManager.isUnsatisfied()) {
            mgr.setRememberMeManager(rememberMeManager.get());
        }
        securityManagerEvent.fire(manager); // to customize it through an observer
        return manager;
    }
}
