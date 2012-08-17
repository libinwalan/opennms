package org.opennms.container.web.bridge.internal;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.opennms.container.web.felix.base.internal.HttpServiceController;
import org.opennms.container.web.felix.base.internal.listener.ServletRequestAttributeListenerManager;

public class DispatcherFilter implements Filter {

    private HttpServiceController m_controller;
    private FilterConfig m_filterConfig;

    public DispatcherFilter(final HttpServiceController controller) {
        m_controller = controller;
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        m_filterConfig = filterConfig;
        m_controller.register(filterConfig.getServletContext());
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        m_filterConfig.getServletContext().log("DispatchFilter: doFilter() called.");
        final ServletRequestEvent sre = new ServletRequestEvent(m_filterConfig.getServletContext(), request);
        m_controller.getRequestListener().requestInitialized(sre);
        try
        {
            if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
                m_filterConfig.getServletContext().log("DispatchFilter: request is an HttpServletRequest, dispatching.");
                final HttpServletRequest req = new AttributeEventRequest(m_filterConfig.getServletContext(), m_controller.getRequestAttributeListener(), (HttpServletRequest)request);
                m_controller.getDispatcher().dispatch(req, (HttpServletResponse)response, chain);
            } else {
                m_filterConfig.getServletContext().log("Request or response were not 'HTTP', falling through to default filter chain.");
                chain.doFilter(request, response);
            }
        } catch (final Exception e) {
            m_filterConfig.getServletContext().log("Something went horribly awry.", e);
        }
        finally
        {
            m_controller.getRequestListener().requestDestroyed(sre);
        }
        m_filterConfig.getServletContext().log("DispatchFilter: doFilter() complete.");
    }

    @Override
    public void destroy() {
        m_controller.unregister();
    }

    private static class AttributeEventRequest extends HttpServletRequestWrapper
    {

        private final ServletContext servletContext;
        private final ServletRequestAttributeListenerManager requestAttributeListener;

        public AttributeEventRequest(ServletContext servletContext, ServletRequestAttributeListenerManager requestAttributeListener, HttpServletRequest request) {
            super(request);
            servletContext.log("AttributeEventRequest: request = " + request);
            this.servletContext = servletContext;
            this.requestAttributeListener = requestAttributeListener;
        }

        public void setAttribute(String name, Object value)
        {
            if (value == null)
            {
                this.removeAttribute(name);
            }
            else if (name != null)
            {
                Object oldValue = this.getAttribute(name);
                super.setAttribute(name, value);

                if (oldValue == null)
                {
                    requestAttributeListener.attributeAdded(new ServletRequestAttributeEvent(servletContext, this,
                        name, value));
                }
                else
                {
                    requestAttributeListener.attributeReplaced(new ServletRequestAttributeEvent(servletContext, this,
                        name, oldValue));
                }
            }
        }

        public void removeAttribute(String name)
        {
            Object oldValue = this.getAttribute(name);
            super.removeAttribute(name);

            if (oldValue != null)
            {
                requestAttributeListener.attributeRemoved(new ServletRequestAttributeEvent(servletContext, this, name,
                    oldValue));
            }
        }
    }
}
