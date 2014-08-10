package com.strider.dataanonymizer;

import java.util.Properties;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;
import org.apache.log4j.Logger;
import static org.apache.log4j.Logger.getLogger;

import static com.strider.dataanonymizer.utils.AppProperties.loadPropertiesFromClassPath;

/**
 * Entry point to Data Discoverer utility. 
 * @author Armenak Grigoryan
 */
public class Discoverer {
    
    private static Logger log = getLogger(Discoverer.class);
    
    public static void main( String[] args ) throws AnonymizerException {

        // Define command line options  
        final Options options = createOptions();
        
        // 
        CommandLine line = null;
        try {
            line = getCommandLine(options, args);
        } catch (ParseException pe) {
            log.error(pe.toString());
        }
        
        
        if (line == null ||  line.hasOption("help")) {
            help(options); 
        } else if (line.hasOption("c")) {
            log.info("Column discovery in process");
            String databasePropertyFile = "db.properties";
            if (line.hasOption("D")) {
                databasePropertyFile = line.getOptionValue("D");
            } 
            Properties dbProperties = null;
            dbProperties = loadPropertiesFromClassPath(databasePropertyFile);
            if (dbProperties == null) {
                throw new AnonymizerException("ERROR: Database property file is not defined.");
            }            

            String columnPropertyFile = "columns.properties";
            if (line.hasOption("C")) {
                columnPropertyFile = line.getOptionValue("C");
            }
            Properties columnProperties = null;
            columnProperties = loadPropertiesFromClassPath(columnPropertyFile);
            if (columnProperties == null) {
                throw new AnonymizerException("ERROR: Column property file is not defined.");
            }                
            
            IDiscoverer discoverer = new ColumnDiscoverer();
            discoverer.discover(databasePropertyFile, columnPropertyFile);
        } else if (line.hasOption("d")) {
            log.info("Data discovery in process");
            
            String databasePropertyFile = "db.properties";
            if (line.hasOption("D")) {
                databasePropertyFile = line.getOptionValue("D");
            } 
            Properties dbProperties = null;
            dbProperties = loadPropertiesFromClassPath(databasePropertyFile);
            if (dbProperties == null) {
                throw new AnonymizerException("ERROR: Database property file is not defined.");
            }            
            
            IDiscoverer discoverer = new DataDiscoverer();
            discoverer.discover(databasePropertyFile);            
        } else {
            help(options);
        }
   }
   
  private static CommandLine getCommandLine(final Options options, final String[] args)
  throws ParseException {
        final CommandLineParser parser = new GnuParser();
        CommandLine line = null;
 
        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            help(options);
        }
 
        return line;
    }    
    
    private static Options createOptions() {
        final Options options = new Options();
        options.addOption( "h", "help", false, "Display help");
        options.addOption( "c", "columns", false, "discover candidate column names for anonymization based on provided patterns" );
        options.addOption( "d", "data", false, "discover candidate column for anonymization based on semantic algorithms" );
        options.addOption( "D", "database-properties", true, "define database property file" );
        options.addOption( "C", "column-properties", true, "define column property file" );        
        return options;
    }
 
    private static void help(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("DataAnonymizer", options);
    }       
}
