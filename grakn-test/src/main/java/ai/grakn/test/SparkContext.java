/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 *
 */

package ai.grakn.test;


import ai.grakn.engine.GraknEngineConfig;
import ai.grakn.engine.util.JWTHandler;
import org.junit.rules.ExternalResource;
import spark.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static ai.grakn.engine.GraknEngineConfig.JWT_SECRET_PROPERTY;
import static ai.grakn.engine.GraknEngineServer.configureSpark;

/**
 * Context that starts spark
 * @author Felix Chapman
 */
public class SparkContext extends ExternalResource {

    private final BiConsumer<Service, GraknEngineConfig> createControllers;
    private final GraknEngineConfig config = GraknEngineConfig.create();

    private Service spark;

    private SparkContext(BiConsumer<Service, GraknEngineConfig> createControllers) {
        this.createControllers = createControllers;
        port(getEphemeralPort());
    }

    public static SparkContext withControllers(BiConsumer<Service, GraknEngineConfig> createControllers) {
        return new SparkContext(createControllers);
    }

    public static SparkContext withControllers(Consumer<Service> createControllers) {
        return new SparkContext((spark, config) -> createControllers.accept(spark));
    }

    public SparkContext port(int port) {
        config.setConfigProperty(GraknEngineConfig.SERVER_PORT_NUMBER, String.valueOf(port));
        return this;
    }

    public int port() {
        return config.getPropertyAsInt(GraknEngineConfig.SERVER_PORT_NUMBER);
    }

    public String uri() {
        return GraknTestEnv.getUri(config);
    }

    public GraknEngineConfig config() {
        return config;
    }

    public void start() {
        spark = Service.ignite();
        configureSpark(spark, config, JWTHandler.create(config.getProperty(JWT_SECRET_PROPERTY)));

        GraknTestEnv.setRestAssuredUri(config);

        createControllers.accept(spark, config);

        spark.awaitInitialization();
    }

    public void stop() {
        spark.stop();

        // Block until server is truly stopped
        // This occurs when there is no longer a port assigned to the Spark server
        boolean running = true;
        while (running) {
            try {
                spark.port();
            } catch(IllegalStateException e){
                running = false;
            }
        }
    }

    @Override
    protected void before() throws Throwable {
        start();
    }

    @Override
    protected void after() {
        stop();
    }

    private static int getEphemeralPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
