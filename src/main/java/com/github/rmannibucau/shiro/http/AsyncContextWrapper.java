package com.github.rmannibucau.shiro.http;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class AsyncContextWrapper implements AsyncContext {
    private final AsyncContext delegate;

    public AsyncContextWrapper(final AsyncContext asyncContext) {
        delegate = asyncContext;
    }

    @Override
    public ServletRequest getRequest() {
        return delegate.getRequest();
    }

    @Override
    public ServletResponse getResponse() {
        return delegate.getResponse();
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        return delegate.hasOriginalRequestAndResponse();
    }

    @Override
    public void dispatch() {
        delegate.dispatch();
    }

    @Override
    public void dispatch(final String s) {
        delegate.dispatch(s);
    }

    @Override
    public void dispatch(final ServletContext servletContext, final String s) {
        delegate.dispatch(servletContext, s);
    }

    @Override
    public void complete() {
        delegate.complete();
    }

    @Override
    public void start(final Runnable runnable) {
        delegate.start(runnable);
    }

    @Override
    public void addListener(final AsyncListener asyncListener) {
        delegate.addListener(asyncListener);
    }

    @Override
    public void addListener(final AsyncListener asyncListener, final ServletRequest servletRequest, final ServletResponse servletResponse) {
        delegate.addListener(asyncListener, servletRequest, servletResponse);
    }

    @Override
    public <T extends AsyncListener> T createListener(final Class<T> aClass) throws ServletException {
        return delegate.createListener(aClass);
    }

    @Override
    public void setTimeout(final long l) {
        delegate.setTimeout(l);
    }

    @Override
    public long getTimeout() {
        return delegate.getTimeout();
    }
}
