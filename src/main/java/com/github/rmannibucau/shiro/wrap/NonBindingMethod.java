package com.github.rmannibucau.shiro.wrap;

import com.github.rmannibucau.shiro.literal.NonbindingLiteral;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class NonBindingMethod<T extends Annotation> implements AnnotatedMethod<T> {
    private static final AnnotationLiteral<Nonbinding> NONBINDING_ANNOTATION_LITERAL = new NonbindingLiteral();

    private final AnnotatedMethod<T> delegate;
    private final Set<Annotation> annotations;

    NonBindingMethod(final AnnotatedMethod<T> m) {
        delegate = m;
        annotations = new HashSet<>(m.getAnnotations().size() + 1);
        this.annotations.addAll(delegate.getAnnotations());
        this.annotations.add(NONBINDING_ANNOTATION_LITERAL);
    }

    @Override
    public Method getJavaMember() {
        return delegate.getJavaMember();
    }

    @Override
    public List<AnnotatedParameter<T>> getParameters() {
        return delegate.getParameters();
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    @Override
    public AnnotatedType<T> getDeclaringType() {
        return delegate.getDeclaringType();
    }

    @Override
    public Type getBaseType() {
        return delegate.getBaseType();
    }

    @Override
    public Set<Type> getTypeClosure() {
        return delegate.getTypeClosure();
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
        return annotationType == Nonbinding.class ? annotationType.cast(NONBINDING_ANNOTATION_LITERAL) : delegate.getAnnotation(annotationType);
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
        return Nonbinding.class == annotationType || delegate.isAnnotationPresent(annotationType);
    }
}
