package com.github.rmannibucau.shiro.bean;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.subject.WebSubject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

@ApplicationScoped
public class SubjectProducer {
    @Produces
    // @RequestScoped but why using this which is actually rarely bound so doing a custom impl
    public Subject subject(final SecurityManager manager) {
        return Subject.class.cast(Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{WebSubject.class},
                (proxy, method, args) -> {
                    try {
                        final Subject subject = ThreadContext.getSubject();
                        return method.invoke(subject, args);
                    } catch (final InvocationTargetException ite) {
                        throw ite.getCause();
                    }
                }));
    }
}
