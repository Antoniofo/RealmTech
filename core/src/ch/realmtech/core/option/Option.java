package ch.realmtech.core.option;

import ch.realmtech.server.datactrl.DataCtrl;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Option {
    private final static Logger logger = LoggerFactory.getLogger(Option.class);
    public final AtomicInteger keyMoveUp = new AtomicInteger();
    public final AtomicInteger keyMoveLeft = new AtomicInteger();
    public final AtomicInteger keyMoveRight = new AtomicInteger();
    public final AtomicInteger keyMoveDown = new AtomicInteger();
    public final AtomicInteger openInventory = new AtomicInteger();
    public final AtomicInteger keyDropItem = new AtomicInteger();
    public final BooleanRun fullScreen = new BooleanRun(bool -> {
        if (bool) Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        else Gdx.graphics.setWindowedMode(DataCtrl.SCREEN_WIDTH, DataCtrl.SCREEN_HEIGHT);
    });
    public final IntegerRun fps = new IntegerRun(fps -> Gdx.graphics.setForegroundFPS(fps));
    public final BooleanRun vsync = new BooleanRun(bool -> Gdx.graphics.setVSync(bool));
    public final AtomicBoolean inventoryBlur = new AtomicBoolean();
    public final AtomicInteger sound = new AtomicInteger();
    private String authServerBaseUrl;
    private String createAccessTokenUrn;
    private String verifyLoginUrn;
    private final Properties properties;

    private Option(Properties propertiesFile) {
        this.properties = propertiesFile;
    }

    public static Option getOptionFileAndLoadOrCreate() throws IOException {
        try (InputStream inputStream = new FileInputStream(DataCtrl.getOptionFile())) {
            Properties propertiesFile = new Properties();
            try {
                propertiesFile.load(inputStream);
                return Option.loadOptionFromFile(propertiesFile);
            } catch (IllegalArgumentException e) {
                return Option.createDefaultOption(propertiesFile);
            }
        }
    }

    private static Option createDefaultOption(Properties propertiesFile) {
        Option option = new Option(propertiesFile);
        option.setDefaultOption();
        return option;
    }

    public void setDefaultOption() {
        keyMoveUp.set(Input.Keys.W);
        keyMoveLeft.set(Input.Keys.A);
        keyMoveRight.set(Input.Keys.D);
        keyMoveDown.set(Input.Keys.S);
        openInventory.set(Input.Keys.E);
        keyDropItem.set(Input.Keys.Q);
        fullScreen.set(false);
        fps.set(60);
        vsync.set(true);
        inventoryBlur.set(true);
        sound.set(100);
        authServerBaseUrl = "https://chattonf01.emf-informatique.ch/RealmTech/auth";
        createAccessTokenUrn = "createAccessToken.php";
        verifyLoginUrn = "verifyPassword.php";
    }

    private static Option loadOptionFromFile(Properties propertiesFile) throws IllegalArgumentException {
        if (propertiesFile.isEmpty()) {
            throw new IllegalArgumentException("Configuration file missing");
        }
        return loadFromPropertiesFile(propertiesFile);
    }

    public void saveOption() {
        try {
            properties.put("keyMoveForward", keyMoveUp.toString());
            properties.put("keyMoveLeft", keyMoveLeft.toString());
            properties.put("keyMoveRight", keyMoveRight.toString());
            properties.put("keyMoveBack", keyMoveDown.toString());
            properties.put("openInventory", openInventory.toString());
            properties.put("keyDropItem", keyDropItem.toString());
            properties.put("fullScreen", fullScreen.toString());
            properties.put("fps", fps.toString());
            properties.put("sound", sound.toString());
            properties.put("vsync", vsync.toString());
            properties.put("inventoryBlur", inventoryBlur.toString());
            properties.put("authServerBaseUrl", authServerBaseUrl);
            properties.put("createAccessTokenUrn", createAccessTokenUrn);
            properties.put("verifyLoginUrn", verifyLoginUrn);
            try (OutputStream outputStream = new FileOutputStream(DataCtrl.getOptionFile())) {
                properties.store(outputStream, "RealmTech option file");
                outputStream.flush();
            }
        } catch (IOException e) {
            logger.error("Option file can not be saved. {}", e.getMessage());
        }
    }

    private static Option loadFromPropertiesFile(Properties propertiesFile) {
        Option option = new Option(propertiesFile);
        option.keyMoveUp.set(Integer.parseInt(propertiesFile.getProperty("keyMoveForward")));
        option.keyMoveLeft.set(Integer.parseInt(propertiesFile.getProperty("keyMoveLeft")));
        option.keyMoveRight.set(Integer.parseInt(propertiesFile.getProperty("keyMoveRight")));
        option.keyMoveDown.set(Integer.parseInt(propertiesFile.getProperty("keyMoveBack")));
        option.openInventory.set(Integer.parseInt(propertiesFile.getProperty("openInventory")));
        option.keyDropItem.set(Integer.parseInt(propertiesFile.getProperty("keyDropItem")));
        option.fullScreen.set(Boolean.parseBoolean(propertiesFile.getProperty("fullScreen")));
        option.fps.set(Integer.parseInt(propertiesFile.getProperty("fps")));
        option.sound.set(Integer.parseInt(propertiesFile.getProperty("sound")));
        option.vsync.set(Boolean.parseBoolean(propertiesFile.getProperty("vsync")));
        option.inventoryBlur.set(Boolean.parseBoolean(propertiesFile.getProperty("inventoryBlur")));
        option.authServerBaseUrl = propertiesFile.getProperty("authServerBaseUrl");
        option.createAccessTokenUrn = propertiesFile.getProperty("createAccessTokenUrn");
        option.verifyLoginUrn = propertiesFile.getProperty("verifyLoginUrn");
        return option;
    }

    public String getAuthServerBaseUrl() {
        return authServerBaseUrl;
    }

    public String getCreateAccessTokenUrn() {
        return createAccessTokenUrn;
    }

    public String getVerifyLoginUrn() {
        return verifyLoginUrn;
    }
}
