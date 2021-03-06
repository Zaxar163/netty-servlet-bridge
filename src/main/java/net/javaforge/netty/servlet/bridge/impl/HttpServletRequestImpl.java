/*
 * Copyright 2013 by Maxim Kalina
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.javaforge.netty.servlet.bridge.impl;

import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AsciiString;
import net.javaforge.netty.servlet.bridge.ChannelThreadLocal;
import net.javaforge.netty.servlet.bridge.HttpSessionThreadLocal;
import net.javaforge.netty.servlet.bridge.util.Utils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.*;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

public class HttpServletRequestImpl implements HttpServletRequest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    private URIParser uriParser;

    private HttpRequest originalRequest;

    private ServletInputStreamImpl inputStream;

    private BufferedReader reader;

    private QueryStringDecoder queryStringDecoder;

    private Map<String, Object> attributes;

    private Principal userPrincipal;

//    private ServerCookieDecoder cookieDecoder = new ServerCookieDecoder();

    private String characterEncoding;

    public HttpServletRequestImpl(HttpRequest request, FilterChainImpl chain) {
        this.originalRequest = request;

        if (request instanceof FullHttpRequest) {
            this.inputStream = new ServletInputStreamImpl((FullHttpRequest) request);
        } else {
            this.inputStream = new ServletInputStreamImpl(request);
        }
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
        this.queryStringDecoder = new QueryStringDecoder(request.uri());
        this.uriParser = new URIParser(chain);
        this.uriParser.parse(request.uri());
        this.characterEncoding = Utils
                .getCharsetFromContentType(getContentType());

    }

    public HttpRequest getOriginalRequest() {
        return originalRequest;
    }

    @Override
    public String getContextPath() {
        return ServletContextImpl.get().getContextPath();
    }

    @Override
    public Cookie[] getCookies() {
        String cookieString = this.originalRequest.headers().get(COOKIE);
        if (cookieString != null) {
            Set<io.netty.handler.codec.http.cookie.Cookie> cookies = ServerCookieDecoder.STRICT
                    .decode(cookieString);
            if (!cookies.isEmpty()) {
                Cookie[] cookiesArray = new Cookie[cookies.size()];
                int indx = 0;
                for (io.netty.handler.codec.http.cookie.Cookie c : cookies) {
                    Cookie cookie = new Cookie(c.name(), c.value());
                    //cookie.setComment(c.);
                    cookie.setDomain(c.domain());
                    cookie.setMaxAge((int) c.maxAge());
                    cookie.setPath(c.path());
                    cookie.setSecure(c.isSecure());
                    cookie.setVersion(0);
                    cookiesArray[indx] = cookie;
                    indx++;
                }
                return cookiesArray;

            }
        }
        return null;
    }

    @Override
    public long getDateHeader(String name) {
        String longVal = getHeader(name);
        if (longVal == null)
            return -1;

        return Long.parseLong(longVal);
    }

    @Override
    public String getHeader(String name) {
        return this.originalRequest.headers().get(name);
    }

    public String getHeader(AsciiString name) {
        return this.originalRequest.headers().get(name);
    }

    @SuppressWarnings("rawtypes")
	@Override
    public Enumeration getHeaderNames() {
        return Utils.enumeration(this.originalRequest.headers().names());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getHeaders(String name) {
        return Utils.enumeration(this.originalRequest.headers().getAll(name));
    }

    @Override
    public int getIntHeader(String name) {
        return this.originalRequest.headers().getInt(name, -1);
    }

    @Override
    public String getMethod() {
        return this.originalRequest.method().name();
    }

    @Override
    public String getQueryString() {
        return this.uriParser.getQueryString();
    }

    @Override
    public String getRequestURI() {
        return this.uriParser.getRequestUri();
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = this.getScheme();
        int port = this.getServerPort();
        String urlPath = this.getRequestURI();

        // String servletPath = req.getServletPath ();
        // String pathInfo = req.getPathInfo ();

        url.append(scheme); // http, https
        url.append("://");
        url.append(this.getServerName());
        if ((scheme.equals("http") && port != 80)
                || (scheme.equals("https") && port != 443)) {
            url.append(':');
            url.append(this.getServerPort());
        }
        // if (servletPath != null)
        // url.append (servletPath);
        // if (pathInfo != null)
        // url.append (pathInfo);
        url.append(urlPath);
        return url;
    }

    @Override
    public int getContentLength() {
        return (int) HttpUtil.getContentLength(this.originalRequest, -1);
    }

    @Override
    public String getContentType() {
        return this.originalRequest.headers().get(
                HttpHeaderNames.CONTENT_TYPE);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return this.inputStream;
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    @Override
    public String getParameter(String name) {
        String[] values = getParameterValues(name);
        return values != null ? values[0] : null;
    }

    @SuppressWarnings("rawtypes")
	@Override
    public Map getParameterMap() {
        return this.queryStringDecoder.parameters();
    }

    @SuppressWarnings("rawtypes")
	@Override
    public Enumeration getParameterNames() {
        return Utils.enumerationFromKeys(this.queryStringDecoder
                .parameters());
    }

    @Override
    public String[] getParameterValues(String name) {
        List<String> values = this.queryStringDecoder.parameters().get(name);
        if (values == null || values.isEmpty())
            return null;
        return values.toArray(new String[values.size()]);
    }

    @Override
    public String getProtocol() {
        return this.originalRequest.protocolVersion().toString();
    }

    @Override
    public Object getAttribute(String name) {
        if (attributes != null)
            return this.attributes.get(name);

        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Utils.enumerationFromKeys(this.attributes);
    }

    @Override
    public void removeAttribute(String name) {
        if (this.attributes != null)
            this.attributes.remove(name);
    }

    @Override
    public void setAttribute(String name, Object o) {
        if (this.attributes == null)
            this.attributes = new HashMap<String, Object>();

        this.attributes.put(name, o);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return this.reader;
    }

    @Override
    public String getRequestedSessionId() {
        HttpSessionImpl session = HttpSessionThreadLocal.get();
        return session != null ? session.getId() : null;
    }

    @Override
    public HttpSession getSession() {
        HttpSession s = HttpSessionThreadLocal.getOrCreate();
        return s;
    }

    @Override
    public HttpSession getSession(boolean create) {
        HttpSession session = HttpSessionThreadLocal.get();
        if (session == null && create) {
            session = HttpSessionThreadLocal.getOrCreate();
        }
        return session;
    }

    @Override
    public String getPathInfo() {
        return this.uriParser.getPathInfo();
    }

    @Override
    public Locale getLocale() {
        String locale = this.originalRequest.headers().get(HttpHeaderNames.ACCEPT_LANGUAGE, DEFAULT_LOCALE.toString());
        return new Locale(locale);
    }

    @Override
    public String getRemoteAddr() {
        InetSocketAddress addr = (InetSocketAddress) ChannelThreadLocal.get()
                .remoteAddress();
        return addr.getAddress().getHostAddress();
    }

    @Override
    public String getRemoteHost() {
        InetSocketAddress addr = (InetSocketAddress) ChannelThreadLocal.get()
                .remoteAddress();
        return addr.getHostName();
    }

    @Override
    public int getRemotePort() {
        InetSocketAddress addr = (InetSocketAddress) ChannelThreadLocal.get()
                .remoteAddress();
        return addr.getPort();
    }

    @Override
    public String getServerName() {
        InetSocketAddress addr = (InetSocketAddress) ChannelThreadLocal.get()
                .localAddress();
        return addr.getHostName();
    }

    @Override
    public int getServerPort() {
        InetSocketAddress addr = (InetSocketAddress) ChannelThreadLocal.get()
                .localAddress();
        return addr.getPort();
    }

    @Override
    public String getServletPath() {
        String servletPath = this.uriParser.getServletPath();
        if (servletPath.equals("/"))
            return "";

        return servletPath;
    }

    @Override
    public String getScheme() {
        return this.isSecure() ? "https" : "http";
    }

    @Override
    public boolean isSecure() {
        return ChannelThreadLocal.get().pipeline().get(SslHandler.class) != null;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return true;
    }

    @Override
    public String getLocalAddr() {
        InetSocketAddress addr = (InetSocketAddress) ChannelThreadLocal.get()
                .localAddress();
        return addr.getAddress().getHostAddress();
    }

    @Override
    public String getLocalName() {
        return getServerName();
    }

    @Override
    public int getLocalPort() {
        return getServerPort();
    }

    @Override
    public void setCharacterEncoding(String env)
            throws UnsupportedEncodingException {
        this.characterEncoding = env;
    }

	@Override
    public Enumeration<Locale> getLocales() {
        Collection<Locale> locales = Utils
                .parseAcceptLanguageHeader(
                        this.originalRequest.headers().get(
                                HttpHeaderNames.ACCEPT_LANGUAGE));

        if (locales == null || locales.isEmpty()) {
            locales = new ArrayList<Locale>();
            locales.add(Locale.getDefault());
        }
        return Utils.enumeration(locales);
    }

    @Override
    public String getAuthType() {
        return getHeader(WWW_AUTHENTICATE);
    }

    @Override
    public String getPathTranslated() {
        throw new IllegalStateException(
                "Method 'getPathTranslated' not yet implemented!");
    }

    @Override
    public String getRemoteUser() {
        return getHeader(AUTHORIZATION);
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        throw new IllegalStateException(
                "Method 'isRequestedSessionIdFromURL' not yet implemented!");
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        throw new IllegalStateException(
                "Method 'isRequestedSessionIdFromUrl' not yet implemented!");
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        throw new IllegalStateException(
                "Method 'isRequestedSessionIdValid' not yet implemented!");
    }

    @Override
    public boolean isUserInRole(String role) {
        throw new IllegalStateException(
                "Method 'isUserInRole' not yet implemented!");
    }

    @Override
    public String getRealPath(String path) {
        throw new IllegalStateException(
                "Method 'getRealPath' not yet implemented!");
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new IllegalStateException(
                "Method 'getRequestDispatcher' not yet implemented!");
    }

}
