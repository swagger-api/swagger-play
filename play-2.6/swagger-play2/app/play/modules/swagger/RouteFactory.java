package play.modules.swagger;


public class RouteFactory {

    private static RouteWrapper instance;

    public static RouteWrapper getRoute() {
        return instance;
    }

    public static void setRoute(RouteWrapper routes) {
        instance = routes;
    }
}
