package ru.zinin.redis.web.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Date: 30.10.11 14:20
 *
 * @author Alexander V. Zinin (mail@zinin.ru)
 */
public class MainServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainServlet.class);

    private int i = 0;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGGER.info("------------ begin servet --------------");

        HttpSession session = req.getSession();

        LOGGER.info("---------- get session --------------");

        resp.setContentType("text/plain");

        if (session.isNew()) {
            session.setAttribute("new", "true");

            resp.getWriter().println("new session");
            resp.getWriter().println("session-id: " + session.getId());
            resp.getWriter().println("session-class: " + session.getClass().getCanonicalName());
        } else {
            session.removeAttribute("new");

            LOGGER.info("i: " + i);
            session.setAttribute("i", i);
            session.setAttribute("obj", new TestObject(System.currentTimeMillis()));

            i++;

            resp.getWriter().println("old session");
            resp.getWriter().println("session-id: " + session.getId());
            resp.getWriter().println("session-class: " + session.getClass().getCanonicalName());
            resp.getWriter().println("creation-time: " + new Date(session.getCreationTime()));
            resp.getWriter().println("last-access-time: " + new Date(session.getLastAccessedTime()));

            List<String> attributes = Collections.list(session.getAttributeNames());

            resp.getWriter().println("attributes: " + attributes);

            resp.getWriter().print("obj: " + session.getAttribute("obj"));
        }

        LOGGER.info("------------ end servet --------------");
    }
}
