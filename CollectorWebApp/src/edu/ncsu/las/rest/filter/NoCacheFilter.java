package edu.ncsu.las.rest.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet Filter implementation class NoCacheFilter
 */
public class NoCacheFilter implements Filter {

    /**
     * Default constructor. 
     */
    public NoCacheFilter() {
    }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            throw new ServletException("This filter can " +
                    " only process HttpServletRequest requests");
        }

        //final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
		

        httpResponse.setHeader("Cache-Control", "no-cache") ;
        httpResponse.setHeader("Expires", "0");
        /*
        // Print out the URL we're filtering
        String name = httpRequest.getRequestURI();
        System.out.println("No Cache Filtering: " + name) ;
        */
        chain.doFilter (request, response);

	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
	}

}
