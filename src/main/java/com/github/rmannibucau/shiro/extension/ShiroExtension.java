package com.github.rmannibucau.shiro.extension;

import com.github.rmannibucau.shiro.bean.SecurityManagerBean;
import com.github.rmannibucau.shiro.interceptor.ShiroInterceptorBridge;
import com.github.rmannibucau.shiro.wrap.NonBindingAnnotation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;
import java.util.stream.Stream;

public class ShiroExtension implements Extension {
    private boolean securityManager;
    private SecurityManagerBean bean;

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
        bean = new SecurityManagerBean();
        afterBeanDiscovery.addBean(bean);
    }

    public boolean isSecurityManager() {
        return securityManager;
    }

    public SecurityManager newSecurityManager() {
        final WebSecurityManager manager = new DefaultWebSecurityManager();
        bean.initSecurityManagerBean(manager);
        return manager;
    }
}
