package framework.servlet;

import framework.annotations.Controller;
import framework.models.ModelAndView;
import framework.models.UrlMethod;
import framework.util.ClasseUtilitaire;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class FrontControllerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String CONTROLLER_PACKAGE_INIT_PARAM = "controllerPackage";
    private static final String DEFAULT_CONTROLLER_PACKAGE = "controller";
    private static final String PREFIX_INIT_PARAM = "prefix";
    private static final String SUFFIX_INIT_PARAM = "suffix";
    private static final String DEFAULT_PREFIX = "/WEB-INF/views/";
    private static final String DEFAULT_SUFFIX = ".jsp";

    private Map<String, Map<UrlMethod, Method>> urlMappingMap;
    private List<Class<?>> controllerClasses;
    private String prefix;
    private String suffix;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String controllerPackage = config.getInitParameter(CONTROLLER_PACKAGE_INIT_PARAM);
        if (controllerPackage == null || controllerPackage.isEmpty()) {
            controllerPackage = DEFAULT_CONTROLLER_PACKAGE;
        }

        List<Class<?>> allClasses = ClasseUtilitaire.getClassesInPackage(controllerPackage);
        this.controllerClasses = ClasseUtilitaire.getAnnotatedClasses(allClasses, Controller.class);
        this.urlMappingMap = ClasseUtilitaire.getUrlMappingMap(controllerPackage);

        this.prefix = config.getInitParameter(PREFIX_INIT_PARAM);
        if (this.prefix == null) this.prefix = DEFAULT_PREFIX;

        this.suffix = config.getInitParameter(SUFFIX_INIT_PARAM);
        if (this.suffix == null) this.suffix = DEFAULT_SUFFIX;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<html><body>");

        out.println("<h2>Controleurs trouvés dans le package</h2>");
        out.println("<ul>");
        for (Class<?> clazz : controllerClasses) {
            Controller ann = clazz.getAnnotation(Controller.class);
            out.println("<li>" + clazz.getSimpleName() + " → @Controller(\"" + ann.value() + "\")</li>");
        }
        out.println("</ul>");

        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String pathInfo = requestURI.substring(contextPath.length());
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }

        out.println("<p>Requested URI: " + requestURI + "</p>");
        out.println("<p>Path info after servlet: " + pathInfo + "</p>");
        out.println("<p>HTTP method: " + req.getMethod() + "</p>");

        UrlMethod key = new UrlMethod(pathInfo, req.getMethod());
        Method method = null;
        String controllerName = null;
        boolean found = false;
        for (Map.Entry<String, Map<UrlMethod, Method>> entry : urlMappingMap.entrySet()) {
            String ctrlName = entry.getKey();
            Map<UrlMethod, Method> methodMap = entry.getValue();
            if (methodMap.containsKey(key)) {
                method = methodMap.get(key);
                controllerName = ctrlName;
                found = true;
                break;
            }
        }

        if (found && method != null) {
            try {
                Class<?> controllerClass = method.getDeclaringClass();
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                Object result = method.invoke(controllerInstance);

                if (result instanceof ModelAndView) {
                    ModelAndView mv = (ModelAndView) result;
                    Map<String, Object> modelData = mv.getData();
                    for (Map.Entry<String, Object> entry : modelData.entrySet()) {
                        req.setAttribute(entry.getKey(), entry.getValue());
                    }
                    String viewPath = prefix + mv.getView() + suffix;
                    req.getRequestDispatcher(viewPath).forward(req, resp);
                    return;
                }

                out.println("<h3>Controller->Method: " + controllerName + "->" + method.getName() + "</h3>");
                out.println("<p>Result: " + result + "</p>");
            } catch (Exception e) {
                out.println("<p>Error invoking controller method: " + e.getMessage() + "</p>");
                e.printStackTrace(out);
            }
        } else {
            out.println("<h3>No mapping found for " + req.getMethod() + " " + pathInfo + "</h3>");
            out.println("<p>Available mappings:</p>");
            out.println("<ul>");
            for (Map.Entry<String, Map<UrlMethod, Method>> entry : urlMappingMap.entrySet()) {
                String ctrl = entry.getKey();
                for (Map.Entry<UrlMethod, Method> mEntry : entry.getValue().entrySet()) {
                    out.println("<li>" + ctrl + " -> " + mEntry.getKey() + " (method: " + mEntry.getValue().getName() + ")</li>");
                }
            }
            out.println("</ul>");
        }

        out.println("</body></html>");
    }
}
