package ru.zinin.redis.web.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

/**
 * Date: 30.10.11 14:30
 *
 * @author Alexander V. Zinin (mail@zinin.ru)
 */
public class SessionAttributeListener implements HttpSessionAttributeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionAttributeListener.class);

    @Override
    public void attributeAdded(HttpSessionBindingEvent se) {
        LOGGER.info("Session attribute added. Name: " + se.getName() + ": " + se.getValue().toString());
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent se) {
        LOGGER.info("Session attribute removed. Name: " + se.getName() + "; value: " + se.getValue());
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent se) {
        LOGGER.info("Session attribute replaced. Name: " + se.getName() + ": " + se.getValue().toString());
    }
}
