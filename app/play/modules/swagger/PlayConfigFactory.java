package play.modules.swagger;


public class PlayConfigFactory {

    private static PlaySwaggerConfig instance;

    public static void setConfig(PlaySwaggerConfig routes) {
        instance = routes;
    }

    public static PlaySwaggerConfig getConfig() {
        return instance;
    }
}
