package org.zahin.db;

import net.dv8tion.jda.api.events.session.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseLoader extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(DatabaseLoader.class);
    private final DatabaseHandler dbHandler;

    public DatabaseLoader(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    // Database
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        dbHandler.loadDatabase();
        log.info("Ready! Loaded Database");
    }

    @Override
    public void onSessionResume(@NotNull SessionResumeEvent event) {
//        dbHandler.loadDatabase();
//        log.info("Loaded Database due to Session Resume");
    }

    @Override
    public void onSessionRecreate(@NotNull SessionRecreateEvent event) {
//        dbHandler.loadDatabase();
//        log.info("Loaded Database due to Session Recreate");
    }

    @Override
    public void onSessionDisconnect(@NotNull SessionDisconnectEvent event) {
//        dbHandler.saveDatabase();
//        log.info("Saved Database due to Session Disconnect");
    }

    @Override
    public void onSessionInvalidate(@NotNull SessionInvalidateEvent event) {
//        dbHandler.saveDatabase();
//        dbHandler.loadDatabase();
//        log.info("Saved and (re)loaded Database due to Session Invalidate");
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
//        dbHandler.saveDatabase();
//        log.info("Saved Database due to Shutdown");
    }
}
