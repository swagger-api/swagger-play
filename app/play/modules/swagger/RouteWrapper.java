package play.modules.swagger;

import play.routes.compiler.Route;

import java.util.Map;


public class RouteWrapper {

    private Map<String, Route> router;

    public RouteWrapper(Map<String, Route> router) {
        this.router = router;
    }

    public Route get(String routeName) {
        return router.get(routeName);
    }

    public boolean exists(String routeName) {
        return router.containsKey(routeName);
    }

    public Map<String, Route> getAll() {
        return router;
    }


}
