package io.osuprotectplus;

import java.io.File;
import java.io.IOException;

import org.slf4j.LoggerFactory;

import com.moandjiezana.toml.Toml;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.javalin.Javalin;
import io.javalin.http.RequestLogger;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.osuprotectplus.handlers.AnalyzeHandler;
import io.osuprotectplus.handlers.BeatmapHandler;
import io.osuprotectplus.handlers.StatsHandler;
import io.osuprotectplus.modules.HomeHandler;
import io.osuprotectplus.modules.JavalinJsonMapper;
import io.osuprotectplus.modules.OpenAPIConfig;

public class App 
{
    private static Logger logger = (Logger) LoggerFactory.getLogger(App.class);
    public static String apiUrl = "";
    public static String runtime = "PRODUCTION";
    public static String version = "1.0.0";

    // Java multiline string
    public static String bigText = """
.########..########...#######..########.########..######..########.......
.##.....##.##.....##.##.....##....##....##.......##....##....##......##..
.##.....##.##.....##.##.....##....##....##.......##..........##......##..
.########..########..##.....##....##....######...##..........##....######
.##........##...##...##.....##....##....##.......##..........##......##..
.##........##....##..##.....##....##....##.......##....##....##......##..
.##........##.....##..#######.....##....########..######.....##..........""";

    public static void main( String[] args ) throws IOException
    {
        long startTime = System.currentTimeMillis();
        System.out.println(bigText);
        logger.info("Starting ProtectPlus AntiCheat v" + version);
        Toml toml = new Toml().read(new File(".config/app.toml"));

        String logLevel = toml.getString("logger.level", "info");
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.toLevel(logLevel.toUpperCase(), Level.INFO));

        int port = toml.getLong("api.port", 4567L).intValue();

        RequestLogger logHandler = (ctx, ms) -> {
            logger.info("[" + ctx.method().toString().toUpperCase() + "] | <" + ctx.host() + ctx.path() + ">" +
                    " | <" + ctx.status() + "> | <" + ms + "ms>");
        };
        runtime = toml.getString("api.runtime", "PRODUCTION").toUpperCase();
       
        if (runtime.equals("PRODUCTION")) {
            apiUrl = toml.getString("api.domain", "http://localhost:" + port);
        } else {
            apiUrl = "http://localhost:" + port;
        }

        
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableRouteOverview("/routes");
            config.requestLogger.http(logHandler);
            config.jsonMapper(new JavalinJsonMapper());
            config.registerPlugin(new SwaggerPlugin());
            config.registerPlugin(new ReDocPlugin());
            config.registerPlugin(new OpenAPIConfig(openApiConf -> {
                logger.debug("Configuring OpenAPI specification");
            }));

        }).start(port);

        app.get("/", new HomeHandler());
        app.post("/api/v1/analyze", new AnalyzeHandler());
        app.get("/api/v1/stats", new StatsHandler());
        app.get("/api/v1/beatmap", new BeatmapHandler());

        logger.info("ProtectPlus AntiCheat is running on " + apiUrl);
        logger.info("Startup completed in " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
