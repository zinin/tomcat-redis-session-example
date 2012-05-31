package ru.zinin.redis.console;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.*;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.mbeans.GlobalResourcesLifecycleListener;
import org.apache.catalina.realm.JAASRealm;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zinin.redis.session.RedisManager;
import ru.zinin.redis.web.servlet.MainServlet;

import java.io.File;
import java.io.IOException;

/**
 * Date: 30.10.11 14:07
 *
 * @author Alexander V. Zinin (mail@zinin.ru)
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final int PORT = 8080;
    private static final String PROTOCOL = "org.apache.coyote.http11.Http11Protocol";
    private static final int SESSION_TIMEOUT = 1;

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String WORK_DIR = "tomcat-redis-session-test";

    private File workDir;
    private Tomcat tomcat;

    private final Object stopObject = new Object();

    private File createWorkDir() {
        workDir = new File(TEMP_DIR, WORK_DIR);
        if (workDir.exists()) {
            if (!workDir.isDirectory()) {
                throw new IllegalStateException("Temp directory " + workDir.getAbsolutePath() + " already exist and it is file.");
            }

            try {
                FileUtils.deleteDirectory(workDir);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        //noinspection ResultOfMethodCallIgnored
        workDir.mkdirs();

        return workDir;
    }

    private void setupSystemProps() {
        LOGGER.info(String.format("Setting %s and %s to %s", Globals.CATALINA_HOME_PROP, Globals.CATALINA_BASE_PROP, workDir.getAbsolutePath()));

        System.setProperty(Globals.CATALINA_HOME_PROP, workDir.getAbsolutePath());
        System.setProperty(Globals.CATALINA_BASE_PROP, workDir.getAbsolutePath());
    }

    private void initTomcat() {
        LOGGER.info("Initializing tomcat...");

        tomcat = new Tomcat();

        tomcat.getServer().addLifecycleListener(new AprLifecycleListener());
        tomcat.getServer().addLifecycleListener(new JasperListener());
        tomcat.getServer().addLifecycleListener(new JreMemoryLeakPreventionListener());
        tomcat.getServer().addLifecycleListener(new GlobalResourcesLifecycleListener());
        tomcat.getServer().addLifecycleListener(new ThreadLocalLeakPreventionListener());

        int port = PORT;
        String portStr = System.getProperty("port");
        if (portStr != null) {
            port = Integer.parseInt(portStr);
        }

        Connector connector = new Connector(PROTOCOL);
        connector.setPort(port);
        connector.setURIEncoding("utf-8");
        connector.setEnableLookups(false);

        tomcat.getService().addConnector(connector);
        tomcat.setConnector(connector);

        tomcat.setBaseDir(workDir.getAbsolutePath());
        tomcat.getHost().setAppBase(new File(workDir, "webapps").getAbsolutePath());
        tomcat.getHost().setAutoDeploy(false);

        ContextResource contextResource = new ContextResource();
        contextResource.setName("JedisPool");
        contextResource.setAuth("Container");
        contextResource.setType("redis.clients.jedis.JedisPool");
        contextResource.setProperty("factory", "ru.zinin.redis.factory.JedisPoolFactory");
        contextResource.setProperty("max-active", "10");
        contextResource.setProperty("timeout", "30");

        tomcat.getServer().getGlobalNamingResources().addResource(contextResource);

        tomcat.getEngine().setRealm(new JAASRealm());
        tomcat.enableNaming();
    }

    private void initContext(Context ctx) {
        Wrapper servlet = Tomcat.addServlet(ctx, "mainServlet", new MainServlet());
        servlet.setLoadOnStartup(1);

        ctx.addServletMapping("/", "mainServlet");

        ctx.setSessionTimeout(SESSION_TIMEOUT);

        ctx.addApplicationListener("ru.zinin.redis.web.listener.SessionListener");
        ctx.addApplicationListener("ru.zinin.redis.web.listener.SessionAttributeListener");

        ctx.setSessionCookieName("PHPSESSID");

        RedisManager redisManager = new RedisManager();
//        redisManager.setDisableListeners(true);
        redisManager.setDbIndex(1);
        ctx.setManager(redisManager);
    }

    private void deployApplication() {
        LOGGER.info("Deploying application...");

        Context ctx = new StandardContext();
        ctx.setName("/");
        ctx.setPath("/");
        ctx.setDocBase(workDir.getPath());

        initContext(ctx);

        ContextConfig ctxCfg = new ContextConfig();
        ctx.addLifecycleListener(ctxCfg);

        tomcat.getHost().addChild(ctx);
    }

    private void startTomcat() {
        LOGGER.info("Starting tomcat...");

        try {
            tomcat.start();
        } catch (LifecycleException e) {
            throw new IllegalStateException("Error starting tomcat", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOGGER.info("Stopping tomcat...");

                try {
                    tomcat.stop();
                } catch (LifecycleException e) {
                    LOGGER.error(e.getMessage(), e);
                }

                synchronized (stopObject) {
                    stopObject.notify();
                }
            }
        });

        synchronized (stopObject) {
            try {
                stopObject.wait();
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        LOGGER.info("Tomcat stopped.");
    }

    private void start() {
        createWorkDir();
        setupSystemProps();
        initTomcat();
        deployApplication();
        startTomcat();
    }

    public static void main(String[] args) {
        Main main = new Main();

        main.start();
    }
}