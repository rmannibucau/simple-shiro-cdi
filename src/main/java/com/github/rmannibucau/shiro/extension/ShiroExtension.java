package com.github.rmannibucau.shiro.extension;

import com.github.rmannibucau.shiro.bean.SecurityManagerBean;
import com.github.rmannibucau.shiro.interceptor.ShiroInterceptorBridge;
import com.github.rmannibucau.shiro.wrap.NonBindingAnnotation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;
import java.util.stream.Stream;

import static com.github.rmannibucau.shiro.loader.Load.load;

public class ShiroExtension implements Extension {
    private boolean securityManager;
    private SecurityManagerBean bean;
    private SecurityManager manager;

    void makeShiroAnnotationsInterceptorBindings(@Observes final BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager bm) {
        Stream.of(RequiresRoles.class, RequiresPermissions.class, RequiresAuthentication.class, RequiresUser.class, RequiresGuest.class)
                .forEach(type -> beforeBeanDiscovery.addInterceptorBinding(new NonBindingAnnotation<>(bm.createAnnotatedType(type))));
        Stream.of(
                ShiroInterceptorBridge.RequiresRolesInterceptor.class,
                ShiroInterceptorBridge.RequirePermissionsInterceptor.class,
                ShiroInterceptorBridge.RequiresAuthenticationInterceptor.class,
                ShiroInterceptorBridge.RequiresUserInterceptor.class,
                ShiroInterceptorBridge.RequiresGuestInterceptor.class
        ).forEach(t -> beforeBeanDiscovery.addAnnotatedType(bm.createAnnotatedType(t)));
    }

    void hasSecurityManager(@Observes final ProcessBean<SecurityManager> securityManagerProcessBean) {
        securityManager = securityManager || !SecurityManagerBean.class.isInstance(securityManagerProcessBean.getBean());
    }

    void addSecurityManagerIfNeeded(@Observes final AfterBeanDiscovery afterBeanDiscovery) {
        if (securityManager) {
            return;
        }
        newSecurityManager();
        bean = new SecurityManagerBean(manager.getClass());
        afterBeanDiscovery.addBean(bean);
    }

    void initSecurityManagerBean(@Observes final AfterDeploymentValidation afterDeploymentValidation) {
        if (bean != null) {
            bean.initSecurityManagerBean(manager);
        }
    }

    public boolean isSecurityManager() {
        return securityManager;
    }

    private void newSecurityManager() {
        try {
            manager = SecurityManager.class.cast(load("org.apache.shiro.web.mgt.DefaultWebSecurityManager", DefaultSecurityManager.class).newInstance());
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public SecurityManager getSecurityManager() {
        return manager;
    }
}
