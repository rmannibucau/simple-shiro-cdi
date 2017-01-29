package com.github.rmannibucau.shiro.wrap;

import com.github.rmannibucau.shiro.literal.InterceptorBindingLiteral;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.util.AnnotationLiteral;
import javax.interceptor.InterceptorBinding;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class NonBindingAnnotation<T extends Annotation> implements AnnotatedType<T> {
    private static final AnnotationLiteral<InterceptorBinding> INTERCEPTOR_BINDING_ANNOTATION_LITERAL = new InterceptorBindingLiteral();

    private final AnnotatedType<T> delegate;
    private final Set<AnnotatedMethod<? super T>> methods;
    private final Set<Annotation> annotations;

    public NonBindingAnnotation(final AnnotatedType<T> annotatedType) {
        this.delegate = annotatedType;
        this.methods = delegate.getMethods().stream()
                .map(m -> new NonBindingMethod<>((AnnotatedMethod<T>) m))
                .collect(toSet());

        this.annotations = new HashSet<>(delegate.getAnnotations().size() + 1);
        this.annotations.addAll(delegate.getAnnotations());
        this.annotations.add(INTERCEPTOR_BINDING_ANNOTATION_LITERAL);
    }

    @Override
    public Class<T> getJavaClass() {
        return delegate.getJavaClass();
    }

    @Override
    public Set<AnnotatedConstructor<T>> getConstructors() {
        return delegate.getConstructors();
    }

    @Override
    public Set<AnnotatedMethod<? super T>> getMethods() {
        return methods;
    }

    @Override
    public Set<AnnotatedField<? super T>> getFields() {
        return delegate.getFields();
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
        return annotationType == InterceptorBinding.class ? annotationType.cast(INTERCEPTOR_BINDING_ANNOTATION_LITERAL) : delegate.getAnnotation(annotationType);
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
        return annotationType == InterceptorBinding.class || delegate.isAnnotationPresent(annotationType);
    }
}
