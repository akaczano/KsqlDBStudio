package com.viasat.ksqlstudio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * This class stores the values of the app configuration. Currently this includes
 * the hostname of a previously connected ksqlDB instance and a list of the paths
 * to all of the files the user currently has open. It also contains static methods
 * to save and load settings from a file.
 */
public class AppSettings {

    private String ksqlHost;
    private final List<String> openFiles = new ArrayList<>();
    private double splitPanePos = 0.2;
    private int baseFontSize = 25;
    private int selectedTab = -1;

    public String getKsqlHost() {
        return ksqlHost;
    }

    public void setKsqlHost(String ksqlHost) {
        this.ksqlHost = ksqlHost;
    }

    public List<String> getOpenFiles() {
        return openFiles;
    }

    public double getSplitPanePos() {
        return splitPanePos;
    }

    public void setSplitPanePos(double splitPanePos) {
        this.splitPanePos = splitPanePos;
    }

    public int getBaseFontSize() {
        return baseFontSize;
    }

    public void setBaseFontSize(int baseFontSize) {
        this.baseFontSize = baseFontSize;
    }

    public int getSelectedTab() {
        return selectedTab;
    }

    public void setSelectedTab(int selectedTab) {
        this.selectedTab = selectedTab;
    }

    /**
     * Loads the values of the settings from a file. Currently the settings file
     * is hardcoded to 'settings.txt'
     * @return A settings object containing the loaded values
     */
    public static AppSettings load() {
        AppSettings settings = new AppSettings();
        File settingsStore = new File("settings.txt");
        if (settingsStore.exists()) {
            try {
                List<String> lines = Files.readAllLines(settingsStore.toPath());
                for (String line : lines) {
                    if (line.contains("<hostname>")) {
                        settings.setKsqlHost(readTag(line));
                    }
                    else if (line.contains("<file>")) {
                        settings.getOpenFiles().add(readTag(line));
                    }
                    else if (line.contains("splitPane1")) {
                        settings.splitPanePos = Double.parseDouble(readTag(line));
                    }
                    else if (line.contains("baseFontSize")) {
                        settings.setBaseFontSize(Integer.parseInt(readTag(line)));
                    }
                    else if (line.contains("selectedTab")) {
                        settings.setSelectedTab(Integer.parseInt(readTag(line)));
                    }
                }
            } catch (IOException e) {
            }
        }
        return settings;
    }

    /**
     * Reads the value of a given xml tag
     * @param line Line of text containing xml tag and value
     * @return The contents of the line without the stuff enclosed in angle brackets
     */
    public static String readTag(String line) {
        String result = "";
        boolean read = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '>') {
                read = true;
            }
            else if (read && c == '<') {
                break;
            }
            else if (read) {
                result += c;
            }
        }
        return result;
    }

    /**
     * Saves settings to the hardcoded 'settings.txt' file
     * @param settings The settings object to serialize
     */
    public static void save(AppSettings settings) {
        if (settings.getKsqlHost() != null) {
            File settingsStore = new File("settings.txt");
            try  {
                StringBuilder data = new StringBuilder("");
                if (!settingsStore.exists()) {
                    settingsStore.createNewFile();
                }
                data.append(String.format("<hostname>%s</hostname>\n", settings.getKsqlHost()));
                for (String f : settings.getOpenFiles()) {
                    data.append(String.format("<file>%s</file>\n", f));
                }
                data.append(String.format("<splitPane1>%f</splitPane1>\n", settings.getSplitPanePos()));
                data.append(String.format("<baseFontSize>%d</baseFontSize>\n", settings.getBaseFontSize()));
                data.append(String.format("<selectedTab>%d</selectedTab>\n", settings.getSelectedTab()));

                Files.writeString(settingsStore.toPath(), data.toString(), StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
