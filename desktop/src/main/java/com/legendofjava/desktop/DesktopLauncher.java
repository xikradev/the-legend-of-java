package com.legendofjava.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.legendofjava.core.LegendOfJavaGame;
import com.legendofjava.core.utils.Constants;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("The Legend of Java");
        
        // Window size scaled up from virtual resolution
        int scale = 3;
        config.setWindowedMode((int)Constants.V_WIDTH * scale, (int)Constants.V_HEIGHT * scale);
        
        config.useVsync(true);
        config.setForegroundFPS(60);
        
        new Lwjgl3Application(new LegendOfJavaGame(), config);
    }
}
