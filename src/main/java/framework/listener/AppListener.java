package framework.listener;

import framework.annotations.Controller;
import framework.models.UrlMethod;
import framework.util.ClasseUtilitaire;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class AppListener implements ServletContextListener {

    private static final String CONTROLLER_PACKAGE_CONTEXT_PARAM = "controllerPackage";
    private static final String DEFAULT_CONTROLLER_PACKAGE = "controller";

    public static final String CONTROLLER_CLASSES_ATTR = "frameworkControllerClasses";
    public static final String URL_MAPPING_MAP_ATTR = "frameworkUrlMappingMap";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        String controllerPackage = ctx.getInitParameter(CONTROLLER_PACKAGE_CONTEXT_PARAM);
        if (controllerPackage == null || controllerPackage.isEmpty()) {
            controllerPackage = DEFAULT_CONTROLLER_PACKAGE;
        }

        List<Class<?>> allClasses = ClasseUtilitaire.getClassesInPackage(controllerPackage);
        List<Class<?>> controllerClasses = ClasseUtilitaire.getAnnotatedClasses(allClasses, Controller.class);
        Map<String, Map<UrlMethod, Method>> urlMappingMap = ClasseUtilitaire.getUrlMappingMap(controllerPackage);

        ctx.setAttribute(CONTROLLER_CLASSES_ATTR, controllerClasses);
        ctx.setAttribute(URL_MAPPING_MAP_ATTR, urlMappingMap);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
