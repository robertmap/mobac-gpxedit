/*******************************************************************************
 * Copyright (c) MOBAC developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.program;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import mobac.StartMOBAC;
import mobac.utilities.GUIExceptionHandler;
import mobac.utilities.OSUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

public class Logging {

    public static final Logger LOG = LoggerFactory.getLogger("MAC");

    public static void configureLogging() {
        // We test for the configuration file, if it exists we use it, otherwise
        // we keep  simple logging to the console (configured by logback.xml in src/main/resources)
        if (!loadLog4JConfigXml()) {
            configureDefaultFileLogging();
            LOG.info("logback.xml not found - enabling default error log to console");
        }
    }

    public static boolean loadLog4JConfigXml() {
        if (loadLogbackConfigXmlFromDir(DirectoryManager.mobacUserAppDataDir)) {
            return true;
        }
        if (loadLogbackConfigXmlFromDir(DirectoryManager.userSettingsDir)) {
            return true;
        }
        if (loadLogbackConfigXmlFromDir(DirectoryManager.currentDir)) {
            return true;
        }
        if (loadLogbackConfigXmlFromDir(DirectoryManager.programDir)) {
            return true;
        }
        return false;
    }

    public static void configureDefaultFileLogging() {
        Path errorLog = DirectoryManager.mobacUserAppDataDir.toPath().resolve("Mobile Atlas Creator.log");
        createFileLogger(errorLog, Level.INFO);
    }

    public static boolean loadLogbackConfigXmlFromDir(File dir) {
        Path logbackXml = dir.toPath().resolve("logback.xml");
        if (!Files.isRegularFile(logbackXml)) {
            return false;
        }
        return loadLogbackXml(logbackXml);
    }

    public static boolean loadLogbackXml(Path logbackXml) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        try (InputStream configStream = Files.newInputStream(logbackXml)) {
            configurator.setContext(loggerContext);
            loggerContext.reset();
            configurator.doConfigure(configStream);
            StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
            Logger logger = LoggerFactory.getLogger("LogSystem");
            logger.info("Logging configured by \"{}\"", logbackXml.toAbsolutePath());
            return true;
        } catch (IOException | JoranException e) {
            LOG.error("Failed to load logback configuration file {}", logbackXml.toAbsolutePath(), e);
        }
        return false;
    }

    private static FileAppender createFileLogger(Path logFile, Level levelFilter) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();

        patternLayoutEncoder.setPattern("%date %-5level %logger %msg%n");
        patternLayoutEncoder.setContext(loggerContext);
        patternLayoutEncoder.start();
        FileAppender<ILoggingEvent> fileAppender = new FileAppender();
        fileAppender.setFile(logFile.toAbsolutePath().normalize().toString());
        fileAppender.setEncoder(patternLayoutEncoder);
        fileAppender.setContext(loggerContext);
        fileAppender.setAppend(false);

        if (levelFilter != null) {
            ThresholdFilter filter = new ThresholdFilter();
            filter.setContext(loggerContext);
            filter.setLevel(levelFilter.toString());
            filter.start();
            fileAppender.addFilter(filter);
        }
        fileAppender.start();

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logger.addAppender(fileAppender);
        return fileAppender;
    }

    /**
     * returns the first configured {@link FileAppender} or <code>null</code>.
     *
     * @return
     */
    public static String getLogFile() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        Iterator<Appender<ILoggingEvent>> it = logger.iteratorForAppenders();
        while (it.hasNext()) {
            Appender<ILoggingEvent> appender = it.next();
            if (appender instanceof FileAppender) {
                FileAppender fileAppender = (FileAppender) appender;
                return fileAppender.getFile();
            }
        }
        return null;
    }

    public static void logSystemInfo() {
        Logger log = LoggerFactory.getLogger("SysInfo");
        if (log.isInfoEnabled()) {
            String n = System.getProperty("line.separator");
            log.info("Version: {}", ProgramInfo.getCompleteTitle());
            log.info("Platform: {} ({})", GUIExceptionHandler.prop("os.name"), GUIExceptionHandler.prop("os.version"));
            log.info("Java VM: {} ({})", GUIExceptionHandler.prop("java.vm.name"), GUIExceptionHandler.prop("java.runtime.version"));
            log.info("Available processors: {}", Runtime.getRuntime().availableProcessors());
            log.info("Directories:" /**/
                    + n + "currentDir: \t\t" + DirectoryManager.currentDir /**/
                    + n + "programDir: \t\t" + DirectoryManager.programDir /**/
                    + n + "mapSourcesDir: \t\t" + DirectoryManager.mapSourcesDir /**/
                    + n + "tempDir:     \t\t" + DirectoryManager.tempDir /**/
                    + n + "userHomeDir: \t\t" + DirectoryManager.userHomeDir /**/
                    + n + "userSettingsDir: \t" + DirectoryManager.userSettingsDir /**/
                    + n + "atlasProfilesDir: \t" + DirectoryManager.atlasProfilesDir /**/
                    + n + "mobacUserAppDataDir: \t" + DirectoryManager.mobacUserAppDataDir /**/
            );
            log.info("System console available: {}", (System.console() != null));
            log.info("Startup arguments (count={}):", StartMOBAC.ARGS.length);
            for (int i = 0; i < StartMOBAC.ARGS.length; i++)
                log.info("\t{}:{}", i, StartMOBAC.ARGS[i]);
        }
        if (log.isDebugEnabled()) {
            log.debug("Detected operating system: {} ({})", OSUtilities.detectOs(), System.getProperty("os.name"));
            boolean desktopSupport = Desktop.isDesktopSupported();
            log.debug("Desktop support: {}", desktopSupport);
            if (desktopSupport) {
                Desktop d = Desktop.getDesktop();
                for (Action a : Action.values()) {
                    log.debug("Desktop action {}  supported: {}", a, d.isSupported(a));
                }
            }
        }
        if (log.isTraceEnabled()) {
            Properties props = System.getProperties();
            StringWriter sw = new StringWriter(8192);
            sw.write("System properties:\n");
            TreeMap<Object, Object> sortedProps = new TreeMap<>(props);
            for (Entry<Object, Object> entry : sortedProps.entrySet()) {
                sw.write(entry.getKey() + " = " + entry.getValue() + "\n");
            }
            log.trace(sw.toString());
        }
    }
}
