/*
 * Copyright 2014-2018, Armenak Grigoryan, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package com.strider.datadefender;

import com.strider.datadefender.requirement.Requirement;
import com.strider.datadefender.requirement.file.Loader;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.TypeConversionException;
import picocli.CommandLine.UnmatchedArgumentException;

import lombok.extern.log4j.Log4j2;

/**
 * Entry point to Data Defender.
 *
 * This class will parse and analyze the parameters and execute appropriate
 * service.
 *
 * @author Zaahid Bateson
 */
@Command(
    name = "datadefender",
    mixinStandardHelpOptions = true,
    version = DataDefender.VERSION,
    description = "Data detection and anonymization tool",
    synopsisSubcommandLabel = "COMMAND",
    subcommands = {
        HelpCommand.class,
        //FileDiscoverer.class,
        Anonymize.class,
        //DataGenerator.class,
        //ColumnDiscoverer.class
    }
)
@Log4j2
public class DataDefender implements Callable<Integer> {

    public static final String VERSION = "2.0.0";

    /**
     * Copied from picocli documentation, presents a shorter "Usage" help when
     * there's an error in the options/arguments.
     *
     * https://picocli.info
     */
    public static class ShortErrorMessageHandler implements IParameterExceptionHandler {

        public int handleParseException(ParameterException ex, String[] args) {
            CommandLine cmd = ex.getCommandLine();
            PrintWriter writer = cmd.getErr();

            writer.println(ex.getMessage());
            UnmatchedArgumentException.printSuggestions(ex, writer);
            writer.print(cmd.getHelp().fullSynopsis()); // since 4.1

            CommandSpec spec = cmd.getCommandSpec();
            writer.printf("Try '%s --help' for more information.%n", spec.qualifiedName());

            return cmd.getExitCodeExceptionMapper() != null
                        ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                        : spec.exitCodeOnInvalidInput();
        }
    }

    public static class RequirementConverter implements CommandLine.ITypeConverter<Requirement> {
        public Requirement convert(String value) throws Exception {
            Loader loader = new Loader();
            try {
                return loader.load(value, DataDefender.VERSION);
            } catch (FileNotFoundException e) {
                log.debug("Error loading requirements file", e);
                throw new TypeConversionException("Unable to load requirements file: " + e.getMessage());
            } catch (Exception e) {
                Throwable exc = e;
                if (StringUtils.isBlank(e.getMessage()) && e.getCause() != null) {
                    exc = e.getCause();
                }
                log.debug("Error loading requirements.", exc);
                throw new TypeConversionException("Unable to load requirements file: " + exc.getMessage());
            }
        }
    }

    @Option(names = "--debug", description = "enable debug logging")
    public void setDebug(boolean debug) {
        System.out.println("DEBUG logging turned on. DEBUG level messages only "
            + "appear in the log file by default.");
        Configurator.setRootLevel(Level.DEBUG);
        log.warn("Private/sensitive data that should be anonymized will be "
            + "logged to configured debug output streams.");
    }

    @Option(names = { "-v", "--verbose" }, description = "enable more verbose output")
    public void setVerbose(boolean verbose) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        LoggerConfig conf = ctx.getLogger("com.strider.datadefender").get();
        Map<String, Appender> appenders = conf.getAppenders();
        List<AppenderRef> refs = conf.getAppenderRefs();
        for (AppenderRef ref : refs) {
            if (ref.getLevel().isMoreSpecificThan(Level.INFO)) {
                conf.removeAppender(ref.getRef());
                conf.addAppender(appenders.get(ref.getRef()), Level.INFO, null);
            }
        }
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String... args) throws Exception {
        CommandLine cmd = new CommandLine(new DataDefender())
            .registerConverter(Requirement.class, new RequirementConverter())
            .setParameterExceptionHandler(new ShortErrorMessageHandler());
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
