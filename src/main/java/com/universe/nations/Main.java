package com.universe.nations;

import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.universe.nations.commands.NationsCommand;
import com.universe.nations.files.DatabaseManager;
import com.universe.nations.nation.NationManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.io.File;

public class Main extends JavaPlugin {

    public Main(@NonNullDecl JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        var logger = this.getLogger().getSubLogger("Nations");

        //Création du chemin pour la base de données
        String MAIN_PATH = Constants.UNIVERSE_PATH.resolve("Nations").toAbsolutePath().toString();
        String DATABASE_PATH = MAIN_PATH + File.separator + "nations.db";

        var file = new File(MAIN_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }

        DatabaseManager db = new DatabaseManager(logger, DATABASE_PATH);
        NationManager nationManager = new NationManager(logger, db);

        this.getCommandRegistry().registerCommand(new NationsCommand(nationManager));
    }

    @Override
    protected void shutdown() {
        super.shutdown();
    }
}
