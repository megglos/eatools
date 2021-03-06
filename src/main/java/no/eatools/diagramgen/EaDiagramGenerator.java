package no.eatools.diagramgen;

import java.io.File;

import no.eatools.util.EaApplicationProperties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility to be used from the command line to output all diagrams in an EA repo
 * with logical filenames, i.e. the name used in the model, instead of the arbitrary
 * name generated by EA when using its 'HTML Report' function.
 * 
 * @author Per Spilling (per.spilling@objectware.no)
 */
public class EaDiagramGenerator {
    static Log log = LogFactory.getLog(EaDiagramGenerator.class);

    public static void main(String[] args) {
        try {
            final String propertyFilename;
            String outputDir = null;
            if (args.length > 0) {
                propertyFilename = args[0];
                EaApplicationProperties.init(propertyFilename);
                if (args.length > 1) {
                    outputDir = args[1];
                }
            } else {
                EaApplicationProperties.init();
            }

            File modelFile = new File(EaApplicationProperties.EA_PROJECT.value());
            EaRepo eaRepo = new EaRepo(modelFile);
            eaRepo.open();
            if (!EaApplicationProperties.EA_DIAGRAM_TO_GENERATE.value().equals("")) {
                EaDiagram.generateSpecificDiagram(eaRepo, outputDir);
            } else {
                // geenrate all diagrams
                int count = EaDiagram.generateAll(eaRepo, outputDir);
                log.info("Generated " + count + " diagrams");
            }
            eaRepo.close();
        } catch (Exception e) {
            String msg = "An error occurred. This might be caused by an incorrect diagramgen-repo connect string.\n" +
                    "Verify that the connect string in the ea.application.properties file is the same as\n" +
                    "the connect string that you can find in Enterprise Architect via the File->Open Project dialog";
            System.out.println(msg);
        }
    }

}
